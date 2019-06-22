package me.asnxthaony.botapi;

import java.util.UUID;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.eclipse.jetty.http.HttpGenerator;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.util.log.Log;

import com.google.common.base.Charsets;

import net.ess3.api.IEssentials;
import net.milkbowl.vault.permission.Permission;

public class BotApi extends JavaPlugin implements Listener {

	/**
	 * -1 —— 公众版 0 —— 星域世界 1 —— 星之都 2 —— 星梦之音
	 */
	public static final int brand = 0;

	public static Plugin plugin;

	public static final Server server = new Server();
	public static Logger logger;

	private static IEssentials ess = null;
	private static Permission perms = null;

	@Override
	public void onEnable() {
		plugin = this;

		if (!getDataFolder().exists()) {
			getDataFolder().mkdir();
		}

		saveDefaultConfig();
		reloadConfig();

		WebHandler.API_TOKEN = getConfig().getString("api-token");

		ess = (IEssentials) Bukkit.getPluginManager().getPlugin("Essentials");
		setupPermissions();

		Bukkit.getServer().getPluginManager().registerEvents(this, this);

		logger = getLogger();

		Log.setLog(new JettyLogger());

		ContextHandler context = new ContextHandler("/");
		context.setHandler(new WebHandler());
		context.setErrorHandler(new WebErrorHandler());

		ServerConnector connector = new ServerConnector(server);
		connector.setAcceptQueueSize(50);
		connector.setPort(10493);
		connector.setIdleTimeout(5000);
		server.addConnector(connector);

		server.setStopAtShutdown(true);

		server.setHandler(context);

		new BukkitRunnable() {
			public void run() {
				try {
					server.start();
					HttpGenerator.setJettyVersion(String.format("%s/%s", plugin.getDescription().getName(),
							plugin.getDescription().getVersion()));
				} catch (Exception e) {
					logger.severe("Unable to bind web server to port.");
					e.printStackTrace();
					setEnabled(false);
				}
			}
		}.runTaskAsynchronously(this);
	}

	@Override
	public void onDisable() {
		try {
			server.stop();
		} catch (Exception e) {
			logger.severe("Unable to stop web server.");
			e.printStackTrace();
		}
	}

	private boolean setupPermissions() {
		RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager().getRegistration(Permission.class);
		perms = rsp.getProvider();
		return perms != null;
	}

	public static boolean hasPlayedBefore(String username) {
		UUID uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(Charsets.UTF_8));
		OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
		return player.hasPlayedBefore();
	}

	public static String getOfflinePlayerUUID(String username) {
		UUID uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(Charsets.UTF_8));
		return uuid.toString();
	}

	public static IEssentials getIEssentials() {
		return ess;
	}

	public static Permission getPermissions() {
		return perms;
	}

	public static void log(String msg) {
		Bukkit.getConsoleSender().sendMessage("§c[BotApi]§f " + msg);
	}

}
