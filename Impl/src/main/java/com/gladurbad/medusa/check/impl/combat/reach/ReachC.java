package com.gladurbad.medusa.check.impl.combat.reach;

import com.gladurbad.api.check.CheckInfo;
import com.gladurbad.medusa.check.Check;
import com.gladurbad.medusa.data.PlayerData;
import com.gladurbad.medusa.exempt.type.ExemptType;
import com.gladurbad.medusa.packet.Packet;
import com.gladurbad.medusa.util.raytrace.RayTrace;
import io.github.retrooper.packetevents.packetwrappers.play.in.useentity.WrappedPacketInUseEntity;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

@CheckInfo(name = "Reach (C)", experimental = true, description = "Checks for reach hacks on the first attack")
public final class ReachC extends Check {

    private static final double MAX_REACH = 3.05;
    private static final long ATTACK_RESET_TIME = 500; // 500ms

    private double buffer;
    private long lastAttackTime;

    public ReachC(final PlayerData data) {
        super(data);
        this.buffer = 0.0;
        this.lastAttackTime = 0L;
    }

    @Override
    public void handle(final Packet packet) {
        if (packet.isUseEntity()) {
            WrappedPacketInUseEntity wrapper = new WrappedPacketInUseEntity(packet.getRawPacket());
            if (wrapper.getAction() != WrappedPacketInUseEntity.EntityUseAction.ATTACK || !(wrapper.getEntity() instanceof Player)) {
                return;
            }

        }
    }
}