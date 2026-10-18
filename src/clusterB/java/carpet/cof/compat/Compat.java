// SPDX-License-Identifier: MIT
// Copyright (c) 2026 CoF contributors

package carpet.cof.compat;

import carpet.cof.CauldronFreezeData;
import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.UUID;

/**
 * 版本适配层：<b>clusterB</b> 实现（MC 1.21.5 起）。
 *
 * <p>与 {@code src/clusterA/java/carpet/cof/compat/Compat.java} <b>同名同包</b>，
 * 由 {@code build.gradle} 按 {@code minecraft_version} 二选一加入源码集。</p>
 *
 * <p>本簇用 1.21.5 重构后的新 API：NBT 取值 {@code getXxxOr}、职业为 {@code Holder}、
 * 力量效果叫 {@code STRENGTH}、存档数据改为 {@code SavedDataType} + {@code Codec}。</p>
 */
public final class Compat {
    private Compat() {
    }

    // ---------- NBT：取值 ----------

    public static int getInt(CompoundTag tag, String key, int def) {
        return tag.getIntOr(key, def);
    }

    public static long getLong(CompoundTag tag, String key, long def) {
        return tag.getLongOr(key, def);
    }

    public static String getString(CompoundTag tag, String key, String def) {
        return tag.getStringOr(key, def);
    }

    public static boolean getBoolean(CompoundTag tag, String key, boolean def) {
        return tag.getBooleanOr(key, def);
    }

    /** 不存在时返回 {@code null}（与 clusterA 的语义对齐）。 */
    public static CompoundTag getCompound(CompoundTag tag, String key) {
        return tag.contains(key) ? tag.getCompoundOrEmpty(key) : null;
    }

    /** 不存在时返回空数组（不返回 null ✓）。 */
    public static long[] getLongArray(CompoundTag tag, String key) {
        return tag.getLongArray(key).orElse(new long[0]);
    }

    /** 不存在时返回空列表 ✓。 */
    public static ListTag getCompoundList(CompoundTag tag, String key) {
        return tag.getList(key).orElse(new ListTag());
    }

    public static boolean hasCompound(CompoundTag tag, String key) {
        return tag.getCompound(key).isPresent();
    }

    public static boolean hasLongArray(CompoundTag tag, String key) {
        return tag.getLongArray(key).isPresent();
    }

    public static boolean hasString(CompoundTag tag, String key) {
        return tag.getString(key).isPresent();
    }

    // ---------- NBT：UUID（两簇统一用字符串存，避免版本差异）----------

    public static void putUuid(CompoundTag tag, String key, UUID id) {
        if (id != null) {
            tag.putString(key, id.toString());
        }
    }

    public static UUID getUuid(CompoundTag tag, String key) {
        String raw = getString(tag, key, null);
        if (raw == null) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // ---------- 效果 ----------

    /** 发光效果（/cofdebug poi 用来高亮"绑定该工作方块的村民"）。 */
    public static Holder<MobEffect> glowing() {
        return MobEffects.GLOWING;
    }

    /** POI 类型的注册表 id（诊断命令显示用）。 */
    public static String poiTypeId(Holder<PoiType> type) {
        return type.unwrapKey().map(key -> key.location().toString()).orElse("?");
    }

    public static Holder<MobEffect> strength() {
        return MobEffects.STRENGTH;
    }

    // ---------- 村民职业 ----------

    public static VillagerProfession professionOf(VillagerData data) {
        return data.profession().value();
    }

    /** 1.21.5+：VillagerData 的等级方法改名 withLevel。 */
    public static VillagerData withLevel(VillagerData data, int level) {
        return data.withLevel(level);
    }

    public static VillagerData withProfession(VillagerData data, VillagerProfession profession) {
        return data.withProfession(net.minecraft.core.registries.BuiltInRegistries.VILLAGER_PROFESSION.wrapAsHolder(profession));
    }

    public static int[] getIntArray(CompoundTag tag, String key) {
        return tag.getIntArray(key).orElse(new int[0]);
    }

    /** 读取列表里第 i 个复合标签（越界返回 null ✓）。 */
    public static CompoundTag getCompoundAt(ListTag list, int i) {
        return i >= 0 && i < list.size() ? list.getCompound(i).orElse(null) : null;
    }

    /** 村民职业的注册表 id（拿不到返回 null ✓）—— 跨版本比较职业请用它，别比常量值 ✓。 */
    public static ResourceLocation professionId(VillagerData data) {
        return data.profession().unwrapKey().map(ResourceKey::location).orElse(null);
    }

    /** 按职业值取注册表 id ✓。 */
    public static ResourceLocation professionId(VillagerProfession profession) {
        return BuiltInRegistries.VILLAGER_PROFESSION.getKey(profession);
    }

    /** 按注册表 id 设置职业（id 无效时原样返回 ✓）。 */
    public static VillagerData withProfessionById(VillagerData data, ResourceLocation id) {
        return BuiltInRegistries.VILLAGER_PROFESSION
                .getOptional(ResourceKey.create(Registries.VILLAGER_PROFESSION, id))
                .map(profession -> data.withProfession(
                        BuiltInRegistries.VILLAGER_PROFESSION.wrapAsHolder(profession)))
                .orElse(data);
    }
    // ---------- 存档数据：炼药锅结冰计数（新写法：SavedDataType + Codec）----------

    private static final Codec<CauldronFreezeData> COF_FREEZE_CODEC =
            CompoundTag.CODEC.xmap(CauldronFreezeData::fromTag, CauldronFreezeData::toTag);

    private static final SavedDataType<CauldronFreezeData> COF_FREEZE_TYPE =
            new SavedDataType<>(CauldronFreezeData.FILE_ID, CauldronFreezeData::new,
                    COF_FREEZE_CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    /** 取（必要时创建）该维度的结冰计数 —— 只在真的要写入时使用。 */
    public static CauldronFreezeData getFreezeData(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(COF_FREEZE_TYPE);
    }

    /** 只读取、<b>不创建</b>（该维度还没有记录时返回 {@code null}）。 */
    public static CauldronFreezeData getFreezeDataIfPresent(ServerLevel level) {
        return level.getDataStorage().get(COF_FREEZE_TYPE);
    }
}
