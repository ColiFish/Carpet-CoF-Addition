// SPDX-License-Identifier: MIT
// Copyright (c) 2026 CoF contributors

package carpet.cof.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;

/**
 * 「该处降水形态是雪」的纯气候判断 —— <b>1.21.2 ~ 1.21.4 版</b>。
 *
 * <p>不要用 {@code Biome#shouldSnow}：它除了气候还会要求<b>坐标上的方块是空气或雪层</b>，
 * 而调用方传进来的坐标上放的就是炼药锅，会恒判为"非雪"。</p>
 *
 * <p>1.21.2 起 {@code getPrecipitationAt} 多了海平面参数，1.21 / 1.21.1 还是单参数版本，
 * 所以这个判断按源码集分三份（clusterA / clusterA21 / clusterB）。</p>
 */
public final class SnowCheck {
    private SnowCheck() {
    }

    public static boolean snowsHere(ServerLevel level, BlockPos pos) {
        return level.getBiome(pos).value().getPrecipitationAt(pos, level.getSeaLevel()) == Biome.Precipitation.SNOW;
    }
}
