// SPDX-License-Identifier: MIT
// Copyright (c) 2026 CoF contributors

package carpet.cof;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * 结冰判定与执行。
 *
 * <h2>结冰的结果：每产出一次都要重新预热</h2>
 *
 * <p>规则 {@code cauldronFreezingProduct} 决定产出什么，同时决定"每产出 1 个需要满足几次结冰"：</p>
 *
 * <table border="1">
 *   <caption>产物与节奏</caption>
 *   <tr><th>产物</th><th>第 1 次</th><th>第 2 次</th><th>第 3 次</th><th>第 4 次</th><th>节奏</th></tr>
 *   <tr><td>{@code ice}（默认）</td><td><b>产出冰</b></td><td>冰</td><td>冰</td><td>冰</td><td>每次都给</td></tr>
 *   <tr><td>{@code packed_ice}</td><td>不产出</td><td><b>产出浮冰</b></td><td>不产出</td><td>浮冰</td><td>每 2 次给 1 个</td></tr>
 *   <tr><td>{@code blue_ice}</td><td>不产出</td><td>不产出</td><td><b>产出蓝冰</b></td><td>不产出</td><td>每 3 次给 1 个</td></tr>
 * </table>
 *
 * <p><b>每次产出后计数都会清零</b>，所以下一个产物需要重新预热：`packed_ice` 交替"不给/给"，
 * `blue_ice` 是"不给、不给、给"的循环。预热阶段不产出任何东西、<b>水保持不动</b>
 * （锅仍是满水锅，下一次检测继续累计）；产出的那一次会把锅清空（水被消耗，同原版水源结冰）。</p>
 *
 * <p>预热进度按方块坐标持久化在 {@link CauldronFreezeData} 里（每个维度一份，重启后继续），
 * 只在<b>预热进行中</b>才存在记录 —— 产出即清零（记录被删掉），所以数据量天然很小。
 * {@code ice} 的阈值是 1、计数毫无作用，因此这种产物完全不碰计数与存档数据。</p>
 *
 * <p><b>本模组不注册任何新方块或物品。</b>产物都是原版物品，卸载后存档里不会留下未定义方块。</p>
 *
 * <h2>判定条件（按需求）</h2>
 * <ol>
 *   <li><b>不要求当前正在降雪</b> —— 条件完全由位置决定</li>
 *   <li><b>在可降雪区域</b>：{@code Biome#getPrecipitationAt(pos, seaLevel) == SNOW}</li>
 *   <li><b>降雪能落到这个位置</b>：只检查位于本列顶面（{@code MOTION_BLOCKING} 高度图）的锅，
 *       再叠加 {@code canSeeSky(pos.above())}，所以屋顶下、玻璃/树叶下面的锅不会被冻上</li>
 *   <li><b>该位置方块光 &lt; 10</b>（与原版水结冰一致，看的是方块光，所以白天不阻碍）</li>
 * </ol>
 *
 * <h2>检测频率：与原版降水循环同结构、同概率门</h2>
 *
 * <pre>
 * for (int i = 0; i &lt; randomTickSpeed; i++) {          // 默认 3 次，不是 16
 *     if (this.random.nextInt(48) == 0) {             // 再乘 1/48 概率门
 *         this.tickPrecipitation(this.getBlockRandomPos(minX, 0, minZ, 15));
 *     }
 * }
 * </pre>
 *
 * <p>也就是某列每 tick 被检测到的概率是 1/4096（期望约 205 秒轮到一次）。
 * 本类复刻同一个循环结构与 1/48 概率门（{@code cauldronFreezingChance} 就是那个分母），
 * 与那段代码有两处刻意的差别：去掉外面 {@code if (isRaining())} 那层（这是需求），
 * 以及取坐标时只抽 x、z 两个随机数（y 对本模组无意义，原版会连 y 一起抽）。</p>
 *
 * <p><b>曾经的错误（已修）</b>：早先写成"每区块 tick 抽查 4 列"（= 1/64，比原版快 64 倍），
 * 那就是"结冰速率明显异常"的原因。</p>
 */
public final class CauldronFreezing {

    /** 一次结冰检查的结果。区分"仅累计"与"真产出"，诊断命令需要如实汇报。 */
    public enum FreezeResult {
        /** 条件不满足，或该位置已经不是水炼药锅。 */
        NONE,
        /** 条件满足，但当前产物需要预热，这一次只是累计，没有产出。 */
        WARMED,
        /** 条件满足并真的产出了产物（锅被清空 + 掉出产物）。 */
        PRODUCED
    }

    private CauldronFreezing() {
    }

    /**
     * 区块 tick 时调用。
     *
     * @param randomTickSpeed 区块 tick 的第二个参数（即 {@code randomTickSpeed} 游戏规则），
     *                        原版就是拿它当循环次数，这里保持一致
     */
    public static void tickChunk(ServerLevel level, LevelChunk chunk, int randomTickSpeed) {
        if (!CofSettings.waterCauldronFreezing) {
            return;
        }
        ChunkPos chunkPos = chunk.getPos();
        int minX = chunkPos.getMinBlockX();
        int minZ = chunkPos.getMinBlockZ();

        // 与原版同样的结构：randomTickSpeed 次迭代 + 一次概率判定。
        // 概率门的默认值就是原版的 1/48，可用规则 cauldronFreezingChance 调整（值即分母 N）。
        // 钳到 >= 1：nextInt(1) 恒为 0，也就是"每次迭代都检测"，非法值不会崩。
        int chance = Math.max(1, CofSettings.cauldronFreezingChance);
        for (int i = 0; i < randomTickSpeed; i++) {
            if (level.random.nextInt(chance) != 0) {
                continue;
            }
            // y 无所谓：高度图只看 x/z
            BlockPos any = new BlockPos(minX + level.random.nextInt(16), 0,
                    minZ + level.random.nextInt(16));
            checkColumn(level, any);
        }
    }

    /**
     * 这一列上"真正会被处理的那口锅"：先看地表那一格，再看它下面一格；都不是水炼药锅就返回 {@code null}。
     *
     * <p>炼药锅是空心形状，会不会被算进 {@code MOTION_BLOCKING} 取决于碰撞盒，所以这两格都要看。
     * 诊断命令用它与实际处理目标保持一致（不必自己再猜一遍）。</p>
     */
    public static BlockPos findCauldron(ServerLevel level, BlockPos anyPosInColumn) {
        BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, anyPosInColumn);
        if (level.getBlockState(surface).is(Blocks.WATER_CAULDRON)) {
            return surface;
        }
        BlockPos below = surface.below();
        if (level.getBlockState(below).is(Blocks.WATER_CAULDRON)) {
            return below;
        }
        return null;
    }

    /**
     * 检查"这一列顶部的炼药锅"能不能结冰。传入该列任意一个坐标即可。
     *
     * @return 这一列实际发生了什么（诊断命令用它如实汇报"仅累计"还是"真产出"）
     */
    public static FreezeResult checkColumn(ServerLevel level, BlockPos anyPosInColumn) {
        // 和原版一样：把任意高度换算成这一列的地表位置
        BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, anyPosInColumn);

        // 地表那一格和它下面那一格都试一遍，两种情况都能覆盖
        FreezeResult atSurface = tryFreeze(level, surface);
        if (atSurface != FreezeResult.NONE) {
            return atSurface;
        }
        return tryFreeze(level, surface.below());
    }

    /**
     * 每产出 1 个产物需要满足几次结冰（也就等于预热次数）。
     *
     * <p>包级可见：调试命令的只读诊断也要显示这个阈值，共用一份避免两处漂移。</p>
     */
    static int threshold(String mode) {
        if (mode != null && mode.equalsIgnoreCase("packed_ice")) {
            return 2;
        }
        if (mode != null && mode.equalsIgnoreCase("blue_ice")) {
            return 3;
        }
        return 1;   // ice（默认）以及任何无法识别的值
    }

    /** 该规则值对应产出什么产物。 */
    private static Item product(String mode) {
        if (mode != null && mode.equalsIgnoreCase("packed_ice")) {
            return Items.PACKED_ICE;
        }
        if (mode != null && mode.equalsIgnoreCase("blue_ice")) {
            return Items.BLUE_ICE;
        }
        return Items.ICE;
    }

    private static FreezeResult tryFreeze(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(Blocks.WATER_CAULDRON)) {
            // 只要这个位置不是"水炼药锅"，预热进度就作废：被挖掉、被舀走、换成岩浆锅/细雪锅/
            // 空锅、被其它方块替换……全都算中断。
            // 用 clearIfPresent：清理这个动作不该把一个空的数据文件"创建"出来。
            CauldronFreezeData.clearIfPresent(level, pos);
            return FreezeResult.NONE;
        }
        if (CofSettings.cauldronFreezingRequiresFullWater
                && state.getValue(LayeredCauldronBlock.LEVEL) != LayeredCauldronBlock.MAX_FILL_LEVEL) {
            // 当前规则下这口锅冻不了，预热进度也就没有意义 → 一并作废
            //（满水规则关掉时不会走到这里——任意水位都还能继续累计）
            CauldronFreezeData.clearIfPresent(level, pos);
            return FreezeResult.NONE;
        }
        if (!canFreezeHere(level, pos)) {
            return FreezeResult.NONE;
        }

        String mode = CofSettings.cauldronFreezingProduct;
        int threshold = threshold(mode);

        // 需要预热的产物：累计次数，达标那一次产出并把计数清零（所以每次产出都要重新预热）。
        // ice 的阈值是 1、计数完全没有作用，所以这种产物压根不碰计数与存档数据。
        if (threshold > 1) {
            CauldronFreezeData data = CauldronFreezeData.get(level);
            int count = data.get(pos) + 1;
            if (count < threshold) {
                data.set(pos, count);   // 只记进度，不产出（水保持不动）
                return FreezeResult.WARMED;
            }
            data.set(pos, 0);           // 产出即清零（set(0) 会直接删掉这个位置的记录）
        }

        // 达产：锅清空 + 锅中心掉出该产物
        level.setBlockAndUpdate(pos, Blocks.CAULDRON.defaultBlockState());
        dropItem(level, pos, product(mode));
        return FreezeResult.PRODUCED;
    }

    /**
     * 在炼药锅中心生成 1 个掉落物。
     *
     * <h2>竖向位置：正好贴住锅内壁底面</h2>
     *
     * <p>原版炼药锅内部对实体是有"地板"的：实测把实体放进锅里，它的碰撞箱底面稳定停在
     * <b>{@code 方块Y + 0.25}</b>（4/16 格），既不会悬空也不会穿过锅底。所以这里直接生成在
     * {@code +0.25} 上 —— 掉落物一出现就已经静止（初速度为 0），不会先往下掉一小段。</p>
     *
     * <p>水平方向取方块中心（{@code +0.5}）：{@code Entity} 的坐标是碰撞箱底面中心。</p>
     */
    private static void dropItem(ServerLevel level, BlockPos pos, Item item) {
        ItemEntity drop = new ItemEntity(level,
                pos.getX() + 0.5, pos.getY() + 0.25, pos.getZ() + 0.5,
                new ItemStack(item));
        drop.setDefaultPickUpDelay();
        level.addFreshEntity(drop);
    }

    /** 位置相关的那部分条件（天气不参与）。 */
    private static boolean canFreezeHere(ServerLevel level, BlockPos pos) {
        // 1) 可降雪区域：该处降水形态是雪（不要求此刻正在下雪）
        Biome biome = level.getBiome(pos).value();
        if (biome.getPrecipitationAt(pos, level.getSeaLevel()) != Biome.Precipitation.SNOW) {
            return false;
        }
        // 2) 降雪能落到这个位置：头顶露天、没有遮挡
        if (!level.canSeeSky(pos.above())) {
            return false;
        }
        // 3) 方块光照 < 10（与原版水结冰一致）
        return level.getBrightness(LightLayer.BLOCK, pos) < 10;
    }
}
