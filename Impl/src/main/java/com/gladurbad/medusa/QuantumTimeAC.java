package com.gladurbad.medusa;

import com.gladurbad.medusa.listener.BukkitEventListener;
import com.gladurbad.medusa.listener.ClientBrandListener;
import com.gladurbad.medusa.listener.NetworkListener;
import com.gladurbad.medusa.listener.JoinQuitListener;
import com.gladurbad.medusa.manager.*;
import io.github.retrooper.packetevents.PacketEvents;
import lombok.Getter;
import com.gladurbad.medusa.command.CommandManager;
import com.gladurbad.medusa.config.Config;
import com.gladurbad.medusa.packet.processor.ReceivingPacketProcessor;
import com.gladurbad.medusa.packet.processor.SendingPacketProcessor;
import org.bukkit.Bukkit;
import org.bukkit.plugin.messaging.Messenger;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Getter
public enum QuantumTimeAC {

    INSTANCE;

    private QuantumTimePlugin plugin;

    private long startTime;

    private final TickManager tickManager = new TickManager();
    private final ReceivingPacketProcessor receivingPacketProcessor = new ReceivingPacketProcessor();
    private final SendingPacketProcessor sendingPacketProcessor = new SendingPacketProcessor();
    private final PlayerDataManager playerDataManager = new PlayerDataManager();
    private final CommandManager commandManager = new CommandManager(this.getPlugin());
    private final ExecutorService packetExecutor = Executors.newSingleThreadExecutor();

    public void start(final QuantumTimePlugin plugin) {
        this.plugin = plugin;
        assert plugin != null : "Error while starting qtac.";

        this.getPlugin().saveDefaultConfig();
        Config.updateConfig();

        CheckManager.setup();
        ThemeManager.setup();

        Bukkit.getOnlinePlayers().forEach(player -> this.getPlayerDataManager().add(player));

        getPlugin().saveDefaultConfig();
        getPlugin().getCommand("qtac").setExecutor(commandManager);

        tickManager.start();

        final Messenger messenger = Bukkit.getMessenger();
        messenger.registerIncomingPluginChannel(plugin, "minecraft:brand", new ClientBrandListener());

        startTime = System.currentTimeMillis();

        Bukkit.getServer().getPluginManager().registerEvents(new BukkitEventListener(), plugin);
        Bukkit.getServer().getPluginManager().registerEvents(new ClientBrandListener(), plugin);
        Bukkit.getServer().getPluginManager().registerEvents(new JoinQuitListener(), plugin);

        PacketEvents.get().registerListener(new NetworkListener());
    }

    public void stop(final QuantumTimePlugin plugin) {
        this.plugin = plugin;
        assert plugin != null : "Error while shutting down qtac.";

        tickManager.stop();
    }
}
