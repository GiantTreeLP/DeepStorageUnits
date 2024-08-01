package de.gianttree.mc.deepstorage

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.gson.reflect.TypeToken
import de.gianttree.mc.deepstorage.commands.Debug
import de.gianttree.mc.deepstorage.listeners.InventoryInteraction
import de.gianttree.mc.deepstorage.listeners.ItemInteraction
import de.gianttree.mc.deepstorage.listeners.WorldInteraction
import de.gianttree.mc.deepstorage.unit.DeepStorageUnit
import de.gianttree.mc.deepstorage.unit.EyeCandy
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.Chest
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.persistence.PersistentDataHolder
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import java.time.Duration

class DeepStorageUnits : JavaPlugin() {

    internal val blockMarker = NamespacedKey(this, "dsuBlocker")
    internal val dsuMarker = NamespacedKey(this, "dsuMarker")
    internal val createdMarker = NamespacedKey(this, "dsuCreated")

    internal val loreKey = NamespacedKey(this, "dsuOriginalLore")

    internal val itemCountKey = NamespacedKey(this, "dsuItemCount")
    internal val itemLimitKey = NamespacedKey(this, "dsuItemLimit")

    internal val upgradeMarker = NamespacedKey(this, "dsuUpgradeMarker")
    internal val upgradeKey = NamespacedKey(this, "dsuUpgrades")

    internal val items = Items(this)

    internal val componentSerializer = GsonComponentSerializer.builder().build()

    private val dsuCache = CacheBuilder.newBuilder()
        .expireAfterAccess(Duration.ofMinutes(1))
        .softValues()
        .build<Chest, DeepStorageUnit>(
            CacheLoader.from { chest ->
                DeepStorageUnit(this, chest)
            }
        )

    val cached get() = this.dsuCache.asMap().mapNotNull { it.value }

    fun dsuForChest(chest: Chest): DeepStorageUnit? {
        return this.dsuCache.get(chest)
    }

    fun invalidate(chest: Chest) {
        this.dsuCache.invalidate(chest)
    }

    override fun onEnable() {
        this.saveDefaultConfig()

        this.addRecipes()
        this.registerListeners()
        this.registerCommands()
        this.registerEyeCandy()
    }

    private fun registerCommands() {
        this.getCommand("deepstorageunits")?.apply {
            val command = Debug(this@DeepStorageUnits)
            this.tabCompleter = command
            this.setExecutor(command)
            this.permission = "deepstorageunits.debug"
        }
    }

    private fun registerListeners() {
        Bukkit.getPluginManager().registerEvents(InventoryInteraction(this), this)
        Bukkit.getPluginManager().registerEvents(WorldInteraction(this), this)
        Bukkit.getPluginManager().registerEvents(ItemInteraction(this), this)
    }

    private fun addRecipes() {
        Bukkit.addRecipe(ShapedRecipe(NamespacedKey(this, "dsu_item"), this.items.emptyDeepStorageUnit).apply {
            shape("ABA", "BCB", "ABA")
            setIngredient('A', Material.EMERALD)
            setIngredient('B', Material.DIAMOND)
            setIngredient('C', Material.ENDER_CHEST)
        })
        Bukkit.addRecipe(ShapedRecipe(NamespacedKey(this, "dsu_upgrade"), this.items.upgradeItem).apply {
            shape("ACA", "ABA", "ADA")
            setIngredient('A', Material.OBSIDIAN)
            setIngredient('B', Material.ENDER_EYE)
            setIngredient('C', Material.DIAMOND)
            setIngredient('D', Material.EMERALD)
        })
    }

    private fun registerEyeCandy() {
        val eyeCandy = EyeCandy(this)

        eyeCandy.start()
    }

    internal fun isDSU(dataHolder: PersistentDataHolder?): Boolean {
        return dataHolder?.persistentDataContainer?.get(this.dsuMarker, PersistentDataType.BYTE) == 1.toByte()
    }

    internal fun isBlocker(itemStack: ItemStack?): Boolean {
        return itemStack?.itemMeta?.persistentDataContainer?.get(
            this.blockMarker,
            PersistentDataType.BYTE
        ) == 1.toByte()
    }

    internal fun isCreated(dataHolder: PersistentDataHolder): Boolean {
        return dataHolder.persistentDataContainer[this.createdMarker, PersistentDataType.BYTE] == 1.toByte()
    }

    companion object {
        const val DSU_NAME = "Deep Storage Unit"
        const val DSU_LORE = "A storage, deep as the ocean."

        const val UPGRADE_NAME = "Deep Upgrade"
        const val UPGRADE_LORE = "Upgrade your Deep Storage Unit with this item."

        internal val componentListType = TypeToken.getParameterized(List::class.java, Component::class.java).type

    }
}
