package com.example.bsfinder;

import com.magmaguy.betterstructures.api.BuildPasteCompleteEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class BSFinderPlugin extends JavaPlugin implements Listener {
    private final Map<Integer, StructureRecord> records = new TreeMap<>();
    private int nextId = 1;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadRecords();
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("BSFinder enabled. Tracking completed BetterStructures builds.");
    }

    @Override
    public void onDisable() {
        saveRecords();
    }

    @EventHandler
    public void onStructurePasted(BuildPasteCompleteEvent event) {
        Location low = event.getLowestCorner();
        Location high = event.getHighestCorner();
        if (low == null || high == null || low.getWorld() == null) return;

        Location center = new Location(
                low.getWorld(),
                (low.getX() + high.getX()) / 2.0,
                low.getY(),
                (low.getZ() + high.getZ()) / 2.0
        );

        int id = nextId++;
        StructureRecord record = new StructureRecord(
                id,
                low.getWorld().getName(),
                center.getBlockX(),
                center.getBlockY(),
                center.getBlockZ(),
                low.getBlockX(), low.getBlockY(), low.getBlockZ(),
                high.getBlockX(), high.getBlockY(), high.getBlockZ(),
                System.currentTimeMillis()
        );
        records.put(id, record);
        saveRecords();

        String message = "§6[BSFinder] §eBetterStructures building generated! §f#" + id
                + " §7(" + record.world + ") §bX: " + record.x
                + " Y: " + record.y + " Z: " + record.z;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.isOp()) player.sendMessage(message);
        }
        getLogger().info("Detected BetterStructures build #" + id + " at " + record.world
                + " " + record.x + ", " + record.y + ", " + record.z);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Strict OP-only access: granting the plugin permission to a non-OP
        // must not allow them to use BSFinder.
        if (!(sender instanceof Player player) || !player.isOp()) {
            sender.sendMessage("§cOnly server operators (OPs) can use BSFinder.");
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sender.sendMessage("§6BSFinder commands:");
            sender.sendMessage("§e/bsfind list §7- Show recently generated buildings");
            sender.sendMessage("§e/bsfind latest §7- Show the newest building");
            sender.sendMessage("§e/bsfind tp <number> §7- Teleport to a building (players only)");
            return true;
        }
        if (args[0].equalsIgnoreCase("list")) {
            if (records.isEmpty()) {
                sender.sendMessage("§cNo BetterStructures buildings have been recorded yet.");
                return true;
            }
            sender.sendMessage("§6Recorded BetterStructures buildings (newest first):");
            records.values().stream()
                    .sorted(Comparator.comparingInt((StructureRecord r) -> r.id).reversed())
                    .limit(15)
                    .forEach(r -> sender.sendMessage("§e#" + r.id + " §f" + r.world
                            + " §7X: " + r.x + " Y: " + r.y + " Z: " + r.z));
            sender.sendMessage("§7Use §e/bsfind tp <number> §7to teleport.");
            return true;
        }
        if (args[0].equalsIgnoreCase("latest")) {
            if (records.isEmpty()) {
                sender.sendMessage("§cNo BetterStructures buildings have been recorded yet.");
                return true;
            }
            StructureRecord latest = records.values().stream()
                    .max(Comparator.comparingInt(r -> r.id)).orElseThrow();
            sender.sendMessage("§aLatest building: §e#" + latest.id + " §f" + latest.world
                    + " §7X: " + latest.x + " Y: " + latest.y + " Z: " + latest.z);
            return true;
        }
        if (args[0].equalsIgnoreCase("tp")) {
            if (args.length < 2) {
                sender.sendMessage("§cUsage: /bsfind tp <number>");
                return true;
            }
            int id;
            try {
                id = Integer.parseInt(args[1]);
            } catch (NumberFormatException ex) {
                sender.sendMessage("§cThat is not a valid building number.");
                return true;
            }
            StructureRecord record = records.get(id);
            if (record == null) {
                sender.sendMessage("§cI don't have a building recorded with that number.");
                return true;
            }
            World world = Bukkit.getWorld(record.world);
            if (world == null) {
                sender.sendMessage("§cThat world is not currently loaded: " + record.world);
                return true;
            }
            Location destination = new Location(world, record.x + 0.5, record.y + 1.0, record.z + 0.5);
            player.teleport(destination);
            player.sendMessage("§aTeleported to BetterStructures building §e#" + id + "§a.");
            return true;
        }
        sender.sendMessage("§cUnknown subcommand. Use /bsfind help.");
        return true;
    }

    private void loadRecords() {
        records.clear();
        ConfigurationSection section = getConfig().getConfigurationSection("buildings");
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            ConfigurationSection s = section.getConfigurationSection(key);
            if (s == null) continue;
            try {
                int id = Integer.parseInt(key);
                StructureRecord record = new StructureRecord(id,
                        s.getString("world", "world"),
                        s.getInt("x"), s.getInt("y"), s.getInt("z"),
                        s.getInt("lowX"), s.getInt("lowY"), s.getInt("lowZ"),
                        s.getInt("highX"), s.getInt("highY"), s.getInt("highZ"),
                        s.getLong("time"));
                records.put(id, record);
                nextId = Math.max(nextId, id + 1);
            } catch (NumberFormatException ignored) {
                getLogger().warning("Skipping invalid building record: " + key);
            }
        }
    }

    private void saveRecords() {
        getConfig().set("buildings", null);
        for (StructureRecord r : records.values()) {
            String path = "buildings." + r.id;
            getConfig().set(path + ".world", r.world);
            getConfig().set(path + ".x", r.x);
            getConfig().set(path + ".y", r.y);
            getConfig().set(path + ".z", r.z);
            getConfig().set(path + ".lowX", r.lowX);
            getConfig().set(path + ".lowY", r.lowY);
            getConfig().set(path + ".lowZ", r.lowZ);
            getConfig().set(path + ".highX", r.highX);
            getConfig().set(path + ".highY", r.highY);
            getConfig().set(path + ".highZ", r.highZ);
            getConfig().set(path + ".time", r.time);
        }
        saveConfig();
    }

    private static final class StructureRecord {
        private final int id;
        private final String world;
        private final int x, y, z;
        private final int lowX, lowY, lowZ, highX, highY, highZ;
        private final long time;

        private StructureRecord(int id, String world, int x, int y, int z,
                                int lowX, int lowY, int lowZ,
                                int highX, int highY, int highZ, long time) {
            this.id = id;
            this.world = world;
            this.x = x; this.y = y; this.z = z;
            this.lowX = lowX; this.lowY = lowY; this.lowZ = lowZ;
            this.highX = highX; this.highY = highY; this.highZ = highZ;
            this.time = time;
        }
    }
}
