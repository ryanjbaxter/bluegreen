package org.springframework.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.cloud.netflix.zuul.EnableZuulServer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableDiscoveryClient
@EnableZuulProxy
@RestController
public class BlueOrGreenFrontendApplication {

	@Autowired
	RestTemplate rest;

	public static void main(String[] args) {
		SpringApplication.run(BlueOrGreenFrontendApplication.class, args);
	}

	@RequestMapping("/color")
	public String color() {
		return rest.getForObject("http://blueorgreen", String.class);
	}

	@Configuration
	protected static class RestTemplateConfig {

		@LoadBalanced
		@Bean
		public RestTemplate rest() {
			return new RestTemplateBuilder().build();
		}
	}
}
