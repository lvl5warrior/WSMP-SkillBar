package com.warriorssmp.skillbar.listener;

import com.warriorssmp.skillbar.SkillBarPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerConnectionListener implements Listener {

    private final SkillBarPlugin plugin;

    public PlayerConnectionListener(SkillBarPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.bossBarManager().showFor(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.bossBarManager().removeBar(event.getPlayer().getUniqueId());
    }
}
