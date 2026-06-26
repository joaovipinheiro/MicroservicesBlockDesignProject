package br.com.arenamanager.notification_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "de.flapdoodle.mongodb.embedded.version=7.0.0",
        "spring.cloud.discovery.enabled=false",
        "management.tracing.enabled=false"
})
class NotificationServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
