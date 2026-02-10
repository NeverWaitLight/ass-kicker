# SMTP Sender Configuration Specification

## Purpose

This specification defines the requirements for configuring SMTP senders in the system. It includes defining the configuration schema, validating parameters, and mapping configuration properties to SMTP sender parameters.

## Requirements

### Requirement: SMTP sender configuration schema
The system SHALL define a configuration schema for SMTP sender, containing fields key, type, required status, default values and hints for frontend rendering.

#### Scenario: Get SMTP configuration schema
- **WHEN** frontend requests mail protocol configuration schema
- **THEN** the system SHALL return field definitions for SMTP protocol (containing at least host, port, username, password and their required/default value information)

### Requirement: SMTP sender parameter validation and default values
The system SHALL validate required parameters and fill default values for optional parameters when constructing SMTP sender.

#### Scenario: Validation and default value filling
- **WHEN** system constructs SMTP sender based on temporary configuration for test sending
- **THEN** the system SHALL validate required fields host, port, username, password, and set default values for protocol, sslEnabled, from, connectionTimeout, readTimeout, maxRetries, retryDelay (if not provided)

### Requirement: SMTP sender parameter mapping
The system SHALL map SMTP configuration in properties to structured parameters required for SMTP sender construction.

#### Scenario: Parameter mapping
- **WHEN** properties contain protocol and smtp object configuration
- **THEN** the system SHALL map them to parameters required for SMTP sender instantiation