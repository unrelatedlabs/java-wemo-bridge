package com.unrelatedlabs.wemo;

import java.io.IOException;
import java.util.Date;

public class Device {
	public Date stateUpdateTime = new Date();
	public String friendlyName;
	public String name;
	public String type="unknown";
	public String serialNumber;
	public boolean colorSupported;

	public boolean on;

	public boolean isOn(){
		return on;
	}
	
	public void setOn(boolean onOff) throws IOException {
	}
	
	public static String normalizeName(String deviceName) {
		return deviceName.toLowerCase().replace(" ", "_");
	}
	
	public boolean matches(String name){
		return  this.name.equals( normalizeName(name) ) || this.serialNumber.equals( name);
	}
	
	public void retrieveState() {
	
	}
	
	public void setColor(String color) throws IOException{
		
	}
}
