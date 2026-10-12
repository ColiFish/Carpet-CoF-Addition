// SPDX-License-Identifier: MIT
// Copyright (c) 2026 CoF contributors

package carpet.cof.mixin;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 只读访问 {@code TargetGoal#mob}。
 *
 * <p>为什么需要它：{@code mob} 字段声明在 {@code TargetGoal} 自己，但**子类**的 mixin 不能用
 * {@code @Shadow} 引用继承来的字段（实测报 "field mob was not located in the target class"）。
 * 做法是把访问器混入目标类本体，再在子类里把 {@code this} 转成这个接口。</p>
 */
@Mixin(TargetGoal.class)
public interface TargetGoalAccessor {

    @Accessor("mob")
    Mob cof$getMob();
}
