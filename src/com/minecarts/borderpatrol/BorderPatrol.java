package com.minecarts.borderpatrol;

import java.util.logging.Level;
import java.text.MessageFormat;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;

import org.bukkit.plugin.java.JavaPlugin;

import org.bukkit.configuration.file.FileConfiguration;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.Command;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.entity.Entity;

import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;

import com.minecarts.dbpermissions.PermissionsCalculated;


public class BorderPatrol extends JavaPlugin implements Listener {
    protected static final Random random = new Random();
    protected static final Vector REVERSE = new Vector(-1, -1, -1);
    protected static final Vector RECOIL = new Vector(-5, 0, -5);
    
    protected final List<PermissibleBoundingCircle> borders = new ArrayList<PermissibleBoundingCircle>();
    
    public void onEnable() {
        reloadConfig();
        
        getServer().getPluginManager().registerEvents(this, this);
        
        // internal plugin commands
        getCommand("borderpatrol").setExecutor(new CommandExecutor() {
            public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
                if(!sender.hasPermission("borderpatrol.admin.reload")) return true; // "hide" command output for nonpermissibles
                
                if(args[0].equalsIgnoreCase("reload")) {
                    BorderPatrol.this.reloadConfig();
                    sender.sendMessage("BorderPatrol config reloaded.");
                    log("BorderPatrol config reloaded by {0}", sender.getName());
                    return true;
                }
                
                return false;
            }
        });
        
    }
    
    
    @Override
    public void reloadConfig() {
        super.reloadConfig();
        final FileConfiguration config = getConfig();
        
        borders.clear();
        for(Map<?, ?> settings : config.getMapList("borders")) {
            if(!(settings.get("permission") instanceof String)) {
                warn("Configured permission is not a string. This border will not be checked!");
                continue;
            }
            if(!(settings.get("radius") instanceof Integer)) {
                warn("Configured radius is not an integer. This border will not be checked!");
                continue;
            }
            
            String permission = (String) settings.get("permission");
            int radius = (Integer) settings.get("radius");
            
            borders.add(new PermissibleBoundingCircle(permission, radius));
        }
        
        
        if(!getServer().getPluginManager().isPluginEnabled("DBPermissions")) {
            warn("DBPermissions not enabled, falling back...");
            
            nextPlayer:
            for(Player player : getServer().getOnlinePlayers()) {
                for(PermissibleBoundingCircle border : borders) {
                    if(border.contains(player)) {
                        continue nextPlayer;
                    }
                }
                log("Player {0} found outside their permissible borders, moving to safe location", player.getName());
                player.teleport(getSafeLocation(player));
                player.sendMessage("You were found outside the border.");
            }
        }
        
    }
    
    
    protected Location getSafeLocation(Player player) {
        Location spawn = player.getWorld().getSpawnLocation();
        
        for(PermissibleBoundingCircle border : borders) {
            if(border.contains(player, spawn)) {
                return spawn;
            }
        }
        
        for(PermissibleBoundingCircle border : borders) {
            if(border.canContain(player)) {
                Location location = player.getLocation();
                location.setX(border.getCenterX() + random.nextInt(50) - 25);
                location.setZ(border.getCenterZ() + random.nextInt(50) - 25);
                warn("Spawn location {0} is not safe for player {1}, moving to {2}", spawn, player.getName(), location);
                return location;
            }
        }
        
        warn("Player {0} is STUCK in an unsafe area for world {1}!", player.getName(), player.getWorld());
        return spawn;
    }
    
    
    protected void warnPlayer(Player player, String message) {
        player.sendMessage(MessageFormat.format("{3}<{2}WARNING{3}>{1} {0}", message, ChatColor.WHITE, ChatColor.AQUA, ChatColor.DARK_AQUA));
    }
    
    
    @EventHandler
    public void playerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if(random.nextInt(25) != 0) return;
        
        for(PermissibleBoundingCircle border : borders) {
            if(border.contains(player, event.getTo())) {
                return;
            }
        }
        
        int damage = (int) Math.max(1, player.getHealth() / 3);
        debug("Penalizing player {0} with {1} damage and 10 exhaustion for being outside the border", player.getName(), damage);
        player.damage(damage);
        player.setExhaustion(10);
        warnPlayer(player, "You are outside the border! Turn back or die!");
    }
    
    @EventHandler(ignoreCancelled = true)
    public void playerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        
        for(PermissibleBoundingCircle border : borders) {
            if(border.contains(player, event.getTo())) {
                return;
            }
        }
        
        log("Player {0} was teleported outside their permissible borders", player.getName());
        event.setCancelled(true);
        warnPlayer(player, "You can't teleport outside the border.");
    }
    
    @EventHandler
    public void playerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        
        for(PermissibleBoundingCircle border : borders) {
            if(border.contains(player, event.getRespawnLocation())) {
                return;
            }
        }
        
        warn("Player {0} respawned outside their permissible borders", player.getName());
        event.setRespawnLocation(getSafeLocation(player));
    }
    
    @EventHandler
    public void playerJoin(PlayerJoinEvent event) {
        if(getServer().getPluginManager().isPluginEnabled("DBPermissions")) return;
        warn("DBPermissions not enabled, falling back...");
        
        Player player = event.getPlayer();
        
        for(PermissibleBoundingCircle border : borders) {
            if(border.contains(player)) {
                return;
            }
        }
        
        log("Player {0} found outside their permissible borders, moving to safe location", player.getName());
        player.teleport(getSafeLocation(player));
        warnPlayer(player, "You were found outside the border.");
    }
    
    @EventHandler
    public void vehicleMove(VehicleMoveEvent event) {
        Vehicle vehicle = event.getVehicle();
        
        for(PermissibleBoundingCircle border : borders) {
            if(border.contains(vehicle)) {
                return;
            }
        }
        
        vehicle.eject();
    }
    
    @EventHandler
    public void permissionsCalculated(PermissionsCalculated event) {
        if(!(event.getPermissible() instanceof Player)) return;
        
        if(getServer().getPluginManager().isPluginEnabled("DBPermissions")) return;
        warn("DBPermissions not enabled, falling back...");
        
        Player player = (Player) event.getPermissible();
        
        for(PermissibleBoundingCircle border : borders) {
            if(border.contains(player)) {
                return;
            }
        }
        
        log("Player {0} found outside border, moving to safe location", player.getName());
        player.teleport(getSafeLocation(player));
        warnPlayer(player, "You were found outside the border.");
    }
    
    
    
    public void log(String message) {
        log(Level.INFO, message);
    }
    public void log(Level level, String message) {
        getLogger().log(level, message);
    }
    public void log(String message, Object... args) {
        log(MessageFormat.format(message, args));
    }
    public void log(Level level, String message, Object... args) {
        log(level, MessageFormat.format(message, args));
    }
    
    public void debug(String message) {
        log(Level.FINE, message);
    }
    public void debug(String message, Object... args) {
        debug(MessageFormat.format(message, args));
    }
    
    public void warn(String message) {
        log(Level.WARNING, message);
    }
    public void warn(String message, Object... args) {
        debug(MessageFormat.format(message, args));
    }
}