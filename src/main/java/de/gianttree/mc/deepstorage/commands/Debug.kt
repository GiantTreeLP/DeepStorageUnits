package de.gianttree.mc.deepstorage.commands

import de.gianttree.mc.deepstorage.DeepStorageUnits
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.block.data.type.Chest
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor

class Debug(
    private val plugin: DeepStorageUnits
) : TabExecutor {
    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): MutableList<String>? {
        when (args.size) {
            1 -> return mutableListOf("debug").filter { it.startsWith(args.last()) }.toMutableList()
            2 -> when (args[0]) {
                "debug" -> return mutableListOf("chests").filter { it.startsWith(args.last()) }.toMutableList()
            }
        }
        return null
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isNotEmpty()) {
            when (args[0]) {
                "debug" -> {
                    return this.debug(sender, command, label, args.slice(1 until args.size))
                }
            }
        }
        return false
    }

    private fun debug(sender: CommandSender, command: Command, label: String, args: List<String>): Boolean {
        if (args.isNotEmpty()) {
            when (args[0]) {
                "chests" -> {
                    sender.sendMessage("Cached chests: ${this.plugin.cached.size}")
                    this.plugin.cached.forEach { dsu ->
                        val chest = dsu.chest
                        val chestBlock = chest.block
                        val chestBlockData = chestBlock.blockData
                        if (chest.type == Material.CHEST && chestBlockData is Chest) {
                            sender.sendMessage(Component.text {
                                val location = dsu.chest.location
                                val tpLocation = chestBlock.getRelative((chestBlockData).facing).location
                                it.append(
                                    Component.text("Chest: ").color(NamedTextColor.YELLOW)
                                        .append(Component.text("[Item]").color(NamedTextColor.DARK_AQUA))
                                        .hoverEvent(
                                            dsu.getItem() ?: Component.text("No Item").color(NamedTextColor.RED)
                                        )
                                )
                                it.append(Component.newline())
                                it.append(Component.text("  Location: ").color(NamedTextColor.GREEN))
                                it.append(
                                    Component.text("${location.world.name} ${location.blockX} ${location.blockY} ${location.blockZ}")
                                        .color(NamedTextColor.YELLOW)
                                        .clickEvent(
                                            ClickEvent.clickEvent(
                                                ClickEvent.Action.SUGGEST_COMMAND,
                                                "/execute in ${location.world.key} run tp @s ${tpLocation.blockX} ${tpLocation.blockY} ${tpLocation.blockZ}"
                                            )
                                        )
                                        .hoverEvent(
                                            HoverEvent.hoverEvent(
                                                HoverEvent.Action.SHOW_TEXT,
                                                Component.text("Click to teleport to this location")
                                            )
                                        )
                                )
                                it.append(Component.newline())
                                it.append(
                                    Component.text("  Item count: ").color(NamedTextColor.GREEN)
                                        .append(Component.text(dsu.itemCount.toString()).color(NamedTextColor.YELLOW))
                                )
                                it.append(Component.newline())
                                it.append(
                                    Component.text("  Item limit: ").color(NamedTextColor.GREEN)
                                        .append(Component.text(dsu.limit.toString()).color(NamedTextColor.YELLOW))
                                )
                                it.append(Component.newline())
                                it.append(
                                    Component.text("  Upgrade count: ").color(NamedTextColor.GREEN)
                                        .append(Component.text(dsu.upgrades.toString()).color(NamedTextColor.YELLOW))
                                )
                                it.append(Component.newline())

                            })
                        }
                    }
                    return true
                }
            }
        }
        return false
    }
}
