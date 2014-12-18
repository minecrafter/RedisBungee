/**
 * Copyright © 2013 tuxed <write@imaginarycode.com>
 * This work is free. You can redistribute it and/or modify it under the
 * terms of the Do What The Fuck You Want To Public License, Version 2,
 * as published by Sam Hocevar. See http://www.wtfpl.net/ for more details.
 */
package com.imaginarycode.minecraft.redisbungee;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.*;
import io.minimum.minecraft.redisbungee.RedisBungee;
import io.minimum.minecraft.redisbungee.players.RedisBungeePlayer;
import lombok.NonNull;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;

import java.net.InetAddress;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * This class exposes some internal RedisBungee functions. You obtain an instance of this object by invoking {@link RedisBungee#getApi()}.
 *
 * @author tuxed
 * @since 0.2.3
 * @deprecated Please use the new API in io.minimum.minecraft.redisbungee
 */
@Deprecated
public class RedisBungeeAPI
{
    /**
     * Get a combined count of all players on this network.
     *
     * @return a count of all players found
     * @deprecated Please use the new API in io.minimum.minecraft.redisbungee
     */
    @Deprecated
    public final int getPlayerCount()
    {
        return RedisBungee.getApi().getPlayerCount();
    }

    /**
     * Get the last time a player was on. If the player is currently online, this will return 0. If the player has not been recorded,
     * this will return -1. Otherwise it will return a value in milliseconds.
     *
     * @param player a player name
     * @return the last time a player was on, if online returns a 0
     * @deprecated Please use the new API in io.minimum.minecraft.redisbungee
     */
    @Deprecated
    public final long getLastOnline(@NonNull UUID player)
    {
        return RedisBungee.getApi().getPlayer(player).getLastOnline();
    }

    /**
     * Get the server where the specified player is playing. This function also deals with the case of local players
     * as well, and will return local information on them.
     *
     * @param player a player name
     * @return a {@link net.md_5.bungee.api.config.ServerInfo} for the server the player is on.
     * @deprecated Please use the new API in io.minimum.minecraft.redisbungee and use Bungee's getServerInfo() method.
     */
    @Deprecated
    public final ServerInfo getServerFor(@NonNull UUID player)
    {
        return ProxyServer.getInstance().getServerInfo(RedisBungee.getApi().getPlayer(player).getServer());
    }

    /**
     * Get a combined list of players on this network.
     * <p>
     * <strong>Note that this function returns an instance of {@link com.google.common.collect.ImmutableSet}.</strong>
     *
     * @return a Set with all players found
     * @deprecated Please use the new API in io.minimum.minecraft.redisbungee
     */
    @Deprecated
    public final Set<UUID> getPlayersOnline()
    {
        return ImmutableSet.copyOf(Collections2.transform(RedisBungee.getApi().getOnlinePlayers(), RedisBungeePlayer::getUniqueId));
    }

    /**
     * Get a combined list of players on this network, as a collection of usernames.
     * <p>
     * <strong>Note that this function returns an immutable {@link java.util.Collection}, and usernames
     * are lazily calculated (but cached, see the contract of {@link #getNameFromUuid(java.util.UUID)}).</strong>
     *
     * @return a Set with all players found
     * @see #getNameFromUuid(java.util.UUID)
     * @since 0.3
     * @deprecated Please use the new API in io.minimum.minecraft.redisbungee
     */
    @Deprecated
    public final Collection<String> getHumanPlayersOnline()
    {
        return RedisBungee.getApi().getHumanPlayersOnline();
    }

    /**
     * Get a full list of players on all servers.
     *
     * @return a immutable Multimap with all players found on this server
     * @since 0.2.5
     * @deprecated Please use the new API in io.minimum.minecraft.redisbungee
     */
    @Deprecated
    public final Multimap<String, UUID> getServerToPlayers()
    {
        Multimap<String, UUID> multimap = HashMultimap.create();
        RedisBungee.getApi().getOnlinePlayers().forEach(p -> multimap.put(p.getServer(), p.getUniqueId()));
        return ImmutableMultimap.copyOf(multimap);
    }

    /**
     * Get a list of players on the server with the given name.
     *
     * @param server a server name
     * @return a Set with all players found on this server
     * @deprecated Please use the new API in io.minimum.minecraft.redisbungee
     */
    @Deprecated
    public final Set<UUID> getPlayersOnServer(@NonNull String server)
    {
        return ImmutableSet.copyOf(getServerToPlayers().get(server));
    }

    /**
     * Convenience method: Checks if the specified player is online.
     *
     * @param player a player name
     * @return if the player is online
     * @deprecated Please use the new API in io.minimum.minecraft.redisbungee
     */
    @Deprecated
    public final boolean isPlayerOnline(@NonNull UUID player)
    {
        return RedisBungee.getApi().getPlayer(player).getLastOnline() == 0;
    }

    /**
     * Get the {@link java.net.InetAddress} associated with this player.
     *
     * @param player the player to fetch the IP for
     * @return an {@link java.net.InetAddress} if the player is online, null otherwise
     * @since 0.2.4
     * @deprecated Please use the new API in io.minimum.minecraft.redisbungee
     */
    @Deprecated
    public final InetAddress getPlayerIp(@NonNull UUID player)
    {
        return RedisBungee.getApi().getPlayer(player).getAddress();
    }

    /**
     * Get the RedisBungee proxy ID this player is connected to.
     *
     * @param player the player to fetch the IP for
     * @return the proxy the player is connected to, or null if they are offline
     * @since 0.3.3
     * @deprecated Please use the new API in io.minimum.minecraft.redisbungee
     */
    @Deprecated
    public final String getProxy(@NonNull UUID player)
    {
        return RedisBungee.getApi().getPlayer(player).getProxy();
    }

    /**
     * Sends a proxy command to all proxies.
     *
     * @param command the command to send and execute
     * @see #sendProxyCommand(String, String)
     * @since 0.2.5
     * @deprecated Please use the new API in io.minimum.minecraft.redisbungee
     */
    @Deprecated
    public final void sendProxyCommand(@NonNull String command)
    {
        RedisBungee.getApi().sendProxyCommand("allservers", command);
    }

    /**
     * Sends a proxy command to the proxy with the given ID. "allservers" means all proxies.
     *
     * @param proxyId a proxy ID
     * @param command the command to send and execute
     * @see #getServerId()
     * @see #getAllServers()
     * @since 0.2.5
     * @deprecated Please use the new API in io.minimum.minecraft.redisbungee
     */
    @Deprecated
    public final void sendProxyCommand(@NonNull String proxyId, @NonNull String command)
    {
        RedisBungee.getApi().sendProxyCommand(proxyId, command);
    }

    /**
     * Sends a message to a PubSub channel. The channel has to be subscribed to on this, or another redisbungee instance for {@link com.imaginarycode.minecraft.redisbungee.events.PubSubMessageEvent} to fire.
     *
     * @param channel The PubSub channel
     * @param message the message body to send
     * @since 0.3.3
     * @deprecated Please use the new API in io.minimum.minecraft.redisbungee
     */
    @Deprecated
    public final void sendChannelMessage(@NonNull String channel, @NonNull String message)
    {
        RedisBungee.getApi().sendChannelMessage(channel, message);
    }

    /**
     * Get the current BungeeCord server ID for this server.
     *
     * @return the current server ID
     * @see #getAllServers()
     * @since 0.2.5
     * @deprecated Please use the new API in io.minimum.minecraft.redisbungee
     */
    @Deprecated
    public final String getServerId()
    {
        return RedisBungee.getApi().getServerId();
    }

    /**
     * Get all the linked proxies in this network.
     *
     * @return the list of all proxies
     * @see #getServerId()
     * @since 0.2.5
     * @deprecated Please use the new API in io.minimum.minecraft.redisbungee
     */
    @Deprecated
    public final List<String> getAllServers()
    {
        return RedisBungee.getApi().getAllServers();
    }

    /**
     * Register (a) PubSub channel(s), so that you may handle {@link com.imaginarycode.minecraft.redisbungee.events.PubSubMessageEvent} for it.
     *
     * @param channels the channels to register
     * @since 0.3
     * @deprecated Please use the new API in io.minimum.minecraft.redisbungee
     */
    @Deprecated
    public final void registerPubSubChannels(String... channels)
    {
        RedisBungee.getApi().registerPubSubChannels(channels);
    }

    /**
     * Unregister (a) PubSub channel(s).
     *
     * @param channels the channels to unregister
     * @since 0.3
     * @deprecated Please use the new API in io.minimum.minecraft.redisbungee
     */
    @Deprecated
    public final void unregisterPubSubChannels(String... channels)
    {
        RedisBungee.getApi().unregisterPubSubChannels(channels);
    }

    /**
     * Fetch a name from the specified UUID. UUIDs are cached locally and in Redis. This function falls back to Mojang
     * as a last resort, so calls <strong>may</strong> be blocking.
     * <p>
     * For the common use case of translating a list of UUIDs into names, use {@link #getHumanPlayersOnline()}
     * as the efficiency of that function is slightly greater as the names are calculated lazily.
     * <p>
     * If performance is a concern, use {@link #getNameFromUuid(java.util.UUID, boolean)} as this allows you to disable Mojang lookups.
     *
     * @param uuid the UUID to fetch the name for
     * @return the name for the UUID
     * @since 0.3
     * @deprecated Please use the new API in io.minimum.minecraft.redisbungee
     */
    @Deprecated
    public final String getNameFromUuid(@NonNull UUID uuid)
    {
        return RedisBungee.getApi().getNameFromUuid(uuid, true);
    }

    /**
     * Fetch a name from the specified UUID. UUIDs are cached locally and in Redis. This function can fall back to Mojang
     * as a last resort if {@code expensiveLookups} is true, so calls <strong>may</strong> be blocking.
     * <p>
     * For the common use case of translating the list of online players into names, use {@link #getHumanPlayersOnline()}
     * as the efficiency of that function is slightly greater as the names are calculated lazily.
     * <p>
     * If performance is a concern, set {@code expensiveLookups} to false as this will disable lookups via Mojang.
     *
     * @param uuid             the UUID to fetch the name for
     * @param expensiveLookups whether or not to perform potentially expensive lookups
     * @return the name for the UUID
     * @since 0.3.2
     * @deprecated Please use the new API in io.minimum.minecraft.redisbungee
     */
    @Deprecated
    public final String getNameFromUuid(@NonNull UUID uuid, boolean expensiveLookups)
    {
        return RedisBungee.getApi().getNameFromUuid(uuid, expensiveLookups);
    }

    /**
     * Fetch a UUID from the specified name. Names are cached locally and in Redis. This function falls back to Mojang
     * as a last resort, so calls <strong>may</strong> be blocking.
     * <p>
     * If performance is a concern, see {@link #getUuidFromName(String, boolean)}, which disables the following functions:
     * <ul>
     * <li>Searching Mojang</li>
     * </ul>
     *
     * @param name the UUID to fetch the name for
     * @return the UUID for the name
     * @since 0.3
     * @deprecated Please use the new API in io.minimum.minecraft.redisbungee
     */
    @Deprecated
    public final UUID getUuidFromName(@NonNull String name)
    {
        return RedisBungee.getApi().getUuidFromName(name, true);
    }

    /**
     * Fetch a UUID from the specified name. Names are cached locally and in Redis. This function falls back to Mojang
     * as a last resort if {@code expensiveLookups} is true, so calls <strong>may</strong> be blocking.
     * <p>
     * If performance is a concern, set {@code expensiveLookups} to false to disable searching Mojang.
     *
     * @param name             the UUID to fetch the name for
     * @param expensiveLookups whether or not to perform potentially expensive lookups
     * @return the UUID for the name
     * @since 0.3.2
     * @deprecated Please use the new API in io.minimum.minecraft.redisbungee
     */
    @Deprecated
    public final UUID getUuidFromName(@NonNull String name, boolean expensiveLookups)
    {
        return RedisBungee.getApi().getUuidFromName(name, expensiveLookups);
    }
}
