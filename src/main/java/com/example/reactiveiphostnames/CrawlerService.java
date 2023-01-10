package com.example.reactiveiphostnames;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

@Service
@Slf4j
public class CrawlerService {

	private final IpValidator ipValidator;
	private final CidrConverter cidrConverter;
	private final HostCrawlerForNulls hostCrawlerForNulls;
	private final CidrCrawler cidrCrawler;
	private final HostCrawler hostCrawler;

	public CrawlerService(IpValidator ipValidator, CidrConverter cidrConverter, HostCrawlerForNulls hostCrawlerForNulls, CidrCrawler cidrCrawler, HostCrawler hostCrawler) {
		this.ipValidator = ipValidator;
		this.cidrConverter = cidrConverter;
		this.hostCrawlerForNulls = hostCrawlerForNulls;
		this.cidrCrawler = cidrCrawler;
		this.hostCrawler = hostCrawler;
	}

	public Mono<Map<String, String>> crawl(String startIp, String endIp) {
		try {
//			if (!ipValidator.validate(startIp, endIp)) {
//				return Mono.error(new IllegalArgumentException("Invalid start or end IP"));
//			}
			/* 			String cidr = cidrConverter.toCidr(startIp, endIp);
			return cidrCrawler.crawl(cidr)
							.onErrorResume(error -> {
								if (error instanceof IOException) {
									return Mono.error(new RuntimeException("Error executing 'host' command", error));
								}
								return Mono.error(error);
							})
							.doOnSuccessOrError((result, error) -> {
								if (error == null && result == null) {
									log.info("{} Cidr host not found", cidr);
								}
							}); */

			String cidr = cidrConverter.toCidr(startIp, endIp);
			return cidrCrawler.crawl(cidr)
//							.switchIfEmpty(Mono.just(Map.of(startIp+"-"+endIp,"Not_Found")) //Mono.just(Map.of(startIp+"-"+endIp,"Not_Found")
					.onErrorResume(error -> {
						if (error instanceof IOException) {
							return Mono.error(new RuntimeException("Error executing 'host' command", error));
						}
						return Mono.error(error);
					});
		} catch (NumberFormatException e) {
			return Mono.error(new IllegalArgumentException("Invalid start or end IP", e));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}

