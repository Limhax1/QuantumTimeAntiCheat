package com.gladurbad.medusa.check.impl.player.protocol;

import com.gladurbad.api.check.CheckInfo;
import com.gladurbad.medusa.check.Check;
import com.gladurbad.medusa.data.PlayerData;
import com.gladurbad.medusa.packet.Packet;

@CheckInfo(name = "Protocol (Z)", description = "Notification handler for player timeouts.", complextype = "Null")
public class ProtocolZ extends Check {

    public ProtocolZ(PlayerData data) {
        super(data);
    }

    @Override
    public void handle(Packet packet) {
        
    }
    
}
