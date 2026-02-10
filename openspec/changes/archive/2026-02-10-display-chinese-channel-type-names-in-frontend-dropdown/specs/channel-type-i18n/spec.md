## ADDED Requirements

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