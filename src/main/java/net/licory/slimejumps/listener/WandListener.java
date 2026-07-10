package net.licory.slimejumps.listener;

import net.licory.slimejumps.SlimeJumpsPlugin;
import net.licory.slimejumps.model.JumpPad;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

/**
 * The pad editor wand ({@code /sj wand}): a stick that creates and
 * removes pads without typing commands.
 * <ul>
 *   <li><b>Left click</b> a block — register it as a new pad
 *       ({@code pad1}, {@code pad2}, … with default values).</li>
 *   <li><b>Right click</b> a pad block — delete that pad.</li>
 * </ul>
 * The wand is identified by a persistent data tag, so renaming the item
 * does not break it and ordinary sticks are never mistaken for it.
 */
public final class WandListener implements Listener {

    private static final String ADMIN_PERMISSION = "slimejumps.admin";
    private static final String WAND_TAG = "wand";

    private final SlimeJumpsPlugin plugin;
    private final NamespacedKey wandKey;

    public WandListener(SlimeJumpsPlugin plugin) {
        this.plugin = plugin;
        this.wandKey = new NamespacedKey(plugin, WAND_TAG);
    }

    /** Builds a new wand item. */
    public ItemStack createWand() {
        ItemStack wand = new ItemStack(Material.STICK);
        ItemMeta meta = wand.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(plugin.getMessages().get("wand-name"));
            meta.setLore(List.of(
                    plugin.getMessages().get("wand-lore-create"),
                    plugin.getMessages().get("wand-lore-remove")));
            meta.getPersistentDataContainer().set(wandKey, PersistentDataType.BYTE, (byte) 1);
            wand.setItemMeta(meta);
        }
        return wand;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        ItemStack item = event.getItem();
        if (item == null || !isWand(item)) {
            return;
        }

        Player player = event.getPlayer();
        if (!player.hasPermission(ADMIN_PERMISSION)) {
            return;
        }

        // In route edit mode the wand edits waypoints instead of pads.
        if (plugin.getRouteEditManager().isEditing(player)) {
            event.setCancelled(true);
            boolean leftClick = event.getAction() == Action.LEFT_CLICK_BLOCK
                    || event.getAction() == Action.LEFT_CLICK_AIR;
            plugin.getRouteEditManager().handleWandClick(player, leftClick);
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        event.setCancelled(true);

        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            JumpPad pad = plugin.getPadManager().createAuto(block.getLocation());
            if (pad == null) {
                plugin.getMessages().send(player, "wand-pad-here");
                return;
            }
            plugin.getMessages().send(player, "wand-created", "name", pad.getName());
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            JumpPad pad = plugin.getPadManager().getAt(block);
            if (pad == null) {
                plugin.getMessages().send(player, "wand-no-pad");
                return;
            }
            String name = pad.getName();
            plugin.getPadManager().delete(name);
            plugin.getStatsManager().clearPad(name);
            plugin.getHologramManager().remove(name);
            plugin.getMessages().send(player, "pad-removed", "name", name);
        }
    }

    private boolean isWand(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        return meta != null
                && meta.getPersistentDataContainer().has(wandKey, PersistentDataType.BYTE);
    }
}
