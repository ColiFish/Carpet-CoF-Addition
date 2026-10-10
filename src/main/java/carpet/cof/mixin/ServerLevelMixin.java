// SPDX-License-Identifier: MIT
// Copyright (c) 2026 CoF contributors

package carpet.cof.mixin;

import carpet.cof.CauldronFreezing;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 把结冰检查挂到区块 tick 上。
 *
 * <h2>为什么不再挂在 tickPrecipitation 上</h2>
 *
 * <p>上一版挂在 {@code ServerLevel#tickPrecipitation} 上 —— 那是原版<b>只有在下雨/下雪时</b>
 * 才会调用的方法（外面裹着 {@code if (isRaining())}），于是"必须正在降雪"这个条件被隐式带进来了。
 * 现在的需求明确要求不依赖当前天气，所以换成每 tick 都会跑的 {@code tickChunk}。</p>
 *
 * <h2>为什么把 randomTickSpeed 传下去</h2>
 *
 * <p>原版 {@code tickChunk} 里的降水循环是
 * {@code for (i = 0; i < randomTickSpeed; i++) if (random.nextInt(48) == 0) tickPrecipitation(...)}。
 * 为了让检测频率与原版<b>结构与概率门一致</b>（而不是自己拍一个数字，早先就是拍成了 4 列/区块 tick，
 * 结果比原版快 64 倍），这里把 {@code randomTickSpeed} 原样转给
 * {@link CauldronFreezing#tickChunk}，由它复刻同一个循环结构与 1/48 概率门。</p>
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {

    @Inject(method = "tickChunk", at = @At("HEAD"))
    private void cof$freezeWaterCauldrons(LevelChunk chunk, int randomTickSpeed, CallbackInfo ci) {
        CauldronFreezing.tickChunk((ServerLevel) (Object) this, chunk, randomTickSpeed);
    }
}
