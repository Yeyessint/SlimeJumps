package net.licory.slimejumps.manager;

import net.licory.slimejumps.SlimeJumpsPlugin;
import net.licory.slimejumps.util.ChatUI;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Drives the interactive in-game tutorial ({@code /sj tutorial}).
 * <p>
 * Each player picks a language, then walks through a series of steps
 * that explain the plugin with examples and clickable "try it" buttons.
 * Navigation is done entirely by clicking buttons in chat, which run
 * {@code /sj tutorial next|back|stop} behind the scenes.
 */
public final class TutorialManager implements Listener {

    /** Number of tutorial steps (message keys tutorial-1..N-title/-body). */
    public static final int STEP_COUNT = 8;

    /**
     * Example command each step invites the player to try, or {@code null}
     * for steps with no single command (they are suggested, not run, so
     * the player can edit the name first).
     */
    private static final String[] TRY_COMMANDS = {
            null,
            "/sj create spawn",
            "/sj preview spawn",
            "/sj sethologram spawn &a&lJUMP!",
            "/sj wand",
            "/sj route create lobby2arena",
            "/sj setcharge spawn 1500",
            null,
    };

    private final SlimeJumpsPlugin plugin;
    private final Map<UUID, String> language = new HashMap<>();
    private final Map<UUID, Integer> step = new HashMap<>();

    public TutorialManager(SlimeJumpsPlugin plugin) {
        this.plugin = plugin;
    }

    /** Entry point: pick a language first, then show the current step. */
    public void open(Player player) {
        if (!language.containsKey(player.getUniqueId())) {
            showLanguagePicker(player);
            return;
        }
        showStep(player, step.getOrDefault(player.getUniqueId(), 0));
    }

    /** Shows the clickable language picker. */
    public void showLanguagePicker(Player player) {
        String lang = language.get(player.getUniqueId());
        player.sendMessage(plugin.getMessages().getIn(lang, "tutorial-pick-language"));
        for (String code : plugin.getMessages().availableLanguages()) {
            TextComponent button = ChatUI.runButton(
                    plugin.getMessages().getIn(lang, "tutorial-language-" + code),
                    "/sj tutorial lang " + code,
                    plugin.getMessages().getIn(lang, "tutorial-language-hover"));
            ChatUI.send(player, ChatUI.text("&8» "), button);
        }
    }

    /** Sets the tutorial language for a player and starts from step 1. */
    public void setLanguage(Player player, String code) {
        String normalised = code.toLowerCase(Locale.ROOT);
        if (!plugin.getMessages().hasLanguage(normalised)) {
            plugin.getMessages().send(player, "tutorial-language-unknown", "input", code);
            return;
        }
        language.put(player.getUniqueId(), normalised);
        showStep(player, 0);
    }

    /** Advances to the next step, finishing the tutorial past the last one. */
    public void next(Player player) {
        int current = step.getOrDefault(player.getUniqueId(), 0);
        if (current + 1 >= STEP_COUNT) {
            finish(player);
        } else {
            showStep(player, current + 1);
        }
    }

    /** Goes back one step (clamped at the first). */
    public void back(Player player) {
        int current = step.getOrDefault(player.getUniqueId(), 0);
        showStep(player, Math.max(0, current - 1));
    }

    /** Ends the tutorial for a player. */
    public void stop(Player player) {
        step.remove(player.getUniqueId());
        String lang = language.get(player.getUniqueId());
        player.sendMessage(plugin.getMessages().getIn(lang, "tutorial-stopped"));
    }

    private void finish(Player player) {
        step.remove(player.getUniqueId());
        String lang = language.get(player.getUniqueId());
        for (String line : plugin.getMessages().getListIn(lang, "tutorial-finished")) {
            player.sendMessage(line);
        }
    }

    private void showStep(Player player, int index) {
        int clamped = Math.max(0, Math.min(STEP_COUNT - 1, index));
        step.put(player.getUniqueId(), clamped);
        String lang = language.get(player.getUniqueId());
        int number = clamped + 1;

        player.sendMessage("");
        player.sendMessage(plugin.getMessages().getIn(lang, "tutorial-header",
                "step", String.valueOf(number),
                "total", String.valueOf(STEP_COUNT),
                "title", plugin.getMessages().getIn(lang, "tutorial-" + number + "-title")));
        for (String line : plugin.getMessages().getListIn(lang, "tutorial-" + number + "-body")) {
            player.sendMessage(line);
        }

        String tryCommand = TRY_COMMANDS[clamped];
        if (tryCommand != null) {
            TextComponent tryButton = ChatUI.suggestButton(
                    plugin.getMessages().getIn(lang, "tutorial-try", "command", tryCommand),
                    tryCommand,
                    plugin.getMessages().getIn(lang, "tutorial-try-hover"));
            ChatUI.send(player, tryButton);
        }

        sendNavigation(player, lang, clamped);
    }

    /** Builds the [Back] · step x/y · [Next]/[Finish] navigation line. */
    private void sendNavigation(Player player, String lang, int index) {
        TextComponent back = index > 0
                ? ChatUI.runButton(plugin.getMessages().getIn(lang, "tutorial-back"),
                        "/sj tutorial back", plugin.getMessages().getIn(lang, "tutorial-back-hover"))
                : ChatUI.text("&8[«]");

        TextComponent forward = index + 1 >= STEP_COUNT
                ? ChatUI.runButton(plugin.getMessages().getIn(lang, "tutorial-finish"),
                        "/sj tutorial stop", plugin.getMessages().getIn(lang, "tutorial-finish-hover"))
                : ChatUI.runButton(plugin.getMessages().getIn(lang, "tutorial-next"),
                        "/sj tutorial next", plugin.getMessages().getIn(lang, "tutorial-next-hover"));

        ChatUI.send(player,
                back,
                ChatUI.text("  &7" + (index + 1) + "&8/&7" + STEP_COUNT + "  "),
                forward);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        step.remove(id);
        language.remove(id);
    }
}
