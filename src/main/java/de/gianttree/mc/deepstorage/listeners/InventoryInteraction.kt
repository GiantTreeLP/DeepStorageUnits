package de.gianttree.mc.deepstorage.listeners

import de.gianttree.mc.deepstorage.DeepStorageUnits
import org.bukkit.Material
import org.bukkit.block.Chest
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.InventoryHolder
import org.bukkit.persistence.PersistentDataHolder

class InventoryInteraction(private val plugin: DeepStorageUnits) : Listener {

    @EventHandler
    fun blockBlockedSlots(event: InventoryClickEvent) {
        val inventory = event.clickedInventory ?: return
        if (plugin.isDSU(inventory.holder as? PersistentDataHolder)) {
            if (plugin.isBlocker(event.currentItem)) {
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
        if (container is Chest && plugin.isDSU(container) && !plugin.isBlocker(event.currentItem)) {
            val dsu = plugin.dsuForChest(container) ?: return

            if (event.action in PLACE_ACTIONS) {
                event.view.setCursor(dsu.addItem(event.cursor))
                event.result = Event.Result.DENY
            } else if (event.action in HOT_BAR_ACTIONS) {
                if (event.hotbarButton == -1 && event.whoClicked.inventory.itemInOffHand.type != Material.AIR) {
                    event.whoClicked.inventory.setItemInOffHand(dsu.addItem(event.whoClicked.inventory.itemInOffHand))
                    event.result = Event.Result.DENY
                } else if (event.hotbarButton != -1 && event.whoClicked.inventory.getItem(event.hotbarButton)?.type != Material.AIR) {
                    event.whoClicked.inventory.setItem(
                        event.hotbarButton,
                        dsu.addItem(event.whoClicked.inventory.getItem(event.hotbarButton))
                    )
                    event.result = Event.Result.DENY
                }

            }
        } else if (container is InventoryHolder) {
            val chest = event.inventory.holder
            if (chest is Chest && plugin.isDSU(chest)) {
                val dsu = plugin.dsuForChest(chest) ?: return
                if (!plugin.isBlocker(event.currentItem) &&
                    event.clickedInventory != event.inventory &&
                    event.action == InventoryAction.MOVE_TO_OTHER_INVENTORY
                ) {
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
        if (container is Chest && plugin.isDSU(container) && !plugin.isBlocker(event.currentItem)) {
            val dsu = plugin.dsuForChest(container) ?: return

            val cursor = event.cursor
            if (cursor.type == Material.AIR) {
                when (event.action) {
                    InventoryAction.PICKUP_ALL -> {
                        event.view.setCursor(dsu.retrieveItemFullStack())
                        event.result = Event.Result.DENY
                    }

                    InventoryAction.PICKUP_HALF -> {
                        event.view.setCursor(dsu.retrieveItemFullStack())
                        event.result = Event.Result.DENY
                    }

                    in HOT_BAR_ACTIONS -> {
                        if (dsu.hasItem()) {
                            if (event.hotbarButton == -1) {
                                if (event.whoClicked.inventory.itemInOffHand.type == Material.AIR) {
                                    event.whoClicked.inventory.setItemInOffHand(dsu.retrieveItemFullStack())
                                    event.result = Event.Result.DENY
                                }
                            } else {
                                if (event.whoClicked.inventory.getItem(event.hotbarButton) == null) {
                                    event.whoClicked.inventory.setItem(event.hotbarButton, dsu.retrieveItemFullStack())
                                    event.result = Event.Result.DENY
                                }
                            }
                        }
                    }

                    else -> {}
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

    companion object {
        private val PLACE_ACTIONS = setOf(
            InventoryAction.PLACE_ALL,
            InventoryAction.PLACE_SOME,
            InventoryAction.PLACE_ONE,
            InventoryAction.SWAP_WITH_CURSOR,
        )

        private val HOT_BAR_ACTIONS = setOf(
            InventoryAction.HOTBAR_SWAP,
        )
    }
}
