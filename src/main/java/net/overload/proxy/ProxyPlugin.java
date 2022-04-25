package net.overload.proxy;

import com.google.gson.GsonBuilder;

import net.md_5.bungee.api.plugin.Plugin;
import net.overload.commons.CommonsPluginBungee;
import net.overload.commons.databases.OverloadRedis;
import net.overload.commons.logger.LogLevel;
import net.overload.commons.logger.Logger;
import net.overload.commons.server.MinecraftServerInfo;
import net.overload.proxy.commands.RegisterCommand;
import net.overload.proxy.events.PlayerJoinEvent;
import net.overload.proxy.server.ProxyServerManager;
import redis.clients.jedis.JedisPubSub;

public class ProxyPlugin extends Plugin {

	private static Logger log = new Logger("Proxy");

	public static ProxyPlugin instance;
	public OverloadRedis redis;
	public ProxyServerManager psm;
	
	
	@Override
	public void onLoad() {

	}
	
	@Override
	public void onEnable() {
		instance = this;
		
		connectRedis();
		
		//Starting checking for servers
		psm = new ProxyServerManager();
		new Thread(new Runnable() {
			@Override
			public void run() {
				psm.startRegistering();
				psm.publishServers();
			}
		}).start();
		
		getProxy().getPluginManager().registerCommand(this, new RegisterCommand());
		
		
		getProxy().getPluginManager().registerListener(this, new PlayerJoinEvent());
		
		log.send(LogLevel.SUCCESS, "Enabled Proxy plugin !");
	}
	
	@Override
	public void onDisable() {
		log.send(LogLevel.INFO, "Disabling Proxy plugin!");

	}
	
	/**
	 * Functions
	 */
	public void connectRedis() {
		redis = new OverloadRedis();
		redis.connect(true);
		new Thread(new Runnable() {
			@Override
			public void run() {
				JedisPubSub pubsub = new JedisPubSub() {
					@Override
					public void onMessage(String channel, String message) {
						if(CommonsPluginBungee.get().config.getShowJedisMessaging()) logger().send(LogLevel.CMD, "Received on : " + channel + ". Message : " + message);
						if(channel.equals("toBungee")) {
							String[] argss = message.split(";");
							if(argss[0].equals("send")) {
								
							}
						}
						
						if(channel.equals("updateServer")) {
							MinecraftServerInfo msi = new GsonBuilder().create().fromJson(message, MinecraftServerInfo.class);
							psm.getList().put(msi.getServerName(), msi);
						}
						
						if(channel.equals("deleteServer")) {
							psm.removeServer(message);
						}
					}
					
					@Override
					public void onSubscribe(String channel, int subscribedChannels) {
						log.send(LogLevel.INFO, "Redis", String.format("Subscribed to channel : %s.", channel));
					}
				};

				redis.setPubSub(pubsub);
				redis.getJedis().subscribe(pubsub, new String[] { "toBungee", "updateServer" });
			}
		}).start();
	}
	
	/**
	 * Getters & Setters
	 */
	
	public static Logger logger() {
		return log;
	}
	
	public static ProxyPlugin get() {
		return instance;
	}
}
