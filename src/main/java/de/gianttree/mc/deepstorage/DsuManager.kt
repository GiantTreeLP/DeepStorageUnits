package de.gianttree.mc.deepstorage

import de.gianttree.mc.deepstorage.listeners.InventoryInteraction
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.block.Chest
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataHolder
import org.bukkit.persistence.PersistentDataType

class DsuManager(
    private val plugin: DeepStorageUnits,
    private val inventoryInteraction: InventoryInteraction
) {

    private val blockerItem = ItemStack(Material.BLACK_STAINED_GLASS_PANE, 1).apply {
        val meta = itemMeta ?: return@apply
        meta.displayName(Component.space())
        meta.persistentDataContainer[plugin.blockMarker, PersistentDataType.BYTE] = 1
        itemMeta = meta
    }

    private fun fillInventory(chest: Chest) {
        val inv = chest.snapshotInventory

        if (!chest.wasCreated()) {
            val contents = inv.contents
            if (contents != null) {
                inv.setContents(Array(inv.size) {
                    if (it != inv.size / 2) {
                        blockerItem
                    } else {
                        contents[it] ?: ItemStack(Material.AIR)
                    }
                })
            }
            chest.persistentDataContainer[plugin.createdMarker, PersistentDataType.BYTE] = 1.toByte()
        }
    }

    fun createInventory(chest: Chest, name: Component?) {
        chest.customName(name)
        fillInventory(chest)
    }

    private fun PersistentDataHolder.wasCreated(): Boolean {
        return this.persistentDataContainer[plugin.createdMarker, PersistentDataType.BYTE] == 1.toByte()
    }

}
