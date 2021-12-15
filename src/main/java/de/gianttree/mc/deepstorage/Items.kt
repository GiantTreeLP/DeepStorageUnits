package de.gianttree.mc.deepstorage

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

class Items(
    private val plugin: DeepStorageUnits
) {
    internal val emptyDeepStorageUnit = ItemStack(Material.CHEST, 1).apply {
        this.editMeta {
            it.displayName(Component.text(DeepStorageUnits.dsuName).color(NamedTextColor.AQUA))
            it.lore(listOf(Component.text(DeepStorageUnits.dsuLore)))

            it.persistentDataContainer[plugin.dsuMarker, PersistentDataType.BYTE] = 1
        }
    }

    internal val upgradeItem = ItemStack(Material.ENDER_EYE, 1).apply {
        this.editMeta {
            it.displayName(Component.text(DeepStorageUnits.upgradeName).color(NamedTextColor.GOLD))
            it.lore(listOf(Component.text(DeepStorageUnits.upgradeLore)))
            it.persistentDataContainer[plugin.upgradeMarker, PersistentDataType.BYTE] = 1

            it.addEnchant(Enchantment.DURABILITY, 1, false)
            it.addItemFlags(ItemFlag.HIDE_ENCHANTS)
        }
    }

    internal val blockerItem = ItemStack(Material.BLACK_STAINED_GLASS_PANE, 1).apply {
        this.editMeta {
            it.displayName(Component.space())
            it.persistentDataContainer[plugin.blockMarker, PersistentDataType.BYTE] = 1
        }
    }

    internal val infoItem = ItemStack(Material.NETHERITE_SHOVEL, 1).apply {
        this.editMeta {
            it.displayName(Component.space())
            it.persistentDataContainer[plugin.blockMarker, PersistentDataType.BYTE] = 1
        }
    }
}
