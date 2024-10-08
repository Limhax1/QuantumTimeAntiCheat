package com.gladurbad.medusa.check.impl.combat.reach;

import com.gladurbad.api.check.CheckInfo;
import com.gladurbad.medusa.check.Check;
import com.gladurbad.medusa.data.PlayerData;
import com.gladurbad.medusa.packet.Packet;
import com.gladurbad.medusa.util.raytrace.RayTrace;
import com.gladurbad.medusa.util.raytrace.RayTraceResult;
import io.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.packetwrappers.play.in.useentity.WrappedPacketInUseEntity;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

@CheckInfo(name = "Reach (C)", experimental = true, description = "I hate cigany", complextype = "Race")
public final class ReachC extends Check {

    private static final double BASE_MAX_REACH = 3.0;
    private static final double PING_COMPENSATION = 0.003;
    private static final int BUFFER_LIMIT = 3;
    private static final double MAX_REACH = 3.5;

    private double buffer;

    public ReachC(final PlayerData data) {
        super(data);
    }

    @Override
    public void handle(final Packet packet) {
        if (packet.isUseEntity()) {
            WrappedPacketInUseEntity wrapper = new WrappedPacketInUseEntity(packet.getRawPacket());
            if (wrapper.getAction() != WrappedPacketInUseEntity.EntityUseAction.ATTACK || !(wrapper.getEntity() instanceof Player)) {
                return;
            }

            Player target = (Player) wrapper.getEntity();
            Location playerLoc = data.getPlayer().getEyeLocation();
            Vector direction = data.getPlayer().getEyeLocation().getDirection();
            long ping = PacketEvents.get().getPlayerUtils().getPing(data.getPlayer());
            double pingCompensation = Math.min(ping * PING_COMPENSATION, 0.6); // Cap at 0.6 blocks

            double maxReach = Math.min(BASE_MAX_REACH + pingCompensation, MAX_REACH);

            RayTrace rayTrace = new RayTrace(data.getPlayer(), playerLoc, direction, 6.0, 0.1);
            RayTraceResult result = rayTrace.trace();

            debug("RayTrace result: " + result.toString());

            if (result.getHitType() == RayTraceResult.HitType.ENTITY && result.getHitEntity() == target) {
                double distance = result.getDistance();

                debug("Hit distance: " + distance + ", Max reach: " + maxReach + ", Buffer: " + buffer);

                if (distance > maxReach) {
                    if (++buffer > BUFFER_LIMIT) {
                        fail("Dist " + distance + "MaxDist " + maxReach);
                    }
                } else {
                    buffer = Math.max(0, buffer - 0.5);
                }
            } else {
                debug("No hit or hit different entity. HitType: " + result.getHitType() + ", HitEntity: " + result.getHitEntity());
            }
        }
    }
}