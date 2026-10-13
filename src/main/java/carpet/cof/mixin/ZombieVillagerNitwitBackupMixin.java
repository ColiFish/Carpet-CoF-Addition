// SPDX-License-Identifier: MIT
// Copyright (c) 2026 CoF contributors

package carpet.cof.mixin;

import carpet.cof.NitwitBackupHolder;
import carpet.cof.NitwitBackupKeys;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.npc.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 让「傻子可逆转化」的备份能**跨僵尸化往返**存活。
 *
 * <p>僵尸村民自己携带一份备份（自定义 NBT 标签 {@code CofNitwitBackup}），
 * 治愈完成时（原版 lambda {@code method_63659(ServerLevel, Villager)}）交还给新生成的村民。</p>
 *
 * <p>因此"傻子 → 僵尸村民 → 治愈 → 傻子"之后，{@code CofNitwitBackup} 仍在，
 * 依然可以喂金苹果转回原来的普通村民职业。</p>
 */
@Mixin(ZombieVillager.class)
public abstract class ZombieVillagerNitwitBackupMixin implements NitwitBackupHolder {

    @Unique
    private CompoundTag cof$nitwitBackup;

    @Override
    public CompoundTag cof$getNitwitBackup() {
        return this.cof$nitwitBackup;
    }

    @Override
    public void cof$setNitwitBackup(CompoundTag backup) {
        this.cof$nitwitBackup = backup;
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void cof$saveNitwitBackup(CompoundTag tag, CallbackInfo ci) {
        if (this.cof$nitwitBackup != null) {
            tag.put(NitwitBackupKeys.BACKUP, this.cof$nitwitBackup);
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void cof$readNitwitBackup(CompoundTag tag, CallbackInfo ci) {
        if (tag.contains(NitwitBackupKeys.BACKUP, 10)) {
            this.cof$nitwitBackup = tag.getCompound(NitwitBackupKeys.BACKUP);
        }
    }

    /** 治愈完成：把备份交还给新生成的村民。 */
    @Inject(method = "method_63659", at = @At("HEAD"))
    private void cof$handBackOnCure(ServerLevel level, Villager villager, CallbackInfo ci) {
        if (this.cof$nitwitBackup != null && villager instanceof NitwitBackupHolder holder) {
            holder.cof$setNitwitBackup(this.cof$nitwitBackup);
        }
    }
}
