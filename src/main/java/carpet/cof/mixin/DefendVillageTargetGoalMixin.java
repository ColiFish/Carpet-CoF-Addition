// SPDX-License-Identifier: MIT
// Copyright (c) 2026 CoF contributors

package carpet.cof.mixin;

import carpet.cof.CofSettings;
import net.minecraft.world.entity.ai.goal.target.DefendVillageTargetGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 「正义的伙伴」：铁傀儡不再因村庄声望（流言）把玩家设为目标。
 *
 * <p>原版 {@code DefendVillageTargetGoal#canUse} 的逻辑是：取自己 10×8×10 范围内的村民与玩家，
 * 逐个配对检查村民对该玩家的声望，只要有一个村民的声望 ≤ -100，就把该玩家设为攻击目标
 * （旁观与创造模式的玩家除外）。这个目标对所有铁傀儡都注册，因此：</p>
 *
 * <ul>
 *   <li>村民生成的铁傀儡：旁边就有村民 → 低声望玩家会被攻击</li>
 *   <li>前哨站 / 自然生成的铁傀儡：本该中立，但只要有村民走进它的判定框，同样会触发</li>
 *   <li>玩家自建的铁傀儡：原版靠 {@code PlayerCreated} 标志在 {@code canAttackType} 里豁免玩家</li>
 * </ul>
 *
 * <p>本 mixin 在 {@code canUse} 入口直接返回 false，三种来源一视同仁地保持友善。
 * <b>被玩家直接攻击后的还手</b>走的是另一条路径（{@code HurtByTargetGoal} + 愤怒值），不在本规则范围内。</p>
 */
@Mixin(DefendVillageTargetGoal.class)
public abstract class DefendVillageTargetGoalMixin {

    @Inject(method = "canUse", at = @At("HEAD"), cancellable = true)
    private void cof$alwaysFriendly(CallbackInfoReturnable<Boolean> cir) {
        if (CofSettings.righteousCompanion) {
            cir.setReturnValue(false);
        }
    }
}
