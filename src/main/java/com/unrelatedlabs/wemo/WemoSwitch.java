package com.unrelatedlabs.wemo;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.util.Date;
import java.util.Map;

import org.apache.commons.io.IOUtils;

public class WemoSwitch {
	public String location;
	public Map<String, String> headers;
	public String friendlyName;
	public String name;

	public InetAddress address;
	public boolean isOn;
	public Date stateUpdateTime = new Date();
	public String serialNumber;

	public void retrieveState() {

		try {
			String resp = call(
					"/upnp/control/basicevent1",
					"urn:Belkin:service:basicevent:1#GetBinaryState",
					IOUtils.toString(getClass().getResourceAsStream(
							"GetRequest.xml")));

			String state = resp.replaceAll(
					"[\\d\\D]*<BinaryState>(.*)</BinaryState>[\\d\\D]*", "$1");

			isOn = state.equals("1") ? true : false;
			stateUpdateTime = new Date();

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public void setOn(boolean onOff) throws IOException {
		WemoBridge.log.info("setOn " + onOff);
		String resp = call(
				"/upnp/control/basicevent1",
				"urn:Belkin:service:basicevent:1#SetBinaryState",
				IOUtils.toString(
						getClass().getResourceAsStream("SetRequest.xml"))
						.replace("{{state}}", onOff ? "1" : "0"));

		WemoBridge.log.info("setOn " + resp);
		isOn = onOff;
	}

	private String call(String endpoint, String soapCall, String content) {
		try {
			// String urlParameters = "param1=a&param2=b&param3=c";
			// String request = "http://example.com/index.php";
			URL url = new URL(this.location + endpoint);

			Socket s = new Socket(InetAddress.getByName(url.getHost()),
					url.getPort());
			try {
				OutputStream os = s.getOutputStream();
				StringBuffer sb = new StringBuffer();

				sb.append("POST " + url + " HTTP/1.1\r\n");
				sb.append("Content-Type: text/xml; charset=utf-8\r\n");
				sb.append("Content-Length: " + content.getBytes().length
						+ "\r\n");

				sb.append("SOAPACTION: \"" + soapCall + "\"\r\n");
				sb.append("\r\n");

				os.write(sb.toString().getBytes());
				os.write(content.getBytes());

				os.flush();

				String resp = IOUtils.toString(s.getInputStream());
				return resp;

			} finally {
				s.close();
			}
		} catch (Exception e) {
			throw new RuntimeException("Can't call device", e);
		}
	}

	public static String normalizeName(String deviceName) {
		return deviceName.toLowerCase().replace(" ", "_");
	}

	@Override
	public int hashCode() {
		return serialNumber.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof WemoSwitch) {
		} else {
			return false;
		}
		return this.serialNumber.equals(((WemoSwitch) obj).serialNumber);
	}

}