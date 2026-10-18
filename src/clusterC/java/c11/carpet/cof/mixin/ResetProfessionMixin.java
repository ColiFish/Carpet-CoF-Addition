// SPDX-License-Identifier: MIT
// Copyright (c) 2026 CoF contributors

package carpet.cof.mixin;

import carpet.cof.CofSettings;
import carpet.cof.VillagerClaimRelease;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.ResetProfession;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 「新手村民交易不重置」的主拦截点 —— <b>clusterC</b> 变体（MC 1.21.11 起）。
 *
 * <p>与 {@code src/main/java/.../ResetProfessionMixin.java} 同名同包，由 {@code build.gradle}
 * 在 1.21.11 上二选一；差异只有 {@code world.entity.npc.villager.Villager} 包名。
 * 注入点 {@code method_47038} 的签名 {@code (ServerLevel, Villager, long) -> boolean}
 * 在 1.21.11 上未变（已用 javap 核对）。</p>
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
