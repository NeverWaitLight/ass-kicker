# Channel Management Specification

## Purpose

This specification defines the requirements for managing notification channels in the system. It allows for the creation, configuration, and management of different types of notification channels (e.g., email, SMS, push notifications) with flexible key-value properties to accommodate varying configuration needs.

## Requirements

### Requirement: Channel Model Definition
The system SHALL provide a Channel entity model that includes basic fields (id, name, type, description) and flexible key-value properties for storing channel-specific configurations.

#### Scenario: Create Channel Model
- **WHEN** the system initializes the Channel entity
- **THEN** it contains basic fields (id, name, type, description) and a flexible property field for additional configurations

### Requirement: Channel CRUD Operations
The system SHALL provide REST API endpoints for creating, reading, updating, and deleting Channel entities.

#### Scenario: Create New Channel
- **WHEN** a client sends a POST request to /api/channels with valid channel data
- **THEN** the system creates a new Channel entity and returns the created channel with a 201 status code

#### Scenario: Retrieve Channel List
- **WHEN** a client sends a GET request to /api/channels
- **THEN** the system returns a list of all channels with a 200 status code

#### Scenario: Retrieve Specific Channel
- **WHEN** a client sends a GET request to /api/channels/{id}
- **THEN** the system returns the specific channel with a 200 status code

#### Scenario: Update Channel
- **WHEN** a client sends a PUT request to /api/channels/{id} with updated channel data
- **THEN** the system updates the channel and returns the updated channel with a 200 status code

#### Scenario: Delete Channel
- **WHEN** a client sends a DELETE request to /api/channels/{id}
- **THEN** the system deletes the channel and returns a 204 status code

### Requirement: Flexible Key-Value Properties
The system SHALL allow storing and retrieving channel-specific configurations as key-value pairs in the Channel entity.

#### Scenario: Store Channel-Specific Configurations
- **WHEN** a client creates or updates a channel with key-value properties
- **THEN** the system stores these properties in the channel's flexible property field

#### Scenario: Retrieve Channel-Specific Configurations
- **WHEN** a client retrieves a channel
- **THEN** the system returns the channel's key-value properties along with basic fields

### Requirement: Channel Type Classification
The system SHALL support different channel types (e.g., email, sms, push notification) to distinguish between various notification methods.

#### Scenario: Classify Channel Types
- **WHEN** a client creates a channel
- **THEN** the system accepts a type field indicating the channel type (email, sms, push notification, etc.)

### Requirement: Secure Storage of Sensitive Information
The system SHALL securely store sensitive information in channel configurations (e.g., passwords, API keys).

#### Scenario: Encrypt Sensitive Data
- **WHEN** a client creates or updates a channel with sensitive information
- **THEN** the system encrypts the sensitive values before storing them in the database

### Requirement: Channel display as 通道
The system SHALL display all channels as "通道" in the frontend UI.

#### Scenario: Channel listing
- **WHEN** user views the channel list page
- **THEN** all channels are displayed with the label "通道" regardless of their internal type

### Requirement: Channel type selection
The system SHALL provide a dropdown or selection mechanism for choosing the channel type during configuration.

#### Scenario: Channel configuration
- **WHEN** user initiates channel configuration
- **THEN** the system presents a type selection interface before other configuration fields

### Requirement: Channel name configuration
The system SHALL allow users to input a name for the channel after selecting the type.

#### Scenario: Name input
- **WHEN** user selects a channel type
- **THEN** the system displays a field for entering the channel name

### Requirement: KV property configuration
The system SHALL provide a key-value table interface for configuring channel properties instead of raw JSON input.

#### Scenario: Property configuration
- **WHEN** user configures channel properties
- **THEN** the system displays a table with key-value pairs for property entry

### Requirement: Email protocol selection for channel configuration
The system SHALL provide mail protocol selection when channel type is EMAIL, with a default protocol.

#### Scenario: Email protocol selection
- **WHEN** user selects type EMAIL in channel configuration
- **THEN** the system SHALL display protocol selection (containing at least SMTP and HTTP_API), with SMTP selected by default

### Requirement: SMTP configuration fields auto-population
The system SHALL automatically load SMTP required configuration items and default value prompts when selecting SMTP protocol.

#### Scenario: Auto-populate SMTP fields
- **WHEN** user selects SMTP as mail protocol
- **THEN** the system SHALL display and pre-fill required SMTP fields (host, port, username, password, etc.) and their default value prompts

### Requirement: SMTP properties structure
The system SHALL save mail protocol and SMTP configuration in a structured way.

#### Scenario: Persist SMTP properties
- **WHEN** user saves or tests sending mail channel configuration
- **THEN** the system SHALL write protocol field and smtp object field in properties, for backend construction of SMTP sender