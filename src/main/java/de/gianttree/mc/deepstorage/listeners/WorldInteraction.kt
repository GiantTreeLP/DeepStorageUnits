package de.gianttree.mc.deepstorage.listeners

import de.gianttree.mc.deepstorage.DeepStorageUnits
import de.gianttree.mc.deepstorage.DsuManager
import org.bukkit.Bukkit
import org.bukkit.block.Chest
import org.bukkit.block.Hopper
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.inventory.InventoryMoveItemEvent
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataHolder
import org.bukkit.persistence.PersistentDataType

class WorldInteraction(private val plugin: DeepStorageUnits,
                       private val dsuManager: DsuManager) : Listener {

    @EventHandler
    fun onBlockPlace(event: BlockPlaceEvent) {
        val chest = event.block.state
        if (chest is Chest) {
            val meta = event.itemInHand.itemMeta ?: return
            val markerValue = meta.persistentDataContainer.get(plugin.dsuMarker, PersistentDataType.BYTE)
            if (markerValue == 1.toByte()) {

                // Prevent double chests
                if ((event.block.blockData as org.bukkit.block.data.type.Chest).type != org.bukkit.block.data.type.Chest.Type.SINGLE) {
                    event.isCancelled = true
                }

                chest.persistentDataContainer.set(plugin.dsuMarker, PersistentDataType.BYTE, markerValue)
                chest.persistentDataContainer.set(plugin.nameKey, PersistentDataType.STRING, meta.displayName)

                Bukkit.getScheduler().runTask(plugin) { _ ->
                    dsuManager.createInventory(chest, meta.displayName)
                    chest.update()
                }
            }
        }
    }

    @EventHandler
    fun onBreak(event: BlockBreakEvent) {
        val chest = event.block.state
        if (chest is Chest && chest.isDSU()) {
            chest.snapshotInventory.contents = chest.snapshotInventory.contents.filterNot {
                it?.isBlocker() ?: false
            }.toTypedArray()
            chest.persistentDataContainer[plugin.createdMarker, PersistentDataType.BYTE] = 0.toByte()
            chest.update(true)
        }
    }

    @EventHandler
    fun hopperMove(event: InventoryMoveItemEvent) {
        val chest = event.source.holder
        if (chest is Chest && chest.isDSU()) {
            event.isCancelled = true
            val item = chest.getCenterItem()
            val hopper = event.destination.holder as? Hopper ?: return
            if (item != null) {
                event.item = item
                item.amount = 1

                chest.snapshotInventory.removeItem(item)
                chest.update()
                hopper.snapshotInventory.addItem(item)
                hopper.update()
            }
        }
    }

    private fun InventoryHolder.isDSU(): Boolean {
        return this is PersistentDataHolder &&
                this.persistentDataContainer[plugin.dsuMarker, PersistentDataType.BYTE] == 1.toByte()
    }

    private fun ItemStack.isBlocker(): Boolean {
        val meta = this.itemMeta ?: return false
        return meta.persistentDataContainer[plugin.blockMarker, PersistentDataType.BYTE] == 1.toByte()
    }

    private fun Chest.getCenterItem(): ItemStack? {
        return this.inventory.getItem(this.inventory.size / 2)?.clone()
    }

}
