// SPDX-License-Identifier: MIT
// Copyright (c) 2026 CoF contributors

package carpet.cof.mixin;

import carpet.cof.CofSettings;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 「正义的伙伴」：规则开启期间，铁傀儡身上任何"以玩家为目标"的目标行为都不再延续，从而**正常停止**。
 *
 * <p>为什么必须拦在"延续"而不是"读取"：{@code TargetGoal#canContinueToUse} 的判断是
 * {@code 目标 = mob.getTarget(); 若为 null 则退回 this.targetMob;}。早前版本改在 {@code Mob#getTarget}
 * 上清空字段，结果 goal 退回到 {@code targetMob} 后恒为 true、**永远处于运行中**，既占住了该优先级
 * （铁傀儡"打敌对生物"的目标同样是优先级 3，于是被挡住 → 不再攻击敌对生物），又在关掉规则后
 * 因为 {@code target} 已被清空而再也无法出手。改为在延续处返回 false，会走 {@code TargetGoal#stop()}，
 * 把 {@code target} 与 {@code targetMob} 一并清干净，槽位也随之释放。</p>
 *
 * <p>只作用于「铁傀儡 + 玩家目标」：对敌对生物的追击、以及其它生物的还手行为完全不受影响。</p>
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
