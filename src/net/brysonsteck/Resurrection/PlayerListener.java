package net.brysonsteck.Resurrection;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class PlayerListener implements Listener {

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        System.out.println("Resurrection: A player has died!");
        Player p = e.getEntity();
        Long timeOfDeath = System.currentTimeMillis();
        Long resurrectionTime = timeOfDeath + 86400000;

        TimeCheck death = new TimeCheck(timeOfDeath);
        TimeCheck resurrect = new TimeCheck((timeOfDeath + 86400000) - timeOfDeath);

        String deathFormatted = death.formatTime();
        String resurrectFormatted = resurrect.formatTime();

        p.sendMessage("You have died!! You will be able to respawn in the next 24 hours.");
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        p.setGameMode(GameMode.ADVENTURE);

        p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 200, 10, false));
        p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 200, 10, false));
        p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 200, 10, false));
        p.setNoDamageTicks(1728000);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        Location location = p.getLocation();
        if (p.getGameMode() == GameMode.ADVENTURE) {
            p.teleport(location);
        }
    }
}
