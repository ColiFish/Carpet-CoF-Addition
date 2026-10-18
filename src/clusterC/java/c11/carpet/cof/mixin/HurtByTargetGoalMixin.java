// SPDX-License-Identifier: MIT
// Copyright (c) 2026 CoF contributors

package carpet.cof.mixin;

import carpet.cof.CofSettings;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.animal.golem.IronGolem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 「正义的伙伴」：铁傀儡被玩家攻击后不再还手 —— <b>clusterC</b> 变体（MC 1.21.11 起）。
 *
 * <p>与 {@code src/main/java/.../HurtByTargetGoalMixin.java} 同名同包，由 {@code build.gradle}
 * 在 1.21.11 上二选一；差异只有 {@code world.entity.animal.golem.IronGolem} 包名。</p>
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
