// SPDX-License-Identifier: MIT
// Copyright (c) 2026 CoF contributors

package carpet.cof.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;

/**
 * 「该处降水形态是雪」的纯气候判断 —— <b>1.21.5 ~ 1.21.11 版</b>。
 *
 * <p>其余说明见 clusterA 里的同名类。</p>
 */
public final class SnowCheck {
    private SnowCheck() {
    }

    public static boolean snowsHere(ServerLevel level, BlockPos pos) {
        return level.getBiome(pos).value().getPrecipitationAt(pos, level.getSeaLevel()) == Biome.Precipitation.SNOW;
    }
}
