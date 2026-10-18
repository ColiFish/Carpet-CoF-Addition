// SPDX-License-Identifier: MIT
// Copyright (c) 2026 CoF contributors

package carpet.cof.mixin;

import carpet.cof.CofSettings;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 「快速转化」僵尸村民一侧的 <b>1.21.11 变体</b>（实体类搬到 {@code monster.zombie} 包）。
 * 逻辑与 main 里的同名类完全一致。
 */
@Mixin(ZombieVillager.class)
public abstract class ZombieVillagerInstantCureMixin {

    @ModifyVariable(
            method = "startConverting(Ljava/util/UUID;I)V",
            at = @At("HEAD"),
            argsOnly = true,
            index = 2)
    private int cof$instantCure(int ticks) {
        return CofSettings.instantVillagerConversion ? 1 : ticks;
    }
}
