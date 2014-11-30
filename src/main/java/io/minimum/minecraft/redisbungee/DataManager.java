/**
 * Copyright © 2013 tuxed <write@imaginarycode.com>
 * This work is free. You can redistribute it and/or modify it under the
 * terms of the Do What The Fuck You Want To Public License, Version 2,
 * as published by Sam Hocevar. See http://www.wtfpl.net/ for more details.
 */
package io.minimum.minecraft.redisbungee;

import com.google.common.net.InetAddresses;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import io.minimum.minecraft.redisbungee.events.PlayerChangedServerNetworkEvent;
import io.minimum.minecraft.redisbungee.events.PlayerJoinedNetworkEvent;
import io.minimum.minecraft.redisbungee.events.PlayerLeftNetworkEvent;
import io.minimum.minecraft.redisbungee.events.PubSubMessageEvent;
import io.minimum.minecraft.redisbungee.players.GenericRedisBungeePlayer;
import io.minimum.minecraft.redisbungee.players.RedisBungeePlayer;
import io.minimum.minecraft.redisbungee.util.RedisCallable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;

/**
 * This class manages all the data that RedisBungee fetches from Redis, along with updates to that data.
 *
 * @since 0.3.3
 */
@RequiredArgsConstructor
public class DataManager implements Listener
{
    private final RedisBungee plugin;
    private final ConcurrentMap<UUID, String> serverCache = new ConcurrentHashMap<>(192, 0.65f, 4);
    private final ConcurrentMap<UUID, String> proxyCache = new ConcurrentHashMap<>(192, 0.65f, 4);
    private final ConcurrentMap<UUID, InetAddress> ipCache = new ConcurrentHashMap<>(192, 0.65f, 4);
    private final ConcurrentMap<UUID, Long> lastOnlineCache = new ConcurrentHashMap<>(192, 0.65f, 4);
    private final JsonParser parser = new JsonParser();

    public String getServer(final UUID uuid)
    {
        ProxiedPlayer player = plugin.getProxy().getPlayer(uuid);

        if (player != null)
            return player.getServer() != null ? player.getServer().getInfo().getName() : null;

        String server = serverCache.get(uuid);

        if (server != null)
            return server;

        return new RedisCallable<String>(plugin)
        {
            @Override
            protected String call(Jedis jedis)
            {
                String server = jedis.hget("player:" + uuid, "server");

                if (server == null)
                    return null;

                serverCache.put(uuid, server);
                return server;
            }
        }.call();
    }

    public String getProxy(final UUID uuid)
    {
        ProxiedPlayer player = plugin.getProxy().getPlayer(uuid);

        if (player != null)
            return plugin.getServerId();

        String server = proxyCache.get(uuid);

        if (server != null)
            return server;

        return new RedisCallable<String>(plugin)
        {
            @Override
            protected String call(Jedis jedis)
            {
                String server = jedis.hget("player:" + uuid, "proxy");

                if (server == null)
                    return null;

                serverCache.put(uuid, server);
                return server;
            }
        }.call();
    }

    public InetAddress getIp(final UUID uuid)
    {
        ProxiedPlayer player = plugin.getProxy().getPlayer(uuid);

        if (player != null)
            return player.getAddress().getAddress();

        final InetAddress address = ipCache.get(uuid);

        if (address != null)
            return address;

        return new RedisCallable<InetAddress>(plugin)
        {
            @Override
            protected InetAddress call(Jedis jedis)
            {
                String result = jedis.hget("player:" + uuid, "ip");
                if (result != null)
                {
                    InetAddress address = InetAddresses.forString(result);
                    ipCache.put(uuid, address);
                    return address;
                }
                return null;
            }
        }.call();
    }

    public long getLastOnline(final UUID uuid)
    {
        ProxiedPlayer player = plugin.getProxy().getPlayer(uuid);

        if (player != null)
            return 0;

        Long time = lastOnlineCache.get(uuid);

        if (time != null)
            return time;

        return new RedisCallable<Long>(plugin)
        {
            @Override
            protected Long call(Jedis jedis)
            {
                String result = jedis.hget("player:" + uuid, "online");

                if (result == null)
                    return (long) -1;

                return Long.valueOf(result);
            }
        }.call();
    }

    private void invalidate(UUID uuid)
    {
        ipCache.remove(uuid);
        lastOnlineCache.remove(uuid);
        serverCache.remove(uuid);
        proxyCache.remove(uuid);
    }

    @EventHandler
    public void onPostLogin(PostLoginEvent event)
    {
        // Invalidate all entries related to this player, since they now lie.
        invalidate(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerDisconnect(PlayerDisconnectEvent event)
    {
        // Invalidate all entries related to this player, since they now lie.
        invalidate(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPubSubMessage(PubSubMessageEvent event)
    {
        if (!event.getChannel().equals("redisbungee-data"))
            return;

        // Partially deserialize the message so we can look at the action
        JsonObject jsonObject = parser.parse(event.getMessage()).getAsJsonObject();

        DataManagerMessage.Action action = DataManagerMessage.Action.valueOf(jsonObject.get("action").getAsString());

        switch (action)
        {
            case JOIN:
                final DataManagerMessage<LoginPayload> message1 = RedisBungee.getGson().fromJson(jsonObject, new TypeToken<DataManagerMessage<LoginPayload>>()
                {
                }.getType());
                proxyCache.put(message1.getTarget(), message1.getSource());
                lastOnlineCache.put(message1.getTarget(), (long) 0);
                ipCache.put(message1.getTarget(), message1.getPayload().getAddress());
                plugin.getProxy().getScheduler().runAsync(plugin, new Runnable()
                {
                    @Override
                    public void run()
                    {
                        plugin.getProxy().getPluginManager().callEvent(new PlayerJoinedNetworkEvent(message1.getTarget()));
                    }
                });
                break;
            case LEAVE:
                final DataManagerMessage<LogoutPayload> message2 = RedisBungee.getGson().fromJson(jsonObject, new TypeToken<DataManagerMessage<LogoutPayload>>()
                {
                }.getType());
                invalidate(message2.getTarget());
                lastOnlineCache.put(message2.getTarget(), message2.getPayload().getTimestamp());
                plugin.getProxy().getScheduler().runAsync(plugin, new Runnable()
                {
                    @Override
                    public void run()
                    {
                        plugin.getProxy().getPluginManager().callEvent(new PlayerLeftNetworkEvent(message2.getTarget()));
                    }
                });
                break;
            case SERVER_CHANGE:
                final DataManagerMessage<ServerChangePayload> message3 = RedisBungee.getGson().fromJson(jsonObject, new TypeToken<DataManagerMessage<ServerChangePayload>>()
                {
                }.getType());
                serverCache.put(message3.getTarget(), message3.getPayload().getServer());
                plugin.getProxy().getScheduler().runAsync(plugin, new Runnable()
                {
                    @Override
                    public void run()
                    {
                        plugin.getProxy().getPluginManager().callEvent(new PlayerChangedServerNetworkEvent(message3.getTarget(), message3.getPayload().getServer()));
                    }
                });
                break;
        }
    }

    @Getter
    @RequiredArgsConstructor
    static class DataManagerMessage<T>
    {
        private final UUID target;
        private final String source = RedisBungee.getApi().getServerId();
        private final Action action; // for future use!
        private final T payload;

        enum Action
        {
            JOIN,
            LEAVE,
            SERVER_CHANGE
        }
    }

    @Getter
    @RequiredArgsConstructor
    static class LoginPayload
    {
        private final InetAddress address;
    }

    @Getter
    @RequiredArgsConstructor
    static class ServerChangePayload
    {
        private final String server;
    }

    @Getter
    @RequiredArgsConstructor
    static class LogoutPayload
    {
        private final long timestamp;
    }
}
