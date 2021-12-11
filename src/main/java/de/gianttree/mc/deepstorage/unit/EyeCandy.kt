package de.gianttree.mc.deepstorage.unit

import de.gianttree.mc.deepstorage.DeepStorageUnits
import org.bukkit.Bukkit
import org.bukkit.Particle

class EyeCandy(
    private val plugin: DeepStorageUnits
) {
    fun start() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(
            plugin,
            Runnable {
                plugin.cached.forEach {
                    it.chest.world.spawnParticle(
                        Particle.PORTAL,
                        it.chest.location.toCenterLocation(),
                        8,
                    )
                }
            },
            20L,
            20L
        )
    }

}
