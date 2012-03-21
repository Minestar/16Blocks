package de.minestar.sixteenblocks.Commands;

import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.Player;

import de.minestar.minestarlibrary.commands.AbstractCommand;
import de.minestar.minestarlibrary.utils.PlayerUtils;
import de.minestar.sixteenblocks.Core.Core;
import de.minestar.sixteenblocks.Core.TextUtils;
import de.minestar.sixteenblocks.Manager.AreaManager;
import de.minestar.sixteenblocks.Manager.SkinArea;

public class cmdBan extends AbstractCommand {

    private AreaManager areaManager;
    private Set<String> supporter;

    public cmdBan(String syntax, String arguments, String node, AreaManager areaManager, Set<String> supporter) {
        super(Core.NAME, syntax, arguments, node);
        this.areaManager = areaManager;
        this.supporter = supporter;
        this.description = "Ban a player and delete the area";
    }

    @Override
    public void execute(String[] arguments, Player player) {

        // CHECK: PLAYER IS OP
        if (!player.isOp() || !supporter.contains(player.getName())) {
            TextUtils.sendError(player, "You are not allowed to do this!");
            return;
        }

        // GET AREA
        Player target = PlayerUtils.getOnlinePlayer(arguments[0]);
        CraftServer cServer = (CraftServer) Bukkit.getServer();
        if (target == null) {
            cServer.getHandle().addUserBan(arguments[0]);
            TextUtils.sendSuccess(player, "Player '" + arguments[0] + "' banned, but Skin was not deleted (because the player is offline!)");
            return;
        }
        // BAN PLAYER
        cServer.getHandle().addUserBan(target.getName());

        SkinArea thisArea = this.areaManager.getExactPlayerArea(target.getName());

        // CHECK: NO AREA FOUND
        if (thisArea == null) {
            TextUtils.sendSuccess(player, "Player '" + target.getName() + "' banned. This player did not have a skin.");
            target.kickPlayer("You were BANNED!");
            return;
        }

        // CHECK : AREA IS NOT BLOCKED
        if (this.areaManager.isAreaBlocked(thisArea.getZoneXZ())) {
            TextUtils.sendError(player, "Player '" + target.getName() + "' banned, but Skin was not deleted (blocked by another process!)");
            target.kickPlayer("You were BANNED!");
            return;
        }

        // DELETE THIS AREA
        this.areaManager.deletePlayerArea(thisArea, player);
        TextUtils.sendSuccess(player, "Player '" + target.getName() + "' banned and started deletion of '" + thisArea.getAreaOwner() + "'.");

        // KICK PLAYER
        target.kickPlayer("You were BANNED!");
    }
}
