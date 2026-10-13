package com.oguzhan.flash_ticket_engine;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class FlashTicketEngineApplicationTests {

	@Test
	void contextLoads() {
	}

}
