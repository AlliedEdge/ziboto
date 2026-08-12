# Changelog - Email Module

All notable changes to the email module are documented here.

## [0.2.0] - 2026-08-05

### Added (V2)
- Email service with Spring Mail
- SMTP configuration (Gmail support)
- Template-based email sending
- Email verification workflow
- Welcome email on registration
- Password reset email
- File share notification email
- HTML email templates

### Features
- Asynchronous email sending
- Email queue with RabbitMQ
- Retry mechanism for failed emails
- Email templates with placeholders
- Email verification token generation
- Verification link with expiry

### Templates
- **Welcome Email**: Sent on successful registration
- **Verification Email**: Email address verification
- **Password Reset**: Forgot password flow
- **Share Notification**: File/folder shared with user

### Configuration
- SMTP host, port configuration
- Authentication (username/password)
- TLS/SSL support
- From address configuration
- Email queue configuration

### Security
- Email verification required for account activation
- Token expiry (24 hours for verification)
- Rate limiting on email sending
- Spam prevention
