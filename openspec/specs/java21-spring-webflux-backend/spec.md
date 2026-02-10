# Purpose
TBD: 定义 Java 21 + Spring WebFlux 后端的功能与非功能需求基线。

## Requirements

### Requirement: Backend application shall start successfully
The Java 21 Spring WebFlux application SHALL start without errors and be accessible on a configured port.

#### Scenario: Application startup
- **WHEN** the application is started
- **THEN** the Spring Boot application initializes all beans successfully
- **AND** the WebFlux server starts and listens on the configured port

### Requirement: Backend shall provide health check endpoint
The application SHALL provide a health check endpoint to verify service availability.

#### Scenario: Health check endpoint access
- **WHEN** a GET request is made to the health check endpoint
- **THEN** the server returns HTTP 200 OK status
- **AND** the response contains health status information

### Requirement: Backend shall use WebFlux Router/Handler pattern
The system SHALL implement HTTP endpoints using Spring WebFlux Router/Handler pattern instead of traditional Controller pattern.

#### Scenario: Template creation endpoint
- **WHEN** client sends POST request to /api/templates with valid template data
- **THEN** system creates new template using WebFlux Handler and returns success response with template ID

#### Scenario: Template retrieval endpoint
- **WHEN** client sends GET request to /api/templates/{id}
- **THEN** system returns template data using WebFlux Handler if exists

#### Scenario: Template update endpoint
- **WHEN** client sends PUT request to /api/templates/{id} with updated template data
- **THEN** system updates template using WebFlux Handler and returns success response

#### Scenario: Template deletion endpoint
- **WHEN** client sends DELETE request to /api/templates/{id}
- **THEN** system deletes template using WebFlux Handler and returns success response

### Requirement: Backend shall remove e2e-tests folder
The system SHALL remove the e2e-tests folder and all its contents as part of the refactoring process.

#### Scenario: E2E tests folder removal
- **WHEN** refactoring process is executed
- **THEN** the e2e-tests folder and all contained files are removed from the project

### Requirement: Router function shall define API routes
The system SHALL define API routes using RouterFunction in WebFlux.

#### Scenario: Router defines template routes
- **WHEN** application starts
- **THEN** RouterFunction defines routes for template CRUD operations

### Requirement: Handler shall process API requests
The system SHALL process API requests using Handler classes that implement the business logic.

#### Scenario: Handler processes template creation
- **WHEN** router receives POST request to /api/templates
- **THEN** handler processes the request and creates a new template

### Requirement: Backend shall implement JWT-based authentication
The system SHALL implement JWT-based authentication with access and refresh tokens.

#### Scenario: Login with username and password
- **WHEN** user submits correct username and password to login endpoint
- **THEN** system returns HTTP 200 with JWT and user info
- **AND** system issues access token (valid for 1 day) and refresh token (valid for 30 days)

#### Scenario: Access protected resources with JWT
- **WHEN** user accesses protected endpoint with valid JWT
- **THEN** system validates JWT signature and expiration
- **AND** grants access to the resource

#### Scenario: Access protected resources without JWT
- **WHEN** user accesses protected endpoint without JWT
- **THEN** system returns HTTP 401 Unauthorized

#### Scenario: Refresh access token
- **WHEN** user submits valid refresh token to refresh endpoint
- **THEN** system returns new access token

### Requirement: Backend shall implement user management endpoints
The system SHALL provide REST endpoints for user management with role-based access control.

#### Scenario: Admin creates user
- **WHEN** ADMIN user submits valid user data to create endpoint
- **THEN** system creates new user with hashed password and assigned role
- **AND** returns HTTP 201 with user info

#### Scenario: Admin retrieves user list
- **WHEN** ADMIN user requests user list with pagination parameters
- **THEN** system returns paginated list of users with basic info

#### Scenario: User updates own profile
- **WHEN** authenticated user updates their own profile
- **THEN** system validates user has permission to modify the data
- **AND** updates the user record accordingly
