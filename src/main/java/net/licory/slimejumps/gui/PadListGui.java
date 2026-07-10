package net.licory.slimejumps.gui;

import net.licory.slimejumps.SlimeJumpsPlugin;
import net.licory.slimejumps.model.JumpPad;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Paginated chest GUI listing every jump pad ({@code /sj gui}).
 * <p>
 * Each pad is shown as a slime block with its details in the lore;
 * clicking it teleports the viewer to the pad. Arrows at the bottom
 * switch pages.
 */
public final class PadListGui implements Listener {

    /** Pads per page: 5 full rows; the bottom row is reserved for navigation. */
    private static final int PADS_PER_PAGE = 45;
    private static final int INVENTORY_SIZE = 54;
    private static final int PREVIOUS_SLOT = 45;
    private static final int NEXT_SLOT = 53;

    /** Marks our inventories and remembers which pad each slot holds. */
    private static final class Holder implements InventoryHolder {
        final int page;
        final List<JumpPad> slots = new ArrayList<>(PADS_PER_PAGE);
        Inventory inventory;

        Holder(int page) {
            this.page = page;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    private final SlimeJumpsPlugin plugin;

    public PadListGui(SlimeJumpsPlugin plugin) {
        this.plugin = plugin;
    }

    /** Opens the pad list for a player at the given page (0-based). */
    public void open(Player player, int page) {
        List<JumpPad> pads = new ArrayList<>(plugin.getPadManager().getAll());
        int pageCount = Math.max(1, (pads.size() + PADS_PER_PAGE - 1) / PADS_PER_PAGE);
        int current = Math.min(Math.max(0, page), pageCount - 1);

        Holder holder = new Holder(current);
        String title = plugin.getMessages().get("gui-title",
                "page", String.valueOf(current + 1),
                "pages", String.valueOf(pageCount));
        Inventory inventory = Bukkit.createInventory(holder, INVENTORY_SIZE, title);
        holder.inventory = inventory;

        int start = current * PADS_PER_PAGE;
        int end = Math.min(start + PADS_PER_PAGE, pads.size());
        for (int i = start; i < end; i++) {
            JumpPad pad = pads.get(i);
            inventory.setItem(i - start, padItem(pad));
            holder.slots.add(pad);
        }

        if (current > 0) {
            inventory.setItem(PREVIOUS_SLOT,
                    namedItem(Material.ARROW, plugin.getMessages().get("gui-previous")));
        }
        if (current < pageCount - 1) {
            inventory.setItem(NEXT_SLOT,
                    namedItem(Material.ARROW, plugin.getMessages().get("gui-next")));
        }

        player.openInventory(inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof Holder holder)) {
            return;
        }
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        int slot = event.getRawSlot();

        if (slot == PREVIOUS_SLOT && event.getInventory().getItem(PREVIOUS_SLOT) != null) {
            open(player, holder.page - 1);
            return;
        }
        if (slot == NEXT_SLOT && event.getInventory().getItem(NEXT_SLOT) != null) {
            open(player, holder.page + 1);
            return;
        }
        if (slot < 0 || slot >= holder.slots.size()) {
            return;
        }

        JumpPad pad = holder.slots.get(slot);
        Location location = pad.toLocation();
        if (location == null) {
            plugin.getMessages().send(player, "pad-world-unloaded", "name", pad.getName());
            return;
        }
        player.teleport(location.add(0.5D, 1.0D, 0.5D));
        plugin.getMessages().send(player, "pad-teleported", "name", pad.getName());
    }

    private ItemStack padItem(JumpPad pad) {
        ItemStack item = new ItemStack(pad.isEnabled() ? Material.SLIME_BLOCK : Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(plugin.getMessages().get("gui-pad-name", "name", pad.getName()));
            List<String> lore = new ArrayList<>();
            lore.add(plugin.getMessages().get("gui-pad-location",
                    "world", pad.getWorldName(),
                    "x", String.valueOf(pad.getX()),
                    "y", String.valueOf(pad.getY()),
                    "z", String.valueOf(pad.getZ())));
            lore.add(plugin.getMessages().get("gui-pad-power",
                    "power", String.valueOf(pad.getPower()),
                    "vertical", String.valueOf(pad.getVertical())));
            if (pad.getRouteName() != null) {
                lore.add(plugin.getMessages().get("gui-pad-route", "route", pad.getRouteName()));
            }
            if (!pad.isEnabled()) {
                lore.add(plugin.getMessages().get("gui-pad-disabled"));
            }
            lore.add(plugin.getMessages().get("gui-pad-click"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack namedItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }
}
