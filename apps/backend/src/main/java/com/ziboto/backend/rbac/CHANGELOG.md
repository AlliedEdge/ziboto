# Changelog - RBAC (Role-Based Access Control) Module

All notable changes to the RBAC module are documented here.

## [0.2.0] - 2026-08-05

### Added (V2)
- Role-based access control system
- User roles: ADMIN, USER
- Permission system
- Authorization checks
- Method-level security

### Roles
- **ADMIN**: Full system access, user management, system configuration
- **USER**: Standard access, own files and folders

### Features
- Role assignment on user creation
- Role-based endpoint protection
- `@PreAuthorize` annotations
- Admin-only endpoints
- User context access

### Security
- Authorization checks in services
- Spring Security integration
- JWT role claims
- Method security enabled

### Admin Capabilities
- View all users
- Manage user accounts
- View system statistics
- Manual cleanup tasks
- System configuration

### Future Enhancements (V5)
- Custom roles
- Fine-grained permissions
- Organization/team roles
- Resource-level permissions
- Permission inheritance
