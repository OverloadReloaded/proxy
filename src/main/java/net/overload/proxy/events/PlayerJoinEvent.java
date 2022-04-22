package net.overload.proxy.events;

import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerKickEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import net.overload.proxy.ProxyPlugin;

public class PlayerJoinEvent implements Listener {

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerJoinEvent(PostLoginEvent event) {
		ProxyPlugin.get().psm.sendToLobby(event.getPlayer().getUniqueId());
	}

	@SuppressWarnings("deprecation")
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onKick(ServerKickEvent event) {
		if (event.getKickReason().contains("closed")) {
			event.setCancelled(true);
			ProxyPlugin.get().psm.removeServer(event.getCancelServer().getName());
			ProxyPlugin.get().psm.sendToLobby(event.getPlayer().getUniqueId());
		}
	}

}