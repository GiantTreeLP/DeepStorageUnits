package de.gianttree.mc.deepstorage.listeners

import de.gianttree.mc.deepstorage.DeepStorageUnits
import de.gianttree.mc.deepstorage.DsuManager
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.NamespacedKey
import org.bukkit.block.EnderChest
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.persistence.PersistentDataType

class WorldInteraction(private val plugin: DeepStorageUnits,
                       private val dsuManager: DsuManager) : Listener {

    private val dsuMarkerKey = NamespacedKey(plugin, "dsuMarker")
    private val nameKey = NamespacedKey(plugin, "dsuName")

    @EventHandler
    fun onBlockPlace(event: BlockPlaceEvent) {
        val enderChest = event.block.state
        if (enderChest is EnderChest) {
            val meta = event.itemInHand.itemMeta ?: return
            val markerValue = meta.persistentDataContainer.get(dsuMarkerKey, PersistentDataType.BYTE)
            if (markerValue == 1.toByte()) {
                enderChest.persistentDataContainer.set(dsuMarkerKey, PersistentDataType.BYTE, markerValue)
                enderChest.persistentDataContainer.set(nameKey, PersistentDataType.STRING, meta.displayName)
            }
            enderChest.update()
        }
    }

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        if (event.action == Action.RIGHT_CLICK_BLOCK) {
            val enderChest = event.clickedBlock?.state
            if (enderChest is EnderChest) {
                val dsuMarker = enderChest.persistentDataContainer.get(dsuMarkerKey, PersistentDataType.BYTE)
                if (dsuMarker == 1.toByte()) {
                    event.isCancelled = true
                    val name = ChatColor.AQUA.toString() + enderChest.persistentDataContainer.get(nameKey, PersistentDataType.STRING)
                    Bukkit.getScheduler().runTaskAsynchronously(plugin) { _ ->
                        dsuManager.openInventory(event.player, name, enderChest)
                    }
                }
            }
        }
    }
}
