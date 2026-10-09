package com.mb.callassistant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * CRM Call Assistant backend.
 *
 * <p>Core slice: open a call record, download the per-call recording from the
 * FreeSWITCH/EC2 server, and run Vietnamese speech-to-text with RM/KH speaker
 * separation. The call itself is placed from the frontend via MicroSIP; this
 * service never spawns MicroSIP.
 */
@SpringBootApplication
@EnableAsync
public class CallAssistantApplication {

    public static void main(String[] args) {
        SpringApplication.run(CallAssistantApplication.class, args);
    }
}
