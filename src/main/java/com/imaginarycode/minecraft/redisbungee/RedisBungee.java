package com.imaginarycode.minecraft.redisbungee;

/**
 * Compatibility class to retain some level of compatibility with 0.3.x-based plugins.
 */
@Deprecated
public class RedisBungee
{
    public static RedisBungeeAPI getApi()
    {
        return new RedisBungeeAPI();
    }
}
