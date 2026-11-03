package org.pokesplash.gts.command.subcommand;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;
import org.pokesplash.gts.Gts;
import org.pokesplash.gts.command.superclass.Subcommand;
import org.pokesplash.gts.util.ColorUtil;
import org.pokesplash.gts.web.GtsWeb;
import org.pokesplash.gts.web.WebConfig;

/**
 * Gives the player a one-time login link and code for the GTS website.
 */
public class Web extends Subcommand {

	public Web() {
		super("§9Usage:\n§3- gts web");
	}

	@Override
	public LiteralCommandNode<CommandSourceStack> build() {
		return Commands.literal("web")
				.requires(ctx -> ctx.isPlayer() && Gts.permissions.hasPermission(ctx.getPlayer(), "web"))
				.executes(this::run)
				.build();
	}

	@Override
	public int run(CommandContext<CommandSourceStack> context) {
		ServerPlayer player = context.getSource().getPlayer();
		if (player == null) {
			return 0;
		}

		WebConfig config = GtsWeb.getConfig();
		if (!GtsWeb.isRunning()) {
			player.sendSystemMessage(ColorUtil.parse(config.getDisabledMessage()));
			return 0;
		}

		String code = GtsWeb.getAuth().createCode(player.getUUID(), player.getName().getString(),
				config.getLoginCodeMinutes());
		String url = config.getPublicUrl() + "/?code=" + code;

		player.sendSystemMessage(ColorUtil.parse(config.getLoginLinkMessage().replace("{url}", url))
				.copy().withStyle(style -> style
						.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url))
						.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(url)))));
		player.sendSystemMessage(ColorUtil.parse(config.getLoginCodeMessage()
				.replace("{code}", code)
				.replace("{minutes}", String.valueOf(config.getLoginCodeMinutes()))));
		return 1;
	}
}
