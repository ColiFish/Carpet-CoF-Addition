// SPDX-License-Identifier: MIT
// Copyright (c) 2026 CoF contributors

package carpet.cof.mixin;

import carpet.cof.CofSettings;
import carpet.cof.VillagerClaimRelease;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.ResetProfession;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


/**
 * 「新手村民交易不重置」的主拦截点：让 {@code ResetProfession} 行为**只在"自发刷新"时**不执行。
 *
 * <p><b>两种情形必须区别对待</b>（这是本类改版的核心理由）：</p>
 * <ul>
 *   <li><b>自发弄丢认领</b>：空间局促 / 够不到工作站时，
 *       {@code SetWalkTargetFromBlockMemory} 会 {@code villager.releasePoi(...)} 并擦除记忆——
 *       此时**工作方块仍然存在于世界里**。这种"村民自己把交易弄丢"的情形要拦。</li>
 *   <li><b>玩家拆掉工作方块</b>：{@code ValidateNearbyPoi} 发现
 *       {@code !poiManager.exists(pos, 该职业可持有类型)} 后擦除记忆——此时**方块已经没了**。
 *       这是玩家主动为之，应当放行：职业按原版清零，随后重放方块即可手动重刷交易。</li>
 * </ul>
 *
 * <p>判据因此是一句世界状态查询：重置发生时，村民附近是否还存在**该职业的有效工作方块**
 * （{@code PoiManager#findClosest} + {@code VillagerProfession#heldJobSite}，48 格内）。
 * 存在 → 拦；不存在 → 放行。</p>
 *
 * <p><b>为什么必须拦在行为入口</b>（1.1.0 实测踩过的坑）：该 lambda 的体是</p>
 *
 * <pre>if (职业 != NONE && 职业 != NITWIT && 经验 == 0 && 等级 &lt;= 1) {
 *    villager.setVillagerData(当前数据.setProfession(NONE));
 *    villager.refreshBrain(serverLevel);
 *    return true;
 * }
 * return false;</pre>
 *
 * <p>若只在 {@code setVillagerData} 里拦职业变更，条件会永远成立：该行为每 tick 都返回 true 并调用
 * {@code refreshBrain()}（内部 stopAllActivities + 重建大脑）→ 村民 AI 每 tick 被清一次 → <b>完全不动</b>。
 * 在入口直接返回 false 则职业保留、{@code refreshBrain} 不再被调用、交易列表也不会被清空。</p>
 *
 * <p>{@code method_47038} 是官方映射给该 lambda 的方法名，描述符 {@code (ServerLevel, Villager, long) -> boolean}；
 * 写错会在构建期被 mixin 注解处理器报错（{@code defaultRequire = 1}），不会静默失效。</p>
 */
@Mixin(ResetProfession.class)
public abstract class ResetProfessionMixin {

    @Inject(method = "method_47038", at = @At("HEAD"), cancellable = true)
    private static void cof$skipResetProfession(ServerLevel level, Villager villager, long time,
                                                CallbackInfoReturnable<Boolean> cir) {
        if (CofSettings.noviceVillagerTradeNotReset
                && VillagerClaimRelease.isFresh(villager.getUUID(), level.getGameTime())) {
            cir.setReturnValue(false);   // 刚刚自己释放过认领 = 自发弄丢 → 拦下，职业与交易保持
        }
        // 方块已不在附近（玩家拆掉）→ 不拦，职业按原版清零，可重放方块手动重刷
    }
}
