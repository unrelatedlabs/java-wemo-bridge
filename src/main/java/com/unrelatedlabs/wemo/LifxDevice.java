package com.unrelatedlabs.wemo;

import java.awt.Color;
import java.io.IOException;

import org.timothyb89.lifx.bulb.Bulb;
import org.timothyb89.lifx.bulb.LIFXColor;
import org.timothyb89.lifx.bulb.PowerState;

public class LifxDevice extends Device {

	private Bulb bulb;

	public LifxDevice(Bulb bulb) {
		type = "lifx";
		colorSupported = true;
		
		this.bulb = bulb;
		this.name = bulb.getLabel();
		this.friendlyName = this.name;
		this.serialNumber = this.bulb.getAddress().getHex();
		
		this.on = bulb.getPowerState() == PowerState.ON;
	}
	
	@Override
	public void retrieveState() {
		this.on = bulb.getPowerState() == PowerState.ON;
	}
	
	@Override
	public int hashCode() {
		return bulb.getAddress().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof LifxDevice) {
		} else {
			return false;
		}
		return this.bulb.getAddress().equals(((LifxDevice) obj).bulb.getAddress());
	}
	
	@Override
	public void setOn(boolean onOff) throws IOException {
		on = onOff;
		bulb.setPowerState( onOff ? PowerState.ON : PowerState.OFF );
	}
	
	public void setColor(String colorString) throws IOException{
		
		Color color = Color.decode(colorString);
		if( color == null){
			color = Color.WHITE;
		}
		
		bulb.setColor( LIFXColor.fromRGB(color.getRed(),color.getGreen(),color.getBlue()) );
	}


}
