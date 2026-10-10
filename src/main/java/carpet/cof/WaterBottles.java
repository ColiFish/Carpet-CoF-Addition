// SPDX-License-Identifier: GPL-3.0-only
// Copyright (c) 2026 CoF contributors

package carpet.cof;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

/**
 * 水瓶判定，供炼药锅交互（{@code AbstractCauldronBlockMixin}）与诊断命令共用。
 *
 * <p>只有"药水物品 + 药水内容是水"才算水瓶，所以喷溅型/滞留型药水不会被误判。</p>
 */
public final class WaterBottles {
    private WaterBottles() {
    }

    /** 是不是水瓶。 */
    public static boolean isWaterBottle(ItemStack stack) {
        if (!stack.is(Items.POTION)) {
            return false;
        }
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        return contents != null && contents.is(Potions.WATER);
    }
}
