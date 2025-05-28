package org.springframework.ai.mcp.sample.server;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Main application class for ONES Wiki MCP Server.
 * This Spring Boot application provides MCP (Model Context Protocol) server
 * functionality
 * for retrieving and processing ONES Wiki content.
 */
@SpringBootApplication
public class McpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpServerApplication.class, args);
    }

    /**
     * Configures the ONES Wiki tools for MCP server.
     * 
     * @param onesWikiService the service for handling ONES Wiki operations
     * @return ToolCallbackProvider configured with ONES Wiki tools
     */
    @Bean
    public ToolCallbackProvider onesWikiTools(OnesWikiService onesWikiService) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(onesWikiService)
                .build();
    }
}
