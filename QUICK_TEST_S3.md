# QUICK S3 INTEGRATION TEST GUIDE

## ✅ CURRENT STATUS
- **Backend:** Running on port 8080 ✅
- **S3 Service:** Initialized and verified ✅
- **Database:** Connected ✅
- **Redis:** Connected ✅

---

## 🧪 MANUAL TEST STEPS

### Option 1: PowerShell Script (Recommended)
```powershell
# Run the test script
powershell -ExecutionPolicy Bypass -File "d:\Projects\Ziboto\test-s3-with-existing-user.ps1"

# You'll be prompted for the password of user: rakinmohammedrafeeq
# (The user that's already in your database)
```

**What the script does:**
1. Logs in with existing verified user
2. Uploads `test-file.txt` to S3
3. Downloads the file from S3
4. Retrieves file metadata
5. Shows S3 storage key for verification

---

### Option 2: Manual cURL Commands

#### Step 1: Login
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"rakinmohammedrafeeq@gmail.com","password":"YOUR_PASSWORD"}'
```

Save the `accessToken` from the response.

#### Step 2: Upload File to S3
```bash
curl -X POST http://localhost:8080/api/v1/files/upload \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -F "file=@d:\Projects\Ziboto\test-file.txt"
```

Save the `fileId` and `storageKey` from the response.

#### Step 3: Download File from S3
```bash
curl -X GET http://localhost:8080/api/v1/files/{FILE_ID}/download \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -o downloaded-file.txt
```

#### Step 4: Get File Metadata
```bash
curl -X GET http://localhost:8080/api/v1/files/{FILE_ID} \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

---

## 🔍 VERIFICATION CHECKLIST

### Backend Logs to Check
```bash
# In the terminal where backend is running, look for:

✅ "Uploading to S3: user=1, file=test-file.txt, size=..."
✅ "Successfully uploaded to S3: key=files/user-1/..."
✅ "Downloading from S3: key=files/user-1/..."
✅ "File downloaded successfully"
```

### AWS S3 Console Verification
1. Go to: https://s3.console.aws.amazon.com/s3/buckets/ziboto-files-277522752099-eu-north-1-an
2. Region: eu-north-1
3. Look for: `files/user-1/{UUID}/test-file.txt`
4. Verify:
   - File exists ✅
   - File size matches ✅
   - Metadata includes user-id, file-id ✅
   - Encryption: AES256 ✅

### Expected S3 Key Format
```
files/user-1/550e8400-e29b-41d4-a716-446655440000/test-file.txt
     │      │                                      │
     │      │                                      └─ Sanitized filename
     │      └─ UUID (unique file identifier)
     └─ User ID (authorization boundary)
```

---

## ✅ SUCCESS INDICATORS

### Upload Success
```json
{
  "success": true,
  "data": {
    "fileId": "550e8400-e29b-41d4-a716-446655440000",
    "fileName": "test-file.txt",
    "fileSize": 234,
    "formattedFileSize": "234 B",
    "mimeType": "text/plain",
    "sha256Hash": "abc123...",
    "storageKey": "files/user-1/550e8400.../test-file.txt",
    "uploadedAt": "2026-08-11T13:20:00"
  }
}
```

### Download Success
- File downloads without errors
- Content matches original file
- No "file not found" errors

### S3 Console Success
- File visible in bucket
- Correct path: `files/user-{id}/{uuid}/{filename}`
- Metadata tags present
- AES256 encryption enabled

---

## 🐛 TROUBLESHOOTING

### "S3 bucket access denied"
**Fix:** Check AWS credentials
```bash
# Verify credentials are configured
aws s3 ls s3://ziboto-files-277522752099-eu-north-1-an --region eu-north-1

# If fails, configure AWS CLI
aws configure
```

### "File upload failed"
**Check:**
1. Backend logs for detailed error
2. File size (max 100MB for single upload)
3. Authorization token is valid
4. User is authenticated

### "File not found in S3"
**Possible causes:**
1. Upload didn't complete
2. Wrong storage key
3. File deleted
4. S3 permissions issue

**Debug:**
```bash
# Check backend logs
# Look for "Successfully uploaded to S3: key=..."

# Verify in S3
aws s3 ls s3://ziboto-files-277522752099-eu-north-1-an/files/ --recursive --region eu-north-1
```

---

## 📊 PERFORMANCE METRICS TO NOTE

During testing, observe:
- **Upload time:** Should be fast for small files (<1s for <1MB)
- **Download time:** Should stream immediately
- **Memory usage:** Should NOT spike (streaming in action)
- **Error rate:** Should be 0%

---

## 🔄 BACKEND RESTART (If Needed)

If you need to restart the backend:

```powershell
# Stop current process (Terminal ID: 2)
# Press Ctrl+C in the terminal where it's running

# Or use Kiro to stop it
control_pwsh_process(action="stop", terminalId="2")

# Start again
cd d:\Projects\Ziboto\apps\backend
.\mvnw.cmd spring-boot:run
```

Wait for: "S3 storage service initialized successfully"

---

## 📝 TEST RESULTS TEMPLATE

Copy this and fill in your results:

```
=== S3 INTEGRATION TEST RESULTS ===

Date: August 11, 2026
Tester: [Your Name]

1. Backend Startup
   Status: [ ] Success [ ] Failed
   S3 Init: [ ] Success [ ] Failed
   
2. File Upload
   Status: [ ] Success [ ] Failed
   File ID: _______________________
   Storage Key: ____________________
   Upload Time: ______ ms
   
3. File Download
   Status: [ ] Success [ ] Failed
   Content Match: [ ] Yes [ ] No
   Download Time: ______ ms
   
4. S3 Console Verification
   File Found: [ ] Yes [ ] No
   Encryption: [ ] AES256 [ ] None
   Metadata: [ ] Present [ ] Missing
   
5. Issues Encountered
   [List any problems]

6. Overall Assessment
   [ ] PASS - Ready for production
   [ ] FAIL - Needs fixes
   
Notes:
_________________________________
```

---

## 🎯 NEXT STEPS AFTER TESTING

### If Tests Pass ✅
1. Document test results
2. Move to frontend integration
3. Write automated tests
4. Start Docker deployment setup

### If Tests Fail ❌
1. Document specific errors
2. Check backend logs
3. Verify AWS credentials
4. Check S3 bucket permissions
5. Fix issues and retest

---

## 📞 GETTING HELP

If you encounter issues:
1. Check backend logs in terminal
2. Check AWS CloudWatch logs
3. Verify IAM permissions
4. Review this troubleshooting guide
5. Check SESSION_SUMMARY_S3_INTEGRATION.md for implementation details

---

**Ready to test?** Run this command:
```powershell
powershell -ExecutionPolicy Bypass -File "d:\Projects\Ziboto\test-s3-with-existing-user.ps1"
```

Good luck! 🚀
