# Marriage Bio-Data Maker

A web application to create, preview, and download marriage bio-data in PDF format. Built with Spring Boot, Thymeleaf, and an external PDF generation service.

## Tech Stack

- **Backend:** Spring Boot 3, Java 21
- **Frontend:** Thymeleaf, HTML/CSS
- **Database:** H2 (dev), PostgreSQL (prod)
- **PDF Generation:** External Node.js service using Puppeteer + chrome-headless-shell
- **Authentication:** Google OAuth2

## Local Development

### Prerequisites

- Java 21
- Maven 3.9+
- The [PdfService](../PdfService) project running on port 3001

### Running

1. Start the PDF service (separate project):
   ```bash
   cd ../PdfService
   CHROME_PATH="./chrome-headless-shell/mac_arm-148.0.7778.167/chrome-headless-shell-mac-arm64/chrome-headless-shell" node server.js
   ```

2. Start the Spring Boot app:
   ```bash
   mvn spring-boot:run
   ```

3. Open http://localhost:8080

The app defaults to `http://localhost:3001` for PDF generation.

## Production Deployment

Deployed on Render. Set the following environment variables:

| Variable | Value |
|----------|-------|
| `PDF_SERVICE_URL` | `https://pdfservice-adw0.onrender.com` |
| `DATABASE_URL` | PostgreSQL connection URL |
| `DATABASE_USERNAME` | DB username |
| `DATABASE_PASSWORD` | DB password |
| `GOOGLE_CLIENT_ID` | OAuth2 client ID |
| `GOOGLE_CLIENT_SECRET` | OAuth2 client secret |
| `UPLOAD_PATH` | File upload directory (default: `./uploads`) |
| `UPI_ID` | UPI ID for payment QR |

The Spring Boot app is containerized via the root `Dockerfile`.

## Project Structure

```
src/main/java/com/biodatamaker/
  controller/    - REST & MVC controllers
  service/       - Business logic (PdfService, BioDataService)
  entity/        - JPA entities
  template/      - Bio-data template definitions
src/main/resources/
  templates/     - Thymeleaf HTML templates (view + PDF variants)
  application.yml - Configuration (dev/prod profiles)
```
