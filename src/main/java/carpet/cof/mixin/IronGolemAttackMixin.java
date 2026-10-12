// SPDX-License-Identifier: MIT
// Copyright (c) 2026 CoF contributors

package carpet.cof.mixin;

import carpet.cof.CofSettings;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 「正义的伙伴」（只作用于 {@code IronGolem}）：不记账愤怒、清残留愤怒、兜底不伤害玩家。
 *
 * <ol>
 *   <li>{@code setRemainingPersistentAngerTime}：规则开启期间把写入值强制为 0（不累积新的愤怒）。</li>
 *   <li>{@code getRemainingPersistentAngerTime}：读取时把字段**就地清零**并返回 0。
 *       这一条是必需的补充——只拦"写入"无法清除"规则开启之前就已存在的愤怒计时"，
 *       否则先打傀儡、再开规则、再关规则时，残留计时会立刻生效（实测复现过）。
 *       字段 {@code remainingPersistentAngerTime} 声明在 {@code IronGolem} 自己，可直接 shadow。</li>
 *   <li>{@code doHurtTarget}：即便已锁定玩家（例如规则是追击开始后才打开的），
 *       挥击时取消伤害并立刻 {@code setTarget(null)} 放弃目标。</li>
 * </ol>
 *
 * <p>所有注入点与字段都声明在 {@code IronGolem} 本类，避开"成员其实在父类"这类运行期错误。</p>
 */
@Mixin(IronGolem.class)
public abstract class IronGolemAttackMixin {

    @Shadow
    private int remainingPersistentAngerTime;

    @ModifyVariable(method = "setRemainingPersistentAngerTime(I)V", at = @At("HEAD"), argsOnly = true)
    private int cof$noNewAnger(int time) {
        return CofSettings.righteousCompanion ? 0 : time;
    }

    @Inject(method = "getRemainingPersistentAngerTime()I", at = @At("HEAD"), cancellable = true)
    private void cof$clearLingeringAnger(CallbackInfoReturnable<Integer> cir) {
        if (CofSettings.righteousCompanion) {
            this.remainingPersistentAngerTime = 0;
            cir.setReturnValue(0);
        }
    }

    @Inject(
            method = "doHurtTarget(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;)Z",
            at = @At("HEAD"), cancellable = true)
    private void cof$righteousCompanion(ServerLevel level, Entity target, CallbackInfoReturnable<Boolean> cir) {
        if (CofSettings.righteousCompanion && target instanceof Player) {
            ((IronGolem) (Object) this).setTarget(null);
            cir.setReturnValue(false);
        }
    }
}
