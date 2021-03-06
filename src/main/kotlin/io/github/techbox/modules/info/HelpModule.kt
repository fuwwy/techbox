package io.github.techbox.modules.info

import io.github.techbox.TechboxLauncher
import io.github.techbox.core.modules.Module
import io.github.techbox.core.modules.commands.*
import io.github.techbox.data.Config.prefix
import io.github.techbox.utils.addField
import io.github.techbox.utils.nameAndDiscriminator
import me.xdrop.fuzzywuzzy.FuzzySearch
import me.xdrop.fuzzywuzzy.model.BoundExtractedResult
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.entities.MessageEmbed

@Module
class HelpModule {
    val commandRegistry = TechboxLauncher.core.commandRegistry

    fun onLoad() {
        command("help", "h") {
            category = Category.INFO

            execute {
                if (args.isEmpty()) {
                    sendHelp()
                } else {
                    findHelp(args.joinToString(" "))
                }
            }

            help {
                title = "Help Command"
                aliases = arrayOf("help", "h")
                description = "Here's some help about helping."

                usages {
                    usage("help", "Lists all commands.")
                    usage("help <command>", "Displays a command's help.")
                    usage("help <category>", "Displays a category's help.")
                }
            }
        }
    }

    fun CommandContext.sendHelp() {
        replyEmbed {
            setAuthor("Techbox | Help")

            setDescription(
                """
            Here's all the cmmands I have available.
            To check command usage, use `${usedPrefix}help <command>`
        """
            )

            setFooter(
                "${commandRegistry.commands.size} commands | Requested by ${author.nameAndDiscriminator}",
                author.effectiveAvatarUrl
            )

            Category.values().forEach { category ->
                var count = 0
                val commands = commandRegistry.commands
                    .map { it.value }
                    .filter { it.category == category }
                    .map { it.aliases[0] }
                    .apply { ++count }
                    .sorted()
                    .toList()

                if (command.isNotEmpty()) {
                    addField(
                        "${category.categoryName} ($count)",
                        commands.joinToString(prefix = "`", separator = "` `", postfix = "`"),
                        false
                    )
                }
            }
        }
    }

    fun findAnything(args: String): Any? {
        return commandRegistry.commands[args.toLowerCase()]
            ?: commandRegistry.commands[commandRegistry.aliases.getOrDefault(args.toLowerCase(), "")]
            ?: Category.values()
                .firstOrNull {
                    it.name.equals(args, ignoreCase = true) || it.categoryName.equals(
                        args,
                        ignoreCase = true
                    )
                }
    }

    fun CommandContext.findHelp(args: String) {
        var toHelp = findAnything(args)
        var extra: String? = null
        if (toHelp == null) {
            val match: BoundExtractedResult<String> = FuzzySearch.extractOne(
                args.toLowerCase(), commandRegistry.helpPossibilities
            ) { it -> it }
            if (match.score > 70) {
                val newArg = match.referent
                toHelp = findAnything(newArg)
                if (toHelp != null) extra = "Couldn't find anything named `$args`, did you mean `$newArg`?"
            }
        }
        var help: MessageEmbed? = null

        when (toHelp) {
            is ICommand, is Command -> {
                help = (toHelp as ICommand).onHelp(this)
            }
            is Category -> {
                help = EmbedBuilder().also {
                    it.setAuthor("Techbox | Help: ${toHelp.categoryName}")
                    it.setDescription(
                        """
                    Here's all the category's commands.
                    To check the command usage, type `${prefix[0]}help <command>`.
                """
                    )

                    val commands = commandRegistry.commands
                        .map { it.value }
                        .filter { it.category == toHelp }
                        .map { it.aliases[0] }
                        .sorted()
                        .toList()

                    if (commands.isNotEmpty()) {
                        it.addField("Commands:", commands.joinToString(prefix = "`", separator = "` `", postfix = "`"))
                    } else {
                        it.setDescription("Nobody here but us chickens.")
                    }

                    it.setFooter(
                        "${commands.size} commands | Requested by ${author.nameAndDiscriminator}",
                        author.effectiveAvatarUrl
                    )
                }.build()
            }
            null -> {
                reply("❌There's no command or category with that name!")
                return
            }
        }
        if (help != null) reply(MessageBuilder().setEmbed(help).append(extra ?: "").build())
        else reply("❌Sorry, there is no help available for that command.")
    }

}
