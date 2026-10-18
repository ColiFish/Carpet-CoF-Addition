// SPDX-License-Identifier: MIT
// Copyright (c) 2026 CoF contributors

package carpet.cof;

/**
 * 「傻子转化」倒计时的持久化接口。
 *
 * <p>转化要跑 3~5 分钟，和原版治愈僵尸村民一样会跨重启；所以倒计时写在村民自己的实体 NBT 里
 * （一个 int），由按版本分的存档 mixin 读写，转化逻辑本身只管这个字段。</p>
 */
public interface ConvertingState {

    /** 村民 NBT 里的键：剩余 tick（只在进行中的转化时写入，0/缺省表示没有）。 */
    String NBT_KEY = "CofConverting";

    int cof$getConvertingTicks();

    void cof$setConvertingTicks(int ticks);
}
