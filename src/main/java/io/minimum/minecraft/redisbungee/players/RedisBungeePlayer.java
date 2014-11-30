package io.minimum.minecraft.redisbungee.players;

import java.net.InetAddress;
import java.util.Calendar;
import java.util.UUID;

/**
 * This interface defines a player connected to the network.
 */
public interface RedisBungeePlayer
{
    /**
     * Fetches the name for this player.
     *
     * @return the name
     */
    String getName();

    /**
     * Fetches the UUID for this player.
     *
     * @return the UUID
     */
    UUID getUniqueId();

    /**
     * Fetches the server for this player.
     *
     * @return the server
     */
    String getServer();

    /**
     * Fetches the proxy this player is on.
     *
     * @return the proxy
     */
    String getProxy();

    /**
     * Fetches when this player was last on. If they are currently online, this will return null.
     *
     * @return the last time the player was on, null if they are online
     */
    Calendar getLastOnline();

    /**
     * Fetches the IP this player has connected from.
     *
     * @return the IP the player is connecting from
     */
    InetAddress getAddress();
}
