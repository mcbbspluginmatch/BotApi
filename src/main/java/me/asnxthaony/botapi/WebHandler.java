package me.asnxthaony.botapi;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import com.earth2me.essentials.utils.NumberUtil;
import com.google.common.base.Charsets;
import com.google.gson.Gson;

public class WebHandler extends AbstractHandler {

	public static String API_TOKEN = "";

	private static final Gson GSON = new Gson();

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType("application/json;charset=utf-8");
		baseRequest.setHandled(true);

		PrintWriter out = response.getWriter();

		// String userAgent = request.getHeader("User-Agent");
		String token = request.getParameter("token");
		String username = request.getParameter("username");
		String message = request.getParameter("message");

		switch (target) {
		case "/":
			response.sendRedirect("https://dnspod.qcloud.com/static/webblock.html");
			return;
		case "/onlinePlayers":

			List<String> onlinePlayers = new ArrayList<String>();
			for (Player player : Bukkit.getOnlinePlayers()) {
				String prefix = "";

				/*
				 * switch (BotApi.getPermissions().getPrimaryGroup(player)) { case "SVIP+":
				 * prefix = "[SVIP+]"; break; case "svip": prefix = "[SVIP]"; break; default:
				 * break; }
				 */

				String name = player.getName();
				switch (name) {
				case "liuhanwen":
					prefix = "[开发者]";
					break;
				/*
				 * case "qyh07": prefix = "[星贵一]"; break; case "--": prefix = "[星贵二]"; break;
				 * case "XianShou": prefix = "[星贵三]"; break;
				 */
				default:
					break;
				}

				if (player.isOp()) {
					prefix = "[管理员]";
				}

				if (!BotApi.getIEssentials().getUser(player).isVanished()) {
					onlinePlayers.add(prefix + player.getName());
				}
			}

			out.println(GSON.toJson(onlinePlayers));

			return;
		case "/playerStats":
			if (username == null) {
				out.print(toJson(401, StringConsts.MISSING_ARGUMENTS));
				return;
			}

			// Map<String, String> map = new HashMap<String, String>();

			return;
		case "/v2/sendMessage":
			if (token == null) {
				out.print(toJson(401, StringConsts.MISSING_API_TOKEN));
				return;
			}
			if (!token.equals(API_TOKEN)) {
				out.print(toJson(403, StringConsts.INVALID_API_TOKEN));
				return;
			}

			if (username == null || message == null) {
				out.print(toJson(401, StringConsts.MISSING_ARGUMENTS));
				return;
			}

			String msg = new String(Base64.getDecoder().decode(message), "UTF-8");
			msg = String.format("§2[CHAT] §r%s: %s", username,
					ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', msg)));

			Bukkit.broadcastMessage(msg);

			out.print(0);
			return;
		case "/runCommand":
			if (token == null) {
				out.print(toJson(401, StringConsts.MISSING_API_TOKEN));
				return;
			}
			if (!token.equals(API_TOKEN)) {
				out.print(toJson(403, StringConsts.INVALID_API_TOKEN));
				return;
			}

			String command = new String(Base64.getDecoder().decode(request.getParameter("command")), "UTF-8");
			if (command == null || command.isEmpty()) {
				out.print(toJson(401, StringConsts.MISSING_ARGUMENTS));
				return;
			}

			if (command.startsWith("op") || command.startsWith("deop") || command.startsWith("stop")) {
				out.print(-1);
				return;
			}

			Bukkit.dispatchCommand(Bukkit.getServer().getConsoleSender(), command);

			out.print(0);
			return;
		case "/tps":
			double tps = BotApi.getIEssentials().getTimer().getAverageTPS();

			out.print(NumberUtil.formatDouble(tps));
			return;
		case "/getRank":
			if (username == null) {
				out.print(toJson(401, StringConsts.MISSING_ARGUMENTS));
				return;
			}

			UUID uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(Charsets.UTF_8));
			OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
			if (!player.hasPlayedBefore()) {
				out.print("null");
				return;
			}

			out.print(BotApi.getPermissions().getPrimaryGroup("world", player).toLowerCase());
			return;
		default:
			out.print(toJson(404, StringConsts.INVALID_ACTION));
			break;
		}

	}

	public String toJson(int code, String message) {
		return GSON.toJson(new Response(code, message));
	}
}
