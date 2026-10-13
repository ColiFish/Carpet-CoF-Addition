// SPDX-License-Identifier: MIT
// Copyright (c) 2026 CoF contributors

package carpet.cof;

/**
 * 「傻子可逆转化」备份标签里用到的所有 NBT 键名。
 *
 * <p>这些键会在村民、僵尸村民两侧以及"僵尸化往返"的搬运逻辑里共用，集中一处可避免拼写不一致
 * 导致的静默丢数据（键名写错时 {@code contains} 只会返回 false，不会报错）。</p>
 */
public final class NitwitBackupKeys {

    /** 挂在实体身上的备份标签本体。 */
    public static final String BACKUP = "CofNitwitBackup";
    /** 转化前的职业 id。 */
    public static final String PROFESSION = "Profession";
    /** 转化前的整份交易列表。 */
    public static final String OFFERS = "Offers";
    /** 转化前的经验。 */
    public static final String XP = "Xp";
    /** 转化前认领的工作站点坐标（打包成 long）。 */
    public static final String JOB_SITE = "JobSite";
    /** 工作站点所在维度 id。 */
    public static final String JOB_SITE_DIM = "JobSiteDim";
    /** 转化倒计时（剩余 tick）。 */
    public static final String CONVERTING = "Converting";
    /** 喂毒马铃薯的玩家 UUID。 */
    public static final String TO_PLAYER = "ToPlayer";
    /** 把傻子转回普通的玩家 UUID。 */
    public static final String BACK_PLAYER = "BackPlayer";

    private NitwitBackupKeys() {
    }
}
