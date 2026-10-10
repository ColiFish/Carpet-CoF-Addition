// SPDX-License-Identifier: MIT
// Copyright (c) 2026 CoF contributors

package carpet.cof.mixin;

import carpet.cof.CauldronFreezeData;
import carpet.cof.CofSettings;
import carpet.cof.WaterBottles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 炼药锅交互上的两件事。
 *
 * <h2>一、水瓶灌水不消耗（规则 {@code waterBottleNotConsumed}）</h2>
 *
 * <p>原版把水瓶里的水倒进锅的顺序是：<b>先消耗水瓶（shrink）→ 再把手上的东西换成空玻璃瓶 →
 * 再改方块</b>。所以"事后还原"这条路走不通：等到方法返回时，传入的 {@code stack}
 * 已经被消耗成空气了（实测日志 {@code preStack=0 minecraft:air}），既拿不回原物品，
 * 也无从判断它原本是不是水瓶。</p>
 *
 * <p>因此这条规则挂在 <b>HEAD 并取消原方法</b>：在交互发生之前就接管，水瓶压根不进入消耗流程，
 * 手上那一格自始至终没被碰过，NBT（比如铁砧改过的名字）自然完整保留。水位、音效、游戏事件
 * 按原版那几条调用自己补上，其余行为与原版一致，差别只是水瓶留在手上。</p>
 *
 * <p><b>要接管两种方块状态</b>（都是原版会消耗水瓶的情形，实测确认）：</p>
 * <ul>
 *   <li><b>空炼药锅</b> {@code minecraft:cauldron} —— 原版用水瓶直接灌成 <b>1 级水锅</b></li>
 *   <li><b>水锅 1~2 级</b> —— 水位 +1</li>
 * </ul>
 * <p>岩浆锅、细雪锅、满水锅原版本来就不消耗水瓶，直接放行给原版。</p>
 *
 * <p><b>为什么不拦 {@code ItemUtils#createFilledResult}</b>：喝药水
 * （{@code PotionItem#finishUsingItem}）走的是同一个方法、返回的也是空玻璃瓶，
 * 在那里拦会连"喝水瓶不消耗"一起实现掉，等于凭空刷水瓶。所以这里只拦炼药锅交互。</p>
 *
 * <h2>二、取水即作废预热进度</h2>
 *
 * <p><b>注意执行顺序</b>：上面那条 HEAD 注入一旦 {@code setReturnValue}，本 RETURN 注入<b>仍会执行</b>（Mixin 的语义如此），
 * 所以"倒水瓶"这条路径会清两次。目前安全，是因为 {@code clearIfPresent} 幂等（记录不存在时不会标脏）；
 * 但若将来把这里的清理换成更重或非幂等的逻辑，请改成只在"未接管"分支执行。</p>
 *
 * <p>空桶舀水、玻璃瓶装水之后，立刻清掉该位置的预热记录。挂在 RETURN，是为了<b>用实际结果判断</b>
 * 而不是猜：只有"水位真的变少了 / 已经不是水锅了"才清。被别的模组拦掉的交互、
 * 或者压根没取走水的右键，都不会误清。</p>
 */
@Mixin(AbstractCauldronBlock.class)
public abstract class AbstractCauldronBlockMixin {

    /**
     * 原版查"某物品在炼药锅里做什么"的注册表（{@code protected final} 且没有 getter，只能 shadow）。
     * 别的模组可以往里面替换/新增处理器，所以接管前必须先看它。
     */
    @Shadow
    @Final
    protected CauldronInteraction.InteractionMap interactions;

    /** 把一瓶水倒进炼药锅：补上水位、音效与游戏事件，并取消原方法（于是水瓶不被消耗）。 */
    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void cof$pourWaterBottleWithoutConsuming(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                                     Player player, InteractionHand hand, BlockHitResult hit,
                                                     CallbackInfoReturnable<InteractionResult> cir) {
        if (!CofSettings.waterBottleNotConsumed || !WaterBottles.isWaterBottle(stack)) {
            return;
        }
        // 只有"注册表里仍是原版那条填水交互"时才接管。别的模组可以替换 POTION 的处理器，
        // 如果我们无条件在 HEAD 取消，它们的处理器永远不会被调用（跨模组破坏）。
        CauldronInteraction handler = this.interactions.map().get(stack.getItem());
        if (handler == null || !handler.getClass().getName().startsWith("net.minecraft.")) {
            return;
        }
        // 原版只有这两种情况会消耗水瓶；其余（岩浆锅、细雪锅、满水锅）本来就不消耗，交给原版
        BlockState filled;
        if (state.is(Blocks.CAULDRON)) {
            filled = Blocks.WATER_CAULDRON.defaultBlockState();   // 空锅 → 1 级水锅
        } else if (state.is(Blocks.WATER_CAULDRON)
                && state.getValue(LayeredCauldronBlock.LEVEL) < LayeredCauldronBlock.MAX_FILL_LEVEL) {
            filled = state.setValue(LayeredCauldronBlock.LEVEL,
                    state.getValue(LayeredCauldronBlock.LEVEL) + 1);
        } else {
            return;
        }
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.setBlockAndUpdate(pos, filled);
            serverLevel.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
            // 空锅 → 1 级水锅时原版发的是 FLUID_PLACE，而在已有水锅上加水位会额外发 BLOCK_CHANGE
            if (state.is(Blocks.CAULDRON)) {
                serverLevel.gameEvent(null, GameEvent.FLUID_PLACE, pos);
            } else {
                serverLevel.gameEvent(null, GameEvent.BLOCK_CHANGE, pos);
            }
            player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
            player.awardStat(Stats.USE_CAULDRON);   // 原版倒水瓶也会记这一条
            // 水位变了，按既定规则作废预热进度
            clearWarmupIfWaterChanged(serverLevel, pos, state);
        }
        cir.setReturnValue(InteractionResult.SUCCESS);
    }

    /** 取水即作废预热进度（只在服务端做，因为要改的是存档数据）。 */
    @Inject(method = "useItemOn", at = @At("RETURN"))
    private void cof$clearWarmupWhenWaterTaken(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hit,
                                               CallbackInfoReturnable<InteractionResult> cir) {
        if (level instanceof ServerLevel serverLevel) {
            clearWarmupIfWaterChanged(serverLevel, pos, state);
        }
    }

    /**
     * 预热进度是否该作废：交互前是水炼药锅，而交互后水位变了、或者干脆不再是水锅。
     *
     * <p>"任意刻度都能结冰"模式（{@code cauldronFreezingRequiresFullWater=false}）下，
     * 只要还有水就不影响进度，所以那种情况直接返回。</p>
     */
    private static void clearWarmupIfWaterChanged(ServerLevel level, BlockPos pos, BlockState before) {
        if (!before.is(Blocks.WATER_CAULDRON)) {
            return;
        }
        BlockState after = level.getBlockState(pos);
        if (after.is(Blocks.WATER_CAULDRON)) {
            if (!CofSettings.cauldronFreezingRequiresFullWater) {
                return;   // 任意刻度都能结冰 → 只要还有水，增减水位都不影响进度
            }
            if (after.getValue(LayeredCauldronBlock.LEVEL)
                    == before.getValue(LayeredCauldronBlock.LEVEL)) {
                return;   // 满水模式，但水位没变 → 水没被动过
            }
        }
        CauldronFreezeData.clearIfPresent(level, pos);
    }
}
