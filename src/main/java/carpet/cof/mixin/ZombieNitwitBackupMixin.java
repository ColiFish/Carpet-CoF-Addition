// SPDX-License-Identifier: MIT
// Copyright (c) 2026 CoF contributors

package carpet.cof.mixin;

import carpet.cof.NitwitBackupHolder;
import carpet.cof.NitwitBackupKeys;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.npc.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 感染转化（村民 → 僵尸村民）时，把「傻子可逆转化」的备份一起搬过去。
 *
 * <p>注入点是原版转化 lambda {@code method_63655(ServerLevel, Villager, ZombieVillager)} ——
 * 这是唯一同时拿到"旧村民"和"新僵尸村民"两个实体的位置，因此备份的搬运必须在这里做。</p>
 */
@Mixin(Zombie.class)
public abstract class ZombieNitwitBackupMixin {

    @Inject(method = "method_63655", at = @At("HEAD"))
    private void cof$carryBackupToZombieVillager(ServerLevel level, Villager villager,
                                                 ZombieVillager zombieVillager, CallbackInfo ci) {
        if (villager instanceof NitwitBackupHolder from && zombieVillager instanceof NitwitBackupHolder to) {
            to.cof$setNitwitBackup(from.cof$getNitwitBackup());
        }
    }
}
