package com.gladurbad.medusa.check.impl.combat.hitbox;

import com.gladurbad.api.check.CheckInfo;
import com.gladurbad.medusa.check.Check;
import com.gladurbad.medusa.data.PlayerData;
import com.gladurbad.medusa.packet.Packet;

@CheckInfo(name = "Hitbox (A)", description = "Checks for invalid attack angles and hitbox expansion.", complextype = "Hitbox")
public class HitboxA extends Check {


    public HitboxA(PlayerData data) {
        super(data);
    }

    @Override
    public void handle(Packet packet) {

    }
}