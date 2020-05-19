package de.gianttree.mc.deepstorage.unit

import de.gianttree.mc.deepstorage.DeepStorageUnits
import org.bukkit.block.Chest
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import java.lang.ref.WeakReference
import java.util.*

class DeepStorageUnit(private val plugin: DeepStorageUnits,
                      private val chest: Chest) {

    private val slot = chest.inventory.size / 2

    private var itemCount: Long = chest.persistentDataContainer[plugin.itemCountKey, PersistentDataType.LONG]
            ?: 0
        set(value) {
            field = value
            chest.persistentDataContainer[plugin.itemCountKey, PersistentDataType.LONG] = value
            this.update()
        }

    private var limit: Long = chest.persistentDataContainer[plugin.itemLimitKey, PersistentDataType.LONG]
            ?: (chest.inventory.size * 64).toLong()
        set(value) {
            field = value
            chest.persistentDataContainer[plugin.itemLimitKey, PersistentDataType.LONG] = value
            this.update()
        }


    fun addItem(item: ItemStack?): ItemStack? {
        if (item != null) {
            val containedItem = getItem()
            if (containedItem == null) {
                setItem(item)
                return null
            } else if (item.isSimilar(containedItem)) {
                if (this.itemCount + item.amount > this.limit) {
                    item.amount = (this.limit - this.itemCount).toInt()
                    this.itemCount = this.limit
                } else {
                    this.itemCount += item.amount
                    item.amount = 0
                }
            }
        }
        return item
    }

    private fun setItem(item: ItemStack) {
        chest.blockInventory.setItem(slot, item.clone())
        chest.update()
        this.itemCount = item.amount.toLong()
        this.limit = (chest.inventory.size * item.maxStackSize).toLong()
    }

    fun retrieveItem(count: Int): ItemStack? {
        return getItem()?.apply {
            amount = count
        }.also {
            this.itemCount -= count
        }
    }

    private fun getItem(): ItemStack? {
        val item = chest.inventory.getItem(this.slot)?.clone()
        return item?.apply {
            this.amount = 1
            val meta = this.itemMeta ?: return@apply
            meta.lore = null
            this.itemMeta = meta
        }
    }


    private fun update() {
        val item = chest.inventory.getItem(slot)
        if (item != null) {
            item.amount = this.itemCount.coerceAtMost(item.maxStackSize.toLong()).toInt()
            val meta = item.itemMeta
            meta.lore = listOf("Items: $itemCount (${itemCount / 64} + ${itemCount % 64})")
            item.itemMeta = meta
        }
        chest.inventory.setItem(slot, item)
        chest.update()
    }

    override fun toString(): String {
        return "DeepStorageUnit(plugin=$plugin, chest=$chest, slot=$slot, itemCount=$itemCount)"
    }

    companion object {

        private val cache = WeakHashMap<Chest, WeakReference<DeepStorageUnit>>()

        fun forChest(plugin: DeepStorageUnits, chest: Chest): DeepStorageUnit? {
            return cache.compute(chest) { key, value ->
                if (value?.get() == null) {
                    println("Created new DSU")
                    WeakReference(DeepStorageUnit(plugin, key))
                } else {
                    value
                }
            }!!.get()
        }
    }
}
