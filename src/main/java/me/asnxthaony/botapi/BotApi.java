package me.asnxthaony.botapi;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.StringUtil;
import org.eclipse.jetty.http.HttpGenerator;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.util.log.Log;

import com.google.common.base.Charsets;

import net.ess3.api.IEssentials;
import net.milkbowl.vault.permission.Permission;

public class BotApi extends JavaPlugin implements Listener {

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

		getCommand("link").setTabCompleter(this);
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

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (label.equalsIgnoreCase("link")) {
			if (args.length == 0) {
				if (sender instanceof Player) {
					Player player = (Player) sender;
					String name = player.getName();
					String token = getToken(player.getName());

					player.sendMessage(Constant.dividingLine);
					player.sendMessage(Constant.emptyLine);

					switch (getStatus(name)) {
					case 0:
					case 1:
						player.sendMessage("§6§a请将以下内容私聊发送到机器小域(934664400)");
						player.sendMessage("§6§b.link " + name + " " + token);
						setStatus(name, 1);
						break;
					case 2:
						player.sendMessage("§6§a你的账号已绑定到QQ " + getQQ(name));
						break;
					default:
						break;
					}

					player.sendMessage(Constant.emptyLine);
					player.sendMessage(Constant.dividingLine);
					return true;
				} else {
					sender.sendMessage("§cYou must be a player!");
					return false;
				}
			} else if (args.length == 1) {
				if (sender.isOp() || sender.hasPermission("botapi.link")) {
					String target = args[0];
					if (getStatus(target) == 2) {
						sender.sendMessage(String.format("§a玩家 §b%s §a绑定的QQ为: §b%s ", target, getQQ(target)));
					} else {
						sender.sendMessage(String.format("§a玩家 §b%s §a暂未绑定QQ", target));
						return true;
					}
				} else {
					sender.sendMessage("§c你没有使用该命令的权限");
					return false;
				}
			}
		} else if (label.equalsIgnoreCase("fixlink")) {
			if (sender instanceof Player) {
				Player player = (Player) sender;
				String name = player.getName();

				if (getStatus(name) == 1 && getQQ(name) != 0L) {
					setStatus(name, 2);
					player.removePotionEffect(PotionEffectType.BLINDNESS);
					player.sendMessage("§a修复成功！");
				} else {
					player.sendMessage("§c修复失败！");
				}
			} else {
				sender.sendMessage("§cYou must be a player!");
				return false;
			}
		} else if (label.equalsIgnoreCase("check")) {
			new BukkitRunnable() {
				@Override
				public void run() {
					sender.sendMessage("§bWeb State> " + server.getState());
					if (sender.isOp()) {
						sender.sendMessage("§bDump> " + server.dump());
					}
				}
			}.runTaskAsynchronously(this);
			return true;
		}
		return false;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		if (command.getName().equalsIgnoreCase("link")) {
			List<String> completionList = new ArrayList<String>();
			if (args.length == 1 && (sender.isOp() || sender.hasPermission("botapi.link"))) {
				StringUtil.copyPartialMatches(args[0],
						Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()),
						completionList);
				Collections.sort(completionList);
			}
			return completionList;
		}
		return null;
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
	public void onPlayerMove(PlayerMoveEvent event) {
		Player player = event.getPlayer();
		if (getStatus(player.getName()) != 2) {
			if (player.hasPermission("botapi.link.bypass")) {
				return;
			}

			/*
			 * Limit player X and Z movements to 1 block Deny player Y+ movements (allows
			 * falling)
			 */
			Location from = event.getFrom();
			Location to = event.getTo();
			if (from.getBlockX() == to.getBlockX() && from.getBlockZ() == to.getBlockZ()
					&& from.getY() - to.getY() >= 0) {
				return;
			}

			event.setTo(event.getFrom());

			player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 1, true));
			player.sendMessage(Constant.dividingLine);
			player.sendMessage(Constant.emptyLine);
			player.sendMessage("§6§a请输入 /link 完成绑定后继续游戏");
			player.sendMessage(Constant.emptyLine);
			player.sendMessage(Constant.dividingLine);
		}
	}

	public static boolean hasPlayedBefore(String username) {
		UUID uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(Charsets.UTF_8));
		OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
		return player.hasPlayedBefore();
	}

	public static IEssentials getIEssentials() {
		return ess;
	}

	public static Permission getPermissions() {
		return perms;
	}

	public static String getToken(String name) {
		File dataFile = new File(plugin.getDataFolder() + File.separator + "userdata" + File.separator + name + ".yml");
		FileConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
		String token = config.getString("token");
		if (token == null) {
			resetToken(name);
			return getToken(name);
		}
		return token;
	}

	public static void setToken(String name, String token) {
		File dataFile = new File(plugin.getDataFolder() + File.separator + "userdata" + File.separator + name + ".yml");
		FileConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
		config.set("token", token);
		try {
			config.save(dataFile);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void resetToken(String name) {
		setToken(name, genToken());
	}

	public static int getStatus(String name) {
		File dataFile = new File(plugin.getDataFolder() + File.separator + "userdata" + File.separator + name + ".yml");
		FileConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
		return config.getInt("status");
	}

	public static void setStatus(String name, int status) {
		File dataFile = new File(plugin.getDataFolder() + File.separator + "userdata" + File.separator + name + ".yml");
		FileConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
		config.set("status", status);
		try {
			config.save(dataFile);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static long getQQ(String name) {
		File dataFile = new File(plugin.getDataFolder() + File.separator + "userdata" + File.separator + name + ".yml");
		FileConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
		return config.getLong("qq");
	}

	public static void setQQ(String name, long qq) {
		File dataFile = new File(plugin.getDataFolder() + File.separator + "userdata" + File.separator + name + ".yml");
		FileConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
		config.set("qq", qq);
		try {
			config.save(dataFile);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static String genToken() {
		SecureRandom random = new SecureRandom();
		byte[] bytes = new byte[20];
		random.nextBytes(bytes);
		return reverseString(getHash(bytes).substring(56));
	}

	public static String getHash(byte[] bytes) {
		if (bytes != null)
			try {
				MessageDigest md = MessageDigest.getInstance("SHA-256");
				return new HexBinaryAdapter().marshal(md.digest(bytes)).toLowerCase();
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			}
		return "";
	}

	private static String reverseString(String str) {
		char[] chars = new char[str.length()];
		for (int i = 0; i < chars.length; i++) {
			chars[i] = str.charAt(str.length() - 1 - i);
		}
		return String.copyValueOf(chars);
	}

	public static void log(String message) {
		File logFile = new File(plugin.getDataFolder(), "log.txt");
		try {
			FileWriter fileWriter = new FileWriter(logFile, true);
			PrintWriter printWriter = new PrintWriter(fileWriter);
			printWriter.println(
					String.format("[%s] %s", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()), message));
			printWriter.flush();
			printWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
