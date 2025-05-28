# ONES Wiki MCP Server Usage Examples

This document provides detailed usage examples for the ONES Wiki MCP Server.

## Configuration Examples

### 1. Claude Desktop Configuration

Add the following configuration to your Claude Desktop settings:

```json
{
  "mcpServers": {
    "ones-wiki": {
      "command": "java",
      "args": [
        "-jar",
        "--ones.host=your-ones-host.com",
        "--ones.email=your-email@example.com",
        "--ones.password=your-password",
        "/path/to/ones-wiki-mcp-server-0.0.1-SNAPSHOT.jar"
      ]
    }
  }
}
```

### 2. Environment Variables Configuration

You can also configure using environment variables:

```json
{
  "mcpServers": {
    "ones-wiki": {
      "command": "java",
      "args": [
        "-jar",
        "/path/to/ones-wiki-mcp-server-0.0.1-SNAPSHOT.jar"
      ],
      "env": {
        "ONES_HOST": "your-ones-host.com",
        "ONES_EMAIL": "your-email@example.com",
        "ONES_PASSWORD": "your-password"
      }
    }
  }
}
```

## Usage Examples

### Example 1: Basic Wiki Content Retrieval

**User Input:**
```
Please get the content of this ONES Wiki page:
https://your-ones-host.com/wiki/#/team/AQzvsooq/space/EYvdiwVh/page/4RwySM6h
```

**Expected Response:**
The system will retrieve and format the wiki content, converting it to a readable text format suitable for AI processing.

**User**: Please get the content of this ONES Wiki page and summarize the main information: https://your-ones-host.com/wiki/#/team/AQzvsooq/space/EYvdiwVh/page/4RwySM6h

**Assistant**: I'll retrieve the content from the ONES Wiki page for you.

[The assistant will call the getWikiContent tool and then provide a summary of the retrieved content]

### Example 2: Content Analysis

**User**: Get the wiki content from https://your-ones-host.com/wiki/#/team/AQzvsooq/space/EYvdiwVh/page/4RwySM6h and extract the key technical specifications mentioned.

**Assistant**: I'll retrieve the wiki content and analyze it for technical specifications.

[The assistant will call the tool and then analyze the content for technical details]

### Example 3: Multiple Page Processing

**User**: Please get the content from these wiki pages and compare their information:
1. https://your-ones-host.com/wiki/#/team/AQzvsooq/space/EYvdiwVh/page/4RwySM6h
2. https://your-ones-host.com/wiki/#/team/AQzvsooq/space/EYvdiwVh/page/5TxzAB7i

**Assistant**: I'll retrieve content from both wiki pages and provide a comparison.

[The assistant will make multiple tool calls and then compare the content]

## Supported Content Types

The MCP server can process various types of wiki content:

### 1. Text Content
- Headings (H1-H6)
- Paragraphs
- Formatted text (bold, italic)

### 2. Lists
- Ordered lists (numbered)
- Unordered lists (bullet points)
- Nested lists

### 3. Tables
- Data tables with headers
- Key-value pair tables
- Complex table structures

### 4. Media Content
- Image descriptions and references
- Embedded content information

### 5. Links
- Internal wiki links
- External links
- Reference links

## Error Handling

The service handles various error scenarios gracefully:

### Authentication Errors
```
Login failed, unable to get Wiki content
```

### URL Format Errors
```
URL format error: Invalid wiki URL format
```

### Network Errors
```
Network request failed: Connection timeout
```

### Content Processing Errors
```
Content processing failed: Unable to parse content
```

## Best Practices

1. **URL Format**: Always use the complete wiki URL including the hash fragment
2. **Authentication**: Store credentials securely using environment variables
3. **Error Handling**: Check the response for error messages before processing
4. **Rate Limiting**: Be mindful of API rate limits when making multiple requests

## Troubleshooting

### Common Issues

1. **Authentication Failed**
   - Verify your credentials are correct
   - Check if your account has access to the wiki page
   - Ensure the ONES host URL is correct

2. **Invalid URL Format**
   - Ensure the URL follows the expected format
   - Check that all UUIDs are present in the URL

3. **Empty Content**
   - The page might be empty or restricted
   - Check page permissions in ONES

4. **Network Issues**
   - Verify network connectivity
   - Check if the ONES server is accessible

## Advanced Usage

### Custom Processing
You can extend the service to add custom processing logic for specific content types or formats.

### Batch Processing
For processing multiple pages, consider implementing batch processing to improve efficiency.

### Content Caching
Implement caching mechanisms for frequently accessed content to reduce API calls. 