package io.minimum.minecraft.redisbungee.players;

import io.minimum.minecraft.redisbungee.RedisBungee;
import lombok.AllArgsConstructor;

import java.net.InetAddress;
import java.util.Calendar;
import java.util.UUID;

/**
 * A generic RedisBungee player instance. This is currently implemented the way it currently is
 * until we can rewrite this code some more.
 */
@AllArgsConstructor
public final class GenericRedisBungeePlayer implements RedisBungeePlayer
{
    private final UUID uuid;
    private final RedisBungee plugin;

    @Override
    public String getName()
    {
        return plugin.getUuidTranslator().getNameFromUuid(uuid, false);
    }

    @Override
    public UUID getUniqueId()
    {
        return uuid;
    }

    @Override
    public String getServer()
    {
        return plugin.getDataManager().getServer(uuid);
    }

    @Override
    public String getProxy()
    {
        return plugin.getDataManager().getProxy(uuid);
    }

    @Override
    public long getLastOnline()
    {
        return plugin.getDataManager().getLastOnline(uuid);
    }

    @Override
    public InetAddress getAddress()
    {
        return plugin.getDataManager().getIp(uuid);
    }
}
