## ADDED Requirements

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