# Frontend Integration Guide - Data Bundle Order API

## Base URL
```
https://your-api-domain.com/api
```

---

## 1. Get Available Bundles

**Endpoint:** `GET /bundles`

**Authentication:** Not required

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": 20,
      "name": "1GB Daily",
      "size": "1GB",
      "network": "MTN",
      "validityDays": 1,
      "costPrice": "4.50",
      "sellingPrice": 5.00
    }
  ]
}
```

**Usage:**
- Display bundles to user
- Use `id` as `package_id` when creating order
- Use `name` as `bundleCode` when creating order
- Show `sellingPrice` to user (this is what they pay)

---

## 2. Create Order (Purchase Bundle)

**Endpoint:** `POST /orders`

**Authentication:** Optional (can be guest or authenticated user)

**Request Body:**
```json
{
  "network": "MTN",
  "phoneNumber": "0545294916",
  "bundleCode": "1GB Daily",
  "package_id": 20,
  "email": "customer@example.com"
}
```

**Required Fields:**
- `network` - Network provider (from bundle data)
- `phoneNumber` - Customer's phone number (will receive the bundle)
- `bundleCode` - Bundle name (from bundle data)
- `package_id` - Package ID (from bundle data)

**Optional Fields:**
- `email` - Customer email (auto-generated if not provided)

**Response:**
```json
{
  "success": true,
  "data": {
    "id": "abc123",
    "userId": "user456",
    "network": "MTN",
    "phoneNumber": "0545294916",
    "bundleCode": "1GB Daily",
    "package_id": 20,
    "amount": 5.10,
    "status": "PENDING_PAYMENT",
    "providerOrderNumber": null,
    "paymentReference": "ORDER_abc123",
    "paymentUrl": "https://checkout.paystack.com/xyz",
    "paymentAccessCode": "xyz123",
    "failureReason": null,
    "createdAt": "2026-03-01T10:30:00",
    "updatedAt": "2026-03-01T10:30:00"
  }
}
```

**Important Response Fields:**
- `paymentUrl` - Redirect user here to complete payment
- `phoneNumber` - Save this! User needs it to check order status later
- `providerOrderNumber` - Will be populated after payment is completed (bot order number)

**Flow:**
1. User selects bundle
2. User enters phone number
3. POST to `/orders`
4. Redirect user to `paymentUrl` to complete payment
5. After payment, user can check status using their phone number

---

## 3. Check Order Status (By Phone Number)

**Endpoint:** `GET /orders/status?phoneNumber=0545294916`

**Authentication:** Not required

**Query Parameters:**
- `phoneNumber` (required) - Customer's phone number

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "orderNumber": "2026021512345",
      "customerPhone": "0545294916",
      "status": "completed",
      "packageName": "1GB Daily",
      "packageSize": null,
      "packageNetwork": "MTN"
    },
    {
      "orderNumber": "2026021512346",
      "customerPhone": "0545294916",
      "status": "pending",
      "packageName": "2GB Weekly",
      "packageSize": null,
      "packageNetwork": "MTN"
    }
  ]
}
```

**Status Values:**
- `pending` - Order created, waiting for processing
- `processing` - Being processed by provider
- `completed` - Bundle delivered successfully
- `failed` - Order failed

**Usage:**
- User enters their phone number
- Show all their orders with current status
- No authentication needed - anyone with phone number can check

---

## Implementation Example (React/JavaScript)

### 1. Fetch Bundles
```javascript
const fetchBundles = async () => {
  const response = await fetch('https://api.example.com/api/bundles');
  const result = await response.json();
  
  if (result.success) {
    setBundles(result.data);
  }
};
```

### 2. Create Order
```javascript
const createOrder = async (bundle, phoneNumber, email) => {
  const response = await fetch('https://api.example.com/api/orders', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      network: bundle.network,
      phoneNumber: phoneNumber,
      bundleCode: bundle.name,
      package_id: bundle.id,
      email: email
    })
  });
  
  const result = await response.json();
  
  if (result.success && result.data.paymentUrl) {
    // Save phone number for status checking
    localStorage.setItem('lastOrderPhone', phoneNumber);
    
    // Redirect to payment
    window.location.href = result.data.paymentUrl;
  }
};
```

### 3. Check Order Status
```javascript
const checkOrderStatus = async (phoneNumber) => {
  const response = await fetch(
    `https://api.example.com/api/orders/status?phoneNumber=${phoneNumber}`
  );
  const result = await response.json();
  
  if (result.success) {
    setOrders(result.data);
  }
};
```

---

## User Flow

1. **Browse Bundles**
   - GET `/bundles`
   - Display available packages with prices

2. **Purchase Bundle**
   - User selects bundle
   - User enters phone number (and optionally email)
   - POST `/orders` with bundle details
   - Redirect to Paystack payment URL

3. **After Payment**
   - User is redirected back to your site
   - Show success message
   - Tell user they can check status anytime using their phone number

4. **Check Status Anytime**
   - User enters phone number
   - GET `/orders/status?phoneNumber=XXX`
   - Display all orders for that phone number with current status

---

## Important Notes

1. **No Authentication Required** - Users can purchase and check status without creating an account

2. **Phone Number is Key** - Users need their phone number to check order status later

3. **Payment Flow** - Always redirect to `paymentUrl` from order response

4. **Status Updates** - Status is fetched in real-time from the bot API when available

5. **Multiple Orders** - One phone number can have multiple orders, all returned in status check

6. **Provider Order Number** - The `orderNumber` in status response is the bot's order number (e.g., "2026021512345")
