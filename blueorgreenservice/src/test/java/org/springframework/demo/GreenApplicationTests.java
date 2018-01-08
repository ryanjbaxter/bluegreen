package org.springframework.demo;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;

/**
 * @author Ryan Baxter
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "color:green")
public class GreenApplicationTests {
	@Autowired
	private TestRestTemplate rest;

	@Test
	public void contextLoads() {
		BlueOrGreenApplication.Color color = rest.getForObject("/", BlueOrGreenApplication.Color.class);
		assertEquals("green", color.getId());
	}
}
