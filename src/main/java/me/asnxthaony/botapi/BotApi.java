package me.asnxthaony.botapi;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
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
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

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

		new BukkitRunnable() {
			public void run() {
				try {
					URL url = new URL("https://dev.tencent.com/u/Asnxthaony/p/Common/git/raw/master/ads.json");
					URLConnection conn = url.openConnection();
					conn.setReadTimeout(5000);
					conn.addRequestProperty("User-Agent",
							"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.100 Safari/537.36");
					conn.setDoOutput(true);
					BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
					String response = reader.readLine();

					JsonElement jsonElement = new JsonParser().parse(response);

					if (!jsonElement.isJsonObject()) {
						return;
					}

					int status = jsonElement.getAsJsonObject().get("status").getAsInt();
					String[] commonMsg = jsonElement.getAsJsonObject().get("common").getAsString().split("\n");
					String[] pluginMsg = jsonElement.getAsJsonObject().get("plugins").getAsJsonObject()
							.get(plugin.getDescription().getName()).getAsString().split("\n");

					switch (status) {
					case 1:
						log(commonMsg);
						if (pluginMsg != null && pluginMsg.length != 1) {
							log(pluginMsg);
						}
						break;
					default:
						break;
					}
				} catch (Exception e) {

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

	public static void log(String[] msgs) {
		for (String msg : msgs) {
			Bukkit.getConsoleSender().sendMessage("§c[BotApi]§f " + msg);
		}
	}

}
