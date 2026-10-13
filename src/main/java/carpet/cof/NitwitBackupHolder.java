// SPDX-License-Identifier: MIT
// Copyright (c) 2026 CoF contributors

package carpet.cof;

import net.minecraft.nbt.CompoundTag;

/**
 * 「傻子可逆转化」备份标签的跨实体访问接口。
 *
 * <p>备份由村民持有（`@Unique` 字段），但"村民 → 僵尸村民 → 村民"这条往返链路上，
 * 搬运发生在**另一个类**的转化 lambda 里，无法直接访问那个字段，</p>
 *
 * <p>因此由村民与僵尸村民两侧的 mixin 各自实现本接口，转化 lambda 中通过它传递备份。</p>
 */
public interface NitwitBackupHolder {

    /** 读取备份（可能为 null：该实体不是由本规则转来的）。 */
    CompoundTag cof$getNitwitBackup();

    /** 写入备份。 */
    void cof$setNitwitBackup(CompoundTag backup);
}
