## ADDED Requirements

### Requirement: Aliyun Mail Sender Implementation
The system SHALL implement a concrete Sender class for sending emails via Aliyun email service.

#### Scenario: Email Sending via Aliyun
- **WHEN** the system needs to send an email
- **THEN** it can use the AliyunMailSender implementation to send the message

### Requirement: Configuration Support for Aliyun Mail
The system SHALL support configuration properties for connecting to Aliyun email service.

#### Scenario: Configuring Aliyun Mail Service
- **WHEN** configuring the AliyunMailSender
- **THEN** the system can specify host, port, username, password, and other SMTP settings

### Requirement: SSL Connection for Aliyun Mail
The AliyunMailSender SHALL establish secure connections using SSL when communicating with the email server.

#### Scenario: Secure Email Transmission
- **WHEN** sending an email via AliyunMailSender
- **THEN** the connection to the email server is secured with SSL