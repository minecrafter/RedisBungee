/**
 * Copyright © 2013 tuxed <write@imaginarycode.com>
 * This work is free. You can redistribute it and/or modify it under the
 * terms of the Do What The Fuck You Want To Public License, Version 2,
 * as published by Sam Hocevar. See http://www.wtfpl.net/ for more details.
 */
package io.minimum.minecraft.redisbungee.events;

import io.minimum.minecraft.redisbungee.players.RedisBungeePlayer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.md_5.bungee.api.plugin.Event;

import java.util.UUID;

/**
 * This event is sent when a player connects to a new server. RedisBungee sends the event only when
 * the proxy the player has been connected to is different than the local proxy.
 * <p>
 * This event corresponds to {@link net.md_5.bungee.api.event.ServerConnectedEvent}, and is fired
 * asynchronously.
 *
 * @since 0.3.4
 */
@EqualsAndHashCode(callSuper = false)
@Data
public class PlayerChangedServerNetworkEvent extends Event
{
    private final RedisBungeePlayer player;
    private final String previousServer;
    private final String server;
}
