package me.asnxthaony.botapi;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
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
import org.bukkit.potion.PotionEffectType;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import com.earth2me.essentials.utils.NumberUtil;
import com.google.common.base.Charsets;
import com.google.gson.Gson;

public class WebHandler extends AbstractHandler {

	private static final String API_TOKEN = "5XnkLtCxn52Tr9d2Vqzh7PaTs99LpCVx";

	private static final Gson GSON = new Gson();

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType("application/json;charset=utf-8");
		baseRequest.setHandled(true);

		PrintWriter out = response.getWriter();

		BotApi.log(String.format("%s \"%s %s?%s\"", request.getRemoteAddr(), request.getMethod(), target,
				(request.getQueryString() != null) ? request.getQueryString() : ""));
		// 148.70.60.44 "GET /onlinePlayers?t=20190501"

		// String userAgent = request.getHeader("User-Agent");
		String token = request.getParameter("token");
		String username = request.getParameter("username");
		String qq = request.getParameter("qq");
		String message = request.getParameter("message");

		switch (target) {
		case "/":
			response.sendRedirect("https://dnspod.qcloud.com/static/webblock.html");
			return;
		case "/onlinePlayers":

			List<String> onlinePlayers = new ArrayList<String>();
			for (Player player : Bukkit.getOnlinePlayers()) {
				String prefix = "";

				switch (BotApi.getPermissions().getPrimaryGroup(player)) {
				case "SVIP+":
					prefix = "[SVIP+]";
					break;
				case "svip":
					prefix = "[SVIP]";
					break;
				default:
					break;
				}

				String name = player.getName();
				switch (name) {
				case "qyh07":
					prefix = "[星贵一]";
					break;
				case "--":
					prefix = "[星贵二]";
					break;
				case "XianShou":
					prefix = "[星贵三]";
					break;
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
				out.print(toJson(401, Constant.MISSING_ARGUMENTS));
				return;
			}

			// Map<String, String> map = new HashMap<String, String>();

			return;
		case "/linkAccount":
			String userToken = request.getParameter("userToken");
			qq = request.getParameter("qq");

			if (username == null || userToken == null || qq == null) {
				out.print(toJson(401, Constant.MISSING_ARGUMENTS));
				return;
			}

			if (BotApi.getStatus(username) == 0) {
				out.print(100); // 玩家不存在
				return;
			} else if (BotApi.getStatus(username) == 2) {
				out.print(102); // 已绑定
				return;
			}

			if (BotApi.getToken(username).equals(userToken)) {
				Player targetPlayer = Bukkit.getServer().getPlayer(username);
				if (targetPlayer != null) {
					targetPlayer.removePotionEffect(PotionEffectType.BLINDNESS);
					targetPlayer.sendMessage(Constant.dividingLine);
					targetPlayer.sendMessage(Constant.emptyLine);
					targetPlayer.sendMessage("§6§a你已成功完成绑定！");
					targetPlayer.sendMessage(Constant.emptyLine);
					targetPlayer.sendMessage(Constant.dividingLine);
				}

				BotApi.resetToken(username);
				BotApi.setStatus(username, 2);
				BotApi.setQQ(username, Long.valueOf(qq));

				out.print(0); // 绑定成功
				return;
			} else {
				out.print(101); // 密钥错误
				return;
			}
		case "/v2/sendMessage":
			if (token == null) {
				out.print(toJson(401, Constant.MISSING_API_TOKEN));
				return;
			}
			if (!token.equals(API_TOKEN)) {
				out.print(toJson(403, Constant.INVALID_API_TOKEN));
				return;
			}

			if (username == null || message == null) {
				out.print(toJson(401, Constant.MISSING_ARGUMENTS));
				return;
			}

			String msg = new String(Base64.getDecoder().decode(URLDecoder.decode(message, "UTF-8")), "UTF-8");
			msg = String.format("§2[CHAT] §r%s: %s", username,
					ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', msg)));

			Bukkit.broadcastMessage(msg);

			out.print(0);
			return;
		case "/runCommand":
			if (token == null) {
				out.print(toJson(401, Constant.MISSING_API_TOKEN));
				return;
			}
			if (!token.equals(API_TOKEN)) {
				out.print(toJson(403, Constant.INVALID_API_TOKEN));
				return;
			}

			String command = new String(
					Base64.getDecoder().decode(URLDecoder.decode(request.getParameter("command"), "UTF-8")), "UTF-8");
			if (command == null || command.isEmpty()) {
				out.print(toJson(401, Constant.MISSING_ARGUMENTS));
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
			// "优化" tps
			// if (tps <= 5) {
			// tps = tps + 10;
			// } else if (tps <= 10) {
			// tps = tps + 5;
			// } else if (tps <= 15) {
			// tps = tps + 3;
			// }

			out.print(NumberUtil.formatDouble(tps));
			return;
		case "/getRank":
			if (username == null) {
				out.print(toJson(401, Constant.MISSING_ARGUMENTS));
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
		case "/setUserQQ":
			if (token == null) {
				out.print(toJson(401, Constant.MISSING_API_TOKEN));
				return;
			}
			if (!token.equals(API_TOKEN)) {
				out.print(toJson(403, Constant.INVALID_API_TOKEN));
				return;
			}

			if (username == null || qq == null) {
				out.print(toJson(401, Constant.MISSING_ARGUMENTS));
				return;
			}

			if (!BotApi.hasPlayedBefore(username)) {
				out.print(100);
				return;
			}

			if (BotApi.getStatus(username) == 2) {
				out.print(101);
				return;
			}

			BotApi.resetToken(username);
			BotApi.setStatus(username, 2);
			BotApi.setQQ(username, Long.valueOf(qq));

			out.print(0);
			return;
		default:
			out.print(toJson(404, Constant.INVALID_ACTION));
			break;
		}

	}

	public String toJson(int code, String message) {
		return GSON.toJson(new Response(code, message));
	}
}
