// SPDX-License-Identifier: MIT
// Copyright (c) 2026 CoF contributors

package carpet.cof.mixin;

import carpet.cof.CofSettings;
import carpet.cof.GunpowderIgnition;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.FireChargeItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 「可燃火药」的音效：把烈焰弹的点火音换成"物品被岩浆烧毁"的音效，音量取其 0.2 倍。
 *
 * <p>原版这一处在 {@code FireChargeItem#playSound(Level, BlockPos)} 里播放
 * {@code SoundEvents.FIRECHARGE_USE}（音量 1.0）。原版"物品进岩浆烧毁"用的则是
 * {@code Entity#lavaHurt} 里的 {@code SoundEvents.GENERIC_BURN}（音量 0.4、音调 2.0 + rand×0.4）——
 * 因此这里播 <b>0.4 × 0.5 = 0.2</b> 的 {@code GENERIC_BURN}，音调沿用岩浆烧毁时的那套。</p>
 *
 * <p>只在本模组委托烈焰弹（{@link GunpowderIgnition#isActive()}）时生效：真正的烈焰弹听起来不变。</p>
 */
@Mixin(FireChargeItem.class)
public abstract class FireChargeItemSoundMixin {

    @Inject(method = "playSound", at = @At("HEAD"), cancellable = true)
    private void cof$gunpowderIgniteSound(Level level, BlockPos pos, CallbackInfo ci) {
        if (CofSettings.flammableGunpowder && GunpowderIgnition.isActive()) {
            level.playSound(null, pos, SoundEvents.GENERIC_BURN, SoundSource.BLOCKS,
                    0.4F * 0.5F, 2.0F + level.getRandom().nextFloat() * 0.4F);
            ci.cancel();
        }
    }
}
