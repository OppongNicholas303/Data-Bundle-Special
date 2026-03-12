# Email Exception Testing Script (PowerShell)
# Tests email sending when exceptions occur in Space Bundle webhook processing

# Color definitions
$Green = @{ ForegroundColor = "Green" }
$Red = @{ ForegroundColor = "Red" }
$Yellow = @{ ForegroundColor = "Yellow" }
$Blue = @{ ForegroundColor = "Cyan" }
$Separator = "=" * 70

# Configuration
$ApiBaseUrl = $env:API_URL -if $env:API_URL else "http://localhost:8080"
$WebhookEndpoint = "/webhooks/paystack"
$ValidSignature = "test_signature_123"
$InvalidSignature = "invalid_sig_abc"

Write-Host "╔════════════════════════════════════════════════════════════════╗" @Blue
Write-Host "║    Space Bundle Email Exception Testing Script (PowerShell)    ║" @Blue
Write-Host "╚════════════════════════════════════════════════════════════════╝" @Blue
Write-Host ""

# Function to check if server is running
function Test-ServerRunning {
    Write-Host "[*] Checking if server is running on ${ApiBaseUrl}..." @Yellow
    
    try {
        $response = Invoke-WebRequest -Uri "${ApiBaseUrl}/webhooks/paystack" -Method POST -ErrorAction SilentlyContinue
        Write-Host "[✓] Server is running on ${ApiBaseUrl}" @Green
        return $true
    }
    catch {
        Write-Host "[✗] Server is not running on ${ApiBaseUrl}" @Red
        Write-Host "[*] Please start the server with: mvn spring-boot:run" @Yellow
        return $false
    }
}

# Test 1: Invalid Signature (Should trigger email)
function Test-InvalidSignature {
    Write-Host ""
    Write-Host $Separator @Blue
    Write-Host "TEST 1: Invalid Webhook Signature" @Blue
    Write-Host $Separator @Blue
    Write-Host ""
    
    $payload = @{
        event = "charge.success"
        data = @{
            reference = "TEST_INVALID_SIG_001"
            status = "success"
            amount = 10000
            customer = @{
                email = "test@example.com"
            }
        }
    } | ConvertTo-Json
    
    Write-Host "[*] Sending webhook with invalid signature..." @Yellow
    Write-Host "[*] Endpoint: POST ${ApiBaseUrl}${WebhookEndpoint}" @Yellow
    Write-Host "[*] Payload:" @Yellow
    Write-Host $payload
    Write-Host ""
    
    try {
        $response = Invoke-WebRequest -Uri "${ApiBaseUrl}${WebhookEndpoint}" `
            -Method POST `
            -Headers @{ "x-paystack-signature" = $InvalidSignature; "Content-Type" = "application/json" } `
            -Body $payload `
            -ErrorAction SilentlyContinue
        
        $httpCode = $response.StatusCode
    }
    catch {
        $httpCode = $_.Exception.Response.StatusCode.Value__
    }
    
    Write-Host "[*] Response Code: $httpCode" @Yellow
    
    if ($httpCode -eq 401) {
        Write-Host "[✓] Test passed! Invalid signature rejected (HTTP 401)" @Green
        Write-Host "[✓] Email notification should be sent to admin" @Green
    }
    else {
        Write-Host "[!] Unexpected response: HTTP $httpCode" @Yellow
    }
    Write-Host ""
}

# Test 2: Invalid Payload (Valid Signature)
function Test-InvalidPayload {
    Write-Host ""
    Write-Host $Separator @Blue
    Write-Host "TEST 2: Invalid Payload (Valid Signature)" @Blue
    Write-Host $Separator @Blue
    Write-Host ""
    
    $payload = @{
        event = "charge.invalid"
        data = $null
    } | ConvertTo-Json
    
    Write-Host "[*] Sending webhook with valid signature but invalid payload..." @Yellow
    Write-Host "[*] Payload:" @Yellow
    Write-Host $payload
    Write-Host ""
    
    try {
        $response = Invoke-WebRequest -Uri "${ApiBaseUrl}${WebhookEndpoint}" `
            -Method POST `
            -Headers @{ "x-paystack-signature" = $ValidSignature; "Content-Type" = "application/json" } `
            -Body $payload `
            -ErrorAction SilentlyContinue
        
        $httpCode = $response.StatusCode
    }
    catch {
        $httpCode = $_.Exception.Response.StatusCode.Value__
    }
    
    Write-Host "[*] Response Code: $httpCode" @Yellow
    
    if ($httpCode -eq 500) {
        Write-Host "[✓] Test passed! Invalid payload handled (HTTP 500)" @Green
        Write-Host "[✓] Email notification should be sent for processing error" @Green
    }
    else {
        Write-Host "[!] Response: HTTP $httpCode" @Yellow
    }
    Write-Host ""
}

# Test 3: Empty Payload
function Test-EmptyPayload {
    Write-Host ""
    Write-Host $Separator @Blue
    Write-Host "TEST 3: Empty/Null Payload" @Blue
    Write-Host $Separator @Blue
    Write-Host ""
    
    Write-Host "[*] Sending webhook with empty payload..." @Yellow
    
    try {
        $response = Invoke-WebRequest -Uri "${ApiBaseUrl}${WebhookEndpoint}" `
            -Method POST `
            -Headers @{ "x-paystack-signature" = $ValidSignature; "Content-Type" = "application/json" } `
            -Body "" `
            -ErrorAction SilentlyContinue
        
        $httpCode = $response.StatusCode
    }
    catch {
        $httpCode = $_.Exception.Response.StatusCode.Value__
    }
    
    Write-Host "[*] Response Code: $httpCode" @Yellow
    
    if ($httpCode -eq 400 -or $httpCode -eq 500) {
        Write-Host "[✓] Test passed! Empty payload handled" @Green
        Write-Host "[✓] Email notification should be sent" @Green
    }
    else {
        Write-Host "[!] Response: HTTP $httpCode" @Yellow
    }
    Write-Host ""
}

# Test 4: Valid Webhook
function Test-ValidWebhook {
    Write-Host ""
    Write-Host $Separator @Blue
    Write-Host "TEST 4: Valid Webhook (No Exception Expected)" @Blue
    Write-Host $Separator @Blue
    Write-Host ""
    
    $payload = @{
        event = "charge.success"
        data = @{
            reference = "TEST_VALID_001"
            status = "success"
            amount = 10000
            customer = @{
                email = "user@example.com"
            }
        }
    } | ConvertTo-Json
    
    Write-Host "[*] Sending valid webhook..." @Yellow
    Write-Host "[*] Payload:" @Yellow
    Write-Host $payload
    Write-Host ""
    
    try {
        $response = Invoke-WebRequest -Uri "${ApiBaseUrl}${WebhookEndpoint}" `
            -Method POST `
            -Headers @{ "x-paystack-signature" = $ValidSignature; "Content-Type" = "application/json" } `
            -Body $payload `
            -ErrorAction SilentlyContinue
        
        $httpCode = $response.StatusCode
    }
    catch {
        $httpCode = $_.Exception.Response.StatusCode.Value__
    }
    
    Write-Host "[*] Response Code: $httpCode" @Yellow
    
    if ($httpCode -eq 200) {
        Write-Host "[✓] Test passed! Valid webhook accepted (HTTP 200)" @Green
        Write-Host "[✓] No exception email should be triggered" @Green
    }
    else {
        Write-Host "[!] Response: HTTP $httpCode (Order may fail due to test reference)" @Yellow
    }
    Write-Host ""
}

# Test 5: Concurrent Requests
function Test-ConcurrentRequests {
    Write-Host ""
    Write-Host $Separator @Blue
    Write-Host "TEST 5: Concurrent Invalid Signature Requests" @Blue
    Write-Host $Separator @Blue
    Write-Host ""
    
    Write-Host "[*] Sending 3 concurrent requests with invalid signature..." @Yellow
    
    $jobs = @()
    for ($i = 1; $i -le 3; $i++) {
        $payload = @{
            event = "charge.success"
            data = @{
                reference = "CONCURRENT_$i"
            }
        } | ConvertTo-Json
        
        $jobs += Start-Job -ScriptBlock {
            param($url, $sig, $body)
            try {
                $response = Invoke-WebRequest -Uri $url `
                    -Method POST `
                    -Headers @{ "x-paystack-signature" = $sig; "Content-Type" = "application/json" } `
                    -Body $body `
                    -ErrorAction SilentlyContinue
                return $response.StatusCode
            }
            catch {
                return $_.Exception.Response.StatusCode.Value__
            }
        } -ArgumentList "${ApiBaseUrl}${WebhookEndpoint}", $InvalidSignature, $payload
    }
    
    # Wait for all jobs
    $results = $jobs | Wait-Job | Receive-Job
    
    Write-Host "[Request 1] HTTP $($results[0])" @Yellow
    Write-Host "[Request 2] HTTP $($results[1])" @Yellow
    Write-Host "[Request 3] HTTP $($results[2])" @Yellow
    
    Write-Host "[✓] All concurrent requests completed" @Green
    Write-Host "[✓] Multiple email notifications should be queued" @Green
    Write-Host ""
}

# Print Summary
function Print-Summary {
    Write-Host ""
    Write-Host $Separator @Blue
    Write-Host "Test Summary" @Blue
    Write-Host $Separator @Blue
    Write-Host ""
    
    Write-Host "[*] Check the following to verify email sending:" @Yellow
    Write-Host "    1. Gmail inbox: nictech23@gmail.com" 
    Write-Host "    2. Server logs in space_bundle console"
    Write-Host "    3. Check timestamps match test execution times"
    Write-Host ""
    
    Write-Host "[*] Expected Emails:" @Yellow
    Write-Host "    - Test 1 & 5: 'Webhook Signature Verification Failed'"
    Write-Host "    - Test 2: 'Webhook Processing Exception'"
    Write-Host "    - Test 3: 'Webhook Processing Exception' or similar"
    Write-Host "    - Test 4: No exception email expected"
    Write-Host ""
    
    Write-Host "[*] Server Logs should contain:" @Yellow
    Write-Host "    - ERROR: Signature verification failed"
    Write-Host "    - emailPort.sendEmail() called with exception details"
    Write-Host "    - Response codes expected: 401, 500, 400/500, 200, 401x3"
    Write-Host ""
}

# Main execution
function Main {
    if (-not (Test-ServerRunning)) {
        exit 1
    }
    
    Write-Host ""
    Test-InvalidSignature
    Start-Sleep -Seconds 1
    
    Test-InvalidPayload
    Start-Sleep -Seconds 1
    
    Test-EmptyPayload
    Start-Sleep -Seconds 1
    
    Test-ValidWebhook
    Start-Sleep -Seconds 1
    
    Test-ConcurrentRequests
    
    Print-Summary
    
    Write-Host "╔════════════════════════════════════════════════════════════════╗" @Blue
    Write-Host "║           Testing Complete! Check your email inbox             ║" @Blue
    Write-Host "╚════════════════════════════════════════════════════════════════╝" @Blue
    Write-Host ""
}

# Run main
Main
