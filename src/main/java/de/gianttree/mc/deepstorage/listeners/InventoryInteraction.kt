package de.gianttree.mc.deepstorage.listeners

import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import org.bukkit.NamespacedKey
import org.bukkit.block.EnderChest
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin

class InventoryInteraction(plugin: Plugin) : Listener {

    private val blockMarker = NamespacedKey(plugin, "dsuBlocker")

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

    @EventHandler
    fun blockBlockedSlots(event: InventoryClickEvent) {
        val inventory = event.clickedInventory ?: return
        if (inventory in openInventories.inverse()) {
            val meta = event.currentItem?.itemMeta ?: return
            if (meta.persistentDataContainer[blockMarker, PersistentDataType.BYTE] == 1.toByte()) {
                event.result = Event.Result.DENY
            }
        }
    }

}
