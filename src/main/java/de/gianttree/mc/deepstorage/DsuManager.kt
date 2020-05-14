package de.gianttree.mc.deepstorage

import de.gianttree.mc.deepstorage.listeners.InventoryInteraction
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.EnderChest
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin

class DsuManager(private val plugin: Plugin,
                 private val inventoryInteraction: InventoryInteraction) {

    private val blockMarker = NamespacedKey(plugin, "dsuBlocker")

    fun openInventory(player: Player, name: String, chest: EnderChest) {
        val inventory = createInventory(chest, name)
        Bukkit.getScheduler().runTask(plugin) { _ ->
            player.openInventory(inventory)
        }
    }

    private fun createInventory(chest: EnderChest, name: String): Inventory {
        return inventoryInteraction.openInventories.computeIfAbsent(chest) {
            val inv = Bukkit.createInventory(null, InventoryType.CHEST, name)

            inv.contents = Array(inv.size) {
                if (it != inv.size / 2) {
                    ItemStack(Material.BLACK_STAINED_GLASS_PANE, 1).apply {
                        val meta = itemMeta ?: return@apply
                        meta.setDisplayName(" ")
                        meta.persistentDataContainer[blockMarker, PersistentDataType.BYTE] = 1
                        itemMeta = meta
                    }
                } else {
                    null
                }
            }

            inv
        }
    }
}
