package com.example.reactiveiphostnames;

import org.springframework.stereotype.Component;

@Component
public class IpValidator {

	public boolean validate(String startIp, String endIp) {
		if (!isValidIpFormat(startIp)) {
			throw new IllegalArgumentException("Invalid start IP format: " + startIp);
		}
		if (!isValidIpFormat(endIp)) {
			throw new IllegalArgumentException("Invalid end IP format: " + endIp);
		}
		if (startIp.compareTo(endIp) > 0) {
			throw new IllegalArgumentException("Start IP must be less than or equal to end IP");
		}
		return true;
	}

	private boolean isValidIpFormat(String ip) {
		String[] parts = ip.split("\\.");
		if (parts.length != 4) {
			return false;
		}
		for (String part : parts) {
			try {
				int value = Integer.parseInt(part);
				if (value < 0 || value > 255) {
					return false;
				}
			} catch (NumberFormatException e) {
				return false;
			}
		}
		return true;
	}
}
