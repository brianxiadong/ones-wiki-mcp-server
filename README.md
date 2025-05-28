# ONES Wiki MCP Server

A Spring AI MCP-based service for retrieving ONES Wiki content and converting it to AI-friendly text format.

## Features

- 🔐 ONES platform authentication support
- 🌐 Automatic conversion from Wiki URLs to API endpoints
- 📄 Extract and format Wiki page content
- 🤖 AI-friendly text output format
- ⚙️ Configuration via properties file or command line arguments

## Prerequisites

- Java 17 or higher
- Maven 3.6 or higher
- Access to a ONES platform instance

## Quick Start

### 1. Build the Project

```bash
mvn clean package
```

### 2. Configure Authentication

#### Option 1: Modify application.properties

Edit `src/main/resources/application.properties`:

```properties
ones.host=your-ones-host.com
ones.email=your-email@example.com
ones.password=your-password
```

#### Option 2: Use Command Line Arguments

```bash
java -jar target/ones-wiki-mcp-server-0.0.1-SNAPSHOT.jar \
  --ones.host=your-ones-host.com \
  --ones.email=your-email@example.com \
  --ones.password=your-password
```

#### Option 3: Use Environment Variables

```bash
export ONES_HOST=your-ones-host.com
export ONES_EMAIL=your-email@example.com
export ONES_PASSWORD=your-password
./start-mcp-server.sh
```

### 3. Configure in MCP Client

Add to Claude Desktop configuration file:

```json
{
  "mcpServers": {
    "ones-wiki": {
      "command": "java",
      "args": [
        "-jar",
        "/path/to/ones-wiki-mcp-server-0.0.1-SNAPSHOT.jar",
        "--ones.host=your-ones-host.com",
        "--ones.email=your-email@example.com",
        "--ones.password=your-password"
      ]
    }
  }
}
```

## Usage

### Get Wiki Content

Provide the complete Wiki page URL when using the tool:

```
Please get the content of this Wiki page: https://your-ones-host.com/wiki/#/team/TEAM_UUID/space/SPACE_UUID/page/PAGE_UUID
```

### URL Format

Supported Wiki URL format:
```
https://{host}/wiki/#/team/{team_uuid}/space/{space_uuid}/page/{page_uuid}
```

Automatically converts to API endpoint:
```
https://{host}/wiki/api/wiki/team/{team_uuid}/online_page/{page_uuid}/content
```

## Output Format

The service converts Wiki page HTML content to structured Markdown format, including:

- ✅ Heading levels (H1-H6)
- ✅ Paragraph text
- ✅ Ordered and unordered lists
- ✅ Table data (key-value format)
- ✅ Image descriptions
- ✅ Link information
- ❌ Strikethrough content (automatically filtered)

## Technical Architecture

- **Spring Boot 3.4.5** - Application framework
- **Spring AI MCP** - MCP protocol support
- **Jsoup 1.17.2** - HTML parsing
- **RestClient** - HTTP client

## Security Notes

- Authentication credentials should be managed via environment variables or configuration files
- HTTPS connections supported
- Automatic handling of ONES platform login authentication

## Development

### Project Structure

```
src/main/java/org/springframework/ai/mcp/sample/server/
├── McpServerApplication.java    # Main application
└── OnesWikiService.java        # ONES Wiki service
```

### Running Tests

```bash
mvn test
```

### Building from Source

```bash
git clone https://github.com/your-username/ones-wiki-mcp-server.git
cd ones-wiki-mcp-server
mvn clean package
```

### Extending Functionality

You can add more tool methods to `OnesWikiService`, such as:
- Search Wiki pages
- Get Wiki directory structure
- Batch process multiple pages

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

Apache License 2.0 