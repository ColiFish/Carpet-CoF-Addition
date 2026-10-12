// SPDX-License-Identifier: MIT
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

    /**
     * 低价村民：村民出售的商品，价格区间里的随机项一律取下限。
     *
     * <p>只作用于原版真正带随机价格区间的两类交易：</p>
     * <ul>
     *   <li>图书管理员的<b>附魔书</b>：区间下限 {@code 2 + 3×附魔等级}
     *       （经验修补、冰霜行者等宝藏附魔仍按原版翻倍，仍封顶 64）；
     *       附魔种类与等级照旧随机。</li>
     *   <li>盔甲匠 / 武器匠 / 工具匠的<b>附魔装备</b>：区间下限 {@code 基础价 + 5}；
     *       装备的附魔强度仍是原版随机结果。</li>
     * </ul>
     *
     * <p>其余交易（收购作物、卖固定价商品、藏宝图、可疑 stew 等）价格本来就是固定值，
     * 没有区间，本规则不碰。低价村民只影响<b>之后新刷出</b>的交易——村民升级会重刷交易，
     * 所以打开规则后新生成或升级的村民立刻生效，已经存在的村民保持原价。</p>
     */
    @Rule(categories = {CATEGORY, CATEGORY_SURVIVAL, CATEGORY_FEATURE})
    public static boolean lowPriceVillagers = false;

    /**
     * 声望不涨价：村民因声望调价时只能降价或不变。
     *
     * <p>原版 {@code Villager#updateSpecialPrices} 把声望折算成价格修正
     * {@code -floor(声望 × priceMultiplier)}：声望为正（治愈僵尸村民、正面流言）时得到负值、更便宜；
     * 声望为负（攻击或击杀村民）时得到正值、<b>涨价</b>。打开本规则后这一项被限制为 ≤ 0，
     * 即"只降不涨"，而英雄buff（{@code HERO_OF_THE_VILLAGE}）的折扣照常生效。</p>
     *
     * <p>注意：原版"买得越多越贵"的<b>需求</b>机制（{@code MerchantOffer#updateDemand}）不是声望，
     * 本规则不干预。</p>
     */
    @Rule(categories = {CATEGORY, CATEGORY_SURVIVAL, CATEGORY_FEATURE})
    public static boolean reputationNoPriceHike = false;

    /**
     * 正义的伙伴：铁傀儡永不主动攻击玩家。
     *
     * <p>原版铁傀儡攻击玩家有两条独立路径，本规则把两条都封掉：</p>
     * <ul>
     *   <li><b>村庄防卫</b>：{@code DefendVillageTargetGoal} 扫描傀儡周围 10×8×10 内的村民与玩家，
     *       只要任一村民对该玩家声望 ≤ -100 就把玩家设为目标（该目标对所有铁傀儡都注册）。</li>
     *   <li><b>还手 / 愤怒值</b>：{@code HurtByTargetGoal}（被打了就打回去）与
     *       {@code NearestAttackableTargetGoal} + {@code isAngryAt}（愤怒值驱动的主动寻敌）。</li>
     * </ul>
     *
     * <p>实现分三层：拦下防卫目标、拦下还手目标，并在 {@code Mob#canAttack} 处对"铁傀儡 → 玩家"的组合
     * 恒返回 false（既阻止目标获取，也让**已经在追击玩家**的傀儡立刻停下）。</p>
     *
     * <p>只影响铁傀儡对**玩家**的行为：对僵尸等敌对生物的敌意完全不受影响。</p>
     */
    @Rule(categories = {CATEGORY, CATEGORY_SURVIVAL, CATEGORY_FEATURE})
    public static boolean righteousCompanion = false;

    /**
     * 新手村民交易不重置：村民不会自行把职业丢回"无业"，交易内容因此永不重随机。
     *
     * <p>原版只有一条刷新链路（{@code ResetProfession} 行为）：当村民的 JOB_SITE 记忆为空
     * （工作站被拆掉/被替换/够不到）、职业不是 NONE 或 NITWIT、<b>经验为 0（从未交易过）</b>
     * 且等级 ≤ 1 时，把职业设回 NONE；之后再认领工作站就会得到新职业，而职业一变
     * {@code Villager#setVillagerData} 会清空 {@code offers}，整批交易重随机。</p>
     *
     * <p>打开本规则后，"职业 → NONE"这一步被拦下：未交易过的村民也会保留原职业与原交易列表，
     * 拆掉工作站不再换货（代价是也不能再用"拆工作站"来手动重刷村民交易）。
     * 新建/繁殖/初始化/读档路径不受影响。</p>
     */
    @Rule(categories = {CATEGORY, CATEGORY_SURVIVAL, CATEGORY_FEATURE})
    public static boolean noviceVillagerTradeNotReset = false;
}
