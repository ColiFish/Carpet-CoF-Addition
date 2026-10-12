// SPDX-License-Identifier: MIT
// Copyright (c) 2026 CoF contributors

package carpet.cof;

/**
 * 「低价村民」用到的算式与常量集中在这里。
 *
 * <h2>原版真正带"价格区间"的只有两类交易（1.21.4 反编译确认）</h2>
 *
 * <ul>
 *   <li><b>附魔书</b>：{@code VillagerTrades$EnchantBookForEmeralds#getOffer}
 *       <pre>k = Mth.nextInt(random, 附魔最小等级, 附魔最大等级)   // 附魔等级，随机
 *l = 2 + random.nextInt(5 + k * 10) + 3 * k               // 价格区间
 *if (该附魔在 DOUBLE_TRADE_PRICE 标签里) l *= 2            // 宝藏附魔翻倍
 *if (l &gt; 64) l = 64                                      // 封顶</pre>
 *       区间下限 = 随机项取 0 = <b>{@code 2 + 3k}</b>（宝藏附魔再 ×2）。
 *       例：锋利五（k=5）→ 17~71 封顶 64，下限正是 <b>17</b>，与游戏内一致。</li>
 *   <li><b>附魔装备</b>：{@code VillagerTrades$EnchantedItemForEmeralds#getOffer}
 *       <pre>i = 5 + random.nextInt(15)                          // 附魔强度（也是价格的一部分）
 *price = min(baseEmeraldCost + i, 64)</pre>
 *       区间下限 = <b>{@code min(baseEmeraldCost + 5, 64)}</b>。<br>
 *       注意 {@code i} 同时决定装备的附魔强度，所以压价格时<b>不能</b>把 {@code i} 改掉，
 *       只能改最终那个价格参数。</li>
 * </ul>
 *
 * <p>其余交易实现类（{@code EmeraldForItems}、{@code EmeraldsForVillagerTypeItem}、
 * {@code ItemsForEmeralds}、{@code ItemsAndEmeraldsToItems}、{@code SuspiciousStewForEmerald}、
 * {@code TippedArrowForItemsAndEmeralds}、{@code TreasureMapForEmeralds}、{@code DyedArmorForEmeralds}、
 * {@code FailureItemListing}）的价格都是固定字段，没有随机区间，本规则不碰。</p>
 *
 * <p>价格在交易生成时就被写进 {@code MerchantOffer}（{@code baseCostA} 是 final 字段），
 * 之后无法回改。因此本规则只影响<b>之后新刷出</b>的交易；村民升级会重刷交易，所以打开规则后
 * 新生成或升级的村民立刻按低价出售，已存在的村民保持原价。</p>
 */
public final class LowPriceVillagers {

    private LowPriceVillagers() {
    }

    /** 附魔装备那条交易里附魔强度随机项的下限（{@code i = 5 + random.nextInt(15)} 中的 5）。 */
    public static final int MIN_ENCHANT_POWER = 5;

    /** 原版这两类交易的价格上限（{@code if (l > 64) l = 64}）。 */
    public static final int MAX_PRICE = 64;

    /**
     * 附魔装备的最低售价 = {@code min(baseEmeraldCost + 5, 64)}。
     *
     * <p>只用于算价格；装备实际的附魔强度仍是原版随机结果。</p>
     */
    public static int enchantedItemMinPrice(int baseEmeraldCost) {
        return Math.min(baseEmeraldCost + MIN_ENCHANT_POWER, MAX_PRICE);
    }
}
