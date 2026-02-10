## ADDED Requirements

### Requirement: Admin promotion for first user
The system SHALL automatically assign admin role to the first user who registers in the system.

#### Scenario: First user registration
- **WHEN** the first user registers in the system
- **THEN** the system assigns admin role to that user

### Requirement: Dynamic role assignment
The system SHALL implement conditional role assignment based on registration order.

#### Scenario: Subsequent user registration
- **WHEN** a user registers after the first user
- **THEN** the system assigns default user role to that user

#### Scenario: Role verification
- **WHEN** a registered user attempts to access admin functions
- **THEN** the system verifies their role before granting access