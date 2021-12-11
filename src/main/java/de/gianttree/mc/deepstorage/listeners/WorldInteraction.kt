package de.gianttree.mc.deepstorage.listeners

import de.gianttree.mc.deepstorage.DeepStorageUnits
import de.gianttree.mc.deepstorage.DsuManager
import de.gianttree.mc.deepstorage.unit.DeepStorageUnit
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.block.Chest
import org.bukkit.block.DoubleChest
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
            val chestBlockData = chest.blockData as DataTypeChest
            if (meta.isDSU()) {

                chest.persistentDataContainer.set(plugin.dsuMarker, PersistentDataType.BYTE, 1.toByte())
//                chest.persistentDataContainer.set(
//                    plugin.nameKey,
//                    PersistentDataType.STRING,
//                    meta.displayName().toString()
//                )

                dsuManager.createInventory(chest, meta.displayName())
                chest.blockData = chestBlockData
                chest.update()
            }
            // Check whether the new chest is a double chest
            // and if so, check whether the other chest is already a DSU.
            // Then proceed to make the new chest a single chest.
            Bukkit.getScheduler().runTask(plugin, Runnable {
                val holder = chest.inventory.holder
                if (holder is DoubleChest) {
                    val otherBlockState = when (chestBlockData.type) {
                        DataTypeChest.Type.LEFT -> {
                            holder.leftSide as Chest
                        }
                        DataTypeChest.Type.RIGHT -> {
                            holder.rightSide as Chest
                        }
                        else -> {
                            return@Runnable
                        }
                    }
                    val otherBlockData = otherBlockState.blockData
                    if (otherBlockState.isDSU()
                        && otherBlockData is DataTypeChest
                    ) {
                        otherBlockData.type = DataTypeChest.Type.SINGLE
                        otherBlockState.blockData = otherBlockData
                        otherBlockState.update()
                        chestBlockData.type = DataTypeChest.Type.SINGLE
                        chest.blockData = chestBlockData
                        chest.update()
                    }
                }
            })
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

    private fun PersistentDataHolder.isDSU(): Boolean {
        return this.persistentDataContainer[plugin.dsuMarker, PersistentDataType.BYTE] == 1.toByte()
    }

    private fun ItemStack.isBlocker(): Boolean {
        val meta = this.itemMeta ?: return false
        return meta.persistentDataContainer[plugin.blockMarker, PersistentDataType.BYTE] == 1.toByte()
    }

    private fun Chest.getCenterItem(): ItemStack? {
        return this.inventory.getItem(this.inventory.size / 2)?.clone()
    }

}
