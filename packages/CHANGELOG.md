# Changelog - Packages

All notable changes to shared packages and libraries are documented here.

## [Unreleased]

### Planned (V5)
- Shared TypeScript types package
- Common utilities package
- API client package
- UI component library
- React hooks library
- Constants and enums package

### Future Structure
```
packages/
├── types/           # Shared TypeScript types
├── utils/           # Common utilities
├── api-client/      # API client library
├── ui-components/   # Reusable UI components
└── constants/       # Shared constants
```

---

## Note

This directory is reserved for shared packages in a future monorepo structure. Currently, each app (frontend/backend) manages its own dependencies independently.

### Purpose
- Share code between frontend and backend
- Maintain consistent types across apps
- Reusable component library
- Common business logic

### Tools (Planned)
- Yarn Workspaces or npm Workspaces
- Lerna for monorepo management
- TypeScript project references
- Shared ESLint/Prettier configs
