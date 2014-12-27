/**
 * Copyright © 2013 tuxed <write@imaginarycode.com>
 * This work is free. You can redistribute it and/or modify it under the
 * terms of the Do What The Fuck You Want To Public License, Version 2,
 * as published by Sam Hocevar. See http://www.wtfpl.net/ for more details.
 */
package io.minimum.minecraft.redisbungee.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import net.md_5.bungee.api.plugin.Event;

/**
 * This event is posted when a PubSub message is received.
 * <p>
 * <strong>Warning</strong>: This event is fired in a separate thread!
 *
 * @since 0.2.6
 */
@EqualsAndHashCode(callSuper = false)
@Data
public class PubSubMessageEvent extends Event
{
    private final String channel;
    private final String message;
}
