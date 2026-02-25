# Channel Type Internationalization Update

## Purpose

This capability manages the addition of Chinese names for channel types to improve user experience for Chinese-speaking users.

## Requirements

### Requirement: PUSH channel type Chinese name
The system SHALL display "系统推送" as the Chinese name for the PUSH channel type in the user interface.

#### Scenario: PUSH channel type display
- **WHEN** the system displays the PUSH channel type in the UI
- **THEN** the system SHALL show "系统推送" as the localized name for Chinese users

#### Scenario: PUSH channel type selection
- **WHEN** user selects the PUSH channel type from a dropdown or list
- **THEN** the system SHALL display "系统推送" as the option text for Chinese users

### Requirement: Channel type internationalization
The system SHALL display channel type names in Chinese in the frontend dropdown while maintaining the original type values for API communication.

#### Scenario: Display Chinese names in dropdown
- **WHEN** user opens the channel type selection dropdown on the frontend
- **THEN** the dropdown shall show Chinese names for each channel type option

#### Scenario: Maintain API compatibility
- **WHEN** user selects a channel type and submits the form
- **THEN** the system shall send the original type value (not the Chinese name) to the backend API

#### Scenario: Internationalization mapping
- **WHEN** the channel type selection component loads
- **THEN** the system shall load the mapping between type values and Chinese names

### Requirement: HTTP_API channel type display name
The system SHALL display "HTTP" as the label for the HTTP_API channel type in the frontend channel type selection dropdown.

#### Scenario: HTTP_API channel type display
- **WHEN** the system displays the HTTP_API channel type in the channel type selection dropdown
- **THEN** the system SHALL show "HTTP" as the label (previously "HTTP API")

#### Scenario: HTTP_API channel type selection
- **WHEN** user selects the HTTP channel type from the dropdown
- **THEN** the system SHALL use "HTTP_API" as the type value for API communication while displaying "HTTP" to the user