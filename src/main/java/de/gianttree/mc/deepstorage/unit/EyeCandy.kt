package de.gianttree.mc.deepstorage.unit

import com.destroystokyo.paper.ParticleBuilder
import de.gianttree.mc.deepstorage.DeepStorageUnits
import org.bukkit.Bukkit
import org.bukkit.Particle

class EyeCandy(
    private val plugin: DeepStorageUnits
) {
    fun start() {
        Bukkit.getScheduler().runTaskTimer(
            plugin,
            Runnable {
                plugin.cached.forEach {
                    ParticleBuilder(Particle.PORTAL)
                        .location(it.chest.location.toCenterLocation())
                        .count(4)
                        .receivers(24)
                        .spawn()
                }
            },
            20L,
            20L
        )
    }

}
