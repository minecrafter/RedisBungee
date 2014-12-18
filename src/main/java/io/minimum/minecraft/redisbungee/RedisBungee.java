/**
 * Copyright © 2013 tuxed <write@imaginarycode.com>
 * This work is free. You can redistribute it and/or modify it under the
 * terms of the Do What The Fuck You Want To Public License, Version 2,
 * as published by Sam Hocevar. See http://www.wtfpl.net/ for more details.
 */
package io.minimum.minecraft.redisbungee;

import com.google.common.base.Functions;
import com.google.common.collect.*;
import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import io.minimum.minecraft.redisbungee.events.PubSubMessageEvent;
import io.minimum.minecraft.redisbungee.util.RedisCallable;
import io.minimum.minecraft.redisbungee.util.UUIDTranslator;
import lombok.Getter;
import lombok.NonNull;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * The RedisBungee plugin.
 * <p>
 * The only function of interest is {@link #getApi()}, which exposes some functions in this class.
 */
public final class RedisBungee extends Plugin
{
    private static Configuration configuration;
    @Getter
    private static Gson gson = new Gson();
    private static RedisBungeeAPI api;
    private static PubSubListener psl = null;
    @Getter
    private JedisPool pool;
    @Getter
    private UUIDTranslator uuidTranslator;
    @Getter
    private String serverId;
    @Getter
    private DataManager dataManager;
    @Getter
    private ExecutorService service;
    private List<String> serverIds;
    private AtomicInteger nagAboutServers = new AtomicInteger();
    private ScheduledTask integrityCheck;
    private ScheduledTask heartbeatTask;

    /**
     * Fetch the {@link RedisBungeeAPI} object created on plugin start.
     *
     * @return the {@link RedisBungeeAPI} object
     */
    public static RedisBungeeAPI getApi()
    {
        return api;
    }

    static Configuration getConfiguration()
    {
        return configuration;
    }

    static PubSubListener getPubSubListener()
    {
        return psl;
    }

    final List<String> getServerIds()
    {
        return serverIds;
    }

    final List<String> getCurrentServerIds(Jedis jedis)
    {
        int nag = nagAboutServers.decrementAndGet();
        if (nag <= 0)
        {
            nagAboutServers.set(10);
        }

        Map<String, String> heartbeats = jedis.hgetAll("heartbeats");

        ImmutableList.Builder<String> servers = ImmutableList.builder();

        for (Map.Entry<String, String> entry : heartbeats.entrySet())
        {
            try
            {
                long stamp = Long.valueOf(entry.getValue());
                if (System.currentTimeMillis() < stamp + 30000)
                    servers.add(entry.getKey());
                else if (nag <= 0)
                {
                    getLogger().severe(entry.getKey() + " is " + (System.currentTimeMillis() - stamp) + "ms behind! (Time not synchronized or server down?)");
                }
            }
            catch (NumberFormatException ignored)
            {
            }
        }
        return servers.build();
    }

    final Set<UUID> getLocalPlayers()
    {
        return getProxy().getPlayers().stream().map(ProxiedPlayer::getUniqueId).collect(Collectors.toSet());
    }

    final Collection<String> getLocalPlayersAsUuidStrings()
    {
        return getLocalPlayers().stream().map(UUID::toString).collect(Collectors.toSet());
    }

    final int getCount()
    {
        return new RedisCallable<Integer>(this)
        {
            @Override
            protected Integer call(Jedis jedis)
            {
                int count = 0;

                for (String i : getServerIds())
                {
                    count += jedis.scard("proxy:" + i + ":usersOnline");
                }

                return count;
            }
        }.call();
    }

    final Set<UUID> getPlayers()
    {
        return new RedisCallable<Set<UUID>>(this)
        {
            @Override
            protected Set<UUID> call(Jedis jedis)
            {
                ImmutableSet.Builder<UUID> builder = ImmutableSet.builder();

                List<String> keys = new ArrayList<>();

                for (String i : getServerIds())
                {
                    if (i.equals(serverId))
                        continue;

                    keys.add("proxy:" + i + ":usersOnline");
                }

                if (!keys.isEmpty())
                {
                    Set<String> users = jedis.sunion(keys.toArray(new String[keys.size()]));
                    if (!users.isEmpty())
                    {
                        for (String user : users)
                        {
                            builder.add(UUID.fromString(user));
                        }
                    }
                }

                return builder.build();
            }
        }.call();
    }

    final void sendProxyCommand(@NonNull String proxyId, @NonNull String command)
    {
        checkArgument(getServerIds().contains(proxyId) || proxyId.equals("allservers"), "proxyId is invalid");
        sendChannelMessage("redisbungee-" + proxyId, command);
    }

    final void sendChannelMessage(final String channel, final String message)
    {
        new RedisCallable<Void>(this)
        {
            @Override
            protected Void call(Jedis jedis)
            {
                jedis.publish(channel, message);
                return null;
            }
        }.call();
    }

    @Override
    public void onEnable()
    {
        try
        {
            loadConfig();
        }
        catch (IOException e)
        {
            throw new RuntimeException("Unable to load/save config", e);
        }
        catch (JedisConnectionException e)
        {
            throw new RuntimeException("Unable to connect to your Redis server!", e);
        }
        if (pool != null)
        {
            RedisCallable<Void> heartbeatCallable = new RedisCallable<Void>(this)
            {
                @Override
                protected Void call(Jedis jedis)
                {
                    jedis.hset("heartbeats", serverId, String.valueOf(System.currentTimeMillis()));
                    serverIds = getCurrentServerIds(jedis);
                    return null;
                }
            };

            heartbeatCallable.call();

            uuidTranslator = new UUIDTranslator(this);
            heartbeatTask = getProxy().getScheduler().schedule(this, heartbeatCallable::call, 0, 3, TimeUnit.SECONDS);
            dataManager = new DataManager(this);

            if (configuration.getBoolean("register-bungee-commands", true))
            {
                getProxy().getPluginManager().registerCommand(this, new RedisBungeeCommands.GlistCommand(this));
                getProxy().getPluginManager().registerCommand(this, new RedisBungeeCommands.FindCommand(this));
                getProxy().getPluginManager().registerCommand(this, new RedisBungeeCommands.LastSeenCommand(this));
                getProxy().getPluginManager().registerCommand(this, new RedisBungeeCommands.IpCommand(this));
            }
            getProxy().getPluginManager().registerCommand(this, new RedisBungeeCommands.SendToAll(this));
            getProxy().getPluginManager().registerCommand(this, new RedisBungeeCommands.ServerId(this));
            getProxy().getPluginManager().registerCommand(this, new RedisBungeeCommands.ServerIds());
            getProxy().getPluginManager().registerCommand(this, new RedisBungeeCommands.PlayerProxyCommand(this));
            api = new RedisBungeeAPI(this);
            getProxy().getPluginManager().registerListener(this, new RedisBungeeListener(this));
            getProxy().getPluginManager().registerListener(this, dataManager);
            psl = new PubSubListener();
            getProxy().getScheduler().runAsync(this, psl);
            integrityCheck = getProxy().getScheduler().schedule(this, () ->
            {
                try (Jedis tmpRsc = pool.getResource())
                {
                    Set<String> players = new HashSet<>(getLocalPlayersAsUuidStrings());
                    Set<String> redisCollection = tmpRsc.smembers("proxy:" + serverId + ":usersOnline");

                    for (String member : redisCollection)
                    {
                        if (!players.contains(member))
                        {
                            // Are they simply on a different proxy?
                            boolean found = false;
                            for (String proxyId : getServerIds())
                            {
                                if (proxyId.equals(serverId)) continue;
                                if (tmpRsc.sismember("proxy:" + proxyId + ":usersOnline", member))
                                {
                                    // Just clean up the set.
                                    found = true;
                                    break;
                                }
                            }
                            if (!found)
                            {
                                RedisUtil.cleanUpPlayer(member, tmpRsc);
                                getLogger().warning("Player found in set that was not found locally and globally: " + member);
                            } else
                            {
                                tmpRsc.srem("proxy:" + serverId + ":usersOnline", member);
                                getLogger().warning("Player found in set that was not found locally, but is on another proxy: " + member);
                            }
                        }
                    }

                    for (String player : players)
                    {
                        if (redisCollection.contains(player))
                            continue;

                        // Player not online according to Redis but not BungeeCord. Fire another consumer event.
                        getLogger().warning("Player " + player + " is on the proxy but not in Redis.");
                        tmpRsc.sadd("proxy:" + serverId + ":usersOnline", player);
                    }
                }
            }, 0, 1, TimeUnit.MINUTES);
        }
        getProxy().registerChannel("RedisBungee");
    }

    @Override
    public void onDisable()
    {
        if (pool != null)
        {
            // Poison the PubSub listener
            psl.poison();
            getProxy().getScheduler().cancel(this);
            integrityCheck.cancel();
            heartbeatTask.cancel();
            getProxy().getPluginManager().unregisterListeners(this);

            getLogger().info("Waiting for all tasks to finish.");

            service.shutdown();
            try
            {
                if (!service.awaitTermination(60, TimeUnit.SECONDS))
                {
                    service.shutdownNow();
                }
            }
            catch (InterruptedException ignored)
            {
            }

            Jedis tmpRsc = pool.getResource();
            try
            {
                tmpRsc.hdel("heartbeats", serverId);
                if (tmpRsc.scard("proxy:" + serverId + ":usersOnline") > 0)
                {
                    Set<String> players = tmpRsc.smembers("proxy:" + serverId + ":usersOnline");
                    for (String member : players)
                        RedisUtil.cleanUpPlayer(member, tmpRsc);
                }
            }
            finally
            {
                pool.returnResource(tmpRsc);
            }

            pool.destroy();
        }
    }

    private void loadConfig() throws IOException, JedisConnectionException
    {
        if (!getDataFolder().exists())
        {
            getDataFolder().mkdir();
        }

        File file = new File(getDataFolder(), "config.yml");

        if (!file.exists())
        {
            file.createNewFile();
            try (InputStream in = getResourceAsStream("example_config.yml");
                 OutputStream out = new FileOutputStream(file))
            {
                ByteStreams.copy(in, out);
            }
        }

        configuration = ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);

        final String redisServer = configuration.getString("redis-server", "localhost");
        final int redisPort = configuration.getInt("redis-port", 6379);
        String redisPassword = configuration.getString("redis-password");
        serverId = configuration.getString("server-id");

        if (redisPassword != null && (redisPassword.isEmpty() || redisPassword.equals("none")))
        {
            redisPassword = null;
        }

        // Configuration sanity checks.
        if (serverId == null || serverId.isEmpty())
        {
            throw new RuntimeException("server-id is not specified in the configuration or is empty");
        }

        if (redisServer != null && !redisServer.isEmpty())
        {
            final String finalRedisPassword = redisPassword;
            FutureTask<JedisPool> task = new FutureTask<>(() ->
            {
                JedisPoolConfig config = new JedisPoolConfig();
                config.setMaxTotal(configuration.getInt("max-redis-connections", 8));
                config.setJmxEnabled(false);
                return new JedisPool(config, redisServer, redisPort, 0, finalRedisPassword);
            });

            getProxy().getScheduler().runAsync(this, task);

            try
            {
                pool = task.get();
            }
            catch (InterruptedException | ExecutionException e)
            {
                throw new RuntimeException("Unable to create Redis pool", e);
            }

            // Test the connection
            Jedis rsc = null;
            try
            {
                rsc = pool.getResource();
                rsc.ping();
                // If that worked, now we can check for an existing, alive Bungee:
                File crashFile = new File(getDataFolder(), "restarted_from_crash.txt");
                if (crashFile.exists())
                    crashFile.delete();
                else if (rsc.hexists("heartbeats", serverId))
                {
                    try
                    {
                        Long value = Long.valueOf(rsc.hget("heartbeats", serverId));
                        if (value != null && System.currentTimeMillis() < value + 20000)
                        {
                            getLogger().severe("You have launched a possible impostor BungeeCord instance. Another instance is already running.");
                            getLogger().severe("For data consistency reasons, RedisBungee will now disable itself.");
                            getLogger().severe("If this instance is coming up from a crash, create a file in your RedisBungee plugins directory with the name 'restarted_from_crash.txt' and RedisBungee will not perform this check.");
                            throw new RuntimeException("Possible impostor instance!");
                        }
                    }
                    catch (NumberFormatException ignored)
                    {
                    }
                }

                FutureTask<Void> task2 = new FutureTask<>(() ->
                {
                    service = Executors.newFixedThreadPool(16);
                    return null;
                });

                getProxy().getScheduler().runAsync(this, task2);

                try
                {
                    task2.get();
                }
                catch (InterruptedException | ExecutionException e)
                {
                    throw new RuntimeException("Unable to create executor", e);
                }

                getLogger().log(Level.INFO, "Successfully connected to Redis.");
            }
            catch (JedisConnectionException e)
            {
                if (rsc != null)
                    pool.returnBrokenResource(rsc);
                pool.destroy();
                pool = null;
                rsc = null;
                throw e;
            }
            finally
            {
                if (rsc != null && pool != null)
                {
                    pool.returnResource(rsc);
                }
            }
        } else
        {
            throw new RuntimeException("No redis server specified!");
        }
    }

    class PubSubListener implements Runnable
    {
        private Jedis rsc;
        private JedisPubSubHandler jpsh;

        private PubSubListener()
        {
        }

        @Override
        public void run()
        {
            try
            {
                rsc = pool.getResource();
                jpsh = new JedisPubSubHandler();
                rsc.subscribe(jpsh, "redisbungee-" + serverId, "redisbungee-allservers", "redisbungee-data");
            }
            catch (JedisException | ClassCastException ignored)
            {
            }
        }

        public void addChannel(String... channel)
        {
            jpsh.subscribe(channel);
        }

        public void removeChannel(String... channel)
        {
            jpsh.unsubscribe(channel);
        }

        public void poison()
        {
            jpsh.unsubscribe();
        }
    }

    class JedisPubSubHandler extends JedisPubSub
    {
        @Override
        public void onMessage(final String s, final String s2)
        {
            if (s2.trim().length() == 0) return;
            getProxy().getScheduler().runAsync(RedisBungee.this, () -> getProxy().getPluginManager().callEvent(new PubSubMessageEvent(s, s2)));
        }

        @Override
        public void onPMessage(String s, String s2, String s3)
        {
        }

        @Override
        public void onSubscribe(String s, int i)
        {
        }

        @Override
        public void onUnsubscribe(String s, int i)
        {
        }

        @Override
        public void onPUnsubscribe(String s, int i)
        {
        }

        @Override
        public void onPSubscribe(String s, int i)
        {
        }
    }
}
