# CheckerPort API

**Programmatic Access to WAEC Voucher Purchases & Instant Result Checking**

Base URL: `https://api.checkerport.com`

Prepared for: Amankwah Kwesi Emmanuel
Document type: Integration & Reference Guide
Version: 1.0

## Table of Contents

1. Getting Started
   1.1 Overview
   1.2 Base URL & Request Structure
   1.3 Authentication
   1.4 Key Rules
2. Response Behaviour
   2.1 Standard Response Format
   2.2 HTTP Status Codes
3. Key Concepts
   3.1 referenceId
   3.2 webhookCallbackUrl
4. Endpoints
   4.1 Buy Voucher — POST /voucher-sale/create
   4.2 Check Result — POST /arc/create
   4.3 Corrections API — POST /arc/correction
   4.4 Status Check — GET /status-check/{referenceId}
   4.5 Service Price — GET /service-price
5. Webhook Callbacks
   5.1 Security
   5.2 Behaviour
   5.3 serviceStatus Values
   5.4 Fallback (Status Check)
6. Error Handling
   6.1 Error Response Structure
   6.2 Common Error Codes

---

## 1. Getting Started

### 1.1 Overview

The CheckerPort API provides programmatic access to WAEC voucher purchases and instant result checking. It handles:

- Mobile money payments
- Voucher purchase
- Automatic (instant) result processing
- Transaction status tracking

You integrate, and CheckerPort handles the transaction lifecycle while you receive the results.

### 1.2 Base URL & Request Structure

All requests are made to:

`https://api.checkerport.com`

All endpoints use standard HTTP requests with JSON payloads where required.

### 1.3 Authentication

Authentication is required for every request. The API key must be included as a request header:

`x-api-key: YOUR_API_KEY`

**Example Request**

```
POST https://api.checkerport.com/voucher-sale/create
Content-Type: application/json
x-api-key: YOUR_API_KEY

{
  "platform": "PlatformWaecNew",
  "qty": 2,
  "phoneNumber": "024XXXXXXX",
  "price": 20,
  "webhookCallbackUrl": "https://yourapp.com/webhook",
  "referenceId": "abc123"
}
```

### 1.4 Key Rules

- All requests must use `https://api.checkerport.com`
- `x-api-key` must be sent as a request header
- All payloads must be valid JSON
- `referenceId` must be unique per request
- Webhook URL must be publicly accessible (http/https)

---

## 2. Response Behaviour

Every response follows the same `ApiResponse` structure. A 200 HTTP response does not always mean success — always check `status` and `errorCode`.

### 2.1 Standard Response Format

```typescript
type ApiResponse<T> = {
  status: "SUCCESS" | "FAILED" | "PENDING";
  message: string;
  data: T | null;
  errorCode: string | null;
  errors: null | Array<{
    field?: string;
    message: string;
  }>;
  meta: Record<string, any>;
};
```

| Field | Description |
|---|---|
| status | Overall request outcome |
| message | Human-readable description |
| data | Response payload |
| errorCode | Machine-readable error identifier |
| errors | Validation errors |
| meta | Additional metadata |

### 2.2 HTTP Status Codes

HTTP status codes represent the general outcome of a request.

| Code | Meaning |
|---|---|
| 200 | Request processed |
| 400 | Invalid input |
| 401 | Invalid or missing API key |
| 403 | Forbidden |
| 404 | Not found |
| 409 | Conflict |
| 422 | Validation failed |
| 500 | Internal server error |
| 503 | Service Unavailable |

**Important:** A 200 response does not always mean success. Always check `status` and `errorCode` in the response body.

---

## 3. Key Concepts

### 3.1 referenceId

A unique identifier for each request. Used for:

- Tracking transactions
- Status checks
- Webhook correlation

Must be unique per request.

### 3.2 webhookCallbackUrl

URL where final results are delivered.

- Must be http or https
- Called on success and failure
- May be called multiple times

### Endpoints Overview

| Endpoint | Description |
|---|---|
| POST /voucher-sale/create | Purchase WAEC vouchers |
| POST /arc/create | Check WAEC results |
| GET /status-check/{referenceId} | Check transaction status |
| GET /service-price | Get pricing |

---

## 4. Endpoints

### 4.1 Buy Voucher

This endpoint allows you to purchase WAEC vouchers, which can be used to check results on the official WAEC platforms:

- https://ghana.waecdirect.org
- https://eresults.waecgh.org

| | |
|---|---|
| API Endpoint | `https://api.checkerport.com/voucher-sale/create` |
| Method | POST |
| Content Type | JSON |

**Request Payload**

| Field | Type | Description |
|---|---|---|
| platform | Enum | Type of voucher to buy: `PlatformWaecNew`, `PlatformWaecOld` |
| qty | Integer | Quantity of vouchers to buy. Minimum: 1, maximum: 200 |
| phoneNumber | String | Phone number to send payment prompt to. Must be a registered Mobile Money number |
| price | Float | Amount to charge the customer. Consider CheckerPort's platform charges. Use the Service Price API to check rates beforehand |
| webhookCallbackUrl | String | URL where the final callback is sent after payment succeeds or fails |
| referenceId | String | Unique string used to identify the transaction, and for status checks |

The `referenceId` must never be duplicated for any transaction. A new request using the same `referenceId` will be rejected.

**platform Field Values**

| Value | Description |
|---|---|
| PlatformWaecNew | BECE vouchers |
| PlatformWaecOld | WASSCE vouchers |

**Sample Request Payload**

```json
{
  "platform": "PlatformWaecNew",
  "qty": 2,
  "phoneNumber": "024XXXXXXX",
  "price": 20,
  "webhookCallbackUrl": "https://yourapp.com/webhook",
  "referenceId": "abc123"
}
```

**Sample Response**

```json
{
  "status": "PENDING",
  "message": "Payment initiated",
  "data": {
    "referenceId": "abc123",
    "amount": 20,
    "networkName": "mtn-gh"
  }
}
```

You must make a Status Check API request if you do not receive a callback within 7 minutes.

### 4.2 Check Result

This endpoint allows you to use the Instant Result Checker. It automatically processes the request and returns result details and a link to download the result PDF(s).

| | |
|---|---|
| API Endpoint | `https://api.checkerport.com/arc/create` |
| Method | POST |
| Content Type | JSON |

**Request Payload**

| Field | Type | Description |
|---|---|---|
| type | Enum | Type of result to check: `Bece`, `WassceSchool`, `WasscePrivate`, `ShsPlacement` |
| indexNumber | String | The Index Number of the candidate the result is being checked for |
| phoneNumber | String | Phone number to send payment prompt to. Must be a registered Mobile Money number |
| price | Float | Amount to charge the customer. Use the Service Price API to check rates beforehand |
| webhookCallbackUrl | String | URL where the final callback is sent after payment succeeds or fails |
| referenceId | String | Unique string used to identify the transaction, and for status checks |
| year | String, Optional | Year of examination. Required for all result types except `ShsPlacement` |
| dob | String, Optional | Candidate date of birth (ISO 8601: YYYY-MM-DD). Required only for `WasscePrivate` and `ShsPlacement` |

**type Field Values**

| Value | Description |
|---|---|
| Bece | BECE result |
| WassceSchool | WASSCE (School) result |
| WasscePrivate | WASSCE (Private) result |
| ShsPlacement | SHS placement |

**Sample Request Payload**

```json
{
  "type": "WassceSchool",
  "indexNumber": "1234567890",
  "phoneNumber": "024XXXXXXX",
  "price": 15,
  "webhookCallbackUrl": "https://yourapp.com/webhook",
  "referenceId": "abc123",
  "year": "2023"
}
```

Response follows the same structure as the Voucher Purchase response.

### 4.3 Corrections API

The Corrections API allows you to update invalid details for an existing Instant Result Checker request. This is mainly used when a request enters the pending-input state due to invalid candidate information, such as:

- Incorrect index number
- Invalid date of birth
- Wrong examination year

Once the correction request is successful, processing resumes automatically.

| | |
|---|---|
| API Endpoint | `https://api.checkerport.com/arc/correction` |
| Method | POST |
| Content Type | JSON |

**Request Payload**

| Field | Type | Description |
|---|---|---|
| referenceId | String | The referenceId of the original request being corrected |
| indexNumber | String, Optional | Corrected candidate index number |
| dob | String, Optional | Corrected date of birth (ISO 8601: YYYY-MM-DD) |
| year | String, Optional | Corrected examination year |
| webhookCallbackUrl | String, Optional | New webhook callback URL |

**Notes**

- Only provide fields you want to update
- referenceId must belong to an existing Result Check request
- Once corrections are accepted, the request resumes processing automatically
- If webhookCallbackUrl is provided, it replaces the previously configured callback URL for that request, and subsequent webhook callbacks will be sent to the new URL.

**Sample Request Payload**

```json
{
  "referenceId": "abc123",
  "indexNumber": "1234567890",
  "webhookCallbackUrl": "https://yourapp.com/new-webhook",
  "year": "2023",
  "dob": "2006-03-04"
}
```

**Typical Flow**

1. Result Check request enters pending-input
2. Use statusCode to determine the issue
3. Submit corrected details using this endpoint
4. Request processing resumes
5. Receive updated webhook callback, or use the Status Check endpoint

### 4.4 Status Check

**Note:** Only use this endpoint if you do not receive a webhook callback within at least 7 minutes.

The Status Check endpoint allows you to retrieve the status of a previous request (Voucher Purchase or Result Check). Use this when:

- You do not receive a webhook response
- You want to re-confirm the final state of a transaction

| | |
|---|---|
| API Endpoint | `https://api.checkerport.com/status-check/{REFERENCE_ID}` |
| Method | GET |

**Request Parameters**

| Field | Type | Description |
|---|---|---|
| REFERENCE_ID | String | The referenceId of the transaction you are checking for |

**Sample Response (SHS Placement)**

```json
{
  "status": "SUCCESS",
  "message": "Request Successful",
  "data": {
    "vouchers": [],
    "result": {
      "type": "ShsPlacement",
      "indexNumber": "0123456789025",
      "dob": "2010-02-12",
      "placementDetails": {
        "candidateName": "JOHN LESLIE",
        "schoolPlaced": "PRESBY SENIOR HIGH",
        "programGiven": "GENERAL SCIENCE",
        "residency": "Boarding",
        "region": "Eastern",
        "pdfs": [
          { "url": "https://cdn.checkerport.com/file.pdf", "description": "Placement Information" },
          { "url": "https://cdn.checkerport.com/file.pdf", "description": "Enrolment Form" }
        ]
      }
    },
    "serviceStatus": "complete",
    "statusCode": null,
    "payment": {
      "originalAmount": 25,
      "serviceCharges": 0,
      "serviceCost": 9.59,
      "netAmount": 15.41
    },
    "referenceId": "01KPM88TQGTAHN37EJZ2YRE93D"
  },
  "errorCode": null,
  "errors": [],
  "meta": {}
}
```

**Sample Response (BECE Result)**

```json
{
  "status": "SUCCESS",
  "message": "Request Successful",
  "data": {
    "vouchers": [],
    "result": {
      "type": "Bece",
      "indexNumber": "01234567890",
      "year": "2025",
      "resultContent": {
        "subjects": [
          { "subject": "ENGLISH LANG.", "grade": "1", "remarks": "HIGHEST" }
        ],
        "pdfUrl": "https://cdn.checkerport.com/file.pdf"
      }
    },
    "serviceStatus": "complete",
    "statusCode": null,
    "payment": {
      "originalAmount": 25,
      "serviceCharges": 0,
      "serviceCost": 9.59,
      "netAmount": 15.41
    },
    "referenceId": "01KPM8A002331R0N57HNC71YW5"
  },
  "errorCode": null,
  "errors": [],
  "meta": {}
}
```

**data.serviceStatus Field Values**

| Value | Meaning |
|---|---|
| pending | Processing |
| pending-input | Correction required |
| complete | Finished |

**statusCode**

Explains why a request is in `pending-input`. Examples:

- InvalidIndexNumber
- InvalidDob
- InvalidCredentials

### 4.5 Service Price

This endpoint returns the latest pricing for each service. Use it to determine how much to charge your customer before making a Buy Voucher request or a Check Result request.

| | |
|---|---|
| API Endpoint | `https://api.checkerport.com/service-price` |
| Method | GET |
| Content Type | JSON |

**Query Parameters**

| Field | Type | Description |
|---|---|---|
| serviceName | Enum | ID of the service to check pricing for (see table below) |

**serviceName Parameter Values**

| Value | Description |
|---|---|
| VoucherPricePlatformWaecNew | BECE voucher price |
| VoucherPricePlatformWaecOld | WASSCE voucher price |
| ArcPriceWrcBeceSchool | BECE result checker price |
| ArcPriceWrcWassceSchool | WASSCE (School) result checker price |
| ArcPriceWrcWasscePrivate | WASSCE (Private) result checker price |
| ArcPriceSpr | SHS placement checker price |

---

## 5. Webhook Callbacks

Final results are delivered via HTTP POST to your `webhookCallbackUrl`. The payload is identical to the Status Check response (depending on the type of service).

### 5.1 Security

Each webhook request includes:

`x-api-key: YOUR_API_KEY`

Validate this header before processing. Reject the request if it is missing or invalid.

### 5.2 Behaviour

- Webhooks may be sent multiple times
- Use referenceId for idempotency
- Treat the webhook as the primary source of truth

### 5.3 serviceStatus Values

| Value | Meaning |
|---|---|
| pending | Processing |
| pending-input | Invalid input — request must be corrected and retried |
| complete | Final result ready |

`pending-input` is returned when user input is invalid. Examples: `InvalidDob`, `InvalidIndexNumber`.

### 5.4 Fallback

If no webhook is received within approximately 7 minutes, you are required to make a Status Check request manually to check the status of your transaction.

---

## 6. Error Handling

When a request fails, a structured response is returned:

### 6.1 Error Response Structure

```json
{
  "status": "FAILED",
  "message": "Invalid input",
  "errorCode": "INPUT_INVALID"
}
```

### 6.2 Common Error Codes

| Code | Meaning |
|---|---|
| AUTH_INVALID_API_KEY | Invalid API key |
| INPUT_INVALID | Invalid request data |
| INVALID_REFERENCE_ID | Unknown reference |
| EXISTING_REFERENCE_ID | Duplicate request |
| INVALID_AMOUNT | Pricing issue |
| MISSING_PRICE | Missing price |
| SERVICE_UNAVAILABLE | Requested service is unavailable (e.g. maintenance mode) |

---

*— End of Document —*
