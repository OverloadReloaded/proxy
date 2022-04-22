package net.overload.proxy.commands;

import com.google.gson.GsonBuilder;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.plugin.Command;
import net.overload.proxy.ProxyPlugin;

public class RegisterCommand extends Command {

	public RegisterCommand() {
		super("register", "overload.register", "reg");
	}

	@SuppressWarnings("deprecation")
	@Override
	public void execute(CommandSender sender, String[] args) {
		if(args.length > 2) {
			sender.sendMessage("$cUsage: /register <name> <port>");
		} else {
			ServerInfo serverInfo = ProxyPlugin.get().psm.addServer(args[0], Integer.parseInt(args[1]));
			sender.sendMessage("$aServer registered !");
			sender.sendMessage(new GsonBuilder().create().toJson(serverInfo));
		}
		
	}

}
