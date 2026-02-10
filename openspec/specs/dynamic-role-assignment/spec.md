# Dynamic Role Assignment

## Purpose

This specification defines how the system assigns roles to users based on specific conditions during registration. This allows for flexible role management based on various criteria such as registration order or other business rules.

## Requirements

### Requirement: Conditional role assignment
The system SHALL assign roles to users based on specific conditions during registration.

#### Scenario: Role assignment based on registration order
- **WHEN** a user registers in the system
- **THEN** the system evaluates conditions to determine the appropriate role

### Requirement: Role persistence
The system SHALL persist assigned roles in the user data model.

#### Scenario: Role retrieval
- **WHEN** user data is accessed
- **THEN** the system retrieves the assigned role correctly