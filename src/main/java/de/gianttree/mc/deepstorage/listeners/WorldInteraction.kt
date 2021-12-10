package de.gianttree.mc.deepstorage.listeners

import de.gianttree.mc.deepstorage.DeepStorageUnits
import de.gianttree.mc.deepstorage.DsuManager
import de.gianttree.mc.deepstorage.unit.DeepStorageUnit
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Particle
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
import org.bukkit.block.data.type.Chest as DataTypeChest

class WorldInteraction(
    private val plugin: DeepStorageUnits,
    private val dsuManager: DsuManager
) : Listener {

    @EventHandler
    fun onBlockPlace(event: BlockPlaceEvent) {
        val chest = event.block.state
        if (chest is Chest) {
            val meta = event.itemInHand.itemMeta ?: return
            val markerValue = meta.persistentDataContainer.get(plugin.dsuMarker, PersistentDataType.BYTE)
            val blockData = chest.blockData
            if (markerValue == 1.toByte()) {

                // Prevent double chests
                if ((chest.blockData as DataTypeChest).type != DataTypeChest.Type.SINGLE) {
                    event.isCancelled = true
                }

                chest.persistentDataContainer.set(plugin.dsuMarker, PersistentDataType.BYTE, markerValue)
//                chest.persistentDataContainer.set(
//                    plugin.nameKey,
//                    PersistentDataType.STRING,
//                    meta.displayName().toString()
//                )

                dsuManager.createInventory(chest, meta.displayName())
                chest.update()
            } else {
                Bukkit.getScheduler().runTask(plugin, Runnable {
                    if ((blockData as DataTypeChest).type != DataTypeChest.Type.SINGLE) {
                        for (x in -1..1) {
                            for (z in -1..1) {
                                val otherBlock =
                                    event.block.world.getBlockAt(event.block.x + x, event.block.y, event.block.z + z)
                                val otherState = otherBlock.state
                                val otherBlockData = otherState.blockData
                                if (otherState is Chest
                                    && otherState.isDSU()
                                    && otherBlockData is DataTypeChest
                                    && otherBlockData.type != DataTypeChest.Type.SINGLE
                                    && otherBlockData.facing == blockData.facing
                                ) {
                                    otherBlockData.type = DataTypeChest.Type.SINGLE
                                    otherState.blockData = otherBlockData
                                    otherState.update()
                                    blockData.type = DataTypeChest.Type.SINGLE
                                    chest.blockData = blockData
                                    chest.update()
                                    return@Runnable
                                }
                            }
                        }
                    }

                })
            }
        }
    }

    @EventHandler
    fun onBreak(event: BlockBreakEvent) {
        val chest = event.block.state
        if (chest is Chest && chest.isDSU()) {
            val dsu = DeepStorageUnit.forChest(plugin, chest) ?: return
            while (dsu.hasItem()) {
                dsu.retrieveItemFullStack()?.let {
                    if (it.amount > 0 && it.type != Material.AIR) {
                        chest.world.dropItemNaturally(chest.location, it)
                    }
                }
            }
            chest.snapshotInventory.clear()
            chest.update()
            if (event.isDropItems) {
                chest.world.dropItemNaturally(chest.location, dsu.getDrop())
            }
            chest.world.spawnParticle(Particle.BLOCK_CRACK, chest.location, 48, chest.blockData)
            chest.world.playSound(chest.location, chest.blockData.soundGroup.breakSound, 1f, 1f)
            chest.block.type = Material.AIR
            event.isCancelled = true
            DeepStorageUnit.invalidate(chest)
        }
    }

    @EventHandler
    fun hopperMove(event: InventoryMoveItemEvent) {
        val chest = event.source.holder
        if (chest is Chest && chest.isDSU()) {
            event.isCancelled = true
            val hopper = event.destination.holder as? Hopper ?: return
            val dsu = DeepStorageUnit.forChest(plugin, chest) ?: return
            val item = dsu.retrieveItemOne()
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
