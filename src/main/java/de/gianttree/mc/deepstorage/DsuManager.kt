package de.gianttree.mc.deepstorage

import de.gianttree.mc.deepstorage.listeners.InventoryInteraction
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.block.Chest
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataHolder
import org.bukkit.persistence.PersistentDataType

class DsuManager(private val plugin: DeepStorageUnits,
                 private val inventoryInteraction: InventoryInteraction) {

    private val blockerItem = ItemStack(Material.BLACK_STAINED_GLASS_PANE, 1).apply {
        val meta = itemMeta ?: return@apply
        meta.setDisplayName(" ")
        meta.persistentDataContainer[plugin.blockMarker, PersistentDataType.BYTE] = 1
        itemMeta = meta
    }

    private fun fillInventory(chest: Chest) {
        val inv = chest.snapshotInventory

        if (!chest.wasCreated()) {
            inv.contents = Array(inv.size) {
                if (it != inv.size / 2) {
                    blockerItem
                } else {
                    inv.contents[it]
                }
            }
            chest.persistentDataContainer[plugin.createdMarker, PersistentDataType.BYTE] = 1.toByte()
        }
    }

    fun createInventory(chest: Chest, name: String) {
        chest.customName = ChatColor.BLUE.toString() + name
        fillInventory(chest)
    }

    private fun PersistentDataHolder.wasCreated(): Boolean {
        return this.persistentDataContainer[plugin.createdMarker, PersistentDataType.BYTE] == 1.toByte()
    }

}
