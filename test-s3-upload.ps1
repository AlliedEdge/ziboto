# Test S3 File Upload Script

$baseUrl = "http://localhost:8080/api/v1"

# Step 1: Register a new user
Write-Host "=== Step 1: Register User ===" -ForegroundColor Cyan
$registerBody = @{
    username = "s3testuser"
    firstName = "Test"
    lastName = "User"
    email = "s3test@ziboto.com"
    password = "TestPassword123!"
} | ConvertTo-Json

try {
    $registerResponse = Invoke-RestMethod -Uri "$baseUrl/auth/register" -Method POST -Body $registerBody -ContentType "application/json"
    Write-Host "User registered successfully!" -ForegroundColor Green
    Write-Host "Response: $($registerResponse | ConvertTo-Json -Depth 10)" -ForegroundColor Gray
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    Write-Host "Register failed with status: $statusCode" -ForegroundColor Yellow
    
    if ($statusCode -eq 409) {
        Write-Host "User already exists, trying to login..." -ForegroundColor Yellow
    } else {
        $errorBody = $_.ErrorDetails.Message
        Write-Host "Error: $errorBody" -ForegroundColor Red
    }
}

# Step 2: Login
Write-Host "`n=== Step 2: Login ===" -ForegroundColor Cyan
$loginBody = @{
    usernameOrEmail = "s3test@ziboto.com"
    password = "TestPassword123!"
} | ConvertTo-Json

try {
    $loginResponse = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method POST -Body $loginBody -ContentType "application/json"
    Write-Host "Login successful!" -ForegroundColor Green
    $accessToken = $loginResponse.accessToken
    Write-Host "Access Token: $($accessToken.Substring(0, [Math]::Min(50, $accessToken.Length)))..." -ForegroundColor Gray
} catch {
    Write-Host "Login failed: $($_.Exception.Message)" -ForegroundColor Red
    $errorBody = $_.ErrorDetails.Message
    Write-Host "Error details: $errorBody" -ForegroundColor Red
    exit 1
}

# Step 3: Upload file to S3
Write-Host "`n=== Step 3: Upload File to S3 ===" -ForegroundColor Cyan
$testFilePath = "d:\Projects\Ziboto\test-file.txt"

if (-not (Test-Path $testFilePath)) {
    Write-Host "Test file not found: $testFilePath" -ForegroundColor Red
    exit 1
}

$headers = @{
    "Authorization" = "Bearer $accessToken"
}

$fileContent = Get-Content $testFilePath -Raw
$boundary = [System.Guid]::NewGuid().ToString()
$bodyLines = @(
    "--$boundary",
    'Content-Disposition: form-data; name="file"; filename="test-file.txt"',
    'Content-Type: text/plain',
    '',
    $fileContent,
    "--$boundary--"
) -join "`r`n"

try {
    $uploadResponse = Invoke-RestMethod -Uri "$baseUrl/files/upload" -Method POST -Headers $headers -Body $bodyLines -ContentType "multipart/form-data; boundary=$boundary"
    Write-Host "File uploaded successfully!" -ForegroundColor Green
    Write-Host "Response:" -ForegroundColor Gray
    Write-Host ($uploadResponse | ConvertTo-Json -Depth 10) -ForegroundColor Gray
    
    $fileId = $uploadResponse.fileId
    $storageKey = $uploadResponse.storageKey
    
    Write-Host "`nFile Details:" -ForegroundColor Cyan
    Write-Host "  File ID: $fileId" -ForegroundColor White
    Write-Host "  File Name: $($uploadResponse.fileName)" -ForegroundColor White
    Write-Host "  File Size: $($uploadResponse.formattedFileSize)" -ForegroundColor White
    Write-Host "  Storage Key: $storageKey" -ForegroundColor White
    Write-Host "  SHA-256: $($uploadResponse.sha256Hash)" -ForegroundColor White
    
    # Step 4: Download file from S3
    Write-Host "`n=== Step 4: Download File from S3 ===" -ForegroundColor Cyan
    try {
        $downloadPath = "d:\Projects\Ziboto\test-download.txt"
        Invoke-WebRequest -Uri "$baseUrl/files/$fileId/download" -Method GET -Headers $headers -OutFile $downloadPath
        Write-Host "File downloaded successfully to: $downloadPath" -ForegroundColor Green
        
        $downloadedContent = Get-Content $downloadPath -Raw
        Write-Host "`nDownloaded content:" -ForegroundColor Gray
        Write-Host $downloadedContent -ForegroundColor Gray
    } catch {
        Write-Host "Download failed: $($_.Exception.Message)" -ForegroundColor Red
    }
    
    # Step 5: Verify file in S3
    Write-Host "`n=== Step 5: Get File Metadata ===" -ForegroundColor Cyan
    try {
        $metadataResponse = Invoke-RestMethod -Uri "$baseUrl/files/$fileId" -Method GET -Headers $headers
        Write-Host "File metadata retrieved successfully!" -ForegroundColor Green
        Write-Host ($metadataResponse | ConvertTo-Json -Depth 10) -ForegroundColor Gray
    } catch {
        Write-Host "Metadata retrieval failed: $($_.Exception.Message)" -ForegroundColor Red
    }
    
} catch {
    Write-Host "Upload failed: $($_.Exception.Message)" -ForegroundColor Red
    $errorBody = $_.ErrorDetails.Message
    Write-Host "Error details: $errorBody" -ForegroundColor Red
    exit 1
}

Write-Host "`n=== S3 Integration Test Complete ===" -ForegroundColor Green
