# Email Verification Test Plan

**Date:** August 11, 2026  
**API Key:** `[REDACTED]`  
**From Email:** `no-reply@alliededge.app`  
**Status:** READY FOR TESTING

---

## Overview

Ziboto uses **Resend** for transactional emails with OTP-based verification.

**Email Types:**
1. Registration Email Verification
2. Password Reset
3. Welcome Email (after successful verification)

---

## Configuration

### Backend (.env)
```bash
RESEND_API_KEY=[REDACTED]
RESEND_FROM_EMAIL=no-reply@alliededge.app
RESEND_FROM_NAME=Ziboto
RESEND_VERIFICATION_URL=http://localhost:5173/verify-email
RESEND_RESET_URL=http://localhost:5173/reset-password
RESEND_SUPPORT_EMAIL=alliededgehq@gmail.com
```

### Resend Service
- **API Documentation:** https://resend.com/docs
- **Dashboard:** https://resend.com/emails
- **From Email:** no-reply@alliededge.app (verified domain)

---

## Test Scenarios

### Test 1: Registration with Email Verification

**Objective:** Verify end-to-end registration and email verification flow

**Steps:**
1. Start backend: `cd apps/backend && .\mvnw.cmd spring-boot:run`
2. Register new user via API:
   ```bash
   curl -X POST http://localhost:8080/api/v1/auth/register ^
     -H "Content-Type: application/json" ^
     -d "{\"email\":\"test@example.com\",\"username\":\"testuser\",\"password\":\"Test@123\",\"fullName\":\"Test User\"}"
   ```
3. **Expected Response:** `201 Created`
   ```json
   {
     "success": true,
     "message": "Registration successful. Please verify your email.",
     "data": {
       "email": "test@example.com",
       "verificationRequired": true
     }
   }
   ```

4. **Check Email Inbox** (test@example.com)
   - Subject: "Verify your Ziboto account"
   - From: "Ziboto <no-reply@alliededge.app>"
   - Contains: 6-digit OTP code
   - Contains: "Verify my email" button with link

5. **Extract OTP** from email (e.g., `123456`)

6. **Verify Email:**
   ```bash
   curl -X POST http://localhost:8080/api/v1/auth/verify-email ^
     -H "Content-Type: application/json" ^
     -d "{\"email\":\"test@example.com\",\"otp\":\"123456\"}"
   ```

7. **Expected Response:** `200 OK` with JWT tokens
   ```json
   {
     "accessToken": "eyJhbG...",
     "refreshToken": "eyJhbG...",
     "expiresIn": 900000,
     "tokenType": "Bearer",
     "user": {
       "id": 1,
       "email": "test@example.com",
       "username": "testuser",
       "fullName": "Test User",
       "emailVerified": true,
       "status": "ACTIVE"
     }
   }
   ```

8. **Check Welcome Email**
   - Subject: "Welcome to Ziboto!"
   - Content: "Your email has been verified..."
   - Mentions: "5 GB of cloud storage"

**Success Criteria:**
- ✅ Registration email received within 5 seconds
- ✅ OTP code is 6 digits
- ✅ OTP works within 5 minutes
- ✅ Verification returns JWT tokens
- ✅ User status becomes ACTIVE
- ✅ Welcome email received
- ✅ Email template renders correctly (HTML + plain text)

---

### Test 2: Resend Verification Email

**Objective:** Test resending verification OTP

**Steps:**
1. Register a new user (as in Test 1)
2. **Wait 1 minute** (don't verify yet)
3. **Resend verification email:**
   ```bash
   curl -X POST http://localhost:8080/api/v1/auth/send-verification-email ^
     -H "Content-Type: application/json" ^
     -d "{\"email\":\"test@example.com\"}"
   ```

4. **Expected Response:** `200 OK`
   ```json
   {
     "message": "Verification email sent successfully"
   }
   ```

5. **Check Email** - New OTP should be received
6. **Old OTP should NOT work** (invalidated)
7. **New OTP should work**

**Success Criteria:**
- ✅ New email received
- ✅ Old OTP rejected with error
- ✅ New OTP accepted

---

### Test 3: Password Reset Flow

**Objective:** Test forgot password and reset flow

**Steps:**
1. Create and verify a user account
2. **Initiate password reset:**
   ```bash
   curl -X POST http://localhost:8080/api/v1/auth/forgot-password ^
     -H "Content-Type: application/json" ^
     -d "{\"email\":\"test@example.com\"}"
   ```

3. **Check Email Inbox**
   - Subject: "Reset your Ziboto password"
   - From: "Ziboto <no-reply@alliededge.app>"
   - Contains: 6-digit OTP
   - Contains: "Reset my password" button

4. **Extract OTP** (e.g., `789012`)

5. **Reset Password:**
   ```bash
   curl -X POST http://localhost:8080/api/v1/auth/reset-password ^
     -H "Content-Type: application/json" ^
     -d "{\"email\":\"test@example.com\",\"otp\":\"789012\",\"newPassword\":\"NewPass@123\"}"
   ```

6. **Expected Response:** `200 OK`
   ```json
   {
     "message": "Password reset successfully"
   }
   ```

7. **Login with new password:**
   ```bash
   curl -X POST http://localhost:8080/api/v1/auth/login ^
     -H "Content-Type: application/json" ^
     -d "{\"identifier\":\"test@example.com\",\"password\":\"NewPass@123\"}"
   ```

**Success Criteria:**
- ✅ Password reset email received
- ✅ OTP works for password reset
- ✅ Old password no longer works
- ✅ New password works for login

---

### Test 4: OTP Expiration

**Objective:** Verify OTP expires after 5 minutes

**Steps:**
1. Register new user
2. **Wait 6 minutes** (do NOT verify)
3. **Attempt verification with expired OTP:**
   ```bash
   curl -X POST http://localhost:8080/api/v1/auth/verify-email ^
     -H "Content-Type: application/json" ^
     -d "{\"email\":\"test@example.com\",\"otp\":\"<expired-otp>\"}"
   ```

4. **Expected Response:** `400 Bad Request`
   ```json
   {
     "error": "INVALID_OTP",
     "message": "OTP is invalid or expired"
   }
   ```

5. **Resend verification email** (get fresh OTP)
6. **Verify with fresh OTP** - should work

**Success Criteria:**
- ✅ Expired OTP rejected
- ✅ Fresh OTP works after resend

---

### Test 5: Invalid OTP Attempts

**Objective:** Test rate limiting on wrong OTP attempts

**Steps:**
1. Register new user
2. **Attempt verification with wrong OTP 3 times:**
   ```bash
   # Attempt 1
   curl -X POST http://localhost:8080/api/v1/auth/verify-email ^
     -H "Content-Type: application/json" ^
     -d "{\"email\":\"test@example.com\",\"otp\":\"000000\"}"
   
   # Attempt 2
   curl -X POST http://localhost:8080/api/v1/auth/verify-email ^
     -H "Content-Type: application/json" ^
     -d "{\"email\":\"test@example.com\",\"otp\":\"111111\"}"
   
   # Attempt 3
   curl -X POST http://localhost:8080/api/v1/auth/verify-email ^
     -H "Content-Type: application/json" ^
     -d "{\"email\":\"test@example.com\",\"otp\":\"222222\"}"
   ```

3. **Expected:** Each attempt returns `400 Bad Request`

4. **Attempt 4** - should trigger rate limit:
   ```json
   {
     "error": "TOO_MANY_REQUESTS",
     "message": "Too many verification attempts. Please try again later."
   }
   ```

**Success Criteria:**
- ✅ Wrong OTPs rejected
- ✅ Rate limit triggered after 3 attempts
- ✅ User must wait before trying again

---

### Test 6: Email Already Verified

**Objective:** Prevent duplicate verification

**Steps:**
1. Create and verify a user
2. **Attempt to verify again:**
   ```bash
   curl -X POST http://localhost:8080/api/v1/auth/send-verification-email ^
     -H "Content-Type: application/json" ^
     -d "{\"email\":\"test@example.com\"}"
   ```

3. **Expected Response:** `409 Conflict`
   ```json
   {
     "error": "EMAIL_ALREADY_VERIFIED",
     "message": "Email address is already verified"
   }
   ```

**Success Criteria:**
- ✅ Already verified emails cannot be re-verified
- ✅ Proper error message returned

---

### Test 7: Email Template Rendering

**Objective:** Verify email HTML renders correctly across clients

**Test in Multiple Email Clients:**
- Gmail (Web)
- Gmail (Mobile App)
- Outlook (Web)
- Outlook (Desktop)
- Apple Mail
- Thunderbird

**Check:**
- ✅ Logo displays correctly
- ✅ OTP box is prominent and readable
- ✅ Button renders correctly
- ✅ Link is clickable
- ✅ Colors match brand (#4f46e5 primary)
- ✅ Responsive on mobile
- ✅ Plain text fallback works
- ✅ No broken images

---

### Test 8: Resend API Integration

**Objective:** Verify Resend API integration and error handling

**Steps:**
1. **Check Resend Dashboard:** https://resend.com/emails
2. **Verify sent emails appear in dashboard**
3. **Check delivery status** (sent, delivered, bounced, failed)
4. **Test with invalid API key** (temporarily change in .env):
   ```
   RESEND_API_KEY=invalid_key
   ```
5. **Attempt registration** - backend should log error but not crash
6. **Restore correct API key**

**Success Criteria:**
- ✅ All emails appear in Resend dashboard
- ✅ Delivery status is "delivered"
- ✅ Invalid API key handled gracefully
- ✅ Email failure doesn't block user registration

---

## Edge Cases

### Edge Case 1: Email Case Sensitivity
```bash
# Register with uppercase email
curl -X POST http://localhost:8080/api/v1/auth/register ^
  -d "{\"email\":\"TEST@EXAMPLE.COM\",\"username\":\"testuser\",\"password\":\"Test@123\"}"

# Verify with lowercase email
curl -X POST http://localhost:8080/api/v1/auth/verify-email ^
  -d "{\"email\":\"test@example.com\",\"otp\":\"<otp>\"}"
```
**Expected:** Should work (emails are case-insensitive)

### Edge Case 2: Special Characters in Name
```bash
curl -X POST http://localhost:8080/api/v1/auth/register ^
  -d "{\"email\":\"test@example.com\",\"username\":\"test_user\",\"fullName\":\"John O'Brien <test@test>\",\"password\":\"Test@123\"}"
```
**Expected:** HTML escaped properly in email (no XSS)

### Edge Case 3: Very Long Names
```bash
curl -X POST http://localhost:8080/api/v1/auth/register ^
  -d "{\"email\":\"test@example.com\",\"fullName\":\"$(printf 'A%.0s' {1..1000})\",\"password\":\"Test@123\"}"
```
**Expected:** Validation error or truncated gracefully

---

## Performance Tests

### Test P1: Email Send Time
**Objective:** Measure email delivery speed

**Method:**
1. Register user
2. Record timestamp of API response
3. Check email inbox timestamp
4. Calculate: `delivery_time = inbox_timestamp - api_response_timestamp`

**Target:** < 5 seconds

### Test P2: Concurrent Registrations
**Objective:** Test email service under load

**Method:**
1. Register 100 users simultaneously
2. Verify all receive emails
3. Check Resend rate limits

**Target:** All emails delivered, no API errors

---

## Security Tests

### Security Test 1: OTP Brute Force
**Objective:** Verify OTP cannot be brute forced

**Steps:**
1. Register user
2. Attempt 1000 random OTPs
3. **Expected:** Rate limit after 3 attempts

### Security Test 2: Email Injection
**Objective:** Prevent email header injection

**Steps:**
1. Attempt registration with malicious email:
   ```
   test@example.com%0ACc:attacker@evil.com
   ```
2. **Expected:** Validation error or email sanitization

### Security Test 3: XSS in Email
**Objective:** Prevent XSS in email templates

**Steps:**
1. Register with name: `<script>alert('xss')</script>`
2. Check received email HTML
3. **Expected:** Script tags HTML-escaped

---

## Monitoring & Logging

### Backend Logs to Check

```bash
# Start backend with debug logging
LOG_LEVEL=DEBUG .\mvnw.cmd spring-boot:run

# Watch for email logs
tail -f logs/ziboto.log | findstr /i "email"
```

**Expected Log Entries:**
```
2026-08-11 INFO  - Email verification requested for: test@example.com
2026-08-11 INFO  - OTP generated for verification: test@example.com
2026-08-11 INFO  - Email sent [type=email-verification, to=test@example.com, id=abc123]
2026-08-11 INFO  - Email verified successfully: test@example.com
2026-08-11 INFO  - Email sent [type=welcome, to=test@example.com, id=def456]
```

**Error Cases:**
```
2026-08-11 ERROR - Failed to send email-verification email to test@example.com: API key invalid
```

---

## Automated Test Script

**Location:** `apps/backend/src/test/java/com/ziboto/backend/email/EmailIntegrationTest.java`

**Run:**
```bash
cd apps/backend
.\mvnw.cmd test -Dtest=EmailIntegrationTest
```

---

## Test Results Template

| Test Case | Status | Notes | Date |
|-----------|--------|-------|------|
| Test 1: Registration Email | ⏳ | | |
| Test 2: Resend Email | ⏳ | | |
| Test 3: Password Reset | ⏳ | | |
| Test 4: OTP Expiration | ⏳ | | |
| Test 5: Invalid Attempts | ⏳ | | |
| Test 6: Already Verified | ⏳ | | |
| Test 7: Template Rendering | ⏳ | | |
| Test 8: Resend Integration | ⏳ | | |

**Legend:**
- ⏳ Not Started
- 🔄 In Progress
- ✅ Passed
- ❌ Failed

---

## Resend Dashboard Verification

After running tests, verify in Resend dashboard:

1. Go to https://resend.com/emails
2. Check **Sent Emails** tab
3. Verify:
   - All test emails appear
   - Status: "delivered"
   - No bounces or failures
   - Correct templates rendered
   - Correct "From" header

---

## Next Steps

1. **Run Tests:** Execute all test scenarios manually
2. **Document Results:** Fill in test results table
3. **Fix Issues:** Address any failures found
4. **Automated Tests:** Create integration test suite
5. **Load Testing:** Include email tests in k6 scripts

---

**Status:** Ready for execution  
**Resend API Key:** Configured ✅  
**From Email Domain:** Verified ✅  
**Email Templates:** Implemented ✅

