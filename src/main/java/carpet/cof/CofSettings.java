// SPDX-License-Identifier: GPL-3.0-only
// Copyright (c) 2026 CoF contributors

package carpet.cof;

import carpet.api.settings.Rule;

/**
 * 本模组的 Carpet 规则。
 *
 * <p>字段必须是 {@code public static}，支持 boolean / int / double / long / float /
 * String / 枚举，字段初始值就是默认值。这些规则会挂在 Carpet 自己的 settings manager 上，
 * 所以玩家用 {@code /carpet <规则名> <值>} 就能开关，不用记第二个命令。</p>
 *
 * <p><b>本模组不注册任何新方块或物品</b>，全部功能都是对原版行为的可开关改动。</p>
 */
public class CofSettings {
    /** 规则分类，显示在 /carpet 列表里。 */
    public static final String CATEGORY = "CoF";

    // 子分类沿用 Carpet 自己的分类词汇（feature / survival / command / creative），
    // 这样 /carpet list feature 之类能把本模组的规则和 Carpet 本体的一起筛出来。
    // 这些分类的显示名由 Carpet 自带的语言文件提供，因此本地不需要额外的 carpet.category.* 键。
    /** 功能特性。 */
    public static final String CATEGORY_FEATURE = "feature";
    /** 影响生存玩法。 */
    public static final String CATEGORY_SURVIVAL = "survival";
    /** 命令相关。 */
    public static final String CATEGORY_COMMAND = "command";
    /** 创造模式相关。 */
    public static final String CATEGORY_CREATIVE = "creative";

    /** 总开关：水炼药锅能不能在下雪环境里结冰。 */
    @Rule(categories = {CATEGORY, CATEGORY_FEATURE})
    public static boolean waterCauldronFreezing = false;

    /**
     * 结冰产出什么。
     *
     * <p>三种产物：{@code ice}（默认，普通冰块）、{@code packed_ice}（浮冰）、{@code blue_ice}（蓝冰）。
     * 每次都固定产出这一种，锅照常清空、水照常被消耗。</p>
     *
     * <p>用 String + {@code options} 白名单而不是枚举，原因有两个：
     * 枚举常量不能叫 {@code false}；而且旧版本把这个规则存成布尔值（{@code true}/{@code false}），
     * 用 String 的话那些旧值不会引发解析报错 —— {@code CauldronFreezing} 会把无法识别的值
     * 一律当作 {@code ice}，所以升级时配置不会出问题。</p>
     */
    @Rule(categories = {CATEGORY, CATEGORY_FEATURE}, options = {"ice", "packed_ice", "blue_ice"})
    public static String cauldronFreezingProduct = "ice";

    /**
     * 生存模式下用水桶放水（以及把水倒进炼药锅）时不消耗水桶，也不会多出一个空桶。
     * 创造模式本来就不消耗，所以这个规则只影响生存。
     */
    @Rule(categories = {CATEGORY, CATEGORY_SURVIVAL, CATEGORY_FEATURE})
    public static boolean waterBucketNotConsumed = false;

    /**
     * 生存模式下手持水瓶给炼药锅灌水时不消耗水瓶，也不会多出空玻璃瓶。
     *
     * <p>只影响"把水倒进炼药锅"这一个交互。<b>喝水瓶不受影响</b> —— 喝药水也返回空玻璃瓶，
     * 但那条路径刻意不拦，否则就成了可以无限续杯的刷水瓶漏洞。</p>
     */
    @Rule(categories = {CATEGORY, CATEGORY_SURVIVAL, CATEGORY_FEATURE})
    public static boolean waterBottleNotConsumed = false;

    /**
     * 是否只有装满水（水位 3）的炼药锅才会结冰。
     * 关掉的话，只要锅里有水（水位 1~3）都会结冰。
     */
    @Rule(categories = {CATEGORY, CATEGORY_FEATURE})
    public static boolean cauldronFreezingRequiresFullWater = true;

    /**
     * 结冰检测的概率门：每次检测迭代通过的概率是 <b>1/N</b>，N 就是这个值。
     *
     * <p>默认 48 与原版降水循环里的 {@code random.nextInt(48)} 完全一致，
     * 所以环境满足时结冰速度和水源结冰同一节奏。值越小越快
     * （1 = 每次迭代都检测），越大越慢。非法值（&lt;= 0）会被当成 1 处理。</p>
     *
     * <p>{@code options} 只是给 {@code /carpet} 显示用的建议值，{@code strict = false}
     * 表示**不限制**实际取值（任意正整数仍然可用）。加它是为了绕开 Carpet 的一个显示缺陷：
     * 建议值为空的规则会渲染出一个孤立的 {@code ]}（例如
     * {@code current value: 48 (default value) ]}）。</p>
     */
    @Rule(categories = {CATEGORY, CATEGORY_FEATURE},
            options = {"1", "4", "12", "48", "96", "192"},
            strict = false)
    public static int cauldronFreezingChance = 48;

    /**
     * {@code /cofdebug} 诊断命令是否启用。
     *
     * <ul>
     *   <li>{@code false} —— 完全关闭（命令不可用，也不会出现在补全里）</li>
     *   <li>{@code 0}~{@code 4} —— 启用，且只有权限等级 ≥ 该数字的玩家/控制台可以用</li>
     * </ul>
     *
     * <p>默认 {@code 2}，即"需要 OP"。RCON 控制台是 4 级，所以依然可用。
     * 判定发生在**每次执行时**，因此在游戏里改完立刻生效，不需要重启。</p>
     */
    @Rule(categories = {CATEGORY, CATEGORY_COMMAND, CATEGORY_CREATIVE, CATEGORY_FEATURE},
            options = {"false", "0", "1", "2", "3", "4"})
    public static String cofDebugCommand = "2";
}
