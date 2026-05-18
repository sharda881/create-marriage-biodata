# Marriage Bio-Data Maker

A web application to create, preview, and download marriage bio-data in PDF format.

## Architecture

```
User Browser
     │
     ▼
┌──────────────────────┐         ┌──────────────────────┐
│  Spring Boot App     │  POST   │  PDF Service (Node)  │
│  (Port 8080)         │────────▶│  (Port 3001)         │
│                      │  HTML   │                      │
│  - Renders HTML with │◀────────│  - Receives HTML     │
│    Thymeleaf         │  PDF    │  - Renders via       │
│  - Encodes photo as  │  bytes  │    Puppeteer         │
│    base64 in HTML    │         │  - Returns PDF bytes │
│  - Sends HTML to     │         │                      │
│    PDF service       │         │                      │
└──────────────────────┘         └──────────────────────┘
```

**Flow:**
1. User clicks "Download PDF" on the preview page
2. Spring Boot renders the bio-data HTML template (with photo embedded as base64)
3. Spring Boot sends the HTML string to the PDF service via HTTP POST
4. PDF service renders the HTML in headless Chrome and returns PDF bytes
5. Spring Boot sends the PDF file back to the user's browser

## Local Development

### Prerequisites

- Java 21
- Maven 3.9+
- Node.js 18+

### 1. Clone and run the PDF Service

```bash
git clone https://github.com/Xaier310/PdfService.git
cd PdfService
npm install
```

Start the PDF service (macOS Apple Silicon):
```bash
CHROME_PATH="./chrome-headless-shell/mac_arm-148.0.7778.167/chrome-headless-shell-mac-arm64/chrome-headless-shell" node server.js
```

The service starts on http://localhost:3001.

### 2. Run the Spring Boot App

```bash
cd create-marriage-biodata
mvn spring-boot:run
```

Open http://localhost:8080.

## Production

Deployed on Render. Required environment variables:

| Variable | Description |
|----------|-------------|
| `SPRING_PROFILES_ACTIVE` | Set to `prod` |
| `DATABASE_URL` | PostgreSQL connection URL |
| `DATABASE_USERNAME` | DB username |
| `DATABASE_PASSWORD` | DB password |
| `GOOGLE_CLIENT_ID` | OAuth2 client ID |
| `GOOGLE_CLIENT_SECRET` | OAuth2 client secret |
| `PDF_SERVICE_URL` | (Optional) Defaults to `https://pdfservice-adw0.onrender.com` |
