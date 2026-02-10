## ADDED Requirements

### Requirement: Template entity supports multilingual content
The system SHALL support abstract templates without content, allowing content to be stored separately by language.

#### Scenario: Creating a template without content
- **WHEN** user creates a new template with name, code, and description
- **THEN** system stores the template metadata without content

### Requirement: Language-specific template content storage
The system SHALL store template content separately for each language using the LanguageTemplate entity.

#### Scenario: Adding content for a specific language
- **WHEN** user adds content for a specific language to a template
- **THEN** system creates a LanguageTemplate record with template_id, language, and content

### Requirement: Support for international language codes
The system SHALL support the following language codes: zh-CN (Simplified Chinese), zh-TW (Traditional Chinese), en (English), fr (French), de (German).

#### Scenario: Validating language codes
- **WHEN** user specifies a language code for template content
- **THEN** system validates that the code is one of the supported languages

### Requirement: Template and LanguageTemplate relationship
The system SHALL establish a one-to-many relationship between Template and LanguageTemplate entities.

#### Scenario: Retrieving all language versions of a template
- **WHEN** user requests all language versions of a template
- **THEN** system returns all associated LanguageTemplate records

### Requirement: UTC timestamp storage
The system SHALL store timestamps as 64-bit integers representing UTC time.

#### Scenario: Creating a template
- **WHEN** user creates a template
- **THEN** system stores createdAt and updatedAt as UTC timestamps in seconds since epoch

### Requirement: Unique template code constraint
The system SHALL enforce uniqueness of the template code field.

#### Scenario: Attempting to create duplicate template codes
- **WHEN** user attempts to create a template with an existing code
- **THEN** system returns an error indicating the code is already in use