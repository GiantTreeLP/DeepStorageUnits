package de.gianttree.mc.deepstorage.listeners

import de.gianttree.mc.deepstorage.DeepStorageUnits
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.InventoryHolder
import org.bukkit.persistence.PersistentDataHolder
import org.bukkit.persistence.PersistentDataType

class InventoryInteraction(private val plugin: DeepStorageUnits) : Listener {

    @EventHandler
    fun blockBlockedSlots(event: InventoryClickEvent) {
        val inventory = event.clickedInventory ?: return
        if (inventory.holder?.isDSU() == true) {
            val meta = event.currentItem?.itemMeta ?: return
            if (meta.persistentDataContainer[plugin.blockMarker, PersistentDataType.BYTE] == 1.toByte()) {
                event.result = Event.Result.DENY
            }
        }
    }

    private fun InventoryHolder.isDSU(): Boolean {
        return this is PersistentDataHolder &&
                this.persistentDataContainer[plugin.dsuMarker, PersistentDataType.BYTE] == 1.toByte()
    }
}
