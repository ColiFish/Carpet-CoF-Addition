// SPDX-License-Identifier: MIT
// Copyright (c) 2026 CoF contributors

package carpet.cof.mixin;

import carpet.cof.CofSettings;
import carpet.cof.NitwitBackupKeys;
import carpet.cof.NitwitBackupHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.ReputationEventType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
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
 * 「傻子可逆转化」的主实现。
 *
 * <p>成年村民 + 虚弱 + 毒马铃薯 → 傻子；傻子 + 再生 + 金苹果 → 还原为原职业。
 * 备份（职业 / 交易列表 / 经验 / 操作者）存放在村民的自定义 NBT 标签 {@code CofNitwitBackup} 中，
 * 运行时由下面的 {@code @Unique} 字段承载，并随实体存档持久化。</p>
 *
 * <p><b>顺序很重要</b>：{@code setVillagerData} 在职业发生变化时会清空交易列表，所以必须
 * "先备份、后换职业"；转回时则"先换职业、后回填交易与经验"。转化后的傻子因此天然没有交易，
 * 与原版"傻子不能交易"的语义一致。</p>
 *
 * <p>只拦"毒马铃薯 + 虚弱"与"傻子 + 金苹果 + 再生"这两种组合，其余一律放行 ——
 * 特别是金苹果对<b>普通</b>村民仍是原版的繁殖触发，不受本规则影响。</p>
 */
@Mixin(Villager.class)
public abstract class VillagerNitwitConversionMixin implements NitwitBackupHolder {

    /** 备份标签：Profession / Offers / Xp / ToPlayer / BackPlayer。 */
    @Unique
    private CompoundTag cof$nitwitBackup;

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
        if (cof$isConverting()) {
            cir.setReturnValue(InteractionResult.CONSUME);   // 转化中：不再接受任何交互
            return;
        }
        ItemStack stack = player.getItemInHand(hand);
        VillagerProfession profession = self.getVillagerData().getProfession();
        boolean isNitwit = profession == VillagerProfession.NITWIT;

        if (!isNitwit && stack.is(Items.POISONOUS_POTATO) && self.hasEffect(MobEffects.WEAKNESS)) {
            stack.consume(1, player);
            if (!self.level().isClientSide) {
                cof$beginConversion(self, player);
            }
            cir.setReturnValue(InteractionResult.SUCCESS_SERVER);
        } else if (isNitwit && stack.is(Items.GOLDEN_APPLE) && self.hasEffect(MobEffects.REGENERATION)) {
            stack.consume(1, player);
            if (!self.level().isClientSide) {
                cof$beginConversion(self, player);
            }
            cir.setReturnValue(InteractionResult.SUCCESS_SERVER);
        }
    }

    /** 原版治愈僵尸村民用的时长：3600 ~ 6000 tick（3~5 分钟）。 */
    @Unique
    private static final int COF_CONVERSION_MIN = 3600;
    @Unique
    private static final int COF_CONVERSION_RANDOM = 2401;

    /** 是否正在转化中（倒计时存在备份标签里，因此僵尸化往返也会带着它）。 */
    @Unique
    private boolean cof$isConverting() {
        return this.cof$nitwitBackup != null && this.cof$nitwitBackup.getInt(NitwitBackupKeys.CONVERTING) > 0;
    }

    /** 开始转化：只记倒计时与反馈，不做实际改动（与原版治愈一样要等时间到）。 */
    @Unique
    private void cof$beginConversion(Villager self, Player player) {
        if (this.cof$nitwitBackup == null) {
            this.cof$nitwitBackup = new CompoundTag();
        }
        this.cof$nitwitBackup.putInt(NitwitBackupKeys.CONVERTING,
                COF_CONVERSION_MIN + self.getRandom().nextInt(COF_CONVERSION_RANDOM));
        this.cof$nitwitBackup.putUUID(NitwitBackupKeys.TO_PLAYER, player.getUUID());

        // 照原版治愈：开始时移除"作为触发条件的那瓶药水"，并加上力量（持续整个转化期）
        boolean toNormal = self.getVillagerData().getProfession() == VillagerProfession.NITWIT;
        self.removeEffect(toNormal ? MobEffects.REGENERATION : MobEffects.WEAKNESS);
        // 原版治愈的"药水粒子"其实来自这个力量效果（LivingEntity#tickEffects 会为生效中的效果绘制漩涡粒子，
        // 由客户端渲染）。因此这里恢复它：粒子与原版同源，且服务端不需要额外发任何粒子包。
        self.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST,
                this.cof$nitwitBackup.getInt(NitwitBackupKeys.CONVERTING), 0));
        // 原版治愈在开始时播的就是这个音效（ZombieVillager#handleEntityEvent(16) 里唯一的声音）
        self.level().playSound(null, self.getX(), self.getEyeY(), self.getZ(),
                SoundEvents.ZOMBIE_VILLAGER_CURE, self.getSoundSource(),
                1.0F + self.getRandom().nextFloat(), self.getRandom().nextFloat() * 0.7F + 0.3F);
    }

    /** 每 tick 推进倒计时；到点才真正转化，方向由当前职业决定。 */
    @Unique
    private void cof$tickConversion(Villager self, Player player) {
        if (this.cof$nitwitBackup == null) {
            return;
        }
        int left = this.cof$nitwitBackup.getInt(NitwitBackupKeys.CONVERTING);
        if (left <= 0) {
            return;
        }
        left -= 1;
        if (left > 0) {
            this.cof$nitwitBackup.putInt(NitwitBackupKeys.CONVERTING, left);
            cof$tickConversionJitter(self);                       // 每 tick 抖动（否则会被它自己的张望覆盖）

            return;
        }
        this.cof$nitwitBackup.remove(NitwitBackupKeys.CONVERTING);

        if (self.getVillagerData().getProfession() == VillagerProfession.NITWIT) {
            cof$fromNitwit(self, player);
        } else {
            cof$toNitwit(self, player);
        }
    }


    /** 颤抖：每 tick 抖动朝向（±10°），客户端插值后肉眼可见。 */
    @Unique
    private static void cof$tickConversionJitter(Villager self) {
        float jitter = (self.getRandom().nextFloat() - 0.5F) * 20.0F;
        self.setYRot(self.getYRot() + jitter);
        self.setYHeadRot(self.getYHeadRot() + jitter);
    }

    /** 转化推进的挂点：村民的服务端 AI 步进，每 tick 都会跑。 */
    @Inject(method = "customServerAiStep", at = @At("HEAD"))
    private void cof$conversionTick(CallbackInfo ci) {
        Villager self = (Villager) (Object) this;
        if (!CofSettings.nitwitConversion) {
            cof$abortPendingConversion(self);   // 规则被关：作废进行中的转化（不保留倒计时）
            return;
        }
        if (self.level().isClientSide) {
            return;
        }
        Player player = null;
        if (this.cof$nitwitBackup != null && this.cof$nitwitBackup.contains(NitwitBackupKeys.TO_PLAYER)) {
            player = self.level().getPlayerByUUID(this.cof$nitwitBackup.getUUID(NitwitBackupKeys.TO_PLAYER));
        }
        cof$tickConversion(self, player);
    }

    /**
     * 规则被关闭时作废"正在进行中"的转化。
     *
     * <p>只清倒计时与本次转化加上的力量；已经完成的备份（职业 / 交易 / 经验 / 工作站点）<b>保留</b>，
     * 这样"傻子 → 普通"被中断时，原来的数据不会丢，重新喂食即可再次转化。</p>
     *
     * <p>为什么要作废而不是暂停：倒计时会停，但力量效果是自然流逝的，两者会脱钩 ——
     * 玩家会看到"力量都没了，重新开启却从剩余时间继续转"，语义上说不通。</p>
     */
    @Unique
    private void cof$abortPendingConversion(Villager self) {
        if (this.cof$nitwitBackup == null
                || this.cof$nitwitBackup.getInt(NitwitBackupKeys.CONVERTING) <= 0) {
            return;
        }
        this.cof$nitwitBackup.remove(NitwitBackupKeys.CONVERTING);
        self.removeEffect(MobEffects.DAMAGE_BOOST);   // 移除本次转化加上的力量
    }

    /** 普通 → 傻子：先备份，再换职业（换职业会清空交易，正合"傻子无交易"）。 */
    @Unique
    private void cof$toNitwit(Villager self, Player player) {
        CompoundTag backup = new CompoundTag();
        ResourceLocation id = BuiltInRegistries.VILLAGER_PROFESSION.getKey(self.getVillagerData().getProfession());
        if (id != null) {
            backup.putString(NitwitBackupKeys.PROFESSION, id.toString());
        }
        MerchantOffers offers = self.getOffers();
        if (offers != null && !offers.isEmpty()) {
            // 注意：任何异常都不得阻断转化（否则会出现"倒计时跑完但职业没变"）
            try {
                backup.put(NitwitBackupKeys.OFFERS, (Tag) MerchantOffers.CODEC
                        .encodeStart(self.registryAccess().createSerializationContext(NbtOps.INSTANCE), offers)
                        .getOrThrow());
            } catch (RuntimeException e) {
                backup.remove(NitwitBackupKeys.OFFERS);        // 交易无法编码时退化为"不带交易的备份"
            }
        }
        backup.putInt(NitwitBackupKeys.XP, self.getVillagerXp());
        GlobalPos jobSite = self.getBrain().getMemory(MemoryModuleType.JOB_SITE).orElse(null);
        if (jobSite != null) {
            backup.putLong(NitwitBackupKeys.JOB_SITE, jobSite.pos().asLong());
            backup.putString(NitwitBackupKeys.JOB_SITE_DIM, jobSite.dimension().location().toString());
        }
        // 核心动作先执行：只要走到这里就一定要把职业换掉（其后都是善后，出问题也不影响结果）
        self.setVillagerData(self.getVillagerData().setProfession(VillagerProfession.NITWIT));
        backup.putUUID(NitwitBackupKeys.TO_PLAYER, player.getUUID());
        this.cof$nitwitBackup = backup;

        // 关键：释放占用票，否则方块虽在旁边，票还挂在它自己身上，它反而认领不到
        self.releasePoi(MemoryModuleType.JOB_SITE);
        self.releasePoi(MemoryModuleType.POTENTIAL_JOB_SITE);

        if (self.level() instanceof ServerLevel serverLevel) {
            self.refreshBrain(serverLevel);
        }
    }

    /** 傻子 → 普通：还原职业，再回填交易与经验；没有备份（不是本规则转来的傻子）则不动。 */
    @Unique
    private void cof$fromNitwit(Villager self, Player player) {
        CompoundTag backup = this.cof$nitwitBackup;
        if (backup == null) {
            return;
        }
        VillagerProfession profession = null;
        if (backup.contains(NitwitBackupKeys.PROFESSION, 8)) {
            ResourceLocation id = ResourceLocation.tryParse(backup.getString(NitwitBackupKeys.PROFESSION));
            if (id != null) {
                profession = BuiltInRegistries.VILLAGER_PROFESSION.get(id).map(net.minecraft.core.Holder::value).orElse(null);
            }
        }
        if (profession == null) {
            return;
        }
        self.setVillagerData(self.getVillagerData().setProfession(profession));
        if (backup.contains(NitwitBackupKeys.OFFERS, 10)) {
            MerchantOffers.CODEC
                    .parse(self.registryAccess().createSerializationContext(NbtOps.INSTANCE), backup.get(NitwitBackupKeys.OFFERS))
                    .resultOrPartial()
                    .ifPresent(offers -> self.setOffers(offers.copy()));
        }
        self.setVillagerXp(backup.getInt(NitwitBackupKeys.XP));
        backup.putUUID(NitwitBackupKeys.BACK_PLAYER, player.getUUID());

        // 原版治愈影响：写入 ZOMBIE_VILLAGER_CURED 声望事件（大幅正面 → 交易价格下降）
        if (player instanceof ServerPlayer serverPlayer && self.level() instanceof ServerLevel serverLevel2) {
            serverLevel2.onReputationEvent(ReputationEventType.ZOMBIE_VILLAGER_CURED, serverPlayer, self);
        }
        if (self.level() instanceof ServerLevel serverLevel) {
            self.refreshBrain(serverLevel);

            // 必须放在 refreshBrain 之后：重建大脑会清空记忆
            if (backup.contains(NitwitBackupKeys.JOB_SITE, 4) && cof$isStillValidJobSite(serverLevel, profession, backup)) {
                self.getBrain().setMemory(MemoryModuleType.JOB_SITE,
                        GlobalPos.of(serverLevel.dimension(), BlockPos.of(backup.getLong(NitwitBackupKeys.JOB_SITE))));
            }
        }
    }

    /** 备份坐标上是否仍是该职业的有效工作方块（且维度一致）。 */
    @Unique
    private static boolean cof$isStillValidJobSite(ServerLevel level, VillagerProfession profession, CompoundTag backup) {
        if (!backup.contains(NitwitBackupKeys.JOB_SITE_DIM, 8)) {
            return false;
        }
        ResourceLocation dim = ResourceLocation.tryParse(backup.getString(NitwitBackupKeys.JOB_SITE_DIM));
        if (dim == null || !dim.equals(level.dimension().location())) {
            return false;
        }
        return level.getPoiManager().exists(BlockPos.of(backup.getLong(NitwitBackupKeys.JOB_SITE)), profession.heldJobSite());
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void cof$saveNitwitBackup(CompoundTag tag, CallbackInfo ci) {
        if (this.cof$nitwitBackup != null) {
            tag.put(NitwitBackupKeys.BACKUP, this.cof$nitwitBackup);
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void cof$readNitwitBackup(CompoundTag tag, CallbackInfo ci) {
        if (tag.contains(NitwitBackupKeys.BACKUP, 10)) {
            this.cof$nitwitBackup = tag.getCompound(NitwitBackupKeys.BACKUP);
        }
    }

    @Override
    public CompoundTag cof$getNitwitBackup() {
        return this.cof$nitwitBackup;
    }

    @Override
    public void cof$setNitwitBackup(CompoundTag backup) {
        this.cof$nitwitBackup = backup;
    }
}
