// SPDX-License-Identifier: MIT
// Copyright (c) 2026 CoF contributors

package carpet.cof.compat;

import carpet.cof.CauldronFreezeData;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.UUID;

/**
 * 版本适配层：<b>clusterA</b> 实现（MC 1.21 ~ 1.21.4）。
 *
 * <p>与 {@code src/clusterB/java/carpet/cof/compat/Compat.java} <b>同名同包</b>，
 * 由 {@code build.gradle} 按 {@code minecraft_version} 二选一加入源码集，
 * 因此每次构建的类路径上<b>只会存在一套</b>（无死代码、无重复类）。</p>
 */
public final class Compat {
    private Compat() {
    }

    // ---------- NBT：取值 ----------

    public static int getInt(CompoundTag tag, String key, int def) {
        return tag.contains(key, Tag.TAG_INT) ? tag.getInt(key) : def;
    }

    public static long getLong(CompoundTag tag, String key, long def) {
        return tag.contains(key, Tag.TAG_LONG) ? tag.getLong(key) : def;
    }

    public static String getString(CompoundTag tag, String key, String def) {
        return tag.contains(key, Tag.TAG_STRING) ? tag.getString(key) : def;
    }

    public static boolean getBoolean(CompoundTag tag, String key, boolean def) {
        return tag.contains(key, Tag.TAG_BYTE) ? tag.getBoolean(key) : def;
    }

    /** 不存在时返回 {@code null}（与 clusterB 的语义对齐）。 */
    public static CompoundTag getCompound(CompoundTag tag, String key) {
        return tag.contains(key, Tag.TAG_COMPOUND) ? tag.getCompound(key) : null;
    }

    /** 不存在时返回空数组（不返回 null ✓）。 */
    public static long[] getLongArray(CompoundTag tag, String key) {
        return tag.contains(key, Tag.TAG_LONG_ARRAY) ? tag.getLongArray(key) : new long[0];
    }

    /** 不存在时返回空列表 ✓。 */
    public static ListTag getCompoundList(CompoundTag tag, String key) {
        return tag.getList(key, Tag.TAG_COMPOUND);
    }

    public static boolean hasCompound(CompoundTag tag, String key) {
        return tag.contains(key, Tag.TAG_COMPOUND);
    }

    public static boolean hasLongArray(CompoundTag tag, String key) {
        return tag.contains(key, Tag.TAG_LONG_ARRAY);
    }

    public static boolean hasString(CompoundTag tag, String key) {
        return tag.contains(key, Tag.TAG_STRING);
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
        return MobEffects.DAMAGE_BOOST;
    }

    // ---------- 村民职业 ----------

    public static VillagerProfession professionOf(VillagerData data) {
        return data.getProfession();
    }

    /** 1.21 ~ 1.21.4：VillagerData 的等级方法叫 setLevel。 */
    public static VillagerData withLevel(VillagerData data, int level) {
        return data.setLevel(level);
    }

    public static VillagerData withProfession(VillagerData data, VillagerProfession profession) {
        return data.setProfession(profession);
    }

    public static int[] getIntArray(CompoundTag tag, String key) {
        return tag.contains(key, Tag.TAG_INT_ARRAY) ? tag.getIntArray(key) : new int[0];
    }

    /** 读取列表里第 i 个复合标签（越界返回 null ✓）。 */
    public static CompoundTag getCompoundAt(ListTag list, int i) {
        return i >= 0 && i < list.size() ? list.getCompound(i) : null;
    }

    /** 村民职业的注册表 id（拿不到返回 null ✓）—— 跨版本比较职业请用它，别比常量值 ✓。 */
    public static ResourceLocation professionId(VillagerData data) {
        return BuiltInRegistries.VILLAGER_PROFESSION.getKey(data.getProfession());
    }

    /** 按职业值取注册表 id ✓。 */
    public static ResourceLocation professionId(VillagerProfession profession) {
        return BuiltInRegistries.VILLAGER_PROFESSION.getKey(profession);
    }

    /** 按注册表 id 设置职业（id 无效时原样返回 ✓）。 */
    public static VillagerData withProfessionById(VillagerData data, ResourceLocation id) {
        VillagerProfession profession = BuiltInRegistries.VILLAGER_PROFESSION.getOptional(id).orElse(null);
        return profession == null ? data : data.setProfession(profession);
    }
    // ---------- 存档数据：炼药锅结冰计数（旧写法：SavedData.Factory + 手写 NBT）----------

    private static final SavedData.Factory<CauldronFreezeData> COF_FREEZE_FACTORY =
            new SavedData.Factory<>(CauldronFreezeData::new,
                    (tag, registries) -> CauldronFreezeData.fromTag(tag),
                    DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    /** 取（必要时创建）该维度的结冰计数 —— 只在真的要写入时使用。 */
    public static CauldronFreezeData getFreezeData(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(COF_FREEZE_FACTORY, CauldronFreezeData.FILE_ID);
    }

    /** 只读取、<b>不创建</b>（该维度还没有记录时返回 {@code null}）。 */
    public static CauldronFreezeData getFreezeDataIfPresent(ServerLevel level) {
        return level.getDataStorage().get(COF_FREEZE_FACTORY, CauldronFreezeData.FILE_ID);
    }
}
