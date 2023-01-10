package com.example.reactiveiphostnames;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.*;

@Component
@Slf4j
public class CidrCrawler {

//	private static Mono<Map<String,String>> responseMap = Mono.empty();
	private final ProcessExecutor processExecutor;

	private final HostCrawler hostCrawler;

	public CidrCrawler(ProcessExecutor processExecutor, HostCrawler hostCrawler) {
		this.processExecutor = processExecutor;
		this.hostCrawler = hostCrawler;
	}

	public Mono<Map<String, String>> crawl(String cidr) throws IOException {
		String command = "echo " + cidr + " | dnsx -silent -resp -ptr";
		Mono<Map<String,String>> responseMap = processExecutor.execute(command)
						.map(output -> parseOutput(output.getStdout()))
						.onErrorResume(error -> Mono.just(Collections.emptyMap()));

		return responseMap.flatMap(map -> {
			if (map.isEmpty()) {
				log.error("{} - Not Found", cidr);
				return Mono.just(Map.of(cidr,"Not_Found"));
			}
			Mono<Map<String,String>> missingResponses = getMissingAddresses(cidr,responseMap);
			return responseMap.zipWith(missingResponses,(map1,map2)->{
//				map1.forEach(map2::putIfAbsent);
				map1.putAll(map2);
				log.warn("Map Size returning from DNS: {}",map1.size());
				return map1;
			});
		});
	}
	public Map<String, String> parseOutput(String output) {
		Map<String, String> map = new HashMap<>();
		String[] lines = output.split("\\r?\\n");
		if (lines.length > 0) {
			for (String line : lines) {
				String[] parts = line.split("\\s+");
				map.put(parts[0], parts[1]);
			}
		}else{
			return new HashMap<>();
		}
		return map;
	}


	public Mono<Map<String, String>> getMissingAddresses(String cidr, Mono<Map<String, String>> map) {
		return map.flatMap(m -> {
			log.info("Getting Missing Addresses");
			List<String> missingAddresses = new ArrayList<>();
			String[] cidrParts = cidr.split("/");
			String baseIp = cidrParts[0];
			int mask = Integer.parseInt(cidrParts[1]);
			String[] baseIpParts = baseIp.split("\\.");
			long base = (Long.parseLong(baseIpParts[0]) << 24)
							+ (Long.parseLong(baseIpParts[1]) << 16)
							+ (Long.parseLong(baseIpParts[2]) << 8)
							+ Long.parseLong(baseIpParts[3]);
			long end = base + (1 << (32 - mask)) - 1;
			long threshold = end - base + 1;
			for (long i = base; i <= end; i++) {
				String ipAddress = String.format("%d.%d.%d.%d", i >> 24, (i >> 16) & 0xff, (i >> 8) & 0xff, i & 0xff);
				if (!m.containsKey(ipAddress)) {
					missingAddresses.add(ipAddress);
				}
			}
			log.warn("Missing Addresses: {}", missingAddresses.size());

//			if (missingAddresses.size() < threshold) {
//				return hostCrawler.crawl(missingAddresses)
//								.map(crawledMap -> {
//									m.putAll(crawledMap);
//									return m;
//								});
//			} else {
//				log.info("{} crawled : Not_Found", cidr);
//				return Mono.just(m);
//			}
			return hostCrawler.crawl(missingAddresses)
					.map(crawledMap -> {
						m.putAll(crawledMap);
						return m;
					});
		});
	}
}
