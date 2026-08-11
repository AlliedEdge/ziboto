# Changelog - OAuth Module

All notable changes to the OAuth module are documented here.

## [0.2.0] - 2026-08-05

### Added (V2)
- Google OAuth 2.0 integration
- OAuth2 configuration
- OAuth2UserService implementation
- Social login flow
- Account linking (email-based)

### Features
- Login with Google
- Automatic user creation on first login
- Profile picture from Google
- Email verification not required for OAuth users
- Account merging by email

### Configuration
- Google OAuth client ID
- Google OAuth client secret
- Redirect URI configuration
- OAuth scopes (profile, email)

### Security
- OAuth state parameter validation
- CSRF protection
- Secure token exchange
- JWT token generation after OAuth

### User Experience
- One-click social login
- No password required
- Automatic profile setup
- Seamless onboarding

### Future Enhancements (V5)
- Microsoft OAuth
- GitHub OAuth
- Facebook OAuth
- Apple Sign-In
- Multiple OAuth provider linking
