package com.example.reactiveiphostnames;

import org.springframework.stereotype.Component;

@Component
public class CidrConverter {

	public String toCidr(String startIp, String endIp) {
		String[] startIpParts = startIp.split("\\.");
		String[] endIpParts = endIp.split("\\.");

		int prefix = 0;
		for (int i = 0; i < 4; i++) {
			int start = Integer.parseInt(startIpParts[i]);
			int end = Integer.parseInt(endIpParts[i]);
			if (start == end) {
				prefix += 8;
			} else {
				int x = start ^ end;
				int mask = 0x80;
				while (mask > 0) {
					if ((x & mask) == 0) {
						prefix += 1;
					} else {
						break;
					}
					mask >>= 1;
				}
				break;
			}
		}
		return startIp + "/" + prefix;
	}
}
