// SPDX-License-Identifier: GPL-3.0-only
// Copyright (c) 2026 CoF contributors

package carpet.cof;

import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * 「每个炼药锅冻过几次」的持久化记录（规则 {@code cauldronFreezingProduct} 用）。
 *
 * <h2>为什么需要一个存档数据</h2>
 *
 * <p>分阶段产出（第 1 次不给 → 第 2 次浮冰 → 第 3 次起蓝冰）需要记住"这个锅冻过几次"。
 * 但原版炼药锅既没有方块实体、方块状态里也没有空闲字段，而本模组又刻意不注册任何新方块 ——
 * 所以把计数放进<b>世界存档的 SavedData</b>：不新增任何注册项，方块本身保持 100% 原版。</p>
 *
 * <h2>存储开销</h2>
 *
 * <ul>
 *   <li>内存：fastutil 紧凑 map，约 16~20 字节/条</li>
 *   <li>存档：两个平行数组（坐标 long[] + 计数 int[]），约 12 字节/条；
 *       而且写入时不会为每条记录创建临时对象</li>
 *   <li>只有<b>真的结冰过</b>的位置才会被记录 —— 摆一堆锅但环境不满足，不产生任何数据</li>
 * </ul>
 *
 * <p>键是方块坐标（打包成 long）。每个维度各存一份：
 * {@code world/data/cof_cauldron_freezes.dat}。卸载模组后它会被忽略（孤儿数据文件），
 * <b>不会</b>造成未定义方块。</p>
 */
public class CauldronFreezeData extends SavedData {
    private static final String FILE_ID = "cof_cauldron_freezes";
    private static final String KEY_POS = "Pos";
    private static final String KEY_COUNT = "Count";
    /** 旧格式（早期版本用过）：Entries 列表，每条一个 CompoundTag。只用于向下兼容读取。 */
    private static final String KEY_ENTRIES = "Entries";

    private static final SavedData.Factory<CauldronFreezeData> FACTORY =
            new SavedData.Factory<>(CauldronFreezeData::new, CauldronFreezeData::load,
                    DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private final Long2IntMap counts = new Long2IntOpenHashMap();

    /** 取（必要时创建）该维度的记录 —— 只在真的要写入时使用。 */
    public static CauldronFreezeData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, FILE_ID);
    }

    /**
     * 只读取、<b>不创建</b>。
     *
     * <p>清理路径（随机刻碰到不是水炼药锅的位置、玩家用桶舀水、玩家挖锅）用它：否则一个从来没有
     * 冻过任何锅的世界，也会因为"清理"这个动作被建出一个空的数据文件。</p>
     *
     * @return 该维度还没有记录时返回 {@code null}
     */
    public static CauldronFreezeData getIfPresent(ServerLevel level) {
        return level.getDataStorage().get(FACTORY, FILE_ID);
    }

    /**
     * 便捷方法：该位置有预热记录就清掉它。
     *
     * <p>不存在时什么都不做，而且**不会创建数据文件**（走 {@link #getIfPresent}）。</p>
     */
    public static void clearIfPresent(ServerLevel level, BlockPos pos) {
        CauldronFreezeData data = getIfPresent(level);
        if (data != null) {
            data.remove(pos);
        }
    }

    /** 该位置已经满足过几次结冰条件（0 表示还没冻过）。 */
    public int get(BlockPos pos) {
        return counts.getOrDefault(pos.asLong(), 0);
    }

    /** 设置计数；设为 0 或负数等于清除该位置。 */
    public void set(BlockPos pos, int count) {
        if (count <= 0) {
            if (counts.remove(pos.asLong()) != 0) {
                setDirty();
            }
            return;
        }
        if (counts.put(pos.asLong(), count) != count) {
            setDirty();
        }
    }

    /** 清除该位置的记录（锅被拆掉、或那里已经不是炼药锅了）。 */
    public void remove(BlockPos pos) {
        if (counts.remove(pos.asLong()) != 0) {
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        // 两个平行数组：每条只占 8+4 字节，且写入过程中不创建任何临时对象
        //（早先用的是"每条一个 CompoundTag"的列表，数量大时会在存档瞬间制造大量垃圾）
        int size = counts.size();
        long[] positions = new long[size];
        int[] values = new int[size];
        int i = 0;
        for (Long2IntMap.Entry entry : counts.long2IntEntrySet()) {
            positions[i] = entry.getLongKey();
            values[i] = entry.getIntValue();
            i++;
        }
        tag.putLongArray(KEY_POS, positions);
        tag.putIntArray(KEY_COUNT, values);
        return tag;
    }

    private static CauldronFreezeData load(CompoundTag tag, HolderLookup.Provider registries) {
        CauldronFreezeData data = new CauldronFreezeData();

        // 当前格式：两个平行数组
        if (tag.contains(KEY_POS, Tag.TAG_LONG_ARRAY)) {
            long[] positions = tag.getLongArray(KEY_POS);
            int[] values = tag.getIntArray(KEY_COUNT);
            int size = Math.min(positions.length, values.length);
            for (int i = 0; i < size; i++) {
                data.counts.put(positions[i], values[i]);
            }
            return data;
        }

        // 兼容旧格式：Entries 列表
        ListTag list = tag.getList(KEY_ENTRIES, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag one = list.getCompound(i);
            data.counts.put(one.getLong(KEY_POS), one.getInt(KEY_COUNT));
        }
        return data;
    }
}
