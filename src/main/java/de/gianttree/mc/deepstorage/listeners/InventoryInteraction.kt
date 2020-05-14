package de.gianttree.mc.deepstorage.listeners

import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import org.bukkit.block.EnderChest
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory

class InventoryInteraction : Listener {

    val openInventories: BiMap<EnderChest, Inventory> = HashBiMap.create()

    @EventHandler
    fun onClose(event: InventoryCloseEvent) {
        val inventory = event.inventory
        if (inventory in openInventories.inverse()) {
            if (inventory.viewers.size == 1) {
                openInventories.inverse().remove(inventory)
            }
        }
    }

}
