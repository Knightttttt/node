package com.example.reactiveiphostnames;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.AbstractMap;
import java.util.Map;

import static java.util.Objects.isNull;

@Component
@Slf4j
public class HostCrawlerForNulls {

	private final ProcessExecutor processExecutor;

  public HostCrawlerForNulls(ProcessExecutor processExecutor) {
    this.processExecutor = processExecutor;
  }


  public Mono<Map<String, String>> crawl(String startIp, String endIp) {
		log.info("Crawling hosts from {} to {}", startIp, endIp);
		String[] startIpParts = startIp.split("\\.");
		String[] endIpParts = endIp.split("\\.");
		long start = (Long.parseLong(startIpParts[0]) << 24)
						+ (Long.parseLong(startIpParts[1]) << 16)
						+ (Long.parseLong(startIpParts[2]) << 8)
						+ Long.parseLong(startIpParts[3]);
		long end = (Long.parseLong(endIpParts[0]) << 24)
						+ (Long.parseLong(endIpParts[1]) << 16)
						+ (Long.parseLong(endIpParts[2]) << 8)
						+ Long.parseLong(endIpParts[3]);
		return Flux.range((int) start, (int) (end - start + 1))
						.parallel()
						.runOn(Schedulers.boundedElastic())
						.flatMap(ip -> {
							String ipAddress = String.format("%d.%d.%d.%d", (ip >> 24) & 0xff, (ip >> 16) & 0xff, (ip >> 8) & 0xff, ip & 0xff);
							String ip4 = ipAddress.split("\\.")[3];
							String ip3 = ipAddress.split("\\.")[2];
							String ip2 = ipAddress.split("\\.")[1];
							String ip1 = ipAddress.split("\\.")[0];
							try {
								// Perform a DNS lookup for the IP address
								Lookup lookup = new Lookup(ip4+"."+ip3+"."+ip2+"."+ip1+".in-addr.arpa", Type.PTR);
								log.info("Looking up For Nulls {}", ipAddress);
								// You can set the resolver to use by calling lookup.setResolver(Resolver res)
								// If you don't set a resolver, the default resolver will be used
								Record[] records = lookup.run();
								if (lookup.getResult() == Lookup.SUCCESSFUL) {
									String hostname = !isNull(records[0].toString().split("PTR")[1].substring(2))
													? "["+ records[0].toString().split("PTR")[1].substring(1) + "]"
											: "No Hostname";
									log.info("Found host {} for IP {}", hostname, ipAddress);
									return Mono.just(new AbstractMap.SimpleEntry<>(ipAddress, hostname));
								}
							} catch (TextParseException e) {
								log.error("Error parsing IP address: {}", ipAddress,e);
							}
							return Mono.empty();
						})
						.sequential()
						.collectMap(Map.Entry::getKey, Map.Entry::getValue)
						.map(map -> {
							log.info("Crawling complete");
							// Add additional processing here if needed
							return map;
						});
	}

	private Map.Entry<String, String> parseOutput(String ipAddress, String output) {
		String hostname = "not_found";
		String[] lines = output.split("\\r?\\n");
		for (String line : lines) {
			hostname = line.substring(line.indexOf("pointer") + 8).trim();
			break;
		}
		return new AbstractMap.SimpleImmutableEntry<>(ipAddress, hostname);
	}
}
