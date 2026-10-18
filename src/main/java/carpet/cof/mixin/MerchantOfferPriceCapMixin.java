// SPDX-License-Identifier: MIT
// Copyright (c) 2026 CoF contributors

package carpet.cof.mixin;

import carpet.cof.CofSettings;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 「村民交易不涨价」：把最终价格钳到不超过基础价，买入与卖出两个方向都适用。
 *
 * <h2>原版价格公式</h2>
 *
 * <p>{@code MerchantOffer#getModifiedCostCount(ItemCost)} 是<b>买入（costA）与卖出（costB）共用的唯一计算点</b>：
 * <pre>
 * 最终数量 = clamp(基础数量 + floor(demand × priceMultiplier) + specialPriceDiff, 1, 最大堆叠)
 * </pre>
 * 其中 {@code demand} 由补货时的 {@code updateDemand()} 累积（"买得越多越贵"），
 * {@code specialPriceDiff} 由声望计算（{@code updateSpecialPrices}）。</p>
 *
 * <h2>为什么在返回值上钳</h2>
 *
 * <p>两个涨价来源都汇流到这一个返回值，所以在 RETURN 处取 {@code min(结果, 基础数量)} 即可：</p>
 * <ul>
 *   <li>涨价（demand 为正、或声望修正为正）→ 被压回基础价；</li>
 *   <li>降价（英雄折扣、正声望）→ 结果本来小于基础价，原样保留；</li>
 *   <li>因此"只降不涨"，且买入、卖出两个方向同时生效。</li>
 * </ul>
 *
 * <p>这个类在 1.21 ~ 1.21.11 通用：{@code net.minecraft.world.item.trading.MerchantOffer}
 * 的包名、方法名与字段名在这些版本中完全一致，不需要 clusterC 变体。</p>
 */
@Mixin(MerchantOffer.class)
public abstract class MerchantOfferPriceCapMixin {

    @Inject(method = "getModifiedCostCount", at = @At("RETURN"), cancellable = true)
    private void cof$capTradePrice(ItemCost cost, CallbackInfoReturnable<Integer> cir) {
        if (!CofSettings.reputationNoPriceHike) {
            return;
        }
        int base = cost.count();
        if (cir.getReturnValue() > base) {
            cir.setReturnValue(base);
        }
    }
}
