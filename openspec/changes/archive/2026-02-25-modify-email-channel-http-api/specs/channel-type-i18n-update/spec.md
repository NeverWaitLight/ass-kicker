# Channel Type Internationalization Update - HTTP API Name Change

## Purpose

本规范定义了将 HTTP_API 通道类型的显示名称从 "HTTP API" 修改为 "HTTP" 的需求。

## MODIFIED Requirements

### Requirement: HTTP_API channel type display name
The system SHALL display "HTTP" as the label for the HTTP_API channel type in the frontend channel type selection dropdown.

#### Scenario: HTTP_API channel type display
- **WHEN** the system displays the HTTP_API channel type in the channel type selection dropdown
- **THEN** the system SHALL show "HTTP" as the label (previously "HTTP API")

#### Scenario: HTTP_API channel type selection
- **WHEN** user selects the HTTP channel type from the dropdown
- **THEN** the system SHALL use "HTTP_API" as the type value for API communication while displaying "HTTP" to the user
