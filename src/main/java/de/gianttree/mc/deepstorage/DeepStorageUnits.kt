package de.gianttree.mc.deepstorage

import de.gianttree.mc.deepstorage.listeners.InventoryInteraction
import de.gianttree.mc.deepstorage.listeners.WorldInteraction
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.*
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin

class DeepStorageUnits : JavaPlugin() {

    private val emptyDeepStorageUnit = ItemStack(Material.ENDER_CHEST, 1).apply {
        val meta = this.itemMeta ?: throw NullPointerException()
        meta.setDisplayName(dsuName)
        meta.lore = listOf(dsuLore)
        meta.persistentDataContainer.set(
                NamespacedKey(this@DeepStorageUnits, "dsuMarker"),
                PersistentDataType.BYTE,
                1)
        meta.addEnchant(Enchantment.DURABILITY, 1, false)
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS)
        itemMeta = meta
    }

    override fun onEnable() {
        this.saveDefaultConfig()

        this.addRecipes()
        this.registerListeners()
    }

    private fun registerListeners() {
        val inventoryInteraction = InventoryInteraction()
        Bukkit.getPluginManager().registerEvents(inventoryInteraction, this)
        Bukkit.getPluginManager().registerEvents(WorldInteraction(this, DsuManager(this, inventoryInteraction)), this)
    }

    private fun addRecipes() {
        Bukkit.addRecipe(ShapedRecipe(NamespacedKey(this, "DsuItem"), emptyDeepStorageUnit).apply {
            shape("ABA", "BCB", "ABA")
            setIngredient('A', Material.EMERALD)
            setIngredient('B', Material.DIAMOND)
            setIngredient('C', RecipeChoice.ExactChoice(ItemStack(Material.ENDER_CHEST)))
        })
    }

    companion object {
        const val dsuLore = "A storage, deep as the ocean."
        const val dsuName = "Deep Storage Unit"
    }
}
