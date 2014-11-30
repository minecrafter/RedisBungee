/**
 * Copyright © 2013 tuxed <write@imaginarycode.com>
 * This work is free. You can redistribute it and/or modify it under the
 * terms of the Do What The Fuck You Want To Public License, Version 2,
 * as published by Sam Hocevar. See http://www.wtfpl.net/ for more details.
 */
package io.minimum.minecraft.redisbungee.util;

import io.minimum.minecraft.redisbungee.RedisBungee;
import lombok.AllArgsConstructor;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.util.concurrent.Callable;
import java.util.logging.Level;

@AllArgsConstructor
public abstract class RedisCallable<T> implements Callable<T>
{
    private final RedisBungee plugin;

    @Override
    public T call()
    {
        return run(false);
    }

    private T run(boolean retry)
    {
        Jedis jedis = null;

        try
        {
            jedis = plugin.getPool().getResource();
            return call(jedis);
        }
        catch (JedisConnectionException e)
        {
            plugin.getLogger().log(Level.SEVERE, "Unable to get connection", e);

            if (jedis != null)
                plugin.getPool().returnBrokenResource(jedis);

            if (!retry)
            {
                // Wait 100ms before retrying the task
                try
                {
                    Thread.sleep(100);
                }
                catch (InterruptedException e1)
                {
                    throw new RuntimeException("task failed to run", e1);
                }
                run(true);
            }
        }
        finally
        {
            if (jedis != null)
            {
                plugin.getPool().returnResource(jedis);
            }
        }

        throw new RuntimeException("task failed to run");
    }

    protected abstract T call(Jedis jedis);
}