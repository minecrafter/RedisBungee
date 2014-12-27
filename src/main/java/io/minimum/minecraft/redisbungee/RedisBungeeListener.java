/**
 * Copyright © 2013 tuxed <write@imaginarycode.com>
 * This work is free. You can redistribute it and/or modify it under the
 * terms of the Do What The Fuck You Want To Public License, Version 2,
 * as published by Sam Hocevar. See http://www.wtfpl.net/ for more details.
 */
package io.minimum.minecraft.redisbungee;

import com.google.common.base.Joiner;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import io.minimum.minecraft.redisbungee.events.PubSubMessageEvent;
import io.minimum.minecraft.redisbungee.util.RedisCallable;
import lombok.AllArgsConstructor;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.*;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import redis.clients.jedis.Jedis;

import java.util.*;

@AllArgsConstructor
public class RedisBungeeListener implements Listener
{
    private static final BaseComponent[] ALREADY_LOGGED_IN =
            new ComponentBuilder("You are already logged on to this server.").color(ChatColor.RED)
                    .append("\n\nIf you were disconnected forcefully, please wait up to one minute.\nIf this does not resolve your issue, please contact staff.")
                    .color(ChatColor.GRAY)
                    .create();
    private final RedisBungee plugin;

    @EventHandler
    public void onPlayerConnect(final PostLoginEvent event)
    {
        Jedis rsc = plugin.getPool().getResource();
        try
        {
            for (String server : plugin.getServerIds())
            {
                if (rsc.sismember("proxy:" + server + ":usersOnline", event.getPlayer().getUniqueId().toString()))
                {
                    event.getPlayer().disconnect(ALREADY_LOGGED_IN);
                    return;
                }
            }

            plugin.getService().submit(new RedisCallable<Void>(plugin)
            {
                @Override
                protected Void call(Jedis jedis)
                {
                    jedis.sadd("proxy:" + RedisBungee.getApi().getServerId() + ":usersOnline", event.getPlayer().getUniqueId().toString());
                    jedis.hset("player:" + event.getPlayer().getUniqueId().toString(), "online", "0");
                    jedis.hset("player:" + event.getPlayer().getUniqueId().toString(), "ip", event.getPlayer().getAddress().getAddress().getHostAddress());
                    plugin.getUuidTranslator().persistInfo(event.getPlayer().getName(), event.getPlayer().getUniqueId(), jedis);
                    jedis.hset("player:" + event.getPlayer().getUniqueId().toString(), "proxy", plugin.getServerId());
                    jedis.publish("redisbungee-data", RedisBungee.getGson().toJson(new DataManager.DataManagerMessage<>(
                            event.getPlayer().getUniqueId(), DataManager.DataManagerMessage.Action.JOIN,
                            new DataManager.LoginPayload(event.getPlayer().getAddress().getAddress()))));
                    return null;
                }
            });
        }
        finally
        {
            plugin.getPool().returnResource(rsc);
        }
    }

    @EventHandler
    public void onPlayerDisconnect(final PlayerDisconnectEvent event)
    {
        plugin.getService().submit(new RedisCallable<Void>(plugin)
        {
            @Override
            protected Void call(Jedis jedis)
            {
                long timestamp = System.currentTimeMillis();
                jedis.hset("player:" + event.getPlayer().getUniqueId().toString(), "online", String.valueOf(timestamp));
                RedisUtil.cleanUpPlayer(event.getPlayer().getUniqueId().toString(), jedis);
                jedis.publish("redisbungee-data", RedisBungee.getGson().toJson(new DataManager.DataManagerMessage<>(
                        event.getPlayer().getUniqueId(), DataManager.DataManagerMessage.Action.LEAVE,
                        new DataManager.LogoutPayload(timestamp))));
                return null;
            }
        });
    }

    @EventHandler
    public void onServerChange(final ServerConnectedEvent event)
    {
        plugin.getService().submit(new RedisCallable<Void>(plugin)
        {
            @Override
            protected Void call(Jedis jedis)
            {
                jedis.hset("player:" + event.getPlayer().getUniqueId().toString(), "server", event.getServer().getInfo().getName());
                jedis.publish("redisbungee-data", RedisBungee.getGson().toJson(new DataManager.DataManagerMessage<>(
                        event.getPlayer().getUniqueId(), DataManager.DataManagerMessage.Action.SERVER_CHANGE,
                        new DataManager.ServerChangePayload(event.getServer().getInfo().getName()))));
                return null;
            }
        });
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPing(final ProxyPingEvent event)
    {
        event.registerIntent(plugin);
        plugin.getProxy().getScheduler().runAsync(plugin, new Runnable()
        {
            @Override
            public void run()
            {
                ServerPing old = event.getResponse();
                ServerPing reply = new ServerPing();
                reply.setPlayers(new ServerPing.Players(old.getPlayers().getMax(), plugin.getCount(), old.getPlayers().getSample()));
                reply.setDescription(old.getDescription());
                reply.setFavicon(old.getFaviconObject());
                reply.setVersion(old.getVersion());
                event.setResponse(reply);
                event.completeIntent(plugin);
            }
        });
    }

    @EventHandler
    public void onPluginMessage(final PluginMessageEvent event)
    {
        if (event.getTag().equals("RedisBungee") && event.getSender() instanceof Server)
        {
            final byte[] data = Arrays.copyOf(event.getData(), event.getData().length);
            plugin.getProxy().getScheduler().runAsync(plugin, new Runnable()
            {
                @Override
                public void run()
                {
                    ByteArrayDataInput in = ByteStreams.newDataInput(data);

                    String subchannel = in.readUTF();
                    ByteArrayDataOutput out = ByteStreams.newDataOutput();
                    String type;

                    switch (subchannel)
                    {
                        case "PlayerList":
                            out.writeUTF("PlayerList");
                            Set<UUID> original;
                            type = in.readUTF();
                            if (type.equals("ALL"))
                            {
                                out.writeUTF("ALL");
                                original = plugin.getPlayers();
                            } else
                            {
                                original = new HashSet<>();

                                for (UUID uuid : plugin.getPlayers())
                                {
                                    String server = plugin.getDataManager().getServer(uuid);

                                    if (server != null)
                                        original.add(uuid);
                                }
                            }
                            Set<String> players = new HashSet<>();
                            for (UUID uuid : original)
                                players.add(plugin.getUuidTranslator().getNameFromUuid(uuid, false));
                            out.writeUTF(Joiner.on(',').join(players));
                            break;
                        case "PlayerCount":
                            out.writeUTF("PlayerCount");
                            type = in.readUTF();
                            if (type.equals("ALL"))
                            {
                                out.writeUTF("ALL");
                                out.writeInt(plugin.getCount());
                            } else
                            {
                                out.writeUTF(type);

                                int c = 0;

                                for (UUID uuid : plugin.getPlayers())
                                {
                                    String server = plugin.getDataManager().getServer(uuid);

                                    if (server != null)
                                        c++; // I wish I wouldn't have to write anything resembling it
                                }

                                out.writeInt(c);
                            }
                            break;
                        case "LastOnline":
                            String user = in.readUTF();
                            out.writeUTF("LastOnline");
                            out.writeUTF(user);
                            out.writeLong(plugin.getDataManager().getLastOnline(plugin.getUuidTranslator().getTranslatedUuid(user, true)));
                            break;
                        default:
                            break;
                    }

                    ((Server) event.getSender()).sendData("RedisBungee", out.toByteArray());
                }
            });
        }
    }

    @EventHandler
    public void onPubSubMessage(PubSubMessageEvent event)
    {
        if (event.getChannel().equals("redisbungee-allservers") || event.getChannel().equals("redisbungee-" + RedisBungee.getApi().getServerId()))
        {
            String message = event.getMessage();
            if (message.startsWith("/"))
                message = message.substring(1);
            plugin.getLogger().info("Invoking command via PubSub: /" + message);
            plugin.getProxy().getPluginManager().dispatchCommand(RedisBungeeCommandSender.instance, message);
        }
    }
}
