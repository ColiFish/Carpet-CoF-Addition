// SPDX-License-Identifier: MIT
// Copyright (c) 2026 CoF contributors

package carpet.cof.mixin;

import carpet.cof.CofSettings;
import net.minecraft.world.entity.monster.ZombieVillager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 「快速转化」的僵尸村民一侧：把治愈倒计时压到 1 tick。
 *
 * <p>原版治愈靠 {@code startConverting(UUID, int)} 设置剩余 tick（3600~6000），
 * 这里在方法入口把该参数改成 1 —— 下一个 tick 就会走到 {@code finishConversion}。</p>
 *
 * <p>注意：{@code readAdditionalSaveData} 也会调用这个方法，所以存档里"正在治愈"的僵尸村民
 * 读档后也会立刻完成（与"零等待"的语义一致）。</p>
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
