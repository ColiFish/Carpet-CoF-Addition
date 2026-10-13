// SPDX-License-Identifier: MIT
// Copyright (c) 2026 CoF contributors

package carpet.cof;

/**
 * 「可燃火药」委托烈焰弹执行期间的标记。
 *
 * <p>音效那一层（{@code FireChargeItem#playSound}）拿不到"手上拿的是什么"，所以在委托前后打标记，
 * 让音效注入能分清"这次是火药"还是"真的烈焰弹"。</p>
 *
 * <p>委托调用与音效播放都在服务端主线程上同步完成，因此一个普通布尔量就足够；
 * 仍用 try/finally 保证异常时一定会复位。</p>
 */
public final class GunpowderIgnition {

    private static boolean active;

    private GunpowderIgnition() {
    }

    public static boolean isActive() {
        return active;
    }

    public static void begin() {
        active = true;
    }

    public static void end() {
        active = false;
    }
}
