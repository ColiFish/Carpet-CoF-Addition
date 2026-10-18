// SPDX-License-Identifier: MIT
// Copyright (c) 2026 CoF contributors

package carpet.cof.mixin;

import carpet.cof.CofSettings;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 「正义的伙伴」：铁傀儡身上任何"以玩家为目标"的目标行为都不再延续 —— <b>clusterC</b> 变体（MC 1.21.11 起）。
 *
 * <p>与 {@code src/main/java/.../TargetGoalContinueMixin.java} 同名同包，由 {@code build.gradle}
 * 在 1.21.11 上二选一；差异只有 {@code world.entity.animal.golem.IronGolem} 包名。</p>
 */
@Mixin(TargetGoal.class)
public abstract class TargetGoalContinueMixin {

    @Shadow
    @Final
    protected Mob mob;

    @Shadow
    protected LivingEntity targetMob;

    @Inject(method = "canContinueToUse", at = @At("HEAD"), cancellable = true)
    private void cof$stopChasingPlayers(CallbackInfoReturnable<Boolean> cir) {
        if (CofSettings.righteousCompanion && this.mob instanceof IronGolem) {
            if (this.mob.getTarget() instanceof Player || this.targetMob instanceof Player) {
                cir.setReturnValue(false);
            }
        }
    }
}
