package carpet.cof;

import carpet.CarpetServer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 模组主入口。
 *
 * <p>这里只做一件事：把自己交给 Carpet 托管成"扩展"。
 * 必须在 Fabric 的 {@code main} 入口里注册扩展，因为 Fabric 先跑
 * {@code main}，再跑 {@code server}/{@code client} 入口，而 Carpet 的
 * {@code onGameStarted()} 就是从它自己的 server/client 入口触发的。</p>
 *
 * <p>本模组<b>不注册任何方块或物品</b>：结冰功能只是把原版水炼药锅清空、
 * 并在锅中心生成一个原版冰块掉落物，所以没有任何注册时机问题。</p>
 */
public class CofMod implements ModInitializer {
    public static final String MOD_ID = "cof";
    public static final String MOD_NAME = "Carpet CoF Addition";

    public static final String MOD_VERSION = FabricLoader.getInstance()
            .getModContainer(MOD_ID)
            .map(container -> container.getMetadata().getVersion().getFriendlyString())
            .orElse("dev");

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        CarpetServer.manageExtension(CofExtension.INSTANCE);
        LOGGER.info("[{}] v{} 初始化完成", MOD_NAME, MOD_VERSION);
    }
}
