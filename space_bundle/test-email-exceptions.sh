#!/bin/bash

# Email Exception Testing Script
# Tests email sending when exceptions occur in Space Bundle webhook processing

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
API_BASE_URL="${API_URL:-http://localhost:8080}"
WEBHOOK_ENDPOINT="/webhooks/paystack"
VALID_SIGNATURE="test_signature_123"
INVALID_SIGNATURE="invalid_sig_abc"

echo -e "${BLUE}╔════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║    Space Bundle Email Exception Testing Script                 ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════════════╝${NC}\n"

# Check if server is running
echo -e "${YELLOW}[*] Checking if server is running on ${API_BASE_URL}...${NC}"
if ! curl -s "${API_BASE_URL}/webhooks/paystack" > /dev/null 2>&1; then
    echo -e "${RED}[✗] Server is not running on ${API_BASE_URL}${NC}"
    echo -e "${YELLOW}[*] Please start the server with: mvn spring-boot:run${NC}"
    exit 1
fi
echo -e "${GREEN}[✓] Server is running on ${API_BASE_URL}${NC}\n"

# Test 1: Invalid Signature (Should trigger email)
test_invalid_signature() {
    echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}TEST 1: Invalid Webhook Signature${NC}"
    echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
    
    local payload='{
        "event": "charge.success",
        "data": {
            "reference": "TEST_INVALID_SIG_001",
            "status": "success",
            "amount": 10000,
            "customer": {
                "email": "test@example.com"
            }
        }
    }'
    
    echo -e "\n${YELLOW}[*] Sending webhook with invalid signature...${NC}"
    echo -e "${YELLOW}[*] Endpoint${NC}: POST ${API_BASE_URL}${WEBHOOK_ENDPOINT}"
    echo -e "${YELLOW}[*] Payload${NC}:\n$payload\n"
    
    response=$(curl -s -w "\n%{http_code}" -X POST \
        "${API_BASE_URL}${WEBHOOK_ENDPOINT}" \
        -H 'Content-Type: application/json' \
        -H "x-paystack-signature: ${INVALID_SIGNATURE}" \
        -d "$payload")
    
    http_code=$(echo "$response" | tail -n 1)
    body=$(echo "$response" | head -n -1)
    
    echo -e "${YELLOW}[*] Response Code${NC}: $http_code"
    echo -e "${YELLOW}[*] Response Body${NC}: $body\n"
    
    if [ "$http_code" = "401" ]; then
        echo -e "${GREEN}[✓] Test passed! Invalid signature rejected (HTTP 401)${NC}"
        echo -e "${GREEN}[✓] Email notification should be sent to admin${NC}\n"
    else
        echo -e "${RED}[✗] Test failed! Expected HTTP 401, got $http_code${NC}\n"
    fi
}

# Test 2: Valid Signature, Invalid Payload (Should trigger email for processing error)
test_invalid_payload() {
    echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}TEST 2: Invalid Payload (Valid Signature)${NC}"
    echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
    
    local payload='{
        "event": "charge.invalid",
        "data": null
    }'
    
    echo -e "\n${YELLOW}[*] Sending webhook with valid signature but invalid payload...${NC}"
    echo -e "${YELLOW}[*] Payload${NC}:\n$payload\n"
    
    response=$(curl -s -w "\n%{http_code}" -X POST \
        "${API_BASE_URL}${WEBHOOK_ENDPOINT}" \
        -H 'Content-Type: application/json' \
        -H "x-paystack-signature: ${VALID_SIGNATURE}" \
        -d "$payload")
    
    http_code=$(echo "$response" | tail -n 1)
    body=$(echo "$response" | head -n -1)
    
    echo -e "${YELLOW}[*] Response Code${NC}: $http_code"
    echo -e "${YELLOW}[*] Response Body${NC}: $body\n"
    
    if [ "$http_code" = "500" ]; then
        echo -e "${GREEN}[✓] Test passed! Invalid payload handled (HTTP 500)${NC}"
        echo -e "${GREEN}[✓] Email notification should be sent for processing error${NC}\n"
    else
        echo -e "${YELLOW}[!] Response: HTTP $http_code${NC}\n"
    fi
}

# Test 3: Null Payload
test_null_payload() {
    echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}TEST 3: Null/Empty Payload${NC}"
    echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
    
    echo -e "\n${YELLOW}[*] Sending webhook with empty payload...${NC}"
    
    response=$(curl -s -w "\n%{http_code}" -X POST \
        "${API_BASE_URL}${WEBHOOK_ENDPOINT}" \
        -H 'Content-Type: application/json' \
        -H "x-paystack-signature: ${VALID_SIGNATURE}" \
        -d '')
    
    http_code=$(echo "$response" | tail -n 1)
    body=$(echo "$response" | head -n -1)
    
    echo -e "${YELLOW}[*] Response Code${NC}: $http_code"
    echo -e "${YELLOW}[*] Response Body${NC}: $body\n"
    
    if [ "$http_code" = "400" ] || [ "$http_code" = "500" ]; then
        echo -e "${GREEN}[✓] Test passed! Empty payload handled${NC}"
        echo -e "${GREEN}[✓] Email notification should be sent${NC}\n"
    else
        echo -e "${YELLOW}[!] Response: HTTP $http_code${NC}\n"
    fi
}

# Test 4: Valid Webhook (Should not trigger exception email)
test_valid_webhook() {
    echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}TEST 4: Valid Webhook (No Exception Expected)${NC}"
    echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
    
    local payload='{
        "event": "charge.success",
        "data": {
            "reference": "TEST_VALID_001",
            "status": "success",
            "amount": 10000,
            "customer": {
                "email": "user@example.com"
            }
        }
    }'
    
    echo -e "\n${YELLOW}[*] Sending valid webhook...${NC}"
    echo -e "${YELLOW}[*] Payload${NC}:\n$payload\n"
    
    response=$(curl -s -w "\n%{http_code}" -X POST \
        "${API_BASE_URL}${WEBHOOK_ENDPOINT}" \
        -H 'Content-Type: application/json' \
        -H "x-paystack-signature: ${VALID_SIGNATURE}" \
        -d "$payload")
    
    http_code=$(echo "$response" | tail -n 1)
    body=$(echo "$response" | head -n -1)
    
    echo -e "${YELLOW}[*] Response Code${NC}: $http_code"
    echo -e "${YELLOW}[*] Response Body${NC}: $body\n"
    
    if [ "$http_code" = "200" ]; then
        echo -e "${GREEN}[✓] Test passed! Valid webhook accepted (HTTP 200)${NC}"
        echo -e "${GREEN}[✓] No exception email should be triggered${NC}\n"
    else
        echo -e "${YELLOW}[!] Response: HTTP $http_code (Order may fail due to test reference)${NC}\n"
    fi
}

# Test 5: Multiple rapid requests (Concurrency test)
test_concurrent_requests() {
    echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}TEST 5: Concurrent Invalid Signature Requests${NC}"
    echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
    
    echo -e "\n${YELLOW}[*] Sending 3 concurrent requests with invalid signature...${NC}"
    
    for i in {1..3}; do
        local payload="{\"event\":\"charge.success\",\"data\":{\"reference\":\"CONCURRENT_$i\"}}"
        
        (
            response=$(curl -s -w "\n%{http_code}" -X POST \
                "${API_BASE_URL}${WEBHOOK_ENDPOINT}" \
                -H 'Content-Type: application/json' \
                -H "x-paystack-signature: ${INVALID_SIGNATURE}" \
                -d "$payload")
            
            http_code=$(echo "$response" | tail -n 1)
            echo -e "${YELLOW}[Request $i]${NC} HTTP $http_code"
        ) &
    done
    
    wait
    
    echo -e "\n${GREEN}[✓] All concurrent requests completed${NC}"
    echo -e "${GREEN}[✓] Multiple email notifications should be queued${NC}\n"
}

# Summary Report
print_summary() {
    echo -e "\n${BLUE}═══════════════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}Test Summary${NC}"
    echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}\n"
    
    echo -e "${YELLOW}[*] Check the following to verify email sending:${NC}"
    echo -e "    1. Gmail inbox: nictech23@gmail.com"
    echo -e "    2. Server logs in space_bundle console"
    echo -e "    3. Check timestamps match test execution times\n"
    
    echo -e "${YELLOW}[*] Expected Emails:${NC}"
    echo -e "    - Test 1 & 5: 'Webhook Signature Verification Failed'"
    echo -e "    - Test 2: 'Webhook Processing Exception'"
    echo -e "    - Test 3: 'Webhook Processing Exception' or similar"
    echo -e "    - Test 4: No exception email expected\n"
    
    echo -e "${YELLOW}[*] Server Logs should contain:${NC}"
    echo -e "    - ERROR: Signature verification failed"
    echo -e "    - emailPort.sendEmail() called with exception details"
    echo -e "    - Response codes in order: 401, 500, 400/500, 200, 401x3\n"
}

# Main execution
main() {
    test_invalid_signature
    sleep 1  # Brief pause between tests
    
    test_invalid_payload
    sleep 1
    
    test_null_payload
    sleep 1
    
    test_valid_webhook
    sleep 1
    
    test_concurrent_requests
    
    print_summary
    
    echo -e "${BLUE}╔════════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║           Testing Complete! Check your email inbox             ║${NC}"
    echo -e "${BLUE}╚════════════════════════════════════════════════════════════════╝${NC}\n"
}

# Run main function
main
