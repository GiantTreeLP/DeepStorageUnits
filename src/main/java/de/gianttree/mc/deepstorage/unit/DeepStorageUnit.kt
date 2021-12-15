package de.gianttree.mc.deepstorage.unit

import de.gianttree.mc.deepstorage.DeepStorageUnits
import de.gianttree.mc.deepstorage.DeepStorageUnits.Companion.componentListType
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.block.Chest
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import org.bukkit.persistence.PersistentDataType

private const val NUM_STACKS = 64
private const val STACK_SIZE = 64

class DeepStorageUnit(
    private val plugin: DeepStorageUnits,
    internal val chest: Chest
) {

    private val centerSlot = chest.snapshotInventory.size / 2
    private val infoSlot = centerSlot + 9

    private val stackSize: Int
        get() = getItem()?.maxStackSize ?: STACK_SIZE

    private val baseSize: Long
        get() = (NUM_STACKS * stackSize).toLong()

    private val bonusStacks: Int
        get() = this.upgrades * 8

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

    init {
        val inv = chest.snapshotInventory

        if (!plugin.isCreated(chest)) {
            val contents = inv.storageContents
            inv.setContents(Array(inv.size) {
                if (it != centerSlot) {
                    plugin.items.blockerItem
                } else {
                    contents[it]
                }
            })
            chest.persistentDataContainer[plugin.createdMarker, PersistentDataType.BYTE] = 1.toByte()
        }
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
        chest.snapshotInventory.setItem(centerSlot, item.clone())
        chest.update()
        this.itemCount = item.amount.toLong()
        this.limit = baseSize + (this.bonusStacks * stackSize).toLong()
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
        this.limit = baseSize + this.bonusStacks * stackSize
//        this.update()
    }

    private fun getItem(): ItemStack? {
        val item = chest.snapshotInventory.getItem(this.centerSlot)?.clone()
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
        val item = chest.snapshotInventory.getItem(this.centerSlot)
        if (item != null && item.type != Material.AIR) {
            updateItem(item)
        }
        if (this.itemCount == 0L) {
            chest.snapshotInventory.setItem(this.centerSlot, null)
        } else {
            chest.snapshotInventory.setItem(this.centerSlot, item)
        }
        val infoItem = plugin.items.infoItem.clone()
        updateItem(infoItem)
        updateInfoItem(infoItem)
        chest.snapshotInventory.setItem(this.infoSlot, infoItem)
        chest.update()
    }

    private fun updateItem(item: ItemStack) {
        item.editMeta {
            val localStackSize = this.stackSize
            val stored = it.persistentDataContainer[plugin.loreKey, PersistentDataType.STRING]
            if (stored == null) {
                val serialized = this.plugin.componentSerializer.serializer().toJson(it.lore())
                it.persistentDataContainer[this.plugin.loreKey, PersistentDataType.STRING] = serialized
            }

            val formattedLimit = String.format("%,d", this.limit)
            val formattedCount = String.format("%,d", this.itemCount)
            val formattedStackLimit = String.format("%,d", this.limit / localStackSize)
            val formattedUpgrades = String.format("%,d", this.upgrades)
            val formattedStackCount = String.format("%,d", this.itemCount / localStackSize)
            val formattedRemainderCount = String.format("%,d", this.itemCount % localStackSize)
            it.lore(
                listOf(
                    Component.text("Items: $formattedCount (${formattedStackCount}S + $formattedRemainderCount)"),
                    Component.text("Limit: $formattedLimit items ($formattedStackLimit stacks)"),
                    Component.text("Upgrades: $formattedUpgrades")
                )
            )
            item.amount = this.itemCount.coerceAtMost(localStackSize.toLong()).toInt()
        }
    }

    private fun updateInfoItem(item: ItemStack) {
        item.editMeta {
            item.amount = 1
            if (it is Damageable) {
                it.damage = ((this.itemCount.toDouble() / this.limit.toDouble()) * item.type.maxDurability).toInt()
            }
        }
    }

    fun hasItem() = getItem() != null

    fun getDrop(): ItemStack {
        return plugin.items.emptyDeepStorageUnit.clone().apply {
            this.editMeta {
                it.displayName(chest.customName())
            }
        }
    }

    override fun toString(): String {
        return "DeepStorageUnit(chest=$chest, slot=$centerSlot, stackSize=$stackSize, baseSize=$baseSize, scaleUpgrades=$bonusStacks, itemCount=$itemCount, limit=$limit, upgrades=$upgrades)"
    }
}
