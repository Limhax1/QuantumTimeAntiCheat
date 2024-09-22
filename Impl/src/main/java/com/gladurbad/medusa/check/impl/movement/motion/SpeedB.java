package com.gladurbad.medusa.check.impl.movement.motion;

import com.gladurbad.api.check.CheckInfo;
import com.gladurbad.medusa.check.Check;
import com.gladurbad.medusa.data.PlayerData;
import com.gladurbad.medusa.exempt.type.ExemptType;
import com.gladurbad.medusa.packet.Packet;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

@CheckInfo(name = "Speed (B)", description = "Checks for Speed.")
public final class SpeedB extends Check {

	private static final double MAX_BUFFER = 5.0;
	private static final double BUFFER_DECAY = 0.25;
	private static final double JUMP_BOOST = 0.42;
	private static final double LANDING_LENIENCY = 0.1;

	private static final double WALK_SPEED = 0.221;
	private static final double SPRINT_SPEED = 0.29;
	private static final double AIR_SPEED = 0.36;

	private double buffer = 0.0;
	private boolean lastOnGround = true;
	private boolean lastSprinting = false;

	public SpeedB(final PlayerData data) {
		super(data);
	}

	@Override
	public void handle(final Packet packet) {
		if (packet.isPosition()) {
			final double deltaY = data.getPositionProcessor().getDeltaY();
			final double deltaXZ = data.getPositionProcessor().getDeltaXZ();
			final boolean onGround = data.getPositionProcessor().isOnGround();
			final boolean sprinting = data.getActionProcessor().isSprinting();

			if (!data.getPositionProcessor().isInLiquid() && !data.getPositionProcessor().isOnClimbable()) {
				double expectedSpeed = getExpectedSpeed(onGround, sprinting);

				// Speed effect kezelése
				Player player = data.getPlayer();
				double speedCorrection = getSpeedPotionCorrection(player);
				expectedSpeed *= speedCorrection;
				boolean Exempt = isExempt(ExemptType.TELEPORT, ExemptType.SLIME, ExemptType.UNDER_BLOCK, ExemptType.JOINED, ExemptType.ICE, ExemptType.VELOCITY, ExemptType.FLYING);

				if (!onGround && lastOnGround && Math.abs(deltaY - JUMP_BOOST) < 1E-5) {

					expectedSpeed *= 1.1;
				} else if (onGround && !lastOnGround) {
					expectedSpeed += LANDING_LENIENCY;
				}

				if (deltaXZ > expectedSpeed * 1.001 && !Exempt) {
					buffer += 1;
				} else {
					buffer = Math.max(buffer - BUFFER_DECAY, 0);
				}

				if (buffer > MAX_BUFFER && !Exempt) {
					fail(String.format("Going too quick - deltaXZ=%.2f, expectedSpeed=%.2f, buffer=%.2f",
							deltaXZ, expectedSpeed, buffer));
					buffer = 0;
				}

				debug(String.format("DeltaXZ: %.2f, ExpectedSpeed: %.2f, Buffer: %.2f, DeltaY: %.2f, OnGround: %b, Sprinting: %b, SpeedCorrection: %.2f", 
						deltaXZ, expectedSpeed, buffer, deltaY, onGround, sprinting, speedCorrection));

				lastOnGround = onGround;
				lastSprinting = sprinting;
			}
		}
	}

	private double getExpectedSpeed(boolean onGround, boolean sprinting) {
		if (onGround) {
			return sprinting ? SPRINT_SPEED : WALK_SPEED;
		} else {
			return AIR_SPEED;
		}
	}

	private double getSpeedPotionCorrection(Player player) {
		for (PotionEffect effect : player.getActivePotionEffects()) {
			if (effect.getType().equals(PotionEffectType.SPEED)) {
				int speedAmplifier = effect.getAmplifier() + 1;
				return 1.0 + (0.2 * speedAmplifier);
			}
		}
		return 1.0;
	}
}