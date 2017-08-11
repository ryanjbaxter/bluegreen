package org.springframework.demo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@EnableDiscoveryClient
@RestController
public class BlueOrGreenApplication {

	public static void main(String[] args) {
		SpringApplication.run(BlueOrGreenApplication.class, args);
	}

	@Value(value="${color:green}")
	private String color;

	@RequestMapping
	public Color color() {
		if(Color.BLUE.getId().equalsIgnoreCase(color)) {
			return Color.BLUE;
		}
		return Color.GREEN;
	}

	static class Color {
		public static final Color GREEN = new Color("green");
		public static final Color BLUE = new Color("blue");
		private String id;

		public Color(){}

		public Color(String id) { this.id = id; }

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}
	}
}
