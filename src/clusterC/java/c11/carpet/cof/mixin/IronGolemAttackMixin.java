// SPDX-License-Identifier: MIT
// Copyright (c) 2026 CoF contributors

package carpet.cof.mixin;

import carpet.cof.CofSettings;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.golem.IronGolem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 「正义的伙伴」（只作用于 {@code IronGolem}）—— <b>clusterC</b> 变体（MC 1.21.11 起）。
 *
 * <p>与 {@code src/main/java/.../IronGolemAttackMixin.java} 同名同包，由 {@code build.gradle}
 * 在 1.21.11 上二选一。</p>
 *
 * <p><b>1.21.11 的差异不只是包名</b> ✓：原版把"剩余愤怒 tick 数"（{@code remainingPersistentAngerTime}，
 * int）改成了"愤怒结束时间戳"（{@code persistentAngerEndTime}，long），
 * 读写入口也换成了 {@code setPersistentAngerEndTime(long)} / {@code getPersistentAngerEndTime()}。
 * 原版用 {@code NeutralMob.NO_ANGER_END_TIME = -1} 表示"不愤怒"，所以这里把写入值钉成 {@code -1}、
 * 读取时就地清零并返回 {@code -1} —— 语义与 main 版（写入钉成 0、读取返回 0）完全对应。</p>
 */
@Mixin(IronGolem.class)
public abstract class IronGolemAttackMixin {

    @Shadow
    private long persistentAngerEndTime;

    @ModifyVariable(method = "setPersistentAngerEndTime(J)V", at = @At("HEAD"), argsOnly = true)
    private long cof$noNewAnger(long time) {
        return CofSettings.righteousCompanion ? -1L : time;
    }

    @Inject(method = "getPersistentAngerEndTime()J", at = @At("HEAD"), cancellable = true)
    private void cof$clearLingeringAnger(CallbackInfoReturnable<Long> cir) {
        if (CofSettings.righteousCompanion) {
            this.persistentAngerEndTime = -1L;
            cir.setReturnValue(-1L);
        }
    }

    @Inject(method = "canAttackType(Lnet/minecraft/world/entity/EntityType;)Z",
            at = @At("HEAD"), cancellable = true)
    private void cof$righteousCompanion(EntityType<?> type, CallbackInfoReturnable<Boolean> cir) {
        if (CofSettings.righteousCompanion && type == EntityType.PLAYER) {
            cir.setReturnValue(false);
        }
    }
}
