package com.gladurbad.medusa.util;

import com.gladurbad.medusa.data.PlayerData;
import io.github.retrooper.packetevents.utils.server.ServerVersion;
import lombok.experimental.UtilityClass;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

@UtilityClass
public class VersionUtil {

    public static boolean isRiptiding(Player player) {
        if (ServerUtil.getServerVersion().isLowerThan(ServerVersion.v_1_13)) return false;
        try {
            return (boolean) Player.class.getMethod("isRiptiding").invoke(player);
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isSwimming(Player player) {
        if (ServerUtil.getServerVersion().isLowerThan(ServerVersion.v_1_13)) return false;
        try {
            return (boolean) Player.class.getMethod("isSwimming").invoke(player);
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isGliding(Player player) {
        if (ServerUtil.getServerVersion().isLowerThan(ServerVersion.v_1_9)) return false;
        try {
            return (boolean) Player.class.getMethod("isGliding").invoke(player);
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean hasPose(Player player, String poseName) {
        if (ServerUtil.getServerVersion().isLowerThan(ServerVersion.v_1_13)) return false;
        try {
            Class<?> poseClass = Class.forName("org.bukkit.entity.Pose");
            Object pose = Player.class.getMethod("getPose").invoke(player);
            return pose.toString().equals(poseName);
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean hasSlowFalling(Player player) {
        if (ServerUtil.getServerVersion().isLowerThan(ServerVersion.v_1_13)) return false;
        try {
            PotionEffectType slowFalling = (PotionEffectType) PotionEffectType.class.getField("SLOW_FALLING").get(null);
            return player.hasPotionEffect(slowFalling);
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean hasLevitation(Player player) {
        if (ServerUtil.getServerVersion().isLowerThan(ServerVersion.v_1_9)) return false;
        try {
            PotionEffectType levitation = (PotionEffectType) PotionEffectType.class.getField("LEVITATION").get(null);
            return player.hasPotionEffect(levitation);
        } catch (Exception e) {
            return false;
        }
    }
}