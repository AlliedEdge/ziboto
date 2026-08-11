# Changelog - Nginx Infrastructure

All notable changes to the Nginx infrastructure are documented here.

## [0.3.0] - 2026-08-12

### Security
- Added `.gitignore` to protect SSL certificates and private keys
- Prevents accidental commit of sensitive files

### Protected Files
- SSL certificates (`.crt`, `.key`, `.pem`)
- Environment-specific configurations
- Log files and runtime data

---

## [0.2.0] - 2026-08-05

### Enhanced (V2)
- Rate limiting configuration
- Request buffering
- Client body size limits
- Timeout configuration

---

## [0.1.0] - Initial Setup

### Added (V1)
- Reverse proxy configuration
- Load balancing for backend services
- Static file serving
- CORS headers
- Gzip compression
- Client max body size (100MB for file uploads)
- Proxy headers (X-Real-IP, X-Forwarded-For)
- Health check endpoints

### Configuration
- Listen on port 80
- Proxy to backend on port 8080
- Request timeout: 60s
- Client body buffer: 128k
- Gzip compression enabled

### Future (V4)
- HTTPS/SSL configuration
- Let's Encrypt integration
- HTTP/2 support
- WebSocket proxy support
- CDN integration
