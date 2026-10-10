// SPDX-License-Identifier: MIT
// Copyright (c) 2026 CoF contributors

package carpet.cof;

import carpet.CarpetExtension;
import carpet.CarpetServer;
import carpet.utils.Translations;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;

import java.util.Map;

/**
 * Carpet 扩展本体：负责把规则注册进 Carpet，并把本模组的语言文件交给 Carpet。
 */
public class CofExtension implements CarpetExtension {
    public static final CofExtension INSTANCE = new CofExtension();

    private CofExtension() {
    }

    /** Carpet 用这个字符串在日志/列表里标识扩展，填 mod id 即可。 */
    @Override
    public String version() {
        return CofMod.MOD_ID;
    }

    /**
     * 注册规则。用 Carpet 自己的 settings manager，
     * 规则就会出现在 {@code /carpet} 里。
     */
    @Override
    @SuppressWarnings("removal") // CarpetServer.settingsManager 的类型是 Carpet 的旧别名类，但依然是官方推荐的注册点
    public void onGameStarted() {
        CarpetServer.settingsManager.parseSettingsClass(CofSettings.class);
        CofMod.LOGGER.info("[{}] 规则已注册，用 /carpet 查看", CofMod.MOD_NAME);
    }

    /**
     * 注册本模组自己的命令。
     *
     * <p>Carpet 会在注册原版/carpet 命令时回调扩展的这个方法，
     * 所以附属不需要额外依赖就能加命令。</p>
     */
    @Override
    public void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher,
                                 CommandBuildContext commandBuildContext) {
        CofDebugCommand.register(dispatcher);
    }

    /**
     * Carpet 的翻译系统不认识别的模组的资源目录，
     * 所以由我们自己把 assets/&lt;modid&gt;/lang/&lt;语言&gt;.json 读出来交给它。
     * 键名沿用 Carpet 的约定：{@code carpet.rule.<规则名>} 和 {@code carpet.category.<分类>}。
     */
    @Override
    public Map<String, String> canHasTranslations(String lang) {
        return Translations.getTranslationFromResourcePath(
                "assets/" + CofMod.MOD_ID + "/lang/" + lang + ".json");
    }
}
