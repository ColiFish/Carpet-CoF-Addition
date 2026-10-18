// SPDX-License-Identifier: MIT
// Copyright (c) 2026 CoF contributors

package carpet.cof.mixin;

import carpet.cof.CofSettings;
import carpet.cof.ConvertingState;
import carpet.cof.compat.Compat;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 「傻子转化」——<b>重置器语义</b>（自 1.1.2 起）：
 *
 * <ul>
 *   <li><b>普通 → 傻子</b>（虚弱 + 毒马铃薯，等 3~5 分钟）：立刻放弃对工作方块的认领，
 *       交易清空（傻子本来就没有交易），群系外观不动。</li>
 *   <li><b>傻子 → 无职业村民</b>（再生 + 金苹果，等 3~5 分钟）：<b>洗掉职业</b>、
 *       清空交易历史、经验归零、职业等级回到 1，只保留群系风格外观。</li>
 * </ul>
 *
 * <p>因此不存在"还原"：转回来得到的是一个全新的无职业村民，可以重新认领任意工作方块、
 * 从头开始交易。是否绑定过职业、是否交易过，都不影响这两步。</p>
 *
 * <p>倒计时存在 {@link ConvertingState} 字段里，由 {@code VillagerConvertingSaveMixin}
 * （按版本分四份）写进实体 NBT，所以和原版治愈一样跨重启继续。</p>
 */
@Mixin(Villager.class)
public abstract class VillagerNitwitConversionMixin implements ConvertingState {

    /** 「傻子」与「无职业」的注册表 id（1.21.5 起职业常量变成 Holder，统一比 id）。 */
    private static final Identifier NITWIT_ID = Identifier.withDefaultNamespace("nitwit");
    private static final Identifier NONE_ID = Identifier.withDefaultNamespace("none");

    /** 原版治愈僵尸村民用的时长：3600 ~ 6000 tick（3~5 分钟）。 */
    private static final int COF_CONVERSION_MIN = 3600;
    private static final int COF_CONVERSION_RANDOM = 2401;

    /** 剩余 tick（0 = 没有进行中的转化）。 */
    @Unique
    private int cof$convertingTicks;

    @Override
    public int cof$getConvertingTicks() {
        return this.cof$convertingTicks;
    }

    @Override
    public void cof$setConvertingTicks(int ticks) {
        this.cof$convertingTicks = Math.max(0, ticks);
    }

    @Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
    private void cof$nitwitConversion(Player player, InteractionHand hand,
                                      CallbackInfoReturnable<InteractionResult> cir) {
        if (!CofSettings.nitwitConversion) {
            return;
        }
        Villager self = (Villager) (Object) this;
        if (self.isBaby()) {
            return;                                  // 幼年村民：放行原版行为
        }
        if (this.cof$convertingTicks > 0) {
            cir.setReturnValue(InteractionResult.CONSUME);   // 转化中：不再接受任何交互
            return;
        }
        ItemStack stack = player.getItemInHand(hand);
        boolean isNitwit = NITWIT_ID.equals(Compat.professionId(self.getVillagerData()));

        if (!isNitwit && stack.is(Items.POISONOUS_POTATO) && self.hasEffect(MobEffects.WEAKNESS)) {
            stack.consume(1, player);
            if (!self.level().isClientSide()) {
                cof$beginConversion(self);
            }
            cir.setReturnValue(InteractionResult.CONSUME);
        } else if (isNitwit && stack.is(Items.GOLDEN_APPLE) && self.hasEffect(MobEffects.REGENERATION)) {
            stack.consume(1, player);
            if (!self.level().isClientSide()) {
                cof$beginConversion(self);
            }
            cir.setReturnValue(InteractionResult.CONSUME);
        }
    }

    /** 开始转化：只记倒计时与反馈，实际改动等时间到（与原版治愈一致）。 */
    @Unique
    private void cof$beginConversion(Villager self) {
        this.cof$convertingTicks = CofSettings.instantVillagerConversion
                ? 1
                : COF_CONVERSION_MIN + self.getRandom().nextInt(COF_CONVERSION_RANDOM);
        boolean toNormal = NITWIT_ID.equals(Compat.professionId(self.getVillagerData()));
        // 照原版治愈：开始时移除"作为触发条件的那瓶药水"，并加上力量（持续整个转化期，
        // 原版的治愈粒子其实就来自这个效果）。
        self.removeEffect(toNormal ? MobEffects.REGENERATION : MobEffects.WEAKNESS);
        self.addEffect(new MobEffectInstance(Compat.strength(), this.cof$convertingTicks, 0));
        self.level().playSound(null, self.getX(), self.getEyeY(), self.getZ(),
                SoundEvents.ZOMBIE_VILLAGER_CURE, self.getSoundSource(), 1.0F, 1.0F);
    }

    /** 每 tick 推进倒计时；到点按当前职业决定方向。 */
    @Inject(method = "customServerAiStep", at = @At("HEAD"))
    private void cof$conversionTick(CallbackInfo ci) {
        Villager self = (Villager) (Object) this;
        if (!CofSettings.nitwitConversion) {
            cof$abortConversion(self);     // 规则被关：作废进行中的转化
            return;
        }
        if (self.level().isClientSide() || this.cof$convertingTicks <= 0) {
            return;
        }
        // 「快速转化」开启时，连"已经在进行中"的转化也立刻收尾
        if (CofSettings.instantVillagerConversion && this.cof$convertingTicks > 1) {
            this.cof$convertingTicks = 1;
        }
        // 颤抖：每 tick 抖动朝向（±10°），客户端插值后肉眼可见
        float jitter = (self.getRandom().nextFloat() - 0.5F) * 20.0F;
        self.setYRot(self.getYRot() + jitter);
        self.setYHeadRot(self.getYHeadRot() + jitter);

        if (--this.cof$convertingTicks > 0) {
            return;
        }
        if (NITWIT_ID.equals(Compat.professionId(self.getVillagerData()))) {
            cof$toUnemployed(self);
        } else {
            cof$toNitwit(self);
        }
    }

    /** 规则被关闭时作废进行中的转化（已完成的转化不回滚）。 */
    @Unique
    private void cof$abortConversion(Villager self) {
        if (this.cof$convertingTicks <= 0) {
            return;
        }
        this.cof$convertingTicks = 0;
        self.removeEffect(Compat.strength());
    }

    /** 普通 → 傻子：换职业（原版换职业会清交易）、立刻放弃工作方块认领。 */
    @Unique
    private void cof$toNitwit(Villager self) {
        // 先把记住的坐标抓出来：不依赖"记忆里还有值"这一点 —— 只要 POI 占用票还挂在这个
        // 实体身上，就要主动释放，否则方块会被永久占着，别的村民再也绑不上。
        GlobalPos job = self.getBrain().getMemory(MemoryModuleType.JOB_SITE).orElse(null);
        GlobalPos potential = self.getBrain().getMemory(MemoryModuleType.POTENTIAL_JOB_SITE).orElse(null);

        self.setVillagerData(Compat.withProfessionById(self.getVillagerData(), NITWIT_ID));
        self.setOffers(new MerchantOffers());
        self.setVillagerXp(0);

        // 双保险：(1) 直接向对应维度的 POI 管理器释放（幂等）；(2) 让原版清掉记忆。
        cof$releaseTicket(self, job);
        cof$releaseTicket(self, potential);
        self.releasePoi(MemoryModuleType.JOB_SITE);
        self.releasePoi(MemoryModuleType.POTENTIAL_JOB_SITE);

        if (self.level() instanceof ServerLevel serverLevel) {
            self.refreshBrain(serverLevel);
        }
    }

    /** 直接释放一条 POI 占用票（坐标可能属于别的维度；释放是幂等的，重复调用无害）。 */
    @Unique
    private static void cof$releaseTicket(Villager self, GlobalPos pos) {
        if (pos == null || !(self.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        ServerLevel target = serverLevel.getServer().getLevel(pos.dimension());
        if (target != null) {
            target.getPoiManager().release(pos.pos());
        }
    }

    /** 傻子 → 无职业村民：洗掉职业、清空交易与经验、等级回到 1，只保留群系外观。 */
    @Unique
    private void cof$toUnemployed(Villager self) {
        // 只动职业与等级：VillagerData 里的群系 type 原样保留
        self.setVillagerData(Compat.withLevel(
                Compat.withProfessionById(self.getVillagerData(), NONE_ID), 1));
        self.setOffers(new MerchantOffers());
        self.setVillagerXp(0);
        self.releasePoi(MemoryModuleType.JOB_SITE);
        self.releasePoi(MemoryModuleType.POTENTIAL_JOB_SITE);
        self.removeEffect(Compat.strength());
        if (self.level() instanceof ServerLevel serverLevel) {
            self.refreshBrain(serverLevel);
        }
    }
}
