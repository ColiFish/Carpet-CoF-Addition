// SPDX-License-Identifier: MIT
// Copyright (c) 2026 CoF contributors

package carpet.cof.mixin;

import carpet.cof.CofSettings;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 「正义的伙伴」：铁傀儡被玩家攻击后不再还手。
 *
 * <p><b>只拦这一个目标</b>：铁傀儡的目标注册表里，goal 2 是还手、goal 3 才是"愤怒值驱动的玩家寻敌"
 * 与"敌对生物寻敌"。早前版本改在 {@code Mob#setTarget} 上拦截（那是所有目标赋值的必经之路），
 * 结果把"对敌对生物的敌意"也一并掐掉了——本 mixin 只针对 {@code HurtByTargetGoal}，不再有该副作用。</p>
 *
 * <p>取宿主用 {@link TargetGoalAccessor}：子类 mixin 不能 @Shadow 继承来的 {@code mob} 字段。</p>
 */
@Mixin(HurtByTargetGoal.class)
public abstract class HurtByTargetGoalMixin {

    @Inject(method = "canUse", at = @At("HEAD"), cancellable = true)
    private void cof$noRetaliation(CallbackInfoReturnable<Boolean> cir) {
        if (CofSettings.righteousCompanion
                && ((TargetGoalAccessor) (Object) this).cof$getMob() instanceof IronGolem) {
            cir.setReturnValue(false);
        }
    }
}
