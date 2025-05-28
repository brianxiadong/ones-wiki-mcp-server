package org.springframework.ai.mcp.sample.server;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for OnesWikiService.
 * These tests verify the core functionality of the ONES Wiki MCP service.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "ones.host=test.example.com",
        "ones.email=test@example.com",
        "ones.password=testpassword"
})
class OnesWikiServiceTest {

    private OnesWikiService wikiService;

    @BeforeEach
    void setUp() {
        wikiService = new OnesWikiService();
        // Set test properties using reflection
        ReflectionTestUtils.setField(wikiService, "host", "test.example.com");
        ReflectionTestUtils.setField(wikiService, "email", "test@example.com");
        ReflectionTestUtils.setField(wikiService, "password", "testpassword");
    }

    @Test
    @DisplayName("Should convert wiki URL to API URL correctly")
    void testUrlConversion() {
        String wikiUrl = "https://test.example.com/wiki/#/team/TEAM123/space/SPACE456/page/PAGE789";
        String expectedApiUrl = "https://test.example.com/wiki/api/wiki/team/TEAM123/online_page/PAGE789/content";

        // Use reflection to test private method
        String actualApiUrl = (String) ReflectionTestUtils.invokeMethod(
                wikiService, "convertWikiUrlToApiUrl", wikiUrl);

        assertEquals(expectedApiUrl, actualApiUrl, "URL conversion should match expected format");
    }

    @Test
    @DisplayName("Should throw exception for invalid wiki URL format")
    void testInvalidUrlFormat() {
        String invalidUrl = "https://invalid-url.com/not-a-wiki-url";

        assertThrows(IllegalArgumentException.class, () -> {
            ReflectionTestUtils.invokeMethod(wikiService, "convertWikiUrlToApiUrl", invalidUrl);
        }, "Should throw IllegalArgumentException for invalid URL format");
    }

    @Test
    @DisplayName("Should handle empty content gracefully")
    void testEmptyContentProcessing() {
        String result = (String) ReflectionTestUtils.invokeMethod(
                wikiService, "processHtmlContent", "");

        assertEquals("Content is empty", result, "Should return appropriate message for empty content");
    }

    @Test
    @DisplayName("Should handle null content gracefully")
    void testNullContentProcessing() {
        String result = (String) ReflectionTestUtils.invokeMethod(
                wikiService, "processHtmlContent", (String) null);

        assertEquals("Content is empty", result, "Should return appropriate message for null content");
    }

    @Test
    @DisplayName("Should process simple HTML content")
    void testSimpleHtmlProcessing() {
        String htmlContent = "<p>This is a test paragraph.</p>";
        String result = (String) ReflectionTestUtils.invokeMethod(
                wikiService, "processHtmlContent", htmlContent);

        assertNotNull(result, "Result should not be null");
        assertTrue(result.contains("test paragraph"), "Result should contain the paragraph text");
    }

    @Test
    @DisplayName("Should extract text from JSON text array")
    void testTextArrayExtraction() {
        // This would require creating a proper JsonNode for testing
        // For now, we'll just verify the method exists and handles null gracefully
        assertDoesNotThrow(() -> {
            // Test that the method exists and handles null input gracefully
            String result = (String) ReflectionTestUtils.invokeMethod(wikiService, "extractTextFromTextArray",
                    (Object) null);
            // The method should return empty string for null input
            assertEquals("", result, "Should return empty string for null input");
        }, "Method should handle null input gracefully");
    }
}