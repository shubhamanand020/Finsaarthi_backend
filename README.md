# FinSaarthi Backend

## Repositories

- **Frontend**: [FinSaarthi-Frontend](https://github.com/shubhamanand020/Finsaarthi) (React + Vite)
- **Backend**: [FinSaarthi-Backend](https://github.com/shubhamanand020/Finsaarthi_backend) (this repository)

## Overview

The backend is a Spring Boot service that powers the scholarship management platform.
It is responsible for:

- Exposing REST APIs for authentication, scholarships, and applications.
- Enforcing business rules for application submission and review.
- Managing document verification within application processing.
- Tracking review and status transitions through audit records.
- Sending email notifications for key events (for example OTP and application updates).

## Architecture

The application follows a layered architecture:

Controller -> Service -> Repository -> Database

- Controller layer: HTTP endpoints, request validation, role constraints, and response shaping.
- Service layer: Core business logic and workflow orchestration.
- Repository layer: Data access using Spring Data JPA.
- Database layer: MySQL persistence for users, applications, documents, and audit logs.

## Core Features

- Application lifecycle management with defined status transitions.
- Document verification system for uploaded application documents.
- Audit trail logging for admin review actions and status changes.
- Email notifications for authentication and application-related events.
- Role-based access control for ADMIN and STUDENT users.

## Tech Stack

- Spring Boot
- Spring Security
- Spring Data JPA / Hibernate
- MySQL
- Flyway (database migrations)

## Folder Structure

```
finsaarthi-backend/
├── src/
│   ├── main/
│   │   ├── java/com/finsaarthi/
│   │   │   ├── FinsaarthiBackendApplication.java
│   │   │   ├── config/
│   │   │   │   └── (Spring configuration classes)
│   │   │   ├── controller/
│   │   │   │   ├── ApplicationController.java
│   │   │   │   ├── AuthController.java
│   │   │   │   ├── ScholarshipController.java
│   │   │   │   ├── UserController.java
│   │   │   │   └── (other API endpoints)
│   │   │   ├── dto/
│   │   │   │   ├── request/
│   │   │   │   │   └── (Request DTOs)
│   │   │   │   ├── response/
│   │   │   │   │   └── (Response DTOs)
│   │   │   │   ├── ApiResponse.java
│   │   │   │   └── (other DTOs)
│   │   │   ├── entity/
│   │   │   │   ├── User.java
│   │   │   │   ├── Application.java
│   │   │   │   ├── ApplicationDocument.java
│   │   │   │   ├── ApplicationReviewAudit.java
│   │   │   │   ├── Scholarship.java
│   │   │   │   ├── RequiredDocument.java
│   │   │   │   └── (other entities)
│   │   │   ├── enums/
│   │   │   │   ├── ApplicationStatus.java
│   │   │   │   ├── UserRole.java
│   │   │   │   ├── DocumentVerificationStatus.java
│   │   │   │   └── (other enums)
│   │   │   ├── exception/
│   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   ├── ApplicationAlreadyExistsException.java
│   │   │   │   ├── ResourceNotFoundException.java
│   │   │   │   └── (other custom exceptions)
│   │   │   ├── persistence/
│   │   │   │   └── (Data persistence utilities)
│   │   │   ├── repository/
│   │   │   │   ├── ApplicationRepository.java
│   │   │   │   ├── UserRepository.java
│   │   │   │   ├── ScholarshipRepository.java
│   │   │   │   ├── ApplicationReviewAuditRepository.java
│   │   │   │   └── (other JPA repositories)
│   │   │   ├── security/
│   │   │   │   ├── JwtTokenProvider.java
│   │   │   │   ├── JwtAuthenticationFilter.java
│   │   │   │   ├── CustomUserDetailsService.java
│   │   │   │   └── (security configuration)
│   │   │   └── service/
│   │   │       ├── ApplicationService.java
│   │   │       ├── AuthService.java
│   │   │       ├── ScholarshipService.java
│   │   │       ├── UserService.java
│   │   │       ├── EmailService.java
│   │   │       └── (other business services)
│   │   ├── resources/
│   │   │   ├── application.properties
│   │   │   ├── db/migration/
│   │   │   │   └── (Flyway SQL migration files)
│   │   │   └── templates/
│   └── test/
│       └── (Unit and integration tests)
├── pom.xml
├── mvnw / mvnw.cmd
├── .env.example
├── HELP.md
└── README.md
```

Key packages:

- **controller/**: REST API endpoints handling HTTP requests and responses.
- **service/**: Core business logic for applications, authentication, scholarships, and notifications.
- **repository/**: Data access layer using Spring Data JPA for MySQL operations.
- **entity/**: JPA entity classes representing database tables.
- **dto/**: Data transfer objects for request/response payloads.
- **security/**: JWT authentication, authorization, and security configuration.
- **exception/**: Custom exceptions and global error handling.
- **enums/**: Application statuses, user roles, and verification states.

## Database Design

Primary entities include:

- `User`: Stores user account and role information.
- `Application`: Stores scholarship application details and lifecycle status.
- `ApplicationDocument`: Stores uploaded document metadata linked to an application.
- `ApplicationReviewAudit`: Stores admin audit entries for review actions and status transitions.

These entities are linked to support end-to-end processing from submission to final decision.

## API Endpoints

Example endpoints used in the application workflow:

```http
GET /api/applications
```
Returns application list for authorized admin users.

```http
PATCH /api/applications/{id}/status
```
Updates application status (for example `PENDING`, `UNDER_REVIEW`, `VERIFIED`, `APPROVED`, `REJECTED`) and review notes.

```http
PATCH /api/applications/{applicationId}/documents/{documentId}/verification
```
Updates verification state for a specific document within an application.

Other relevant endpoints include application submission, per-user application retrieval, and application stats.

## Setup Instructions

From the backend project root:

```bash
./mvnw spring-boot:run
```

On Windows PowerShell, if needed:

```powershell
.\mvnw.cmd spring-boot:run
```

Default server settings:

- Port: `8080`
- Context path: `/api`

## Configuration

Configure the backend using `src/main/resources/application.properties` and environment variables.

Important properties:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/finsaarthi
spring.datasource.username=your_db_username
spring.datasource.password=your_db_password
spring.jpa.hibernate.ddl-auto=update
```

In this project, datasource and several secrets are environment-backed (for example `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, mail settings).

## Workflow Explanation

The backend workflow for scholarship processing is:

1. Student submits an application through the API.
2. Application data and uploaded document references are stored.
3. Admin reviews application details in the review queue.
4. Admin verifies documents and updates verification status.
5. Admin updates application status through review stages:
   `PENDING -> UNDER_REVIEW -> VERIFIED -> APPROVED` or `REJECTED`.
6. Each admin action and status transition is recorded in `ApplicationReviewAudit`.
7. Email notifications are sent for relevant status or account events.

## Security

The backend applies role-based security with Spring Security.

- Roles: `ADMIN`, `STUDENT`
- Protected endpoints are guarded with role checks (for example `@PreAuthorize`).
- JWT-based authentication is used for authorized API access.
- Public/auth endpoints are separated from protected application-management endpoints.
- Environment variables store sensitive data like `JWT_SECRET`, database credentials, and mail credentials.
- CORS is configured to restrict requests to authorized frontend origins.
- Rate limiting is applied to authentication endpoints to prevent brute force attacks.
- OTP-based email verification adds an additional security layer for registration and password recovery.

## Testing

Run unit and integration tests:

```bash
./mvnw test
```

Or on Windows:

```powershell
.\mvnw.cmd test
```

## Deployment

For production deployment:

```bash
./mvnw clean package
java -jar target/finsaarthi-backend-*.jar
```

Ensure all environment variables are properly configured in the deployment environment.

### Render Deployment (Backend)

Use these settings in Render for this backend service:

- Runtime: Java 21
- Build command: `./mvnw clean package -DskipTests`
- Start command: `java -jar target/finsaarthi-backend-1.0.0.jar`

Recommended Render environment variables:

- `SPRING_PROFILES_ACTIVE=prod`
- `APP_LOG_LEVEL=INFO`
- `PORT` (provided automatically by Render)
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `DB_DRIVER_CLASS_NAME` (for MySQL: `com.mysql.cj.jdbc.Driver`, for PostgreSQL: `org.postgresql.Driver`)
- `HIBERNATE_DIALECT` (for MySQL: `org.hibernate.dialect.MySQLDialect`, for PostgreSQL: `org.hibernate.dialect.PostgreSQLDialect`)
- `JWT_SECRET`
- `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`
- `CORS_ALLOWED_ORIGINS` (include your Vercel frontend URL)

## Troubleshooting

- **Connection refused on port 8080**: Ensure no other service is using the port.
- **Database connection error**: Verify MySQL is running and credentials in `.env` are correct.
- **JWT authentication failures**: Confirm `JWT_SECRET` environment variable is set.
- **Email not sending**: Check SMTP configuration in `application.properties` and email service credentials.

## Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature`
3. Commit your changes: `git commit -m 'Add your feature'`
4. Push and open a pull request

Please follow Java and Spring Boot conventions in your code contributions.

## Contact and Support

For questions, issues, or feedback about the backend:

- Issues: Use the GitHub repository Issues tab
- Email: finsaarthiindia@gmail.com
- GitHub: [@shubhamanand020](https://github.com/shubhamanand020)

