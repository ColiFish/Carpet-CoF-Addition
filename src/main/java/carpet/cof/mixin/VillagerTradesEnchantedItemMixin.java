// SPDX-License-Identifier: MIT
// Copyright (c) 2026 CoF contributors

package carpet.cof.mixin;

import carpet.cof.CofSettings;
import carpet.cof.LowPriceVillagers;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * 「低价村民」：盔甲匠 / 武器匠 / 工具匠刷附魔装备时，价格取下限。
 *
 * <p>原版 {@code VillagerTrades$EnchantedItemForEmeralds#getOffer}：
 * {@code i = 5 + random.nextInt(15)}，{@code price = min(baseEmeraldCost + i, 64)}。</p>
 *
 * <p><b>不能</b>像附魔书那样把 {@code nextInt} 压掉——{@code i} 同时是
 * {@code EnchantmentHelper.enchantItem} 的附魔强度，压掉会让刷出来的装备变弱。
 * 所以只在构造 {@code ItemCost(EMERALD, price)} 时把那个 int 参数替换成区间下限
 * {@code min(baseEmeraldCost + 5, 64)}，装备的附魔强度保持原版随机。</p>
 */
@Mixin(targets = "net.minecraft.world.entity.npc.VillagerTrades$EnchantedItemForEmeralds")
public abstract class VillagerTradesEnchantedItemMixin {

    /** 原版用于算价格的字段（{@code private final int}）。 */
    @Shadow
    @Final
    private int baseEmeraldCost;

    @ModifyArg(
            method = "getOffer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/trading/ItemCost;<init>(Lnet/minecraft/world/level/ItemLike;I)V"),
            index = 1)
    private int cof$minEnchantedItemPrice(int price) {
        if (CofSettings.lowPriceVillagers) {
            return LowPriceVillagers.enchantedItemMinPrice(this.baseEmeraldCost);
        }
        return price;
    }
}
