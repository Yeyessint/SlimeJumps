package net.licory.slimejumps.util;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/**
 * Small helpers for building clickable chat messages.
 * <p>
 * Buttons can either run a command immediately or pre-fill it in the
 * player's chat box so they can complete an argument (e.g. a pad name)
 * before sending.
 */
public final class ChatUI {

    private ChatUI() {
    }

    /** Translates {@code &} colour codes to a chat component. */
    public static TextComponent text(String legacy) {
        return new TextComponent(TextComponent.fromLegacyText(
                ChatColor.translateAlternateColorCodes('&', legacy)));
    }

    /**
     * Builds a button that runs {@code command} when clicked.
     *
     * @param label   coloured label (supports {@code &} codes)
     * @param command command to run, with or without the leading slash
     * @param hover   coloured tooltip shown on hover
     */
    public static TextComponent runButton(String label, String command, String hover) {
        return button(label, command, hover, ClickEvent.Action.RUN_COMMAND);
    }

    /**
     * Builds a button that pre-fills {@code command} in the chat box so
     * the player can finish typing an argument before sending.
     */
    public static TextComponent suggestButton(String label, String command, String hover) {
        return button(label, command, hover, ClickEvent.Action.SUGGEST_COMMAND);
    }

    private static TextComponent button(String label, String command, String hover,
                                        ClickEvent.Action action) {
        TextComponent component = text(label);
        component.setClickEvent(new ClickEvent(action, withSlash(command)));
        component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new Text(TextComponent.fromLegacyText(
                        ChatColor.translateAlternateColorCodes('&', hover)))));
        return component;
    }

    /** Sends one or more components as a single chat line to the player. */
    public static void send(Player player, TextComponent... parts) {
        ComponentBuilder builder = new ComponentBuilder("");
        for (TextComponent part : parts) {
            builder.append(part, ComponentBuilder.FormatRetention.NONE);
        }
        player.spigot().sendMessage(builder.create());
    }

    private static String withSlash(String command) {
        return command.startsWith("/") ? command : "/" + command;
    }
}
