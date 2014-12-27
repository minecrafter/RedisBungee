package io.minimum.minecraft.redisbungee.compat;

import io.minimum.minecraft.redisbungee.events.PlayerChangedServerNetworkEvent;
import io.minimum.minecraft.redisbungee.events.PlayerJoinedNetworkEvent;
import io.minimum.minecraft.redisbungee.events.PlayerLeftNetworkEvent;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class RedisBungee03Compat implements Listener
{
    @EventHandler
    public void onPlayerJoinedNetwork(PlayerJoinedNetworkEvent event)
    {
        ProxyServer.getInstance().getPluginManager().callEvent(new com.imaginarycode.minecraft.redisbungee.events.PlayerJoinedNetworkEvent(event.getPlayer().getUniqueId()));
    }

    @EventHandler
    public void onPlayerLeftNetwork(PlayerLeftNetworkEvent event)
    {
        ProxyServer.getInstance().getPluginManager().callEvent(new com.imaginarycode.minecraft.redisbungee.events.PlayerLeftNetworkEvent(event.getPlayer().getUniqueId()));
    }

    @EventHandler
    public void onPlayerChangeServerNetwork(PlayerChangedServerNetworkEvent event)
    {
        ProxyServer.getInstance().getPluginManager().callEvent(new com.imaginarycode.minecraft.redisbungee.events.PlayerChangedServerNetworkEvent(event.getPlayer().getUniqueId(), event.getPreviousServer(), event.getServer()));
    }
}
