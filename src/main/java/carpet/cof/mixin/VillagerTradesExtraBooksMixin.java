// SPDX-License-Identifier: MIT
// Copyright (c) 2026 CoF contributors

package carpet.cof.mixin;

import carpet.cof.CofSettings;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 「亚历山大图书馆」：让图书管理员也能刷出「迅捷潜行」「灵魂疾行」「风爆」的附魔书。
 *
 * <p>原版 {@code EnchantBookForEmeralds#getOffer} 只有一处决定"卖哪本"——
 * {@code Registry#getRandomElementOf(TagKey, RandomSource)}，它从 {@code #minecraft:tradeable} 标签里均匀取一个。</p>
 *
 * <p>本注入只在这一步介入：规则开启时把池子扩成"标签内容 + 这三种"，随后原版逻辑照常执行，
 * 因此<b>等级范围</b>（1~3 全部等级）、<b>价格公式</b>（这三种在 {@code #minecraft:treasure} 里，
 * 自动走双倍价与 64 封顶）都与原版完全一致，不需要额外改动。</p>
 *
 * <p>既有的交易不受影响：本方法只在生成新交易时被调用，已生成的交易存在村民自己的 {@code Offers} 里。</p>
 */
@Mixin(targets = "net.minecraft.world.entity.npc.VillagerTrades$EnchantBookForEmeralds")
public abstract class VillagerTradesExtraBooksMixin {

    /** 原版拿不到的三种宝藏附魔（它们不在 #minecraft:tradeable 标签里）。 */
    private static final List<String> COF_EXTRA_BOOKS = List.of("swift_sneak", "soul_speed", "wind_burst");

    @Redirect(
            method = "getOffer",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/core/Registry;"
                            + "getRandomElementOf(Lnet/minecraft/tags/TagKey;"
                            + "Lnet/minecraft/util/RandomSource;)Ljava/util/Optional;"))
    private Optional<Holder<Enchantment>> cof$libraryOfAlexandria(Registry<Enchantment> registry,
                                                                  TagKey<Enchantment> tag,
                                                                  RandomSource random) {
        // 只对原版的附魔书标签生效；规则关闭或标签不符时完全不介入
        if (!CofSettings.libraryOfAlexandria || !tag.equals(EnchantmentTags.TRADEABLE)) {
            return registry.getRandomElementOf(tag, random);
        }
        List<Holder<Enchantment>> pool = new ArrayList<>();
        for (Holder<Enchantment> holder : registry.getTagOrEmpty(tag)) {
            pool.add(holder);
        }
        for (String id : COF_EXTRA_BOOKS) {
            registry.get(ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.withDefaultNamespace(id)))
                    .ifPresent(pool::add);
        }
        if (pool.isEmpty()) {
            return registry.getRandomElementOf(tag, random);
        }
        return Optional.of(pool.get(random.nextInt(pool.size())));
    }
}
