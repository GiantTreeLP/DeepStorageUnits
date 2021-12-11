package de.gianttree.mc.deepstorage.listeners

import de.gianttree.mc.deepstorage.DeepStorageUnits
import de.gianttree.mc.deepstorage.unit.DeepStorageUnit
import org.bukkit.block.Chest
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataHolder
import org.bukkit.persistence.PersistentDataType

class ItemInteraction(
    private val plugin: DeepStorageUnits
) : Listener {
    @EventHandler
    fun addUpgrade(event: PlayerInteractEvent) {
        val blockState = event.clickedBlock?.state
        if (event.action == Action.RIGHT_CLICK_BLOCK
            && event.hasItem()
        ) {
            val item = event.item!!
            if (item.isUpgrade()
                && blockState is Chest
                && blockState.isDSU()
            ) {
                val dsu = DeepStorageUnit.forChest(plugin, blockState) ?: return
                dsu.addUpgrade()
                item.amount--
                event.isCancelled = true
            }
        }
    }

    private fun PersistentDataHolder.isDSU(): Boolean {
        return this.persistentDataContainer[plugin.dsuMarker, PersistentDataType.BYTE] == 1.toByte()
    }

    private fun ItemStack.isUpgrade(): Boolean {
        return this.itemMeta.persistentDataContainer[plugin.upgradeMarker, PersistentDataType.BYTE] == 1.toByte()
    }
}
