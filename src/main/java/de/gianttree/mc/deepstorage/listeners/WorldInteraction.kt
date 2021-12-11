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
import org.bukkit.entity.minecart.HopperMinecart
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.inventory.InventoryMoveItemEvent
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
            val centerLocation = chest.location.toCenterLocation()
            val dsu = DeepStorageUnit.forChest(plugin, chest) ?: return
            while (dsu.hasItem()) {
                dsu.retrieveItemFullStack()?.let {
                    if (it.amount > 0 && it.type != Material.AIR) {
                        chest.world.dropItemNaturally(chest.location, it)
                    }
                }
            }
            if (dsu.upgrades > 0) {
                chest.world.dropItemNaturally(
                    centerLocation,
                    plugin.upgradeItem.clone().apply {
                        amount = dsu.upgrades
                    })
            }
            chest.snapshotInventory.clear()
            chest.update()
            if (event.isDropItems) {
                chest.world.dropItemNaturally(chest.location, dsu.getDrop())
            }
            chest.world.spawnParticle(Particle.BLOCK_CRACK, centerLocation, 48, chest.blockData)
            chest.world.playSound(centerLocation, chest.blockData.soundGroup.breakSound, 1f, 1f)
            chest.block.type = Material.AIR
            event.isCancelled = true
            DeepStorageUnit.invalidate(chest)
        }
    }

    @EventHandler
    fun hopperMove(event: InventoryMoveItemEvent) {
        val source = event.source.holder
        val destination = event.destination.holder
        // Items are pulled from a DSU
        if (source is Chest
            && source.isDSU()
        ) {
            if (destination is Hopper) {
                event.isCancelled = true
                val dsu = DeepStorageUnit.forChest(plugin, source) ?: return
                val item = dsu.retrieveItemOne()
                if (item != null) {
                    event.item = item

                    destination.snapshotInventory.addItem(item)
                    destination.update()
                }
            } else if (destination is HopperMinecart) {
                event.isCancelled = true
                val dsu = DeepStorageUnit.forChest(plugin, source) ?: return
                val item = dsu.retrieveItemOne()
                if (item != null) {
                    event.item = item
                    val residual = destination.inventory.addItem(item)
                    residual.forEach { (_, item) ->
                        dsu.addItem(item)
                    }
                }
            }
        } else if (source is Hopper) {
            // Items are pushed into a DSU
            if (destination is Chest && destination.isDSU()) {
//                event.isCancelled = true
                val dsu = DeepStorageUnit.forChest(plugin, destination) ?: return
                val item = dsu.addItem(event.item)
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
