package net.overload.proxy.server;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.google.gson.GsonBuilder;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import net.overload.commons.CommonsPluginBungee;
import net.overload.commons.databases.OverloadRedisStrings;
import net.overload.commons.logger.LogLevel;
import net.overload.commons.server.MinecraftServerInfo;
import net.overload.commons.server.MinecraftServerType;
import net.overload.commons.utils.texts.MessageUtils;
import net.overload.proxy.ProxyPlugin;

@SuppressWarnings("static-access")
public class ProxyServerManager {

	private HashMap<String, MinecraftServerInfo> list = new HashMap<>();
	
	private ScheduledTask thread;

	public ProxyServerManager() {
		super();
	}
	
	@SuppressWarnings("deprecation")
	public void removeServer(String serverName) {
		if(ProxyPlugin.get().getProxy().getServers().containsKey(serverName)) {
			ProxyPlugin.get().getProxy().getServers().remove(serverName);
			if(list.containsKey(serverName)) {
				list.remove(serverName);
			}
		}
		ProxyPlugin.get().logger().send(LogLevel.INFO, "Proxy", String.format("Server removed by messaging : %s.", serverName));
	}
	
	@SuppressWarnings("deprecation")
	public ServerInfo addServer(String name, Integer port) {
		InetSocketAddress inetadresse = new InetSocketAddress("localhost",port);
        ServerInfo si = ProxyPlugin.get().getProxy().constructServerInfo(name, inetadresse, "", false);
        ProxyPlugin.get().getProxy().getServers().put(name, si);
        return si;
	}
	
	public void startRegistering() {
		this.thread = ProxyPlugin.get().getProxy().getScheduler().schedule(ProxyPlugin.get(), new Runnable() {
			@Override
			public void run() {
				try {
					checkOnlineServer();
				} catch (Exception e) {
					ProxyPlugin.get().logger().send(LogLevel.WARNING, "An error occured in the plugin : " + e.getMessage());
					if(CommonsPluginBungee.get().config.getPrintStackTrace()) e.printStackTrace();
				}
				
			}
		},5L, 4L, TimeUnit.SECONDS);
	}

	@SuppressWarnings({ "deprecation"})
	public void checkOnlineServer() {
		
		Set<String> redisAlive = ProxyPlugin.get().redis.getDatabase().keys(String.format(OverloadRedisStrings.SERVER_KEEP_ALIVE.s(), "*"));
		ArrayList<String> newAllAlive = new ArrayList<>();
		
		for(String s : redisAlive) {
			String serverName = s.replace(OverloadRedisStrings.SERVER_KEEP_ALIVE.s().split("%s")[0], "").replace(OverloadRedisStrings.SERVER_KEEP_ALIVE.s().split("%s")[1], "");
			newAllAlive.add(serverName);
		}
		newAllAlive.add("Default");
		
		ArrayList<String> toDelete = new ArrayList<>();	
		
		ProxyPlugin.get().getProxy().getServers().forEach((name, info) -> {
			if(!name.contains("Out-00") || !name.contains("Default")) {
				if(!newAllAlive.contains(name)) toDelete.add(name);
			}
		});

		toDelete.forEach(server -> {
			ProxyPlugin.get().getProxy().getServers().remove(server);
			list.remove(server);
		});
		
		for(String server : newAllAlive) {
			if(!ProxyPlugin.get().getProxy().getServers().containsKey(server)) {
				MinecraftServerInfo msi = new GsonBuilder().create().fromJson(ProxyPlugin.get().redis.getDatabase().get(String.format(OverloadRedisStrings.SERVER_LIST.s(), server)), MinecraftServerInfo.class);
				if(msi != null) {
					InetSocketAddress inetadresse = new InetSocketAddress("localhost", msi.getServerPort());
			        ServerInfo si = ProxyPlugin.get().getProxy().constructServerInfo(server, inetadresse, "", false);
			        ProxyPlugin.get().getProxy().getServers().put(server, si);
				}
		        
		        this.list.put(msi.getServerName(), msi);
		        ProxyPlugin.get().logger().send(LogLevel.INFO, "Registered: " + msi.getServerName() + " as " + msi.getType().toString());
			}
		}
	}
	
	public void publishServers() {
		ProxyPlugin.get().getProxy().getScheduler().schedule(ProxyPlugin.get(), new Runnable() {
			@Override
			public void run() {
				try {
					ProxyPlugin.get().redis.getDatabase().publish("toLobbiesUpdates", new GsonBuilder().create().toJson(list).toString());					
				} catch (Exception e) {
					ProxyPlugin.get().logger().send(LogLevel.WARNING, "An error occured in the plugin : " + e.getMessage());
					if(CommonsPluginBungee.get().config.getPrintStackTrace()) e.printStackTrace();
				}
			}
		}, 0L, 5L, TimeUnit.SECONDS);
	}
	
	@SuppressWarnings("deprecation")
	public void sendToLobby(UUID uuid) {
		ProxiedPlayer p = ProxyServer.getInstance().getPlayer(uuid);
		if(p != null) {
			List<MinecraftServerInfo> msi = this.getLobbies();
			if(msi.size() > 0 || !msi.isEmpty()) {
				try {
					String server = msi.get(new Random().nextInt(msi.size())).getServerName();
					p.connect(ProxyPlugin.get().getProxy().getServerInfo(server));
					p.sendMessage(String.format(MessageUtils.LOBBY_SUCCESSFULL_SEND.getMessage(), server));
				} catch (Exception e) {
					ProxyPlugin.get().logger().send(LogLevel.WARNING, "An error occured in the plugin : " + e.getMessage());
					if(CommonsPluginBungee.get().config.getPrintStackTrace()) e.printStackTrace();
				}
			} else {
				 p.sendMessage(MessageUtils.LOBBY_NO_SERVERS.getMessage());
				 p.connect(ProxyPlugin.get().getProxy().getServerInfo("Default"));
			}
		}
	}
	
	public List<MinecraftServerInfo> getLobbies() {
		List<MinecraftServerInfo> returnList = new ArrayList<>();
		for(MinecraftServerInfo msi : this.list.values()) {
			if(msi.getType() == MinecraftServerType.LOBBY) {
				returnList.add(msi);
			}
			
		}
		return returnList;
	}
	
	public ScheduledTask getThread() {
		return thread;
	}

	public void setThread(ScheduledTask thread) {
		this.thread = thread;
	}

	public HashMap<String, MinecraftServerInfo> getList() {
		return this.list;
	}
}
