package carpet.cof.mixin;

import carpet.cof.CofSettings;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 生存模式下用水桶"消耗掉一份水"时不消耗水桶（放水、倒进炼药锅都算）。
 *
 * <h2>为什么只拦这一个方法就够了</h2>
 *
 * <p>两条路径最后都汇到 {@code ItemUtils.createFilledResult}：</p>
 *
 * <pre>
 * 放水：      BucketItem#use
 *               → step1 = getEmptySuccessItem(stack, player)        // 生存 → new ItemStack(BUCKET)
 *               → step2 = createFilledResult(stack, player, step1)   // ← 汇合点
 *
 * 倒进炼药锅： CauldronInteraction
 *               → createFilledResult(stack, player, new ItemStack(BUCKET))   // ← 同一个汇合点
 * </pre>
 *
 * <p>在 HEAD 直接返回原物品堆，等于整段方法体都不执行：水/炼药锅照常被处理，
 * 手上仍然是那个水桶，也不会多出空桶。</p>
 *
 * <h2>踩过的坑（两个，都写在这）</h2>
 *
 * <p><b>1. 多重重载必须写完整描述符。</b>
 * {@code createFilledResult} 有 3 参数与 4 参数两个重载（后者多一个 boolean）。
 * 只写 {@code method = "createFilledResult"} 会同时匹配两个，Mixin 因参数对不上抛
 * {@code InvalidInjectionException} —— 而且是在该类第一次被加载时才炸，启动时看不出来。
 * 这里只拦 4 参数那个真正的实现（3 参数版会转调它）。</p>
 *
 * <p><b>2. 不要在上游 {@code getEmptySuccessItem} 里返回同一个对象。</b>
 * 最初还额外拦了 {@code BucketItem#getEmptySuccessItem} 并让它返回入参本身，结果是：
 * 第二步变成 {@code createFilledResult(水桶, player, 同一个水桶)}，输入被消耗、返回的也是
 * 那个已被消耗的堆 —— 玩家手上的水桶<b>直接消失</b>。所以那个 mixin 已删除，
 * 这里才是唯一正确的拦截点。</p>
 *
 * <p>条件写得比较窄（水桶 → 空桶）是刻意的：{@code createFilledResult} 还被"空桶装水"、
 * "桶装牛奶"等流程共用，那些情况不该受影响。</p>
 */
@Mixin(ItemUtils.class)
public abstract class ItemUtilsMixin {

    @Inject(
            method = "createFilledResult(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/item/ItemStack;",
            at = @At("HEAD"),
            cancellable = true)
    private static void cof$keepWaterBucket(ItemStack stack, Player player, ItemStack result, boolean flag,
                                            CallbackInfoReturnable<ItemStack> cir) {
        if (CofSettings.waterBucketNotConsumed
                && stack.is(Items.WATER_BUCKET)
                && result.is(Items.BUCKET)) {
            cir.setReturnValue(stack);
        }
    }
}
