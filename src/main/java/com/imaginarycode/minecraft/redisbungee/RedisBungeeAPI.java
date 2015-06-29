/**
 * This is free and unencumbered software released into the public domain.
 *
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 *
 * In jurisdictions that recognize copyright laws, the author or authors
 * of this software dedicate any and all copyright interest in the
 * software to the public domain. We make this dedication for the benefit
 * of the public at large and to the detriment of our heirs and
 * successors. We intend this dedication to be an overt act of
 * relinquishment in perpetuity of all present and future rights to this
 * software under copyright law.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * For more information, please refer to <http://unlicense.org/>
 */
package com.imaginarycode.minecraft.redisbungee;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import lombok.NonNull;
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
 */
public class RedisBungeeAPI {
    private final RedisBungee plugin;
    private final List<String> reservedChannels;

    RedisBungeeAPI(RedisBungee plugin) {
        this.plugin = plugin;
        this.reservedChannels = ImmutableList.of(
                "redisbungee-allservers",
                "redisbungee-" + RedisBungee.getConfiguration().getServerId(),
                "redisbungee-data"
        );
    }

    /**
     * Get a combined count of all players on this network.
     *
     * @return a count of all players found
     */
    public final int getPlayerCount() {
        return plugin.getCount();
    }

    /**
     * Get the last time a player was on. If the player is currently online, this will return 0. If the player has not been recorded,
     * this will return -1. Otherwise it will return a value in milliseconds.
     *
     * @param player a player name
     * @return the last time a player was on, if online returns a 0
     */
    public final long getLastOnline(@NonNull UUID player) {
        return plugin.getDataManager().getLastOnline(player);
    }

    /**
     * Get the server where the specified player is playing. This function also deals with the case of local players
     * as well, and will return local information on them.
     *
     * @param player a player name
     * @return a {@link net.md_5.bungee.api.config.ServerInfo} for the server the player is on.
     */
    public final ServerInfo getServerFor(@NonNull UUID player) {
        String server = plugin.getDataManager().getServer(player);
        return plugin.getProxy().getServerInfo(server);
    }

    /**
     * Get a combined list of players on this network.
     * <p>
     * <strong>Note that this function returns an instance of {@link com.google.common.collect.ImmutableSet}.</strong>
     *
     * @return a Set with all players found
     */
    public final Set<UUID> getPlayersOnline() {
        return plugin.getPlayers();
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
     */
    public final Collection<String> getHumanPlayersOnline() {
        return Collections2.transform(((ImmutableSet<UUID>) getPlayersOnline()).asList(), new Function<UUID, String>() {
            @Override
            public String apply(UUID uuid) {
                return getNameFromUuid(uuid, false);
            }
        });
    }

    /**
     * Get a full list of players on all servers.
     *
     * @return a immutable Multimap with all players found on this server
     * @since 0.2.5
     */
    public final Multimap<String, UUID> getServerToPlayers() {
        return plugin.serversToPlayers();
    }

    /**
     * Get a list of players on the server with the given name.
     *
     * @param server a server name
     * @return a Set with all players found on this server
     */
    public final Set<UUID> getPlayersOnServer(@NonNull String server) {
        return plugin.getPlayersOnServer(server);
    }

    /**
     * Get a list of players on the specified proxy.
     *
     * @param server a server name
     * @return a Set with all UUIDs found on this proxy
     */
    public final Set<UUID> getPlayersOnProxy(@NonNull String server) {
        return plugin.getPlayersOnProxy(server);
    }

    /**
     * Convenience method: Checks if the specified player is online.
     *
     * @param player a player name
     * @return if the player is online
     */
    public final boolean isPlayerOnline(@NonNull UUID player) {
        return getLastOnline(player) == 0;
    }

    /**
     * Get the {@link java.net.InetAddress} associated with this player.
     *
     * @param player the player to fetch the IP for
     * @return an {@link java.net.InetAddress} if the player is online, null otherwise
     * @since 0.2.4
     */
    public final InetAddress getPlayerIp(@NonNull UUID player) {
        return plugin.getDataManager().getIp(player);
    }

    /**
     * Get the RedisBungee proxy ID this player is connected to.
     *
     * @param player the player to fetch the IP for
     * @return the proxy the player is connected to, or null if they are offline
     * @since 0.3.3
     */
    public final String getProxy(@NonNull UUID player) {
        return plugin.getDataManager().getProxy(player);
    }

    /**
     * Sends a proxy command to all proxies.
     *
     * @param command the command to send and execute
     * @see #sendProxyCommand(String, String)
     * @since 0.2.5
     */
    public final void sendProxyCommand(@NonNull String command) {
        plugin.sendProxyCommand("allservers", command);
    }

    /**
     * Sends a proxy command to the proxy with the given ID. "allservers" means all proxies.
     *
     * @param proxyId a proxy ID
     * @param command the command to send and execute
     * @see #getServerId()
     * @see #getAllServers()
     * @since 0.2.5
     */
    public final void sendProxyCommand(@NonNull String proxyId, @NonNull String command) {
        plugin.sendProxyCommand(proxyId, command);
    }

    /**
     * Sends a message to a PubSub channel. The channel has to be subscribed to on this, or another redisbungee instance for {@link com.imaginarycode.minecraft.redisbungee.events.PubSubMessageEvent} to fire.
     *
     * @param channel The PubSub channel
     * @param message the message body to send
     * @since 0.3.3
     */
    public final void sendChannelMessage(@NonNull String channel, @NonNull String message) {
        plugin.sendChannelMessage(channel, message);
    }

    /**
     * Get the current BungeeCord network ID for this network.
     *
     * @return the current network ID
     * @since 0.2.5
     */
    public final String getNetworkId() {
        return RedisBungee.getConfiguration().getNetworkId();
    }

    /**
     * Get the current BungeeCord server ID for this server.
     *
     * @return the current server ID
     * @see #getAllServers()
     * @since 0.2.5
     */
    public final String getServerId() {
        return RedisBungee.getConfiguration().getServerId();
    }

    /**
     * Get all the linked proxies in this network.
     *
     * @return the list of all proxies
     * @see #getServerId()
     * @since 0.2.5
     */
    public final List<String> getAllServers() {
        return plugin.getServerIds();
    }

    /**
     * Register (a) PubSub channel(s), so that you may handle {@link com.imaginarycode.minecraft.redisbungee.events.PubSubMessageEvent} for it.
     *
     * @param channels the channels to register
     * @since 0.3
     */
    public final void registerPubSubChannels(String... channels) {
        RedisBungee.getPubSubListener().addChannel(channels);
    }

    /**
     * Unregister (a) PubSub channel(s).
     *
     * @param channels the channels to unregister
     * @since 0.3
     */
    public final void unregisterPubSubChannels(String... channels) {
        for (String channel : channels) {
            Preconditions.checkArgument(!reservedChannels.contains(channel), "attempting to unregister internal channel");
        }

        RedisBungee.getPubSubListener().removeChannel(channels);
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
     */
    public final String getNameFromUuid(@NonNull UUID uuid) {
        return getNameFromUuid(uuid, true);
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
     */
    public final String getNameFromUuid(@NonNull UUID uuid, boolean expensiveLookups) {
        return plugin.getUuidTranslator().getNameFromUuid(uuid, expensiveLookups);
    }

    /**
     * Fetch a UUID from the specified name. Names are cached locally and in Redis. This function falls back to Mojang
     * as a last resort, so calls <strong>may</strong> be blocking.
     * <p>
     * If performance is a concern, see {@link #getUuidFromName(String, boolean)}, which disables the following functions:
     * <ul>
     * <li>Searching local entries case-insensitively</li>
     * <li>Searching Mojang</li>
     * </ul>
     *
     * @param name the UUID to fetch the name for
     * @return the UUID for the name
     * @since 0.3
     */
    public final UUID getUuidFromName(@NonNull String name) {
        return getUuidFromName(name, true);
    }

    /**
     * Fetch a UUID from the specified name. Names are cached locally and in Redis. This function falls back to Mojang
     * as a last resort if {@code expensiveLookups} is true, so calls <strong>may</strong> be blocking.
     * <p>
     * If performance is a concern, set {@code expensiveLookups} to false to disable searching Mojang and searching for usernames
     * case-insensitively.
     *
     * @param name             the UUID to fetch the name for
     * @param expensiveLookups whether or not to perform potentially expensive lookups
     * @return the UUID for the name
     * @since 0.3.2
     */
    public final UUID getUuidFromName(@NonNull String name, boolean expensiveLookups) {
        return plugin.getUuidTranslator().getTranslatedUuid(name, expensiveLookups);
    }
}
