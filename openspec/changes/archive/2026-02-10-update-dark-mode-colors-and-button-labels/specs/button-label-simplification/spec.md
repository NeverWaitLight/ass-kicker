## ADDED Requirements

### Requirement: Simplified button labels
The system SHALL use simplified labels for common actions to improve interface clarity and reduce visual clutter.

#### Scenario: New button label simplification
- **WHEN** the system displays buttons for creating resources
- **THEN** the system SHALL use "新建" instead of longer labels like "新建通道" or "新建用户"

#### Scenario: Navigation button label simplification
- **WHEN** the system displays navigation buttons
- **THEN** the system SHALL use "返回" instead of "返回列表"

#### Scenario: Button context preservation
- **WHEN** user hovers over or focuses on simplified buttons
- **THEN** the system SHALL provide tooltips or additional context to clarify the button's specific function