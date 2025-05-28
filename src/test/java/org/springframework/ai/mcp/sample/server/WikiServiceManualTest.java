package org.springframework.ai.mcp.sample.server;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Manual test for Wiki service functionality.
 * This test demonstrates how to use the ONES Wiki MCP service.
 */
public class WikiServiceManualTest {

    public static void main(String[] args) {
        // Set system properties for testing
        System.setProperty("ones.host", "your-ones-host.com");
        System.setProperty("ones.email", "your-email@example.com");
        System.setProperty("ones.password", "your-password");
        System.setProperty("spring.main.web-application-type", "none");
        System.setProperty("spring.main.banner-mode", "off");
        System.setProperty("logging.pattern.console", "");

        // Start Spring application context
        ConfigurableApplicationContext context = SpringApplication.run(McpServerApplication.class, args);

        try {
            // Get Wiki service bean
            OnesWikiService wikiService = context.getBean(OnesWikiService.class);

            // Test URL - replace with your actual wiki URL
            String testUrl = "https://your-ones-host.com/wiki/#/team/TEAM_UUID/space/SPACE_UUID/page/PAGE_UUID";

            System.out.println("=== Starting Wiki Service Test ===");
            System.out.println("Test URL: " + testUrl);
            System.out.println("===================================");

            // Call service to get content
            String result = wikiService.getWikiContent(testUrl);

            System.out.println("=== Test Result ===");
            System.out.println(result);
            System.out.println("==================");

        } catch (Exception e) {
            System.err.println("Test failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            context.close();
        }
    }
}