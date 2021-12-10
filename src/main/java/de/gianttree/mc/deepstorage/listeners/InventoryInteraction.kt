package de.gianttree.mc.deepstorage.listeners

import de.gianttree.mc.deepstorage.DeepStorageUnits
import de.gianttree.mc.deepstorage.unit.DeepStorageUnit
import org.bukkit.Material
import org.bukkit.block.Chest
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataHolder
import org.bukkit.persistence.PersistentDataType

class InventoryInteraction(private val plugin: DeepStorageUnits) : Listener {

    @EventHandler
    fun blockBlockedSlots(event: InventoryClickEvent) {
        val inventory = event.clickedInventory ?: return
        if (inventory.holder?.isDSU() == true) {
            if (event.currentItem.isBlocker()) {
                event.result = Event.Result.DENY
            }
        }
    }

    @EventHandler
    fun addItems(event: InventoryClickEvent) {
        if (event.isCancelled) {
            return
        }
        val inventory = event.clickedInventory ?: return
        // Click in DSU
        val container = inventory.holder
        if (container is Chest && container.isDSU() && !event.currentItem.isBlocker()) {
            val dsu = DeepStorageUnit.forChest(plugin, container) ?: return

            if (event.action in PLACE_ACTIONS) {
                event.view.cursor = dsu.addItem(event.cursor)
                event.result = Event.Result.DENY
            }
        } else if (container is InventoryHolder) {
            val chest = event.inventory.holder
            if (chest is Chest && chest.isDSU()) {
                val dsu = DeepStorageUnit.forChest(plugin, chest) ?: return
                if (event.action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                    event.currentItem = dsu.addItem(event.currentItem)
                    event.result = Event.Result.DENY
                }
            }
        }
    }

    @EventHandler
    fun retrieveItems(event: InventoryClickEvent) {
        if (event.isCancelled) {
            return
        }
        val inventory = event.clickedInventory ?: return
        // Click in DSU
        val container = inventory.holder
        if (container is Chest && container.isDSU() && !event.currentItem.isBlocker()) {
            val dsu = DeepStorageUnit.forChest(plugin, container) ?: return

            val cursor = event.cursor
            if (cursor == null || cursor.type == Material.AIR) {
                when (event.action) {
                    InventoryAction.PICKUP_ALL -> {
                        event.view.cursor = dsu.retrieveItemFullStack()
                        event.result = Event.Result.DENY
                    }
                    InventoryAction.PICKUP_HALF -> {
                        event.view.cursor = dsu.retrieveItemHalfStack()
                        event.result = Event.Result.DENY
                    }
                }
            }
            if (event.action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                val item = dsu.retrieveItemFullStack()
                val playerInventory = event.whoClicked.inventory
                if (item != null) {
                    val residual = playerInventory.addItem(item)
                    residual.forEach { (_, item) ->
                        dsu.addItem(item)
                    }
                    event.result = Event.Result.DENY
                }
            }
        }
    }

    private fun InventoryHolder.isDSU(): Boolean {
        return this is PersistentDataHolder &&
                this.persistentDataContainer[plugin.dsuMarker, PersistentDataType.BYTE] == 1.toByte()
    }

    private fun ItemStack?.isBlocker() =
        this?.itemMeta?.persistentDataContainer?.get(plugin.blockMarker, PersistentDataType.BYTE) == 1.toByte()

    companion object {
        private val PLACE_ACTIONS = setOf(
            InventoryAction.PLACE_ALL,
            InventoryAction.PLACE_SOME,
            InventoryAction.SWAP_WITH_CURSOR,
        )
    }
}
