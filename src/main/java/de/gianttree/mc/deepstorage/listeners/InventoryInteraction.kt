package de.gianttree.mc.deepstorage.listeners

import de.gianttree.mc.deepstorage.DeepStorageUnits
import de.gianttree.mc.deepstorage.unit.DeepStorageUnit
import org.bukkit.block.Chest
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryAction
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

    @EventHandler
    fun addItems(event: InventoryClickEvent) {
        val inventory = event.clickedInventory ?: return
        // Click in DSU
        val container = inventory.holder
        if (container?.isDSU() == true && container is Chest) {
            if (event.action in PLACE_ACTIONS) {
                event.result = Event.Result.DENY
                val dsu = DeepStorageUnit.forChest(plugin, container) ?: return
                event.view.cursor = dsu.addItem(event.cursor)

                println(dsu)
            }
        }
    }

    private fun InventoryHolder.isDSU(): Boolean {
        return this is PersistentDataHolder &&
                this.persistentDataContainer[plugin.dsuMarker, PersistentDataType.BYTE] == 1.toByte()
    }

    companion object {
        private val PLACE_ACTIONS = setOf(
                InventoryAction.PLACE_ALL,
                InventoryAction.PLACE_ONE,
                InventoryAction.PLACE_SOME
        )
    }
}
