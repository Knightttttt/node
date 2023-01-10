package com.example.reactiveiphostnames;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@Slf4j
public class CrawlController {
	private final CrawlerService crawlerService;

	public CrawlController(CrawlerService crawlerService) {
		this.crawlerService = crawlerService;
	}
	@GetMapping("/crawl")
	public Mono<Map<String, String>> crawlIps(@RequestParam String startIp, @RequestParam String endIp) {
		log.info("Crawling IPs from {} to {}", startIp, endIp);
		return crawlerService.crawl(startIp, endIp);
	}
}
