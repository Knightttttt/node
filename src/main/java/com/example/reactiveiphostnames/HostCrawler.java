package com.example.reactiveiphostnames;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;
import org.xbill.DNS.tools.lookup;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Objects.isNull;

@Component
@Slf4j
public class HostCrawler {

	private final ProcessExecutor processExecutor;

	public HostCrawler(ProcessExecutor processExecutor) {
		this.processExecutor = processExecutor;
	}

	public Mono<Map<String, String>> crawl(List<String> missingIpAddresses) {
		return Flux.fromIterable(missingIpAddresses)
						.parallel()
						.runOn(Schedulers.boundedElastic())
						.flatMap(ipAddress -> {
							String ip4 = ipAddress.split("\\.")[3];
							String ip3 = ipAddress.split("\\.")[2];
							String ip2 = ipAddress.split("\\.")[1];
							String ip1 = ipAddress.split("\\.")[0];
							try {
								log.info("Looking up host for IP: {}", ipAddress);
								// Perform a DNS lookup for the IP address
								Lookup lookup = new Lookup(ip4 + "." + ip3 + "." + ip2 + "." + ip1 + ".in-addr.arpa", Type.PTR);
								// You can set the resolver to use by calling lookup.setResolver(Resolver res)
								// If you don't set a resolver, the default resolver will be used
								Record[] records = lookup.run();
								if (lookup.getResult() == Lookup.SUCCESSFUL) {
									String hostname = !isNull(records[0].toString().split("PTR")[1].substring(2))
													? "[" + records[0].toString().split("PTR")[1].substring(1) + "]"
													: "No Hostname";
									log.info("Found host {} for IP {}", hostname, ipAddress);
									return Mono.just(new AbstractMap.SimpleEntry<>(ipAddress, hostname));
								}
							} catch (TextParseException e) {
								log.error("Error parsing IP address: {}", ipAddress, e);
							}
							// If hostname was not found, return a Mono with a SimpleEntry mapping the IP address to "Not_Found"
							return Mono.just(new AbstractMap.SimpleEntry<>(ipAddress, "Not_Found"));
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
