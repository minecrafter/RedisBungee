package com.imaginarycode.minecraft.redisbungee;

import com.google.common.collect.ImmutableList;
import com.google.common.net.InetAddresses;
import lombok.Getter;
import net.md_5.bungee.config.Configuration;
import redis.clients.jedis.JedisPool;

import java.net.InetAddress;
import java.util.List;
import java.util.Random;

public class RedisBungeeConfiguration {
    @Getter
    private final JedisPool pool;
    @Getter
    private final String serverId;
    @Getter
    private final boolean registerBungeeCommands;
    @Getter
    private final List<InetAddress> exemptAddresses;
    private final boolean randomId;

    protected String getRandomString() {
        String RANDOMSTRING = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        StringBuilder rand = new StringBuilder();
        Random rnd = new Random();
        while (rand.length() < 18) {
            int index = (int) (rnd.nextFloat() * RANDOMSTRING.length());
            rand.append(RANDOMSTRING.charAt(index));
        }
        String randStr = rand.toString();
        return randStr;

    }
    public RedisBungeeConfiguration(JedisPool pool, Configuration configuration) {

        this.pool = pool;
        if (this.randomId = configuration.getBoolean("random-id")) {

            this.serverId = getRandomString();

        } else {

            this.serverId = configuration.getString("server-id");
        }

        this.registerBungeeCommands = configuration.getBoolean("register-bungee-commands", true);

        List<String> stringified = configuration.getStringList("exempt-ip-addresses");
        ImmutableList.Builder<InetAddress> addressBuilder = ImmutableList.builder();

        for (String s : stringified) {
            addressBuilder.add(InetAddresses.forString(s));
        }

        this.exemptAddresses = addressBuilder.build();
    }
}
