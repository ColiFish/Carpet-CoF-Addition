// SPDX-License-Identifier: MIT
// Copyright (c) 2026 CoF contributors

package carpet.cof.mixin;

import carpet.cof.CofSettings;
import carpet.cof.GunpowderIgnition;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 「可燃火药」：手持火药右键方块时，直接委托给原版烈焰弹。
 *
 * <p>火药在原版只是一个普通物品，没有自己的交互类，所以拦在 {@code Item#useOn} 这一层，
 * 再用"是不是火药这个单例"筛出目标。委托而不是复刻的好处是：点火、点燃营火 / 蜡烛、
 * 激活下界传送门、引燃 TNT、消耗 1 个等行为都与烈焰弹逐项一致，原版以后改动也会自动跟随。</p>
 *
 * <p>委托期间在 {@link GunpowderIgnition} 打标记，供音效层区分"火药"与"真烈焰弹"；
 * 返回值原样透传（烈焰弹什么都不做时返回 PASS，与火药的原版行为一致）。</p>
 */
@Mixin(Item.class)
public abstract class GunpowderIgniteMixin {

    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void cof$igniteLikeFireCharge(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (CofSettings.flammableGunpowder && (Object) this == Items.GUNPOWDER) {
            GunpowderIgnition.begin();
            try {
                cir.setReturnValue(Items.FIRE_CHARGE.useOn(context));
            } finally {
                GunpowderIgnition.end();
            }
        }
    }
}
