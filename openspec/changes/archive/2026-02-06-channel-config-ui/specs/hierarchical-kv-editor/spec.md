## ADDED Requirements

### Requirement: Hierarchical KV editor
The system SHALL provide a key-value editor that supports hierarchical/nested key-value structures.

#### Scenario: Nested property configuration
- **WHEN** user configures properties that require nested structures
- **THEN** the system allows creating nested key-value pairs through the UI

### Requirement: Level-based filling
The system SHALL allow users to fill values at different levels of hierarchy in the KV editor.

#### Scenario: Multi-level configuration
- **WHEN** user configures nested properties
- **THEN** the system provides mechanisms to add values at parent and child levels

### Requirement: KV editor add/remove
The system SHALL allow users to add and remove key-value pairs in the hierarchical editor.

#### Scenario: Dynamic property management
- **WHEN** user needs to modify the property list
- **THEN** the system provides controls to add or remove key-value pairs at any level