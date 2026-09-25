package com.example.notes.bdd;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;

import io.cucumber.spring.CucumberContextConfiguration;

/**
 * Boots the full reactive stack once per test run (Spring's test context cache reuses it
 * across scenarios) and wires a {@link org.springframework.test.web.reactive.server.WebTestClient}
 * for the Cucumber step definitions, exactly like {@code NoteApiIntegrationTest}.
 */
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
public class CucumberSpringConfiguration {
}
