package de.gianttree.mc.deepstorage

import de.gianttree.mc.deepstorage.listeners.InventoryInteraction
import de.gianttree.mc.deepstorage.listeners.ItemInteraction
import de.gianttree.mc.deepstorage.listeners.WorldInteraction
import de.gianttree.mc.deepstorage.unit.EyeCandy
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.RecipeChoice
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.persistence.PersistentDataHolder
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin

class DeepStorageUnits : JavaPlugin() {

    internal val blockMarker = NamespacedKey(this, "dsuBlocker")
    internal val dsuMarker = NamespacedKey(this, "dsuMarker")
    internal val createdMarker = NamespacedKey(this, "dsuCreated")
    internal val itemCountKey = NamespacedKey(this, "dsuItemCount")
    internal val itemLimitKey = NamespacedKey(this, "dsuItemLimit")
    internal val upgradeMarker = NamespacedKey(this, "dsuUpgradeMarker")
    internal val upgradeKey = NamespacedKey(this, "dsuUpgrades")

    internal val emptyDeepStorageUnit = ItemStack(Material.CHEST, 1).apply {
        this.editMeta {
            it.displayName(Component.text(dsuName))
            it.lore(listOf(Component.text(dsuLore)))

            it.persistentDataContainer.set(
                dsuMarker,
                PersistentDataType.BYTE,
                1
            )
        }
    }

    internal val upgradeItem = ItemStack(Material.ENDER_EYE, 1).apply {
        this.editMeta {
            it.displayName(Component.text(upgradeName))
            it.lore(listOf(Component.text(upgradeLore)))
            it.persistentDataContainer.set(
                upgradeMarker,
                PersistentDataType.BYTE,
                1
            )

            it.addEnchant(Enchantment.DURABILITY, 1, false)
            it.addItemFlags(ItemFlag.HIDE_ENCHANTS)
        }
    }

    override fun onEnable() {
        this.saveDefaultConfig()

        this.addRecipes()
        this.registerListeners()
        this.registerEyeCandy()
    }

    private fun registerListeners() {
        val inventoryInteraction = InventoryInteraction(this)
        Bukkit.getPluginManager().registerEvents(inventoryInteraction, this)
        Bukkit.getPluginManager().registerEvents(WorldInteraction(this, DsuManager(this, inventoryInteraction)), this)
        Bukkit.getPluginManager().registerEvents(ItemInteraction(this), this)
    }

    private fun addRecipes() {
        Bukkit.addRecipe(ShapedRecipe(NamespacedKey(this, "DsuItem"), emptyDeepStorageUnit).apply {
            shape("ABA", "BCB", "ABA")
            setIngredient('A', Material.EMERALD)
            setIngredient('B', Material.DIAMOND)
            setIngredient('C', RecipeChoice.ExactChoice(ItemStack(Material.ENDER_CHEST)))
        })
        Bukkit.addRecipe(ShapedRecipe(NamespacedKey(this, "DsuUpgrade"), upgradeItem).apply {
            shape("AAA", "ABA", "AAA")
            setIngredient('A', Material.OBSIDIAN)
            setIngredient('B', Material.ENDER_EYE)
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

    companion object {
        const val dsuName = "Deep Storage Unit"
        const val dsuLore = "A storage, deep as the ocean."

        const val upgradeName = "Deep Upgrade"
        const val upgradeLore = "Upgrade your Deep Storage Unit with this item."
    }
}
