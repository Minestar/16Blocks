package de.minestar.sixteenblocks.Listener;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;

import de.minestar.sixteenblocks.Core.Core;
import de.minestar.sixteenblocks.Core.Settings;
import de.minestar.sixteenblocks.Core.TextUtils;
import de.minestar.sixteenblocks.Threads.AFKThread;
import de.minestar.sixteenblocks.Units.ChatFilter;

public class ChatListener implements Listener {

    private ChatFilter filter;
    private HashMap<String, Long> lastChatList;

    private HashSet<String> mutedPlayers = new HashSet<String>();

    private AFKThread afkThread;

    private long chatPause = Settings.getChatPauseTimeInSeconds() * 1000;

    public static boolean radiusOff = false;

    public ChatListener(ChatFilter filter, AFKThread afkThread) {
        this.filter = filter;
        this.lastChatList = new HashMap<String, Long>();
        this.afkThread = afkThread;
    }

    public boolean toggleMute(Player player) {
        if (this.isMuted(player)) {
            this.unmutePlayer(player);
        } else {
            this.mutePlayer(player);
        }
        return this.isMuted(player);
    }

    public void mutePlayer(Player player) {
        this.mutedPlayers.add(player.getName());
    }

    public void unmutePlayer(Player player) {
        this.mutedPlayers.remove(player.getName());
    }

    public boolean isMuted(Player player) {
        return this.mutedPlayers.contains(player.getName());
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!event.getPlayer().isOp())
            return;

        // ON RELOAD > TUNNEL THE COMMAND
        if (event.getMessage().toLowerCase().startsWith("/reload")) {
            event.setMessage("/rel");
        } else if (event.getMessage().toLowerCase().startsWith("/stop")) {
            event.setMessage("/shutdown");
        }
    }

    @EventHandler
    public void onServerCommand(ServerCommandEvent event) {
        // ON RELOAD > TUNNEL THE COMMAND
        if (event.getCommand().toLowerCase().startsWith("reload")) {
            event.setCommand("rel");
        } else if (event.getCommand().toLowerCase().startsWith("stop")) {
            event.setCommand("shutdown");
        }
    }

    @EventHandler
    public void onPlayerChat(PlayerChatEvent event) {

        afkThread.takeAktion(event.getPlayer());

        // FLOOD-CONTROL
        boolean isSupporter = Core.isSupporter(event.getPlayer());
        if (!isSupporter) {
            if (this.lastChatList.containsKey(event.getPlayer().getName())) {
                long lastChatEvent = this.lastChatList.get(event.getPlayer().getName());
                long delta = System.currentTimeMillis() - lastChatEvent;
                if (delta < chatPause) {
                    TextUtils.sendError(event.getPlayer(), "You can only chat every " + Settings.getChatPauseTimeInSeconds() + " seconds.");
                    event.setCancelled(true);
                    return;
                }
            }
        }

        // USED BAD WORD
        String message = " " + event.getMessage().toLowerCase() + " ";
        if (!isSupporter && !filter.acceptMessage(message)) {
            // troll them by sending the message to them but to no other player
            TextUtils.sendLine(event.getPlayer(), ChatColor.GREEN, event.getPlayer().getName() + ChatColor.WHITE + ": " + event.getMessage());
            event.getRecipients().clear();
            event.setCancelled(true);

            // WARN SUPPORTER
            for (String playerName : Core.getSupporter()) {
                Player player = Bukkit.getPlayer(playerName);
                if (player != null && player.isOnline()) {
                    TextUtils.sendLine(player, ChatColor.RED, "CAUTION! User '" + event.getPlayer().getName() + "' is trolling around. Message: " + ChatColor.GRAY + event.getMessage());
                }
            }
            for (OfflinePlayer op : Bukkit.getOperators()) {
                if (op.isOnline())
                    TextUtils.sendLine(op.getPlayer(), ChatColor.RED, "CAUTION! User '" + event.getPlayer().getName() + "' is trolling around. Message: " + ChatColor.GRAY + event.getMessage());

            }
            return;
        }

        // IS PLAYER MUTED
        if (this.isMuted(event.getPlayer())) {
            // troll them by sending the message to them but to no other player
            TextUtils.sendLine(event.getPlayer(), ChatColor.GREEN, event.getPlayer().getName() + ChatColor.WHITE + ": " + event.getMessage());
            event.getRecipients().clear();
            event.setCancelled(true);

            // WARN ADMINS
            for (String playerName : Core.getSupporter()) {
                Player player = Bukkit.getPlayer(playerName);
                if (player != null && player.isOnline()) {
                    TextUtils.sendLine(player, ChatColor.RED, "[Muted] " + ChatColor.GREEN + event.getPlayer().getName() + ChatColor.WHITE + ": " + event.getMessage());
                }
            }

            return;
        }

        // CHAT-RADIUS
        if (radiusOff) {
            if (Settings.getChatRadius() > 0 && !Core.isSupporter(event.getPlayer())) {
                Location chatLocation = event.getPlayer().getLocation();
                Iterator<Player> iterator = event.getRecipients().iterator();
                Player thisPlayer;
                while (iterator.hasNext()) {
                    thisPlayer = iterator.next();
                    if (!isSupporter && Math.abs(thisPlayer.getLocation().distance(chatLocation)) > Settings.getChatRadius()) {
                        iterator.remove();
                    }
                }
            }
        }

        // FORMAT CHAT
        event.setFormat("%2$s");
        if (isSupporter)
            event.setMessage(ChatColor.RED + event.getPlayer().getName() + ChatColor.WHITE + ": " + event.getMessage().replace("$", ""));
        else
            event.setMessage(ChatColor.GREEN + event.getPlayer().getName() + ChatColor.WHITE + ": " + event.getMessage().replace("$", ""));

        lastChatList.put(event.getPlayer().getName(), System.currentTimeMillis());
    }
}
