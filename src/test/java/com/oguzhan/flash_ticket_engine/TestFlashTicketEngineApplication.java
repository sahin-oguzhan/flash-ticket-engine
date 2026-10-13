package com.oguzhan.flash_ticket_engine;

import org.springframework.boot.SpringApplication;

public class TestFlashTicketEngineApplication {

	public static void main(String[] args) {
		SpringApplication.from(FlashTicketEngineApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
