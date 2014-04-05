package com.unrelatedlabs.wemo;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.omg.CORBA.Request;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

/**
 * Runs a simple UPnP discovery procedure.
 */
public class WemoBridge extends DefaultHandler {

	public static final Logger log = Logger.getAnonymousLogger();

	static ArrayList<WemoSwitch> devices = new ArrayList<WemoSwitch>();

	public void handle(String target, Request baseRequest,
			HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {

	}

	public static void main(String[] args) throws Exception {
	      
		Server server = new Server(9700);

		String webDir = server.getClass().getResource("/com/unrelatedlabs/wemo/www").toExternalForm();
		
		ResourceHandler resource_handler = new ResourceHandler();
		resource_handler.setDirectoriesListed(false);
		resource_handler.setWelcomeFiles(new String[] { "index.html" });
		resource_handler.setResourceBase( webDir );

		ContextHandler apiHandler = new ContextHandler("/devices/");

		WemoBridge bridge = new WemoBridge();
		
		apiHandler.setHandler( bridge );

		HandlerList handlers = new HandlerList();
		handlers.setHandlers(new Handler[] { apiHandler, resource_handler });
		server.setHandler(handlers);

		server.start();

		bridge.startDiscoveryTimer();

		server.join();
	}
	
	public void startDiscoveryTimer(){
		long interval = 10000;
		new Timer().schedule( new TimerTask() {
			@Override
			public void run() {
				try {
					discovery();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}, 0,interval);
	}

	public void discovery() throws IOException {
		// SSDP port
		final int SSDP_PORT = 1900;
		final int SSDP_SEARCH_PORT = 1901;
		// Broadcast address for finding routers.
		final String SSDP_IP = "239.255.255.250";
		// Time out of the connection.
		int TIMEOUT = 5000;
		// Localhost address.
		InetAddress localhost = InetAddress.getLocalHost();

		// Send from localhost:1901
		InetSocketAddress srcAddress = new InetSocketAddress(localhost,
				SSDP_SEARCH_PORT);
		// Send to 239.255.255.250:1900
		InetSocketAddress dstAddress = new InetSocketAddress(
				InetAddress.getByName(SSDP_IP), SSDP_PORT);

		// ----------------------------------------- //
		// Construct the request packet. //
		// ----------------------------------------- //
		StringBuffer discoveryMessage = new StringBuffer();
		discoveryMessage.append("M-SEARCH * HTTP/1.1\r\n");
		discoveryMessage.append("HOST: " + SSDP_IP + ":" + SSDP_PORT + "\r\n");
		//discoveryMessage.append("ST: urn:Belkin:device:controllee:1\r\n");
		discoveryMessage.append("ST: upnp:rootdevice\r\n");
		// ST: urn:schemas-upnp-org:service:WANIPConnection:1\r\n
		discoveryMessage.append("MAN: \"ssdp:discover\"\r\n");
		discoveryMessage.append("MX: 1\r\n");
		discoveryMessage.append("\r\n");
		// System.out.println("Request: " + discoveryMessage.toString() + "\n");
		byte[] discoveryMessageBytes = discoveryMessage.toString().getBytes();
		DatagramPacket discoveryPacket = new DatagramPacket(
				discoveryMessageBytes, discoveryMessageBytes.length, dstAddress);

		// ----------------------------------- //
		// Send multi-cast packet. //
		// ----------------------------------- //
		MulticastSocket multicast = null;
		try {
			multicast = new MulticastSocket(null);
			multicast.bind(srcAddress);
			multicast.setTimeToLive(4);
			System.out.println("Send multicast request.");
			// ----- Sending multi-cast packet ----- //
			multicast.send(discoveryPacket);
		} finally {
			System.out.println("Multicast ends. Close connection.");
			multicast.disconnect();
			multicast.close();
		}

		// -------------------------------------------------- //
		// Listening to response from the router. //
		// -------------------------------------------------- //
		DatagramSocket wildSocket = null;
		DatagramPacket receivePacket = null;
		try {
			wildSocket = new DatagramSocket(SSDP_SEARCH_PORT);
			wildSocket.setSoTimeout(TIMEOUT);
			// ----- Sending datagram packet ----- //
			System.out.println("Send datagram packet.");
			wildSocket.send(discoveryPacket);

			while (true) {
				try {
					System.out.println("Receive ssdp.");
					receivePacket = new DatagramPacket(new byte[1536], 1536);
					wildSocket.receive(receivePacket);
					final String message = new String(receivePacket.getData());
					System.out.println("Recieved messages:");
					System.out.println(message);

					new Thread(new Runnable() {
						@Override
						public void run() {
							addDevice(message);
						}
					}).start();

				} catch (SocketTimeoutException e) {
					System.err.print("Time out.");
					break;
				}
			}
		} finally {
			if (wildSocket != null) {
				wildSocket.disconnect();
				wildSocket.close();
			}
		}
	}

	Map<String, String> messageToHeaders(String message) {
		HashMap<String, String> map = new HashMap<String, String>();

		for (String pair : message.split("\n")) {
			try {
				String[] p = pair.split(":", 2);
				map.put(p[0].trim(), p[1].trim());
			} catch (Exception e) {
			}
		}

		return map;
	}

	public void addDevice(String message) {
		try {
			Map<String, String> map = messageToHeaders(message);

			WemoSwitch device = new WemoSwitch();
			device.name = message;
			device.headers = map;
			device.stateUpdateTime = new Date();
			
			URL url = new URL(map.get("LOCATION"));

			device.location = map.get("LOCATION").replaceAll("(http://.*)/.*",
					"$1");

			// resp.getContent();

			XmlMapper mapper = new XmlMapper();

			// HashMap<String,Object> xml = mapper.readValue( url.openStream() ,
			// new HashMap<String,Object>().getClass() );

			JsonNode xml = mapper.readTree(url.openStream());

			ObjectMapper jmapper = new ObjectMapper();
			jmapper.configure(SerializationFeature.INDENT_OUTPUT, true);
			// device.description =

			//System.out.println(jmapper.writeValueAsString(xml));
			
			if( !jmapper.writeValueAsString(xml).contains("belkin") ){
				return;
			}
			
			
			device.friendlyName = xml.get("device").get("friendlyName")
					.asText();
			device.name = WemoSwitch.normalizeName(device.friendlyName);
			try {
				//device.macAddress = xml.get("device").get("macAddress").asText();
				device.serialNumber = xml.get("device").get("serialNumber")
						.asText();
			} catch (Exception e) {
				log.severe("Hmm, something dont not match");
				return;
			}
			
			device.retrieveState();
			synchronized ( devices ) {

				if (!devices.contains(device)) {
					devices.add(device);
				}else{
					devices.remove(device);
					devices.add(device);
				}
			}
			clearOldDevices();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Remove devices there were not update in last 5 minutes
	 */
	private void clearOldDevices() {
		synchronized ( devices ) {
			Date oldestAllowed = new Date( new Date().getTime()-5*60*1000);
			ArrayList<WemoSwitch> toRemove = new ArrayList<WemoSwitch>();
			for( WemoSwitch device : devices ){
				if( device == null || device.stateUpdateTime.before( oldestAllowed ) ){
					toRemove.add(device);
				}
			}
			devices.removeAll(toRemove);
		}
	}

	public void handle(String arg0,
			org.eclipse.jetty.server.Request baseRequest, HttpServletRequest r,
			HttpServletResponse response) throws IOException, ServletException {
		response.setContentType("application/json;charset=utf-8");
		response.setStatus(HttpServletResponse.SC_OK);

		
		if (r.getPathInfo() == null || r.getPathInfo().equals("/")) {
			HashMap<String, Object> resp = new HashMap<String, Object>();

			resp.put("devices", devices);
			response.getWriter().println(
					new ObjectMapper().writeValueAsString(resp));
			baseRequest.setHandled(true);
		} else if (r.getPathInfo().equals("/discover")) {

			discovery();

			HashMap<String, Object> resp = new HashMap<String, Object>();

			resp.put("status", "OK");

			response.getWriter().println(
					new ObjectMapper().writeValueAsString(resp));
			baseRequest.setHandled(true);
		} else {
			String deviceName = r.getPathInfo().replaceAll("/(.*)/.*", "$1");
			String action = r.getPathInfo().replaceAll("/.*/(.*)", "$1");

			WemoSwitch d = getDevice(deviceName);

			HashMap<String, Object> resp = new HashMap<String, Object>();

			if (action.equals("state")) {
				// d.setOn( action.equals("on") );
				d.retrieveState();
				resp.put("state", d.isOn);
			} else if (action.equals("on") || action.equals("off")) {
				d.setOn(action.equals("on"));
				resp.put("status", "OK");
			} else if (action.equals("toggle")) {
				d.setOn(!d.isOn);
				resp.put("status", "OK");
				resp.put("state", d.isOn);

			} else {

			}

			response.getWriter().println(
					new ObjectMapper().writeValueAsString(resp));
			baseRequest.setHandled(true);
		}
	}

	private  WemoSwitch getDevice(String deviceName) {
		synchronized (devices) {
			for (WemoSwitch s : devices) {
				if (s.name.equals(WemoSwitch.normalizeName(deviceName))) {
					return s;
				}

				if (s.serialNumber.equals(deviceName)) {
					return s;
				}
			}
			return null;
		}
		
	}

}