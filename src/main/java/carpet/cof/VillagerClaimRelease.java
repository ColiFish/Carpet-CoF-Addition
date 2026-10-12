// SPDX-License-Identifier: MIT
// Copyright (c) 2026 CoF contributors

package carpet.cof;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 「新手村民交易不重置」用的临时标记。
 *
 * <p>用来区分"认领是怎么丢的"：</p>
 * <ul>
 *   <li><b>村民自己弄丢</b>（够不到工作站、被抢占）：{@code SetWalkTargetFromBlockMemory} 会调用
 *       {@code villager.releasePoi(JOB_SITE)} —— 由 {@link carpet.cof.mixin.VillagerReleasePoiMixin} 在此记录时间。</li>
 *   <li><b>玩家拆掉方块</b>：{@code ValidateNearbyPoi} 发现该位置没有对应 POI 后只做
 *       {@code memory.erase()}，<b>不会</b>调用 {@code releasePoi} —— 因此没有标记。</li>
 * </ul>
 *
 * <p>重置发生时若标记仍然"新鲜"，说明是村民自己弄丢的 → 拦下；否则放行（职业按原版清零，可手动重刷）。
 * 这样就不会被"附近还有别的同职业工作方块"误导。</p>
 */
public final class VillagerClaimRelease {

    /** 记录多少 tick 内算"刚刚自己释放过认领"（1200 tick = 60 秒）。 */
    public static final long FRESH_TICKS = 1200L;

    private static final Map<UUID, Long> LAST_RELEASE = new ConcurrentHashMap<>();

    private VillagerClaimRelease() {
    }

    public static void mark(UUID uuid, long gameTime) {
        LAST_RELEASE.put(uuid, gameTime);
    }

    public static boolean isFresh(UUID uuid, long gameTime) {
        Long at = LAST_RELEASE.get(uuid);
        if (at == null) {
            return false;
        }
        long delta = gameTime - at;
        return delta >= 0 && delta <= FRESH_TICKS;
    }
}
