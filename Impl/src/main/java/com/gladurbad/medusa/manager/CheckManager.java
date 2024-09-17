package com.gladurbad.medusa.manager;

import com.gladurbad.medusa.check.Check;
import com.gladurbad.medusa.check.impl.combat.aimassist.*;
import com.gladurbad.medusa.check.impl.combat.autoclicker.AutoClickerA;
import com.gladurbad.medusa.check.impl.combat.autoclicker.AutoClickerB;
import com.gladurbad.medusa.check.impl.combat.autoclicker.AutoClickerC;
import com.gladurbad.medusa.check.impl.combat.autoclicker.AutoClickerD;
import com.gladurbad.medusa.check.impl.combat.hitbox.HitboxA;
import com.gladurbad.medusa.check.impl.combat.killaura.KillAuraF;
import com.gladurbad.medusa.check.impl.combat.reach.ReachA;
import com.gladurbad.medusa.check.impl.combat.reach.ReachB;
import com.gladurbad.medusa.check.impl.combat.killaura.*;
import com.gladurbad.medusa.check.impl.combat.reach.ReachC;
import com.gladurbad.medusa.check.impl.combat.velocity.VelocityA;
import com.gladurbad.medusa.check.impl.movement.fly.FlyA;
import com.gladurbad.medusa.check.impl.movement.fly.FlyB;
import com.gladurbad.medusa.check.impl.movement.fly.FlyC;
import com.gladurbad.medusa.check.impl.movement.motion.SpeedA;
import com.gladurbad.medusa.check.impl.movement.motion.SpeedB;
import com.gladurbad.medusa.check.impl.movement.phase.PhaseA;
import com.gladurbad.medusa.check.impl.player.hand.HandA;
import com.gladurbad.medusa.check.impl.player.protocol.*;
import com.gladurbad.medusa.config.Config;
import com.gladurbad.medusa.data.PlayerData;
import org.bukkit.Bukkit;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

public final class CheckManager {

    public static final Class<?>[] CHECKS = new Class[] {
            AimAssistA.class,
            AimAssistB.class,
            AutoClickerA.class,
            AutoClickerB.class,
            AutoClickerC.class,
            AutoClickerD.class,
            KillAuraA.class,
            KillAuraB.class,
            KillAuraC.class,
            KillAuraD.class,
            KillAuraE.class,
            KillAuraF.class,
            KillAuraG.class,
            ReachA.class,
            ReachB.class,
            ReachC.class,
            HitboxA.class,
            VelocityA.class,
            FlyA.class,
            FlyB.class,
            FlyC.class,
            PhaseA.class,
            HandA.class,
            SpeedA.class,
            SpeedB.class,
            ProtocolA.class,
            ProtocolB.class,
            ProtocolC.class,
            ProtocolD.class,
            ProtocolE.class,
            ProtocolF.class,
            ProtocolG.class,
            ProtocolH.class,
            ProtocolI.class,
            ProtocolJ.class,
            ProtocolK.class
    };

    private static final List<Constructor<?>> CONSTRUCTORS = new ArrayList<>();

    public static List<Check> loadChecks(final PlayerData data) {
        final List<Check> checkList = new ArrayList<>();
        for (Constructor<?> constructor : CONSTRUCTORS) {
            try {
                checkList.add((Check) constructor.newInstance(data));
            } catch (Exception exception) {
                System.err.println("Failed to load checks for " + data.getPlayer().getName());
                exception.printStackTrace();
            }
        }
        return checkList;
    }

    public static void setup() {
        for (Class<?> clazz : CHECKS) {
            if (Config.ENABLED_CHECKS.contains(clazz.getSimpleName())) {
                try {
                    CONSTRUCTORS.add(clazz.getConstructor(PlayerData.class));
                    //Bukkit.getLogger().info(clazz.getSimpleName() + " is enabled!");
                } catch (NoSuchMethodException exception) {
                    exception.printStackTrace();
                }
            } else {
                Bukkit.getLogger().info(clazz.getSimpleName() + " is disabled!");
            }
        }
    }
}

