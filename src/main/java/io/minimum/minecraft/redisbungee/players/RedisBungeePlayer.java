/**
 * Copyright © 2013 tuxed <write@imaginarycode.com>
 * This work is free. You can redistribute it and/or modify it under the
 * terms of the Do What The Fuck You Want To Public License, Version 2,
 * as published by Sam Hocevar. See http://www.wtfpl.net/ for more details.
 */
package io.minimum.minecraft.redisbungee.players;

import java.net.InetAddress;
import java.util.UUID;

/**
 * This interface defines a player connected to the network.
 *
 * @since 0.4
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
     * Fetches when this player was last on. If they are currently online, this will return 0.
     *
     * @return the last time the player was on, 0 if they are online
     */
    long getLastOnline();

    /**
     * Fetches the IP this player has connected from.
     *
     * @return the IP the player is connecting from
     */
    InetAddress getAddress();
}
