# Marriage Bio-Data Maker

A production-ready, monolithic web application for creating beautiful marriage bio-data PDFs. Built with Spring Boot 3.x, Thymeleaf, and Tailwind CSS.

## Features

- **Multi-Step Form**: Clean, intuitive UI to collect bio-data with "Save as Draft" functionality
- **6 Beautiful Templates**: Modern, Traditional, Royal, Floral, Simple, and Premium designs
- **PDF Export**: High-quality PDF generation using OpenHTMLtoPDF
- **Authentication**: Both Google OAuth2 and Email/Password login
- **Payment System**: QR code-based UPI payment with admin verification
- **Admin Panel**: Payment verification, user management, and system configuration
- **Responsive Design**: Tailwind CSS for modern, mobile-friendly UI

## Tech Stack

- **Backend**: Java 21, Spring Boot 3.2.x
- **Security**: Spring Security with OAuth2 Client
- **Database**: H2 (dev) / PostgreSQL (prod)
- **Template Engine**: Thymeleaf
- **CSS Framework**: Tailwind CSS (via CDN)
- **PDF Generation**: OpenHTMLtoPDF
- **Build Tool**: Maven

## Quick Start

### Prerequisites

- Java 21 or higher
- Maven 3.8+
- (Optional) PostgreSQL for production

### Running in Development Mode

```bash
# Clone the repository
git clone <repository-url>
cd web-app-biodata

# Run with Maven
./mvnw spring-boot:run

# Or build and run JAR
./mvnw clean package
java -jar target/marriage-biodata-maker-1.0.0.jar
```

The application will start at `http://localhost:8080`

### Default Users (Dev Mode)

| Role  | Email                    | Password   |
|-------|--------------------------|------------|
| Admin | admin@biodatamaker.app   | Admin@123  |
| User  | demo@biodatamaker.app    | Demo@123   |

## Configuration

### Google OAuth2 Setup

1. Create a project in [Google Cloud Console](https://console.cloud.google.com/)
2. Enable OAuth2 and create credentials
3. Set redirect URI: `http://localhost:8080/login/oauth2/code/google`
4. Add environment variables:

```bash
export GOOGLE_CLIENT_ID=your-client-id
export GOOGLE_CLIENT_SECRET=your-client-secret
```

### Payment QR Code

Replace the placeholder QR code at:
```
src/main/resources/static/images/payment_qr.png
```
with your actual UPI payment QR code.

### Production Configuration

Set these environment variables for production:

```bash
# Database
export DATABASE_URL=jdbc:postgresql://host:5432/biodata_db
export DATABASE_USERNAME=your-username
export DATABASE_PASSWORD=your-password

# OAuth2
export GOOGLE_CLIENT_ID=your-client-id
export GOOGLE_CLIENT_SECRET=your-client-secret

# App Config
export UPLOAD_PATH=/var/uploads
export UPI_ID=your-upi@bank
```

Run with production profile:
```bash
java -jar target/marriage-biodata-maker-1.0.0.jar --spring.profiles.active=prod
```

## Project Structure

```
src/main/
├── java/com/biodatamaker/
│   ├── config/          # Security, Web configuration
│   ├── controller/      # MVC Controllers
│   ├── dto/             # Data Transfer Objects
│   ├── entity/          # JPA Entities
│   ├── exception/       # Custom Exceptions
│   ├── repository/      # Spring Data Repositories
│   ├── service/         # Business Logic
│   ├── template/        # Strategy Pattern for Templates
│   └── util/            # Utilities
└── resources/
    ├── templates/       # Thymeleaf templates
    │   ├── fragments/   # Reusable fragments
    │   ├── biodata/     # Bio-data forms & templates
    │   ├── admin/       # Admin panel pages
    │   ├── auth/        # Login & Register
    │   ├── payment/     # Payment pages
    │   └── error/       # Error pages
    └── static/
        └── images/      # Static images
```

## Design Patterns Used

1. **Strategy Pattern**: For template selection (`BioDataTemplate` interface)
2. **Factory Pattern**: For template resolution (`BioDataTemplateFactory`)
3. **DTO Pattern**: Clean data transfer between layers
4. **Service Layer**: Business logic separation

## API Endpoints

### Public
- `GET /` - Landing page
- `GET /login` - Login page
- `GET /register` - Registration page

### User (Authenticated)
- `GET /dashboard` - User dashboard
- `GET /biodata/create` - Create bio-data form
- `GET /biodata/edit/{id}` - Edit bio-data
- `GET /biodata/preview/{id}` - Preview bio-data
- `GET /biodata/download/{id}` - Download PDF
- `GET /payment/checkout/{id}` - Payment page
- `GET /payment/status/{id}` - Payment status

### Admin
- `GET /admin` - Admin dashboard
- `GET /admin/payments` - Payment management
- `POST /admin/payments/{id}/approve` - Approve payment
- `POST /admin/payments/{id}/reject` - Reject payment

## Paywall Logic

1. Each user gets a configurable number of free downloads (default: 2)
2. After free limit, users must pay to download
3. Payment flow:
   - User scans QR code and pays via UPI
   - User enters UPI Transaction ID
   - Admin verifies and approves payment
   - User can download PDF

## Building for Production

```bash
# Build with production profile
./mvnw clean package -Pprod

# The JAR will be at target/marriage-biodata-maker-1.0.0.jar
```

## Database Schema

The application uses JPA with auto DDL generation. Key entities:
- `User` - User accounts
- `BioData` - Bio-data records
- `PaymentTransaction` - Payment tracking
- `SystemConfig` - Feature flags

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

This project is licensed under the MIT License.

---

Built with ❤️ for making marriage bio-data creation simple and beautiful.
