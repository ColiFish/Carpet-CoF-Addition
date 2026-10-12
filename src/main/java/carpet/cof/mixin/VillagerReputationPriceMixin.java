// SPDX-License-Identifier: MIT
// Copyright (c) 2026 CoF contributors

package carpet.cof.mixin;

import carpet.cof.CofSettings;
import net.minecraft.world.entity.npc.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * 「声望不涨价」：村民因声望调价时只能降价或不变。
 *
 * <p>原版 {@code Villager#updateSpecialPrices(Player)} 里有两处价格修正。第一处是声望：
 * {@code addToSpecialPriceDiff(-floor(声望 x priceMultiplier))}；第二处是英雄buff
 * {@code addToSpecialPriceDiff(-max(k, 1))}，它本来就是折扣。</p>
 *
 * <p>声望为正（治愈僵尸村民、正面流言）时第一处算出负值、更便宜；声望为负（攻击或击杀村民）
 * 时算出正值、<b>涨价</b>。本 mixin 只拦第一处（{@code ordinal = 0}），把修正值限制为 ≤ 0，
 * 于是"只降不涨"；英雄buff 的折扣完全不受影响。</p>
 *
 * <p>原版"买得越多越贵"的<b>需求</b>机制（{@code MerchantOffer#updateDemand}）与声望无关，
 * 本规则不干预。</p>
 */
@Mixin(Villager.class)
public abstract class VillagerReputationPriceMixin {

    @ModifyArg(
            method = "updateSpecialPrices(Lnet/minecraft/world/entity/player/Player;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/trading/MerchantOffer;addToSpecialPriceDiff(I)V",
                    ordinal = 0),
            index = 0)
    private int cof$noReputationPriceHike(int delta) {
        if (CofSettings.reputationNoPriceHike) {
            return Math.min(0, delta);   // 只允许降价（负值）或不变（0）
        }
        return delta;
    }
}
