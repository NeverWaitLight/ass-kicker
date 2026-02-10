## MODIFIED Requirements

### Requirement: Channel type shall be limited to predefined values
The system SHALL restrict channel types to SMS, EMAIL, IM, and PUSH only. Any attempt to create or update a channel with a different type SHALL result in an error.

#### Scenario: Creating a channel with valid type
- **WHEN** user creates a new channel with type SMS, EMAIL, IM, or PUSH
- **THEN** the system accepts the request and creates the channel

#### Scenario: Creating a channel with invalid type
- **WHEN** user attempts to create a channel with a type other than SMS, EMAIL, IM, or PUSH
- **THEN** the system rejects the request with an appropriate error message

#### Scenario: Updating a channel to valid type
- **WHEN** user updates an existing channel to a type of SMS, EMAIL, IM, or PUSH
- **THEN** the system accepts the request and updates the channel

#### Scenario: Updating a channel to invalid type
- **WHEN** user attempts to update a channel to a type other than SMS, EMAIL, IM, or PUSH
- **THEN** the system rejects the request with an appropriate error message

### Requirement: Frontend channel type selection
The frontend interface SHALL provide a radio button group for selecting the channel type, allowing selection only from the predefined values: SMS, EMAIL, IM, and PUSH.

#### Scenario: Displaying channel creation form
- **WHEN** user navigates to the channel creation page
- **THEN** the system displays radio buttons for SMS, EMAIL, IM, and PUSH options

#### Scenario: Submitting form with selected channel type
- **WHEN** user selects a channel type and submits the form
- **THEN** the system sends the selected type to the backend API

### Requirement: Backend enum representation
The backend SHALL represent channel types using an enumeration with values SMS, EMAIL, IM, and PUSH in uppercase format.

#### Scenario: Processing channel creation request
- **WHEN** the system receives a channel creation request with a valid type
- **THEN** the system maps the string value to the corresponding enum value

#### Scenario: Returning channel data via API
- **WHEN** the system returns channel data through an API endpoint
- **THEN** the channel type is represented as an uppercase string matching the enum value