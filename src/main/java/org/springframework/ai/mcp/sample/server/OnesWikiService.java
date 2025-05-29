/*
 * Copyright 2024 - 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.ai.mcp.sample.server;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Service for retrieving and processing ONES Wiki content.
 * This service handles authentication, content retrieval, and format conversion
 * to make wiki content more suitable for AI model consumption.
 */
@Service
public class OnesWikiService {

    @Value("${ones.host}")
    private String host;

    @Value("${ones.email}")
    private String email;

    @Value("${ones.password}")
    private String password;

    private String token;
    private String userUuid;
    private final RestClient restClient;

    public OnesWikiService() {
        this.restClient = RestClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, "application/json, text/plain, */*")
                .defaultHeader("accept-language", "en")
                .defaultHeader("sec-ch-ua",
                        "\"Google Chrome\";v=\"135\", \"Not-A.Brand\";v=\"8\", \"Chromium\";v=\"135\"")
                .defaultHeader("sec-ch-ua-mobile", "?0")
                .defaultHeader("sec-ch-ua-platform", "\"macOS\"")
                .defaultHeader("sec-fetch-dest", "empty")
                .defaultHeader("sec-fetch-mode", "cors")
                .defaultHeader("sec-fetch-site", "same-origin")
                .defaultHeader("user-agent",
                        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36")
                .build();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LoginRequest(@JsonProperty("email") String email, @JsonProperty("password") String password) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LoginResponse(@JsonProperty("user") User user) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record User(@JsonProperty("uuid") String uuid, @JsonProperty("token") String token) {
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record WikiContentResponse(@JsonProperty("content") String content) {
    }

    /**
     * Authenticates with ONES system to obtain access token.
     * 
     * @return true if login successful, false otherwise
     */
    private boolean login() {
        try {
            String loginUrl = String.format("https://%s/project/api/project/auth/login", host);
            LoginRequest loginRequest = new LoginRequest(email, password);

            LoginResponse response = restClient.post()
                    .uri(loginUrl)
                    .body(loginRequest)
                    .retrieve()
                    .body(LoginResponse.class);

            if (response != null && response.user() != null) {
                this.token = response.user().token();
                this.userUuid = response.user().uuid();
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Converts wiki URL to API endpoint URL.
     * 
     * @param wikiUrl the wiki page URL
     * @return API endpoint URL for content retrieval
     * @throws IllegalArgumentException if URL format is invalid
     */
    private String convertWikiUrlToApiUrl(String wikiUrl) {
        // Pattern to match wiki URL format
        // https://example.com/wiki/#/team/AQzvsooq/space/EYvdiwVh/page/4RwySM6h
        Pattern pattern = Pattern.compile("https://([^/]+)/wiki/#/team/([^/]+)/space/([^/]+)/page/([^/]+)");
        Matcher matcher = pattern.matcher(wikiUrl);

        if (matcher.find()) {
            String host = matcher.group(1);
            String teamUuid = matcher.group(2);
            String pageUuid = matcher.group(4);

            // Convert to API endpoint
            return String.format("https://%s/wiki/api/wiki/team/%s/online_page/%s/content",
                    host, teamUuid, pageUuid);
        }

        throw new IllegalArgumentException("Invalid wiki URL format");
    }

    /**
     * Converts wiki URL to alternative API endpoint URL format.
     * 
     * @param wikiUrl the wiki page URL
     * @return Alternative API endpoint URL for content retrieval
     * @throws IllegalArgumentException if URL format is invalid
     */
    private String convertWikiUrlToAlternativeApiUrl(String wikiUrl) {
        // Pattern to match wiki URL format
        // https://example.com/wiki/#/team/AQzvsooq/space/EYvdiwVh/page/4RwySM6h
        Pattern pattern = Pattern.compile("https://([^/]+)/wiki/#/team/([^/]+)/space/([^/]+)/page/([^/]+)");
        Matcher matcher = pattern.matcher(wikiUrl);

        if (matcher.find()) {
            String host = matcher.group(1);
            String teamUuid = matcher.group(2);
            String pageUuid = matcher.group(4);

            // Convert to alternative API endpoint format
            return String.format("https://%s/wiki/api/wiki/team/%s/page/%s",
                    host, teamUuid, pageUuid);
        }

        throw new IllegalArgumentException("Invalid wiki URL format");
    }

    /**
     * Processes wiki content and converts it to AI-friendly text format.
     * Supports both HTML and ONES Wiki JSON blocks format.
     * 
     * @param content raw content from wiki API
     * @return processed content in readable text format
     */
    private String processHtmlContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return "Content is empty";
        }

        try {
            // Try to parse as JSON format (ONES Wiki blocks format)
            if (content.trim().startsWith("{")) {
                return processWikiJsonBlocks(content);
            } else {
                // If not JSON, process as HTML
                return processHtmlContent_Legacy(content);
            }

        } catch (Exception e) {
            // If JSON parsing fails, try to process as HTML
            return processHtmlContent_Legacy(content);
        }
    }

    /**
     * Processes ONES Wiki JSON blocks format.
     * 
     * @param jsonContent JSON content from wiki API
     * @return processed text content
     */
    private String processWikiJsonBlocks(String jsonContent) {
        try {
            // Parse JSON using Jackson
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode rootNode = mapper.readTree(jsonContent);

            StringBuilder result = new StringBuilder();

            // Get blocks array
            com.fasterxml.jackson.databind.JsonNode blocksNode = rootNode.get("blocks");
            if (blocksNode != null && blocksNode.isArray()) {
                for (com.fasterxml.jackson.databind.JsonNode block : blocksNode) {
                    String blockContent = processWikiBlock(block, rootNode);
                    if (!blockContent.isEmpty()) {
                        result.append(blockContent).append("\n");
                    }
                }
            }

            String finalResult = result.toString().trim();

            return finalResult.isEmpty() ? "No valid content extracted" : finalResult;

        } catch (Exception e) {
            return "JSON content parsing failed: " + e.getMessage();
        }
    }

    /**
     * Processes a single wiki block.
     * 
     * @param block    the JSON block to process
     * @param rootNode the root JSON node for context
     * @return processed block content
     */
    private String processWikiBlock(com.fasterxml.jackson.databind.JsonNode block,
            com.fasterxml.jackson.databind.JsonNode rootNode) {
        try {
            String type = block.has("type") ? block.get("type").asText() : "";
            StringBuilder blockResult = new StringBuilder();

            switch (type) {
                case "text":
                    blockResult.append(processTextBlock(block));
                    break;
                case "list":
                    blockResult.append(processListBlock(block));
                    break;
                case "table":
                    blockResult.append(processTableBlock(block, rootNode));
                    break;
                case "embed":
                    blockResult.append(processEmbedBlock(block));
                    break;
                case "code":
                    blockResult.append(processCodeBlock(block, rootNode));
                    break;
                default:
                    // For unknown types, try to extract text content
                    if (block.has("text")) {
                        blockResult.append(extractTextFromTextArray(block.get("text")));
                    }
                    break;
            }

            return blockResult.toString();

        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Processes text type block
     */
    private String processTextBlock(com.fasterxml.jackson.databind.JsonNode block) {
        StringBuilder result = new StringBuilder();

        // Check if it's a heading
        if (block.has("heading")) {
            int level = block.get("heading").asInt();
            String prefix = "#".repeat(level) + " ";
            result.append(prefix);
        }

        // Extract text content
        if (block.has("text")) {
            String text = extractTextFromTextArray(block.get("text"));
            result.append(text);
        }

        result.append("\n");
        return result.toString();
    }

    /**
     * Processes list type block
     */
    private String processListBlock(com.fasterxml.jackson.databind.JsonNode block) {
        StringBuilder result = new StringBuilder();

        boolean isOrdered = block.has("ordered") && block.get("ordered").asBoolean();
        int level = block.has("level") ? block.get("level").asInt() : 1;
        String indent = "  ".repeat(Math.max(0, level - 1));

        if (block.has("text")) {
            String text = extractTextFromTextArray(block.get("text"));
            if (!text.trim().isEmpty()) {
                String prefix = isOrdered ? "1. " : "- ";
                result.append(indent).append(prefix).append(text).append("\n");
            }
        }

        return result.toString();
    }

    /**
     * Processes table type block
     */
    private String processTableBlock(com.fasterxml.jackson.databind.JsonNode block,
            com.fasterxml.jackson.databind.JsonNode rootNode) {
        StringBuilder result = new StringBuilder();
        result.append("\n### Table\n\n");

        if (block.has("children")) {
            com.fasterxml.jackson.databind.JsonNode children = block.get("children");
            int cols = block.has("cols") ? block.get("cols").asInt() : 2;

            // Process table rows
            for (int i = 0; i < children.size(); i++) {
                String cellId = children.get(i).asText();
                com.fasterxml.jackson.databind.JsonNode cellNode = rootNode.get(cellId);

                if (cellNode != null && cellNode.isArray()) {
                    for (com.fasterxml.jackson.databind.JsonNode cellContent : cellNode) {
                        if (cellContent.has("text")) {
                            String cellText = extractTextFromTextArray(cellContent.get("text"));
                            if (!cellText.trim().isEmpty()) {
                                result.append("| ").append(cellText).append(" ");
                            }
                        }
                    }

                    // End of row
                    if ((i + 1) % cols == 0) {
                        result.append("|\n");
                    }
                }
            }
        }

        result.append("\n");
        return result.toString();
    }

    /**
     * Processes embed type block (e.g., image)
     */
    private String processEmbedBlock(com.fasterxml.jackson.databind.JsonNode block) {
        StringBuilder result = new StringBuilder();

        if (block.has("embedType")) {
            String embedType = block.get("embedType").asText();

            if ("image".equals(embedType) && block.has("embedData")) {
                com.fasterxml.jackson.databind.JsonNode embedData = block.get("embedData");
                String src = embedData.has("src") ? embedData.get("src").asText() : "Unknown image";
                result.append("\n[Image: ").append(src).append("]\n");
            }
        }

        return result.toString();
    }

    /**
     * Processes code type block
     */
    private String processCodeBlock(com.fasterxml.jackson.databind.JsonNode block,
            com.fasterxml.jackson.databind.JsonNode rootNode) {
        StringBuilder result = new StringBuilder();
        result.append("\n```");

        if (block.has("language")) {
            result.append(block.get("language").asText());
        }
        result.append("\n");

        if (block.has("children")) {
            com.fasterxml.jackson.databind.JsonNode children = block.get("children");
            for (com.fasterxml.jackson.databind.JsonNode child : children) {
                String childId = child.asText();
                com.fasterxml.jackson.databind.JsonNode childNode = rootNode.get(childId);

                if (childNode != null && childNode.has("text")) {
                    String text = extractTextFromTextArray(childNode.get("text"));
                    result.append(text).append("\n");
                }
            }
        }

        result.append("```\n");
        return result.toString();
    }

    /**
     * Extracts pure text content from text array
     */
    private String extractTextFromTextArray(com.fasterxml.jackson.databind.JsonNode textArray) {
        if (textArray == null) {
            return "";
        }

        StringBuilder result = new StringBuilder();

        if (textArray.isArray()) {
            for (com.fasterxml.jackson.databind.JsonNode textNode : textArray) {
                if (textNode.has("insert")) {
                    String text = textNode.get("insert").asText();

                    // Handle special newline markers
                    if (textNode.has("attributes")) {
                        com.fasterxml.jackson.databind.JsonNode attrs = textNode.get("attributes");
                        if (attrs.has("type") && "br".equals(attrs.get("type").asText())) {
                            result.append("\n");
                            continue;
                        }
                    }

                    result.append(text);
                }
            }
        }

        return result.toString();
    }

    /**
     * Original HTML processing method (kept as a fallback)
     */
    private String processHtmlContent_Legacy(String htmlContent) {
        if (htmlContent == null || htmlContent.trim().isEmpty()) {
            return "Content is empty";
        }

        try {
            Document doc = Jsoup.parse(htmlContent);

            // Remove content marked with delete line
            doc.select("s, strike, del").remove();

            StringBuilder result = new StringBuilder();

            // Process all text content, in DOM order
            Elements allElements = doc.select("*");
            for (Element element : allElements) {
                String tagName = element.tagName().toLowerCase();

                // Process heading
                if (tagName.matches("h[1-6]")) {
                    String level = "#".repeat(Integer.parseInt(tagName.substring(1)));
                    String text = element.ownText().trim();
                    if (!text.isEmpty()) {
                        result.append(level).append(" ").append(text).append("\n\n");
                    }
                }
                // Process paragraph
                else if (tagName.equals("p")) {
                    String text = element.ownText().trim();
                    if (!text.isEmpty()) {
                        result.append(text).append("\n\n");
                    }
                }
                // Process list item
                else if (tagName.equals("li")) {
                    String text = element.ownText().trim();
                    if (!text.isEmpty()) {
                        Element parent = element.parent();
                        if (parent != null && parent.tagName().equals("ol")) {
                            result.append("- ").append(text).append("\n");
                        } else {
                            result.append("• ").append(text).append("\n");
                        }
                    }
                }
                // Process table cell
                else if (tagName.equals("td") || tagName.equals("th")) {
                    String text = element.ownText().trim();
                    if (!text.isEmpty()) {
                        result.append("**").append(text).append("** ");
                    }
                }
                // Process other elements containing text
                else if (tagName.equals("div") || tagName.equals("span") || tagName.equals("strong")
                        || tagName.equals("b")) {
                    String text = element.ownText().trim();
                    if (!text.isEmpty() && text.length() > 2) { // Filter out too short text
                        result.append(text).append(" ");
                    }
                }
            }

            // If the above method didn't extract content, try to get all text directly
            if (result.length() == 0) {
                String allText = doc.text().trim();
                if (!allText.isEmpty()) {
                    result.append("Page content:\n").append(allText);
                }
            }

            // Process table (handled separately to maintain structure)
            Elements tables = doc.select("table");
            if (!tables.isEmpty()) {
                result.append("\n\n=== Table data ===\n");
                for (int tableIndex = 0; tableIndex < tables.size(); tableIndex++) {
                    Element table = tables.get(tableIndex);
                    result.append("\n## Table ").append(tableIndex + 1).append("\n");

                    Elements rows = table.select("tr");
                    for (Element row : rows) {
                        Elements cells = row.select("th, td");
                        if (cells.size() >= 2) {
                            String key = cells.get(0).text().trim();
                            String value = cells.get(1).text().trim();
                            if (!key.isEmpty() && !value.isEmpty()) {
                                result.append("**").append(key).append("**: ").append(value).append("\n");
                            }
                        } else if (cells.size() == 1) {
                            String cellText = cells.get(0).text().trim();
                            if (!cellText.isEmpty()) {
                                result.append("- ").append(cellText).append("\n");
                            }
                        }
                    }
                }
            }

            // Process images
            Elements images = doc.select("img");
            if (!images.isEmpty()) {
                result.append("\n\n=== Image information ===\n");
                for (Element img : images) {
                    String alt = img.attr("alt");
                    String src = img.attr("src");
                    result.append("[Image: ").append(alt.isEmpty() ? "No description" : alt);
                    if (!src.isEmpty()) {
                        result.append(" - ").append(src);
                    }
                    result.append("]\n");
                }
            }

            // Process links
            Elements links = doc.select("a[href]");
            if (!links.isEmpty()) {
                result.append("\n\n=== Link information ===\n");
                for (Element link : links) {
                    String text = link.text().trim();
                    String href = link.attr("href");
                    if (!text.isEmpty() && !href.isEmpty() && !href.startsWith("#")) {
                        result.append("[Link: ").append(text).append("](").append(href).append(")\n");
                    }
                }
            }

            String finalResult = result.toString().trim();

            return finalResult.isEmpty() ? "No valid content extracted" : finalResult;

        } catch (Exception e) {
            return "Content processing failed: " + e.getMessage();
        }
    }

    /**
     * Retrieves wiki page content and converts it to AI-friendly format
     * 
     * @param wikiUrl Wiki page URL, format like:
     *                https://example.com/wiki/#/team/AQzvsooq/space/EYvdiwVh/page/4RwySM6h
     * @return Formatted Wiki content
     */
    @Tool(description = "Retrieve ONES Wiki page content and convert it to AI-friendly text format")
    public String getWikiContent(
            @ToolParam(description = "Wiki page URL, format like: https://example.com/wiki/#/team/AQzvsooq/space/EYvdiwVh/page/4RwySM6h") String wikiUrl) {

        try {
            // Ensure logged in
            if (token == null && !login()) {
                return "Login failed, unable to get Wiki content";
            }

            // First try the main API endpoint
            try {
                String apiUrl = convertWikiUrlToApiUrl(wikiUrl);

                WikiContentResponse response = restClient.get()
                        .uri(apiUrl)
                        .header("Referer", String.format("https://%s/wiki/", host))
                        .header("Cookie", String.format("language=en; ones-uid=%s; ones-lt=%s; timezone=Asia/Shanghai",
                                userUuid, token))
                        .retrieve()
                        .body(WikiContentResponse.class);

                if (response != null && response.content() != null) {
                    String processedContent = processHtmlContent(response.content());
                    return processedContent;
                }
            } catch (Exception primaryException) {
                // If primary API fails, try alternative API endpoint
                try {
                    String alternativeApiUrl = convertWikiUrlToAlternativeApiUrl(wikiUrl);

                    WikiContentResponse response = restClient.get()
                            .uri(alternativeApiUrl)
                            .header("Referer", String.format("https://%s/wiki/", host))
                            .header("Cookie",
                                    String.format("language=en; ones-uid=%s; ones-lt=%s; timezone=Asia/Shanghai",
                                            userUuid, token))
                            .retrieve()
                            .body(WikiContentResponse.class);

                    if (response != null && response.content() != null) {
                        String processedContent = processHtmlContent(response.content());
                        return processedContent;
                    } else {
                        return "No Wiki content retrieved from alternative API";
                    }
                } catch (Exception alternativeException) {
                    return String.format("Both primary and alternative APIs failed. Primary: %s, Alternative: %s",
                            primaryException.getMessage(), alternativeException.getMessage());
                }
            }

            return "No Wiki content retrieved";

        } catch (IllegalArgumentException e) {
            return "URL format error: " + e.getMessage();
        } catch (Exception e) {
            return "Failed to get Wiki content: " + e.getMessage();
        }
    }
}