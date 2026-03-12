# Wallet + Paystack Hybrid Payment Implementation

## Overview
The `randy` branch now supports a **hybrid payment system** where users can:
1. **Use wallet balance** (if sufficient) - instant debit
2. **Use Paystack** (if wallet insufficient) - card payment

---

## Payment Flow

### Frontend (BundlesPage.tsx)
```
User clicks "Buy Bundle"
    ↓
Enter phone number & click "Pay"
    ↓
API call: POST /orders {network, phoneNumber, bundleCode, email}
    ↓
Check response:
    ├─ If NO paymentAccessCode → Wallet payment succeeded ✅
    │  └─ Show success toast & close dialog
    │
    └─ If paymentAccessCode exists → Wallet insufficient 💳
       └─ Open Paystack popup
          ├─ User pays → Success ✅
          └─ User cancels → Show error
```

### Backend (OrderService.java)
```
Order created
    ↓
Check if user is authenticated:
    ├─ NO → Go straight to Paystack
    │
    └─ YES → Check wallet balance
       ├─ Balance ≥ amount → Debit wallet (success response, no access code)
       │  └─ Process bundle delivery
       │
       └─ Balance < amount → Need Paystack payment
          └─ Return paymentAccessCode for Paystack
```

---

## Response Formats

### Wallet Payment Success
```json
{
  "success": true,
  "message": "Order created and paid with wallet",
  "data": {
    "id": "order-123",
    "network": "MTN",
    "phoneNumber": "0241234567",
    "bundleCode": "1GB",
    "amount": 5.00,
    "status": "processing",
    "createdAt": "2024-01-15T10:30:00Z"
    // NO paymentAccessCode
  }
}
```

### Paystack Payment Required
```json
{
  "success": true,
  "message": "Paystack payment required",
  "data": {
    "id": "order-456",
    "network": "MTN",
    "phoneNumber": "0241234567",
    "bundleCode": "1GB",
    "amount": 5.10,
    "status": "pending_payment",
    "paymentAccessCode": "c93xx2x",
    "authorization_url": "https://checkout.paystack.com/...",
    "createdAt": "2024-01-15T10:35:00Z"
  }
}
```

---

## Frontend Changes

### Imports Added
- `WalletIcon` from lucide-react
- `useQueryClient` from React Query (for query invalidation)

### Mutation Updated
The `paymentMutation` now:
1. **Checks response** for `paymentAccessCode`
2. **If wallet paid** (no access code):
   - Show "Payment successful!" toast
   - Auto-close dialog after 2 seconds
3. **If Paystack needed** (has access code):
   - Close dialog to prevent z-index conflicts
   - Open Paystack popup with transaction
   - Handle cancel/success callbacks
   - Reopen dialog on success

### Dialog Updated
- Changed description: "Pay with your wallet or Paystack if wallet is insufficient"
- Added payment method info box explaining both scenarios
- Shows bundle price clearly
- Validates phone number before submission

---

## User Experience

### Scenario 1: User has enough wallet balance
1. User selects bundle (e.g., 1GB for ₵5.00)
2. Enters phone number
3. Clicks "Pay ₵5.00"
4. **INSTANT**: Wallet debited, bundle delivered
5. Shows "Payment successful! Your bundle will be delivered within seconds" ✅

### Scenario 2: User has insufficient wallet balance
1. User selects bundle (e.g., 1GB for ₵5.00)
2. Enters phone number
3. Clicks "Pay ₵5.00"
4. Wallet has ₵2.00 (insufficient)
5. **Paystack opens**: User completes card payment
6. Shows "Payment successful! Your bundle will be delivered within seconds" ✅

### Scenario 3: User cancels Paystack
1. Paystack popup opens
2. User clicks close/cancel
3. Shows "Payment cancelled - You closed the payment window" ⚠️
4. Order remains pending (not deleted)

---

## Key Features

✅ **Instant wallet payments** - No redirect, instant confirmation
✅ **Fallback to Paystack** - Always have a way to pay
✅ **Error handling** - Clear messages for all scenarios
✅ **Dialog management** - Smooth transitions between states
✅ **Query invalidation** - Refreshes order history after payment
✅ **Phone validation** - Format: 0XXXXXXXXX (10 digits, starts with 0)

---

## Testing the Flow

### Test Case 1: Wallet Payment
1. Login with user who has wallet balance ≥ bundle price
2. Go to "Buy Data Bundle"
3. Select a bundle
4. Enter phone number
5. Click "Pay"
6. ✅ Should show success immediately (no Paystack)

### Test Case 2: Paystack Payment
1. Login with user who has wallet balance < bundle price
2. Go to "Buy Data Bundle"
3. Select a bundle  
4. Enter phone number
5. Click "Pay"
6. ✅ Paystack should pop up
7. Complete test payment (use Paystack test card)
8. ✅ Should show success

### Test Case 3: Insufficient Balance + Cancel Paystack
1. Login with insufficient wallet balance
2. Select bundle and proceed
3. Paystack opens
4. Click close/ESC
5. ✅ Should show "Payment cancelled" message
6. Dialog reopens for user to retry

---

## Backend Integration

### Required
- ✅ OrderService.java must check wallet balance
- ✅ Must return `paymentAccessCode` only if Paystack needed
- ✅ Must debit wallet and return success if balance sufficient

### Email
Currently hardcoded as `'nictch23@gmail.com'` - should use:
```typescript
email: userData.email || localStorage.getItem('userEmail') || 'default@example.com'
```

---

## Configuration

### Environment Variables
- `VITE_API_BASE_URL`: Backend API base URL (defaults to `http://localhost:3000/api`)
- Paystack public key: Loaded from `@paystack/inline-js` package

---

## Known Limitations

1. **Email hardcoded**: Uses `'nictch23@gmail.com'` - should be user's email
2. **No wallet balance display**: UI doesn't show current wallet balance before payment
3. **No order history**: Users can't see previous orders from this page

## Future Enhancements

1. Display wallet balance before payment
2. Show expected payment source (wallet % or Paystack %)
3. Add "Order History" tab
4. Support multiple payment methods
5. Add order status tracking

---

## Code References

- **Frontend**: [BundlesPage.tsx](bundle-buddy-99/src/pages/BundlesPage.tsx)
- **Backend**: [OrderService.java](space_bundle/src/main/java/com/space/space_bundle/core/services/OrderService.java#L79-L85)
- **API Client**: [api.ts](bundle-buddy-99/src/lib/api.ts)
- **Types**: [types/index.ts](bundle-buddy-99/src/types/index.ts)

