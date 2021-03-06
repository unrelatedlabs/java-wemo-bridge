package com.unrelatedlabs.wemo;

import java.io.IOException;

import org.timothyb89.eventbus.EventHandler;
import org.timothyb89.lifx.bulb.BulbStatusUpdatedEvent;
import org.timothyb89.lifx.bulb.LIFXColor;
import org.timothyb89.lifx.gateway.Gateway;
import org.timothyb89.lifx.gateway.GatewayBulbDiscoveredEvent;
import org.timothyb89.lifx.net.BroadcastListener;
import org.timothyb89.lifx.net.GatewayDiscoveredEvent;

public class LifxTest {

	static public void main(String[] args) throws IOException{
		new LifxTest();
		return;
	}
	
	
    public LifxTest() throws IOException {
        BroadcastListener listener = new BroadcastListener();
        listener.bus().register(this);
        listener.startListen();
    }

    @EventHandler
    public void gatewayFound(GatewayDiscoveredEvent ev) {
        Gateway g = ev.getGateway();
        g.bus().register(this);

        g.isConnected();
    }

    @EventHandler
    public void bulbDiscovered(GatewayBulbDiscoveredEvent event) throws IOException {
        // register for bulb events
        event.getBulb().bus().register(this);

        // send some packets
        event.getBulb().turnOff();
        event.getBulb().setColor(LIFXColor.fromRGB(0, 255, 0));
    }

    @EventHandler
    public void bulbUpdated(BulbStatusUpdatedEvent event) {
        System.out.println("bulb updated");
    }

}