package de.gianttree.mc.deepstorage.unit

import de.gianttree.mc.deepstorage.DeepStorageUnits
import de.gianttree.mc.deepstorage.DeepStorageUnits.Companion.componentListType
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.block.Chest
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

private const val STACK_SIZE = 64

class DeepStorageUnit(
    private val plugin: DeepStorageUnits,
    internal val chest: Chest
) {

    private val slot = chest.snapshotInventory.size / 2

    private val stackSize: Int
        get() {
            return getItem()?.maxStackSize ?: STACK_SIZE
        }

    private val baseSize: Long
        get() {
            return (chest.snapshotInventory.size * stackSize).toLong()
        }

    internal var itemCount: Long = 0
        get() = chest.persistentDataContainer[plugin.itemCountKey, PersistentDataType.LONG]
            ?: 0
        private set(value) {
            field = value
            chest.persistentDataContainer[plugin.itemCountKey, PersistentDataType.LONG] = value
            this.update()
        }

    internal var limit: Long = 0
        get() {
            return chest.persistentDataContainer[plugin.itemLimitKey, PersistentDataType.LONG]
                ?: baseSize
        }
        private set(value) {
            field = value
            chest.persistentDataContainer[plugin.itemLimitKey, PersistentDataType.LONG] = value
            this.update()
        }

    internal var upgrades: Int = 0
        get() = chest.persistentDataContainer[plugin.upgradeKey, PersistentDataType.INTEGER]
            ?: 0
        private set(value) {
            field = value
            chest.persistentDataContainer[plugin.upgradeKey, PersistentDataType.INTEGER] = value
            this.update()
        }


    fun addItem(item: ItemStack?): ItemStack? {
        if (item != null) {
            val containedItem = getItem()
            if (containedItem == null) {
                setItem(item)
                item.amount = 0
                return null
            } else if (item.isSimilar(containedItem)) {
                if (this.itemCount + item.amount > this.limit) {
                    item.amount -= (this.limit - this.itemCount).toInt()
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
        chest.snapshotInventory.setItem(slot, item.clone())
        chest.update()
        this.itemCount = item.amount.toLong()
        this.limit = ((chest.snapshotInventory.size + this.upgrades) * item.maxStackSize).toLong()
    }

    fun retrieveItemFullStack(): ItemStack? {
        return getItem()?.apply {
            amount = stackSize.coerceAtMost(this@DeepStorageUnit.itemCount.toInt())
        }?.also {
            this.itemCount -= it.amount
        }
    }

    fun retrieveItemHalfStack(): ItemStack? {
        return getItem()?.apply {
            amount = (stackSize / 2)
                .coerceAtMost(this@DeepStorageUnit.itemCount.toInt() / 2)
                .coerceAtLeast(1)
        }?.also {
            this.itemCount -= it.amount
        }
    }

    fun retrieveItemOne(): ItemStack? {
        return getItem()?.also {
            this.itemCount -= it.amount
        }
    }

    fun addUpgrade() {
        this.upgrades++
        this.limit = baseSize + this.upgrades * stackSize
//        this.update()
    }

    private fun getItem(): ItemStack? {
        val item = chest.snapshotInventory.getItem(this.slot)?.clone()
        return item?.apply {
            this.amount = 1
            val stored = item.itemMeta.persistentDataContainer[plugin.loreKey, PersistentDataType.STRING]
            if (stored != null) {
                item.editMeta {
                    it.lore(plugin.componentSerializer.serializer().fromJson(stored, componentListType))
                    it.persistentDataContainer.remove(plugin.loreKey)
                }
            } else {
                item.lore(null)
            }
        }
    }

    private fun update() {
        val item = chest.snapshotInventory.getItem(this.slot)
        if (item != null && item.type != Material.AIR) {
            val localStackSize = stackSize
            val stored = item.itemMeta.persistentDataContainer[plugin.loreKey, PersistentDataType.STRING]
            if (stored == null) {
                val serialized = plugin.componentSerializer.serializer().toJson(item.lore())
                item.editMeta {
                    it.persistentDataContainer[plugin.loreKey, PersistentDataType.STRING] = serialized
                }
            }


            val formattedLimit = String.format("%,d", limit)
            val formattedCount = String.format("%,d", itemCount)
            val formattedStackLimit = String.format("%,d", limit / localStackSize)
            val formattedUpgrades = String.format("%,d", upgrades)
            val formattedStackCount = String.format("%,d", itemCount / localStackSize)
            val formattedRemainderCount = String.format("%,d", itemCount % localStackSize)
            item.lore(
                listOf(
                    Component.text("Items: $formattedCount (${formattedStackCount}S + $formattedRemainderCount)"),
                    Component.text("Limit: $formattedLimit items ($formattedStackLimit stacks)"),
                    Component.text("Upgrades: $formattedUpgrades")
                )
            )
            item.amount = this.itemCount.coerceAtMost(localStackSize.toLong()).toInt()
        }
        if (this.itemCount == 0L) {
            chest.snapshotInventory.setItem(this.slot, null)
        } else {
            chest.snapshotInventory.setItem(slot, item)
        }
        chest.update()
    }

    override fun toString(): String {
        return "DeepStorageUnit(plugin=$plugin, chest=$chest, slot=$slot, itemCount=$itemCount)"
    }

    fun hasItem(): Boolean {
        return getItem() != null
    }

    fun getDrop(): ItemStack {
        return plugin.emptyDeepStorageUnit.clone().apply {
            val meta = this.itemMeta
            meta.displayName(chest.customName())
            this.itemMeta = meta
        }
    }
}
