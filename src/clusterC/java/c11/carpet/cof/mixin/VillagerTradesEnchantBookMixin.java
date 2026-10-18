// SPDX-License-Identifier: MIT
// Copyright (c) 2026 CoF contributors

package carpet.cof.mixin;

import carpet.cof.CofSettings;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 「低价村民」：图书管理员刷附魔书时，价格区间的随机项一律取下限。
 *
 * <p>原版 {@code VillagerTrades$EnchantBookForEmeralds#getOffer} 里那句
 * {@code l = 2 + random.nextInt(5 + k * 10) + 3 * k} 是本类唯一一处
 * {@code RandomSource#nextInt(int)} 调用（附魔等级那步走的是 {@code Mth.nextInt(random, i, j)}
 * 的静态调用，不在本重定向范围内），所以把这里的返回值改成 0 即可让价格恒为区间下限
 * {@code 2 + 3k}；宝藏附魔的 ×2 与 64 封顶仍按原版执行，<b>附魔种类与等级也仍然随机</b>。</p>
 *
 * <p><b>clusterC</b> 变体（MC 1.21.11 起）：与 {@code src/main/java/.../VillagerTradesEnchantBookMixin.java}
 * 同名同包，由 {@code build.gradle} 在 1.21.11 上二选一；
 * 差异只有 {@code targets} 字符串里的 {@code world.entity.npc.villager.VillagerTrades$…} 包名。</p>
 */
@Mixin(targets = "net.minecraft.world.entity.npc.villager.VillagerTrades$EnchantBookForEmeralds")
public abstract class VillagerTradesEnchantBookMixin {

    @Redirect(
            method = "getOffer",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;nextInt(I)I"))
    private int cof$minBookPrice(RandomSource random, int bound) {
        if (CofSettings.lowPriceVillagers) {
            return 0;   // 随机项取下限 → 价格 = 2 + 3k（宝藏附魔仍会 ×2、仍封顶 64）
        }
        return random.nextInt(bound);
    }
}
