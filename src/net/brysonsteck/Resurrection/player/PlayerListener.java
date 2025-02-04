package net.brysonsteck.Resurrection.player;

import net.brysonsteck.Resurrection.startup.ParseSettings;
import net.brysonsteck.Resurrection.Resurrection;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class PlayerListener implements Listener {

    boolean stillDead;
    boolean timerRunning = false;
    World world = Bukkit.getWorlds().get(0);
    Location spawn = world.getSpawnLocation();
    //Hashtable<String, Location> playerSpawns = new Hashtable<>();
    Map<String, Location> playerSpawns = new HashMap<>();
    ParseSettings parseSettings;
    boolean DEBUG;
    Logger log = JavaPlugin.getProvidingPlugin(Resurrection.class).getLogger();

    public PlayerListener(ParseSettings parseSettings) {
        this.parseSettings = parseSettings;
        this.DEBUG = Boolean.parseBoolean(parseSettings.getSetting("debug"));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (DEBUG) {
            Bukkit.broadcastMessage(ChatColor.YELLOW  +""+ ChatColor.BOLD + "[Res. DEBUG]: Player " + p.getDisplayName() + " has joined");
        }

        PlayerData playerData = new PlayerData();
        playerData.readData();
        String rawData = playerData.getRawData();
        String[] rawPlayers = rawData.split(";");
        int index = 0;
        boolean found = false;
        boolean resumeDeath = false;
        long timeToResurrection = 0;
        for (String players : rawPlayers) {
            if (players.startsWith(p.getDisplayName())) {
                if (DEBUG) {
                    Bukkit.broadcastMessage(ChatColor.YELLOW  +""+ ChatColor.BOLD + "[Res. DEBUG]: Player " + p.getDisplayName() + " was found in the player data");
                }
                found = true;
                String[] playerSplit = players.split(",");
                boolean dead = Boolean.parseBoolean(playerSplit[1]);
                timeToResurrection = Long.parseLong(playerSplit[2]);

                if (timeToResurrection < System.currentTimeMillis() && timeToResurrection != 0) {
                    dead = false;
                    playerSplit[1] = String.valueOf(dead);
                    timeToResurrection = 0;
                    playerSplit[2] = String.valueOf(timeToResurrection);
                    Bukkit.broadcastMessage(ChatColor.YELLOW  +""+ ChatColor.BOLD + p.getDisplayName() + " has resurrected!");
                }

                if (!dead) {

                    if (DEBUG) {
                        Bukkit.broadcastMessage(ChatColor.YELLOW  +""+ ChatColor.BOLD + "[Res. DEBUG]: Player " + p.getDisplayName() + " is not dead; making sure they are in survival");
                    }

                    for (PotionEffect effect : p.getActivePotionEffects())
                        p.removePotionEffect(effect.getType());
                    p.setGameMode(GameMode.SURVIVAL);
                } else {
                    resumeDeath = true;
                }

                // save data
                rawPlayers[index] = String.join(",", playerSplit);
                rawData = String.join(";", rawPlayers);
                playerData.saveData(rawData);
                break;
            }
            index++;
        }
        if (!found) {
            if (DEBUG) {
                Bukkit.broadcastMessage(ChatColor.YELLOW  +""+ ChatColor.BOLD + "[Res. DEBUG]: Player " + p.getDisplayName() + " was not found in the player data; registering");
            }

            playerData.saveData(rawData + ";" + p.getDisplayName() + ",false,0");
        }
        if (resumeDeath && !timerRunning) {
            if (DEBUG) {
                Bukkit.broadcastMessage(ChatColor.YELLOW  +""+ ChatColor.BOLD + "[Res. DEBUG]: Player " + p.getDisplayName() + " is dead; pushing into dead state until resurrection");
            }
//            spawn = p.getLocation();
            p.sendMessage(ChatColor.RED + "Sigues muerto. Para comprobar cuánto tiempo te queda antes de resucitar, ");
            p.sendMessage(ChatColor.RED + "ejecuta el comando "/howlong\" en el chat.");
            new BukkitRunnable() {
                @Override
                public void run() {
                    p.setGameMode(GameMode.SPECTATOR);
                    PotionEffect blindness = new PotionEffect(PotionEffectType.BLINDNESS, 999999999, 10, false);
                    PotionEffect slowness = new PotionEffect(PotionEffectType.SLOW, 999999999, 10, false);
                    blindness.apply(p);
                    slowness.apply(p);
                    // p.teleport(spawn);
                    playerSpawns.put(p.getDisplayName(), p.getLocation());
                }
            }.runTaskLater(JavaPlugin.getProvidingPlugin(Resurrection.class), 1);
            timeToResurrection = timeToResurrection - System.currentTimeMillis();
            // to seconds
            timeToResurrection = timeToResurrection / 1000;
            // to ticks
            timeToResurrection = timeToResurrection * 20;

            new BukkitRunnable() {
                @Override
                public void run() {
                    playerData.readData();
                    String rawData = playerData.getRawData();
                    int index = 0;
                    boolean alreadyAlive = false;
                    for (String players : rawPlayers) {
                        if (players.startsWith(p.getDisplayName())) {
                            String[] playerSplit = players.split(",");
                            if (playerSplit[1] == "false") {
                                alreadyAlive = true;
                                break;
                            }
                            playerSplit[1] = "false";
                            playerSplit[2] = "0";

                            rawPlayers[index] = String.join(",", playerSplit);
                            rawData = String.join(";", rawPlayers);
                            playerData.saveData(rawData);
                            break;
                        }
                        index++;
                    }
                    if (!alreadyAlive) {
                        stillDead = false;
                        for (PotionEffect effect : p.getActivePotionEffects())
                            p.removePotionEffect(effect.getType());
                        p.setGameMode(GameMode.SURVIVAL);
                        Bukkit.broadcastMessage(ChatColor.YELLOW  +""+ ChatColor.BOLD + p.getDisplayName() + " has resurrected!");
                        if (p.getBedSpawnLocation() != null) {
                            p.teleport(p.getBedSpawnLocation());
                        // } else {
                            // p.teleport(spawn);
                        }
                        for(Player p : Bukkit.getOnlinePlayers()){
                            // for versions > 1.8
                            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_DEATH, 1, 0);
                            // for version 1.8 
                            // p.playSound(p.getLocation(), Sound.WITHER_DEATH, 1, 0);
                        }
                    }
                }
            }.runTaskLater(JavaPlugin.getProvidingPlugin(Resurrection.class), timeToResurrection);

            if (DEBUG) {
                Bukkit.broadcastMessage(ChatColor.YELLOW  +""+ ChatColor.BOLD + "[Res. DEBUG]: Player's resurrection thread is now delayed for (their time to resurrect - current time)");
            }
        }

    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();
        stillDead = true;
        if (DEBUG) {
            Bukkit.broadcastMessage(ChatColor.YELLOW  +""+ ChatColor.BOLD + "[Res. DEBUG]: Player " + p.getDisplayName() + " has died, reading resurrection_time in settings");
        }

        // get human readable form of resurrection time
        TimeCheck timeCheck = new TimeCheck(Long.parseLong(parseSettings.getSetting("resurrection_time")));

        // calculate time that player will resurrect at
        long resurrectionTime = System.currentTimeMillis() + Long.parseLong(parseSettings.getSetting("resurrection_time"));

        e.setDeathMessage(e.getDeathMessage() + " and will respawn in the next " + timeCheck.formatTime('f'));
//        p.sendMessage("You have died!! You will be able to respawn in the next " + timeCheck.formatTime('h'));
        timerRunning = true;

        // save death state
        PlayerData playerData = new PlayerData();
        playerData.readData();
        String rawData = playerData.getRawData();
        String[] rawPlayers = rawData.split(";");
        int index = 0;
        for (String players : rawPlayers) {
            if (players.startsWith(p.getDisplayName())) {
                if (DEBUG) {
                    Bukkit.broadcastMessage(ChatColor.YELLOW + "" + ChatColor.BOLD + "[Res. DEBUG]: Player found in player data file, saving dead state");
                }
                String[] playerSplit = players.split(",");
                playerSplit[1] = "true";
                playerSplit[2] = String.valueOf(resurrectionTime);

                // save data
                rawPlayers[index] = String.join(",", playerSplit);
                rawData = String.join(";", rawPlayers);
                playerData.saveData(rawData);
                break;
            }
            index++;
        }

        long timeToResurrection = Long.parseLong(parseSettings.getSetting("resurrection_time"));
        // to seconds
        timeToResurrection = timeToResurrection / 1000;
        // to ticks
        timeToResurrection = timeToResurrection * 20;

        new BukkitRunnable() {
            // save death information to player file
            @Override
            public void run() {
                // save death to false
                PlayerData playerData2 = new PlayerData();
                playerData2.readData();
                String rawData = playerData2.getRawData();
                int index = 0;
                boolean alreadyAlive = false;
                for (String players : rawPlayers) {
                    if (players.startsWith(p.getDisplayName())) {
                        String[] playerSplit = players.split(",");
                        log.info(playerSplit[1]);
                        if (playerSplit[1] == "false") {
                            alreadyAlive = true;
                            break;
                        }
                        playerSplit[1] = "false";
                        playerSplit[2] = "0";

                        rawPlayers[index] = String.join(",", playerSplit);
                        rawData = String.join(";", rawPlayers);
                        playerData.saveData(rawData);
                        break;
                    }
                    index++;
                }
                if (!alreadyAlive) {
                    stillDead = false;
                    for (PotionEffect effect : p.getActivePotionEffects())
                        p.removePotionEffect(effect.getType());
                    p.setGameMode(GameMode.SURVIVAL);
                    Bukkit.broadcastMessage(ChatColor.YELLOW  +""+ ChatColor.BOLD + p.getDisplayName() + " has resurrected!");
                    if (p.getBedSpawnLocation() != null) {
                        p.teleport(p.getBedSpawnLocation());
                    // } else {
                        // p.teleport(spawn);
                    }
                    for(Player p : Bukkit.getOnlinePlayers()){
                            // for versions > 1.8
                            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_DEATH, 1, 0);
                            // for version 1.8 
                            // p.playSound(p.getLocation(), Sound.WITHER_DEATH, 1, 0);
                    }
                }
            }
        }.runTaskLater(JavaPlugin.getProvidingPlugin(Resurrection.class), timeToResurrection);

        if (DEBUG) {
            Bukkit.broadcastMessage(ChatColor.YELLOW  +""+ ChatColor.BOLD + "[Res. DEBUG]: Player's resurrection thread is now delayed for resurrection_time");
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent e) {
        if (stillDead) {
            final Player p = e.getPlayer();
            if (DEBUG) {
                Bukkit.broadcastMessage(ChatColor.YELLOW  +""+ ChatColor.BOLD + "[Res. DEBUG]: Player " + p.getDisplayName() + " has respawned before their resurrection time");
            }


            TimeCheck timeCheck = new TimeCheck(Long.parseLong(parseSettings.getSetting("resurrection_time")));
            //playerSpawns.put(p.getDisplayName(), p.getLocation());
            p.setGameMode(GameMode.SPECTATOR);
            p.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "¡¡HAS MUERTO!!");
            p.sendMessage(ChatColor.RED + "Usted será capaz de respawn en los próximos " + timeCheck.formatTime('f'));
            if (DEBUG) {
                Bukkit.broadcastMessage(ChatColor.YELLOW  +""+ ChatColor.BOLD + "[Res. DEBUG]: Applying potions, spectator mode");
            }
            new BukkitRunnable() {
                @Override
                public void run() {
//                    spawn = p.getLocation();
//                PotionEffect invisibility = new PotionEffect(PotionEffectType.INVISIBILITY, 1728000, 10, false);
                    PotionEffect blindness = new PotionEffect(PotionEffectType.BLINDNESS, 999999999, 10, false);
                    PotionEffect slowness = new PotionEffect(PotionEffectType.SLOW, 999999999, 10, false);
//                invisibility.apply(p);
                    blindness.apply(p);
                    slowness.apply(p);
                    // put location in map
                    playerSpawns.put(p.getDisplayName(), p.getLocation());
                }
            }.runTaskLater(JavaPlugin.getProvidingPlugin(Resurrection.class), 1);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if (p.getGameMode() == GameMode.SPECTATOR && stillDead) {
            if (DEBUG) {
                Bukkit.broadcastMessage(ChatColor.YELLOW  +""+ ChatColor.BOLD + "[Res. DEBUG]: Player " + p.getDisplayName() + " attempted to move while in dead state, teleporting to spawn until their resurrection time");
            }
            //p.teleport(spawn);
            p.teleport(playerSpawns.get(p.getDisplayName()));
        }
    }
}
