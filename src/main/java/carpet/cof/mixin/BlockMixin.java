// SPDX-License-Identifier: MIT
// Copyright (c) 2026 CoF contributors

package carpet.cof.mixin;

import carpet.cof.CauldronFreezeData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 玩家挖掉炼药锅时，立刻清掉该位置的结冰阶段记录。
 *
 * <h2>为什么挂在 playerDestroy 而不是 onRemove</h2>
 *
 * <p>{@code onRemove} 会在**任何**方块被替换时触发（包括本模组自己把水锅换成空锅、
 * 红石、活塞、/setblock……），那是每一次方块变化都要走的路径；而 {@code playerDestroy}
 * 只在**玩家真正挖掉方块**时触发一次。用后者既便宜，又刚好符合语义：</p>
 *
 * <ul>
 *   <li>玩家把锅挖走 → 记录该清了 ✓</li>
 *   <li>本模组自己把水锅换成空锅（同一个锅继续用）→ <b>不该</b>清 ✓（playerDestroy 不会触发）</li>
 * </ul>
 *
 * <p>注意这里用 {@link CauldronFreezeData#getIfPresent}：没有记录就直接返回，
 * 不会因为挖一个锅就建出一个空数据文件。</p>
 *
 * <p>{@code destroy}/{@code playerWillDestroy} 也能覆盖"活塞/爆炸拆锅"这类情况，
 * 但它们同样更热，收益却只是提前清掉几百字节，所以不挂。</p>
 */
@Mixin(Block.class)
public abstract class BlockMixin {

    @Inject(method = "playerDestroy", at = @At("HEAD"))
    private void cof$clearFreezeRecord(Level level, Player player, BlockPos pos, BlockState state,
                                       BlockEntity blockEntity, ItemStack tool, CallbackInfo ci) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!(((Block) (Object) this) instanceof AbstractCauldronBlock)) {
            return;
        }
        CauldronFreezeData.clearIfPresent(serverLevel, pos);
    }
}
