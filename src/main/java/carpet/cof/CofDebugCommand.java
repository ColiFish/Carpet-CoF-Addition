// SPDX-License-Identifier: MIT
// Copyright (c) 2026 CoF contributors

package carpet.cof;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.npc.Villager;
import java.util.Comparator;
import java.util.List;

/**
 * 诊断命令。
 *
 * <h2>输出规范</h2>
 *
 * <p>每条命令都是这个布局 —— 两格缩进 + 灰色标签（补齐对齐）+ 值：</p>
 *
 * <pre>
 * [cof] /cofdebug tick  8, -59, 8     ← 金色标题：命令名 + 参数
 *   步进     1 次结冰检查
 *   累计     1 / 3
 *   本次检查 结冰了
 *
 * [cof] /cofdebug data  8, -59, 8    ← 只读：该坐标处的完整条件都在这条命令里
 *   规则     水锅结冰:true  满水要求:true  产物=blue_ice  结冰速率:48
 *   位置     8, -59, 8（该列地表）
 *   方块     minecraft:water_cauldron
 *   水位     3
 *   水位条件 需要满水
 *   群系     minecraft:snowy_plains
 *   降水     SNOW
 *   露天     是
 *   方块光   0
 *   累计     1 / 3
 *   产物     blue_ice（每 3 次产出 1 个）
 *   下次结冰  仍不产出
 * </pre>
 *
 * <p>没有勾叉符号：条件是否满足、对比是否一致，一律靠**颜色**表达
 * （满足/一致 = 绿，不满足 = 红，次要信息 = 深灰）。</p>
 *
 * <p>结果统一用 {@code sendSuccess} 回给执行者（玩家或 RCON 控制台都能直接读到，
 * 不依赖"有玩家在线"），同时以纯文本写进服务端日志。</p>
 */
public final class CofDebugCommand {
    private CofDebugCommand() {
    }

    // ---------------------------------------------------------------- 样式

    private static final Style HDR = Style.EMPTY.withColor(ChatFormatting.GOLD);
    private static final Style CMD = Style.EMPTY.withColor(ChatFormatting.YELLOW);
    private static final Style KEY = Style.EMPTY.withColor(ChatFormatting.GRAY);
    private static final Style VAL = Style.EMPTY.withColor(ChatFormatting.WHITE);
    private static final Style OK = Style.EMPTY.withColor(ChatFormatting.GREEN);
    private static final Style BAD = Style.EMPTY.withColor(ChatFormatting.RED);
    private static final Style SUB = Style.EMPTY.withColor(ChatFormatting.GRAY);
    private static final Style ARROW = Style.EMPTY.withColor(ChatFormatting.AQUA);

    private static MutableComponent nl() {
        return Component.literal("\n");
    }

    /** 标题行：[cof] /cofdebug <子命令>  <参数> */
    private static MutableComponent header(String sub, String arg) {
        return Component.literal("[cof] ").withStyle(HDR)
                .append(Component.literal("/cofdebug ").withStyle(CMD))
                .append(Component.literal(sub).withStyle(CMD))
                .append(arg.isEmpty() ? Component.empty() : Component.literal("  " + arg).withStyle(KEY));
    }

    /** 一行明细：两格缩进 + 灰色标签（补齐到 8 格）+ 值。 */
    private static MutableComponent row(String label, String value, Style valueStyle) {
        return Component.literal("  ").append(Component.literal(pad(label)).withStyle(KEY))
                .append(Component.literal(value).withStyle(valueStyle));
    }

    /** 全角字符按 2 格算，把标签补齐到 8 格，再统一加一格分隔 —— 保证标签与值不会粘在一起。 */
    private static String pad(String label) {
        int width = 0;
        for (int i = 0; i < label.length(); i++) {
            width += isWide(label.charAt(i)) ? 2 : 1;
        }
        StringBuilder sb = new StringBuilder(label);
        for (int i = width; i < 8; i++) {
            sb.append(' ');
        }
        return sb.append(' ').toString();
    }

    /**
     * 这个字符在游戏字体里占 2 格吗（用于列对齐）。
     *
     * <p>只把 CJK / 全角区段算作 2 格。此前用的是 {@code charAt(i) > 0x2000}，那会把
     * U+2001~U+206F 的通用标点（{@code …} U+2026、{@code —} U+2014、{@code •} U+2022 等）
     * 也当成全角，将来有标签用到它们就会少补一格、右侧的值错位。</p>
     */
    private static boolean isWide(char c) {
        return (c >= 0x1100 && c <= 0x115F)     // 谚文字母
                || (c >= 0x2E80 && c <= 0xA4CF) // CJK 部首 / 假名 / 注音 / 统一表意文字
                || (c >= 0xAC00 && c <= 0xD7A3) // 谚文音节
                || (c >= 0xF900 && c <= 0xFAFF) // CJK 兼容表意文字
                || (c >= 0xFE30 && c <= 0xFE6F) // CJK 兼容形式
                || (c >= 0xFF00 && c <= 0xFF60) // 全角形式
                || (c >= 0xFFE0 && c <= 0xFFE6); // 全角符号
    }

    private static Style mark(boolean ok) {
        return ok ? OK : BAD;
    }

    /** 是/否只靠颜色区分（绿/红），不加 ✓/✗ 符号。 */
    private static String yes(boolean ok) {
        return ok ? "是" : "否";
    }

    // ---------------------------------------------------------------- 注册

    /**
     * 权限判定：读取规则 {@code cofDebugCommand}。
     *
     * <p>{@code false} = 关闭；数字 = 所需的权限等级。命令解析时都会重新判定，
     * 所以在游戏里改规则立刻生效。<b>解析失败或出现负数时一律视为关闭</b>（fail-closed），不会静默放宽成"需要 OP"；
     * 等级超过 4 时按 4 处理。</p>
     */
    private static boolean isEnabledFor(CommandSourceStack source) {
        String mode = CofSettings.cofDebugCommand;
        if (mode == null || mode.equalsIgnoreCase("false")) {
            return false;
        }
        int required;
        try {
            required = Integer.parseInt(mode.trim());
        } catch (NumberFormatException e) {
            // 规则只允许 false 或 0~4。出现别的值说明配置文件被手工改过，
            // 这时按"关闭"处理（fail-closed）：静默放宽成 2 级可用会让权限形同虚设。
            return false;
        }
        if (required < 0) {
            return false;   // 负数会让 hasPermission(-1) 恒为真，等于人人可用
        }
        return source.hasPermission(Math.min(required, 4));
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("cofdebug")
                .requires(CofDebugCommand::isEnabledFor);

        // ---- /cofdebug tick <pos>：步进一次检查，只报累计与结果（条件复述见 data）
        root.then(Commands.literal("tick")
                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .executes(context -> {
                            ServerLevel level = context.getSource().getLevel();
                            BlockPos pos = BlockPosArgument.getBlockPos(context, "pos");

                            // 这条命令会真实改世界（抽干水、掉产物），所以总开关关闭时直接拒绝执行：
                            // 否则容易让人误以为"功能开着却没生效"，其实锅已经被抽干了。
                            if (!CofSettings.waterCauldronFreezing) {
                                reply(context, header("tick", pos.toShortString())
                                        .append(nl())
                                        .append(row("未执行",
                                                "总开关 waterCauldronFreezing 已关闭；"
                                                        + "要手动步进请先 /carpet waterCauldronFreezing true",
                                                BAD)));
                                return 0;
                            }

                            // 先定位"实际会被处理的那口锅"，让累计值读的与真正动手的是同一口
                            BlockPos target = CauldronFreezing.findCauldron(level, pos);
                            CauldronFreezing.FreezeResult result = CauldronFreezing.checkColumn(level, pos);

                            String verdict;
                            Style verdictStyle;
                            if (result == CauldronFreezing.FreezeResult.PRODUCED) {
                                verdict = "结冰并产出产物";
                                verdictStyle = OK;
                            } else if (result == CauldronFreezing.FreezeResult.WARMED) {
                                verdict = "条件满足，本次只累计";
                                verdictStyle = ARROW;
                            } else {
                                verdict = "没有结冰";
                                verdictStyle = BAD;
                            }

                            MutableComponent out = header("tick", pos.toShortString())
                                    .append(nl())
                                    .append(row("步进", "1 次结冰检查", SUB))
                                    .append(nl())
                                    .append(row("目标",
                                            target == null ? "该列没有水炼药锅" : target.toShortString(),
                                            target == null ? BAD : VAL))
                                    .append(nl())
                                    .append(row("累计",
                                            target == null ? "无记录" : progressText(level, target), VAL))
                                    .append(nl())
                                    .append(row("本次检查", verdict, verdictStyle));
                            reply(context, out);
                            return 1;
                        })));

        // ---- /cofdebug data <pos> [value]
        root.then(Commands.literal("data")
                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .executes(context -> {
                            ServerLevel level = context.getSource().getLevel();
                            BlockPos pos = BlockPosArgument.getBlockPos(context, "pos");
                            int threshold = CauldronFreezing.threshold(CofSettings.cauldronFreezingProduct);
                            // 全程只读：reportConditions 与进度都走 getIfPresent，不会创建数据文件
                            CauldronFreezeData data = CauldronFreezeData.getIfPresent(level);
                            int count = data == null ? 0 : data.get(pos);

                            MutableComponent out = header("data", pos.toShortString())
                                    .append(nl())
                                    .append(reportConditions(level, pos))
                                    .append(nl())
                                    .append(row("产物", CofSettings.cauldronFreezingProduct
                                            + "（每 " + threshold + " 次产出 1 个）", SUB))
                                    .append(nl())
                                    .append(row("下次结冰", count + 1 >= threshold
                                            ? "产出产物"
                                            : "仍不产出",
                                            count + 1 >= threshold ? OK : SUB));
                            reply(context, out);
                            return 1;
                        })
                        .then(Commands.argument("value", IntegerArgumentType.integer(0, 100000))
                                .executes(context -> {
                                    ServerLevel level = context.getSource().getLevel();
                                    BlockPos pos = BlockPosArgument.getBlockPos(context, "pos");
                                    int value = IntegerArgumentType.getInteger(context, "value");

                                    // 写在哪口锅上：与结冰逻辑保持一致（该列地表 / 地表下一格）。
                                    // 输入坐标对不上时退回字面坐标，并在输出里点明，避免"写入成功却读不到"。
                                    BlockPos cauldron = CauldronFreezing.findCauldron(level, pos);
                                    boolean onCauldron = cauldron != null;
                                    BlockPos writePos = onCauldron ? cauldron : pos;

                                    if (value == 0) {
                                        // 清除走"只读不创建"：否则在一个从没预热过的世界里敲一条
                                        // "清空一条并不存在的记录"，也会把数据文件建出来。
                                        CauldronFreezeData data = CauldronFreezeData.getIfPresent(level);
                                        boolean had = data != null && data.get(writePos) != 0;
                                        if (had) {
                                            data.set(writePos, 0);
                                        }
                                        MutableComponent cleared = header("data", writePos.toShortString())
                                                .append(nl())
                                                .append(row("写入结果", had
                                                        ? "已清空该位置的记录"
                                                        : "该位置本来就没有记录", VAL));
                                        if (!onCauldron) {
                                            cleared = cleared.append(nl())
                                                    .append(row("注意", "该列没有水炼药锅，操作的是输入坐标本身", BAD));
                                        }
                                        reply(context, cleared);
                                        return 1;
                                    }

                                    // 只取一次，避免重复 computeIfAbsent
                                    CauldronFreezeData data = CauldronFreezeData.get(level);
                                    data.set(writePos, value);
                                    MutableComponent written = header("data", writePos.toShortString())
                                            .append(nl())
                                            .append(row("写入结果", "现在累计 " + data.get(writePos) + " 次", VAL));
                                    if (!onCauldron) {
                                        written = written.append(nl())
                                                .append(row("注意", "该列没有水炼药锅，写入的是输入坐标本身", BAD));
                                    }
                                    if (!CofSettings.waterCauldronFreezing) {
                                        written = written.append(nl())
                                                .append(row("提示", "总开关 waterCauldronFreezing 已关闭，这份记录不会被用到", BAD));
                                    }
                                    reply(context, written);
                                    return 1;
                                }))));

        // ---- /cofdebug use <pos> [player]
        // reputation：查看某玩家对"最近村民"的声望（账本逐条贡献需另用 /data get … Gossips）
        root.then(Commands.literal("reputation")
                .executes(context -> reputation(context, null))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> reputation(context, EntityArgument.getPlayer(context, "player")))));
        root.then(Commands.literal("use")
                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .executes(context -> useItem(context, null))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> useItem(context,
                                        EntityArgument.getPlayer(context, "player"))))));

        // ---- /cofdebug bucketcheck [player]
        root.then(Commands.literal("bucketcheck")
                .executes(context -> bucketCheck(context, null))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> bucketCheck(context,
                                EntityArgument.getPlayer(context, "player")))));

        dispatcher.register(root);
    }

    /** /cofdebug use 的实现；{@code given} 为 null 时用第一个在线玩家。 */
    private static int useItem(CommandContext<CommandSourceStack> context, ServerPlayer given) {
        ServerLevel level = context.getSource().getLevel();
        BlockPos pos = BlockPosArgument.getBlockPos(context, "pos");
        ServerPlayer player = given != null ? given : firstPlayer(level);
        if (player == null) {
            reply(context, header("use", pos.toShortString())
                    .append(nl())
                    .append(row("失败", "没有在线玩家可供交互（可先 /player Bot spawn）", BAD)));
            return 0;
        }
        BlockState before = level.getBlockState(pos);
        ItemStack held = player.getMainHandItem().copy();
        player.gameMode.useItemOn(player, level, held, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
        ItemStack after = player.getMainHandItem();
        BlockState now = level.getBlockState(pos);

        reply(context, header("use", pos.toShortString())
                .append(nl())
                .append(row("执行者", player.getGameProfile().getName()
                        + (player.isCreative() ? "（创造模式：手持物不会消耗）" : ""), VAL))
                .append(nl())
                .append(row("手持物品", describe(held), VAL))
                .append(nl())
                .append(row("交互后", describe(after),
                        held.getItem() == after.getItem() ? SUB : ARROW))
                .append(bottleRow(held, after))
                .append(nl())
                .append(row("方块变化", idOf(before) + " → " + idOf(now),
                        before == now ? SUB : ARROW)));
        return 1;
    }

    /** /cofdebug bucketcheck 的实现；{@code given} 为 null 时用第一个在线玩家。 */
    private static int bucketCheck(CommandContext<CommandSourceStack> context, ServerPlayer given) {
        ServerLevel level = context.getSource().getLevel();
        ServerPlayer player = given != null ? given : firstPlayer(level);
        if (player == null) {
            reply(context, header("bucketcheck", "")
                    .append(nl())
                    .append(row("失败", "需要一个在线玩家（可先 /player Bot spawn）", BAD)));
            return 0;
        }

        // 创造模式下 ItemUtils.createFilledResult 走"无限材料"分支：它会把结果物品塞进玩家背包，
        // 数字也不代表生存语义。所以直接跳过——连模拟都不做，杜绝给玩家物品。
        if (player.isCreative()) {
            reply(context, header("bucketcheck", "")
                    .append(nl())
                    .append(row("跳过", player.getGameProfile().getName()
                            + " 处于创造模式：原版本就不消耗水桶，该方法还会把测试物品放进背包，"
                            + "无法验证规则；请用生存模式玩家测试", BAD)));
            return 0;
        }

        // 放水是两步链，必须整条模拟才反映真实结果
        ItemStack placementHeld = new ItemStack(Items.WATER_BUCKET);
        ItemStack placement = ItemUtils.createFilledResult(placementHeld, player,
                BucketItem.getEmptySuccessItem(placementHeld, player));
        // 倒进炼药锅路径
        ItemStack cauldron = ItemUtils.createFilledResult(new ItemStack(Items.WATER_BUCKET),
                player, new ItemStack(Items.BUCKET));
        // 两个对照：不该受规则影响
        ItemStack lava = BucketItem.getEmptySuccessItem(new ItemStack(Items.LAVA_BUCKET), player);
        ItemStack fillUp = ItemUtils.createFilledResult(new ItemStack(Items.BUCKET), player,
                new ItemStack(Items.WATER_BUCKET));

        boolean rule = CofSettings.waterBucketNotConsumed;
        String expected = rule ? "minecraft:water_bucket x1" : "minecraft:bucket x1";
        boolean placementOk = describe(placement).equals(expected);
        boolean cauldronOk = describe(cauldron).equals(expected);

                    // 创造模式下原版走"无限材料"分支（getEmptySuccessItem 直接返回输入堆），
        MutableComponent out = header("bucketcheck", "")
                .append(nl())
                .append(row("执行者", player.getGameProfile().getName(), VAL))
                .append(nl())
                .append(row("规则", "waterBucketNotConsumed = " + rule, VAL))
                .append(nl())
                .append(row("期望", expected + "（按当前规则）", SUB))
                .append(nl())
                .append(row("放水", describe(placement), mark(placementOk)))
                .append(nl())
                .append(row("倒进锅", describe(cauldron), mark(cauldronOk)))
                .append(nl())
                .append(row("对照·岩浆桶", describe(lava), SUB))
                .append(nl())
                .append(row("对照·空桶装水", describe(fillUp), SUB));
        reply(context, out);
        return 1;
    }

    // ---------------------------------------------------------------- 只读诊断

    /** 任意炼药锅（水 / 岩浆 / 细雪 / 空）都算 —— 诊断需要把"锅无水"也报出来。 */
    /**
     * 声望查询：输出指定玩家对范围内"最近村民"的声望，并标注是否达到铁傀儡防卫阈值。
     *
     * <p>声望 = Σ(账本条目 Value × 该类型权重)：major_negative -5、minor_negative -1、
     * minor_positive +1、major_positive +5、trading +1。逐条明细见
     * {@code /data get entity <村民> Gossips}。</p>
     */
    private static int reputation(CommandContext<CommandSourceStack> context, ServerPlayer given) {
        ServerPlayer player = given != null ? given : context.getSource().getPlayer();
        if (player == null) {
            reply(context, header("reputation", "?")
                    .append(nl())
                    .append(row("玩家", "未指定：请以玩家身份执行，或写成 /cofdebug reputation <玩家>", mark(false))));
            return 0;
        }

        ServerLevel level = player.serverLevel();
        List<Villager> villagers = level.getEntitiesOfClass(Villager.class, player.getBoundingBox().inflate(64.0));
        if (villagers.isEmpty()) {
            reply(context, header("reputation", player.getName().getString())
                    .append(nl())
                    .append(row("村民", "64 格内没有村民", mark(false))));
            return 0;
        }

        Villager nearest = villagers.stream()
                .min(Comparator.comparingDouble(v -> v.distanceToSqr(player)))
                .orElse(villagers.get(0));
        int value = nearest.getPlayerReputation(player);
        boolean defence = value <= -100;

        MutableComponent out = header("reputation", player.getName().getString())
                .append(nl())
                .append(row("村民", nearest.getDisplayName().getString() + "  " + nearest.blockPosition().toShortString(), SUB))
                .append(nl())
                .append(row("声望", String.valueOf(value), mark(!defence)))
                .append(nl())
                .append(row("铁傀儡防卫", defence ? "是" : "否", mark(!defence)));
        reply(context, out);
        return 1;
    }
    private static boolean isCauldron(BlockState state) {
        return state.getBlock() instanceof AbstractCauldronBlock;
    }

    /**
     * 复述该坐标处的完整条件状况（只读，不改变任何东西）。
     *
     * <p>坐标处**不是水炼药锅**（无论是别的方块、空炼药锅，还是锅没露天）时，
     * 也照样把该方块的完整内容与各项条件输出出来，方便一眼看出卡在哪一条。</p>
     *
     * <p>判断结果一律靠颜色：满足 = 绿，不满足 = 红。行内不再带 ✓/✗ 或"是/否"标记
     * （`露天` 的"是/否"是它本身的值，保留）。</p>
     */
    private static MutableComponent reportConditions(ServerLevel level, BlockPos anyPosInColumn) {
        BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, anyPosInColumn);
        // 要报告的方块：① 传入坐标本身是炼药锅就用它；② 否则在该列地表往下 4 格内找炼药锅
        //（锅可能被方块盖住）；③ 都找不到就报告传入坐标那一格 —— 这样"不是锅/无水/被盖住"
        // 三种情况同样能看到完整内容。
        BlockPos target = null;
        if (isCauldron(level.getBlockState(anyPosInColumn))) {
            target = anyPosInColumn;
        } else {
            // 优先用"实际会被处理的那口锅"（地表 / 地表下一格），保证报告的目标与结冰逻辑一致；
            // 找不到再往下探 4 格，把"被方块整个盖住"的情况也报出来。
            target = CauldronFreezing.findCauldron(level, anyPosInColumn);
            if (target == null) {
                for (int dy = 0; dy >= -4; dy--) {
                    BlockPos probe = surface.offset(0, dy, 0);
                    if (isCauldron(level.getBlockState(probe))) {
                        target = probe;
                        break;
                    }
                }
            }
            if (target == null) {
                target = anyPosInColumn;
            }
        }

        BlockState state = level.getBlockState(target);
        boolean isWater = state.is(Blocks.WATER_CAULDRON);
        int lvl = isWater ? state.getValue(LayeredCauldronBlock.LEVEL) : 0;
        boolean full = isWater && lvl == LayeredCauldronBlock.MAX_FILL_LEVEL;
        boolean waterOk = isWater && (!CofSettings.cauldronFreezingRequiresFullWater || full);
        Holder<Biome> biomeHolder = level.getBiome(target);
        Biome biome = biomeHolder.value();
        // Biome 没有重写 toString，直接打印会得到 "net.minecraft...Biome@1a2b3c"，
        // 所以这里取注册表里的 id；拿不到时给个明确占位符
        String biomeId = biomeHolder.unwrapKey().map(key -> key.location().toString())
                .orElse("(不在注册表中)");
        Biome.Precipitation precipitation = biome.getPrecipitationAt(target, level.getSeaLevel());
        boolean snow = precipitation == Biome.Precipitation.SNOW;
        boolean sky = level.canSeeSky(target.above());
        int light = level.getBrightness(LightLayer.BLOCK, target);
        boolean lightOk = light < 10;
        int threshold = CauldronFreezing.threshold(CofSettings.cauldronFreezingProduct);
        CauldronFreezeData data = CauldronFreezeData.getIfPresent(level);
        int count = data == null ? 0 : data.get(target);

        MutableComponent out = row("规则", "水锅结冰:" + CofSettings.waterCauldronFreezing
                + "  满水要求:" + CofSettings.cauldronFreezingRequiresFullWater
                + "  产物:" + CofSettings.cauldronFreezingProduct
                + "  结冰速率:" + CofSettings.cauldronFreezingChance,
                CofSettings.waterCauldronFreezing ? VAL : BAD)
                .append(nl())
                .append(row("位置", target.toShortString(), VAL))
                .append(nl())
                .append(row("维度", level.dimension().location().toString(),
                        level.dimension() == Level.OVERWORLD ? VAL : BAD))
                .append(nl())
                .append(row("方块", idOf(state), isWater ? VAL : BAD));
        if (isWater) {
            out = out.append(nl()).append(row("水位", String.valueOf(lvl), waterOk ? VAL : BAD));
        }
        return out.append(nl())
                .append(row("水位条件", CofSettings.cauldronFreezingRequiresFullWater
                        ? "需要满水" : "任意水位", waterOk ? VAL : BAD))
                .append(nl())
                .append(row("群系", biomeId, VAL))
                .append(nl())
                .append(row("降水", String.valueOf(precipitation), mark(snow)))
                .append(nl())
                .append(row("露天", yes(sky), mark(sky)))
                .append(nl())
                .append(row("方块光", String.valueOf(light), mark(lightOk)))
                .append(nl())
                .append(row("累计", count + " / " + threshold, VAL));
    }
    private static String progressText(ServerLevel level, BlockPos pos) {
        CauldronFreezeData data = CauldronFreezeData.getIfPresent(level);
        int count = data == null ? 0 : data.get(pos);
        int threshold = CauldronFreezing.threshold(CofSettings.cauldronFreezingProduct);
        return count + " / " + threshold;
    }


    private static String idOf(BlockState state) {
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
    }

    private static String describe(ItemStack stack) {
        if (stack.isEmpty()) {
            return "空手";
        }
        return BuiltInRegistries.ITEM.getKey(stack.getItem())
                + " x" + stack.getCount();
    }

    /**
     * 若交互前手里是水瓶，返回一行"水瓶消耗"判定（是否被换成了空玻璃瓶）。
     *
     * <p>自带行首换行，不适用时返回空组件，因此可以直接 append 进输出链。</p>
     */
    private static MutableComponent bottleRow(ItemStack before, ItemStack after) {
        if (!WaterBottles.isWaterBottle(before)) {
            return Component.empty();
        }
        boolean kept = WaterBottles.isWaterBottle(after);
        return nl().append(row("水瓶消耗", kept ? "否" : "是", kept ? OK : BAD));
    }

    private static ServerPlayer firstPlayer(ServerLevel level) {
        return level.getServer().getPlayerList().getPlayers().stream().findFirst().orElse(null);
    }

    // ---------------------------------------------------------------- 输出

    /** 回给执行者（玩家或 RCON 都能读到），同时以纯文本写日志。 */
    private static void reply(CommandContext<CommandSourceStack> context,
                              Component message) {
        context.getSource().sendSuccess(() -> message, false);
        CofMod.LOGGER.info("[{}] {}", CofMod.MOD_NAME,
                message.getString().replace("\n", " | ").replaceAll("\\s+", " ").trim());
    }

    
}
