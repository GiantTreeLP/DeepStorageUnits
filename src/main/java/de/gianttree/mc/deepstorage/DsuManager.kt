package de.gianttree.mc.deepstorage

import de.gianttree.mc.deepstorage.listeners.InventoryInteraction
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.EnderChest
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin

class DsuManager(private val plugin: Plugin,
                 private val inventoryInteraction: InventoryInteraction) {

    private val blockMarker = NamespacedKey(plugin, "dsuBlocker")

    fun openInventory(player: Player, name: String, chest: EnderChest) {
        val inventory = inventoryInteraction.openInventories.computeIfAbsent(chest) { _ ->
            val inv = Bukkit.createInventory(player, InventoryType.CHEST, name)

            inv.contents = Array(inv.size) {
                ItemStack(Material.BLACK_STAINED_GLASS_PANE, 1).apply {
                    val meta = itemMeta ?: return@apply
                    meta.setDisplayName(" ")
                    meta.persistentDataContainer[blockMarker, PersistentDataType.BYTE] = 1
                    itemMeta = meta
                }
            }

            inv
        }
        Bukkit.getScheduler().runTask(plugin) { _ ->
            player.openInventory(inventory)
        }
    }
}
