// SPDX-License-Identifier: MIT
// Copyright (c) 2026 CoF contributors

package carpet.cof.mixin;

import carpet.cof.ConvertingState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.npc.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 「傻子转化」倒计时的村民侧存档钩子（原版同款：只有进行中才写，缺省 0）。
 *
 * <p>倒计时存在实体 NBT 里，所以和原版治愈僵尸村民一样<b>跨重启继续</b>。</p>
 */
@Mixin(Villager.class)
public abstract class VillagerConvertingSaveMixin {

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void cof$saveConverting(CompoundTag tag, CallbackInfo ci) {
        int ticks = ((ConvertingState) this).cof$getConvertingTicks();
        if (ticks > 0) {
            tag.putInt(ConvertingState.NBT_KEY, ticks);
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void cof$readConverting(CompoundTag tag, CallbackInfo ci) {
        if (tag.contains(ConvertingState.NBT_KEY, net.minecraft.nbt.Tag.TAG_INT)) {
            ((ConvertingState) this).cof$setConvertingTicks(tag.getInt(ConvertingState.NBT_KEY));
        }
    }
}
