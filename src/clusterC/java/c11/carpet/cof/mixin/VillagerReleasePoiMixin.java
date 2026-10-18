// SPDX-License-Identifier: MIT
// Copyright (c) 2026 CoF contributors

package carpet.cof.mixin;

import carpet.cof.CofSettings;
import carpet.cof.VillagerClaimRelease;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 记录"村民自己释放了工作站点认领"的时刻（只记 JOB_SITE）。
 *
 * <p>原版两种情形调用路径不同：自发弄丢（够不到 / 被抢占）会走
 * {@code SetWalkTargetFromBlockMemory -> villager.releasePoi(JOB_SITE)}；
 * 玩家拆掉方块则走 {@code ValidateNearbyPoi} 的 {@code memory.erase()}，不经过这里。
 * 因此这个标记可以精确区分二者，而不必去猜"附近还有没有同职业工作方块"。</p>
 */
@Mixin(Villager.class)
public abstract class VillagerReleasePoiMixin {

    @Inject(method = "releasePoi", at = @At("HEAD"))
    private void cof$markSelfRelease(MemoryModuleType<GlobalPos> type, CallbackInfo ci) {
        if (CofSettings.noviceVillagerTradeNotReset && type == MemoryModuleType.JOB_SITE) {
            Villager self = (Villager) (Object) this;
            VillagerClaimRelease.mark(self.getUUID(), self.level().getGameTime());
        }
    }
}
