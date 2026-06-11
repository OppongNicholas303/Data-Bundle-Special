# End-to-End Payment Verification Test Plan

## Prerequisites
- Backend running on `http://localhost:8080` (or your production server)
- Frontend running on `http://localhost:5173` (or your production domain)
- Paystack test credentials configured in `application.yaml`

## Test Scenario 1: Agent Link Order with Paystack Payment

### Setup
```bash
# Start backend
cd space_bundle
mvn spring-boot:run

# Start frontend (in another terminal)
cd bundle-buddy-99
npm run dev
```

### Steps
1. Open browser and navigate to: `http://localhost:5173/?agent=AGENT_CODE`
   - Replace `AGENT_CODE` with an actual agent code from your database
   - Verify the "Referral" text appears on the page

2. Click on a bundle to purchase
   - Network: MTN / airteltigo / telecel
   - Phone: `024XXXXXXX` (a valid Ghana mobile number)
   - Click "Buy Now"

3. Complete payment
   - Paystack popup appears
   - Select payment method (card or mobile money)
   - Use Paystack test credentials:
     - **Card:** 4084084084084081
     - **CVV:** 408
     - **Expiry:** 01/25
     - **OTP:** 123456

4. Verify success page
   - ✅ Should see: "Payment Successful!"
   - ❌ Should NOT see: "Verification Failed"
   - Display order details: reference, amount, channel

### Backend Verification
Check logs for:
```
[WEBHOOK] Order completed: {orderId}
Order created: id={orderId}, network=MTN, base={price}, customer={price}, commission={commission}
```

## Test Scenario 2: Direct Purchase (No Agent)

### Steps
1. Open: `http://localhost:5173/bundles`
   - No agent code in URL

2. Select a bundle and complete payment
   - Same payment steps as above

3. Verify success
   - Should show "Payment Successful!"
   - Agent commission should be ZERO (since no agent)

### Database Check
```javascript
db.orders.findOne({_id: {$regex: "ORDER_"}})
// Should show:
// agentId: null
// commissionAmount: 0
// status: "COMPLETED"
```

## Test Scenario 3: Wallet Payment (Instant)

### Setup
1. Fund user wallet to GHS 100+
2. Open bundle purchase dialog

### Steps
1. If wallet balance >= bundle price + 2% fee:
   - Order should complete immediately
   - No Paystack popup should appear
   - Should show "Payment successful!" in dialog

2. Check database:
   - Order status: COMPLETED
   - No payment reference needed

## Test Scenario 4: Concurrent Orders from Multiple Agents

### Steps
1. Open two browser tabs (or incognito windows)
2. Tab 1: `http://localhost:5173/?agent=AGENT1`
3. Tab 2: `http://localhost:5173/?agent=AGENT2`
4. Both buy MTN 1GB bundles with Paystack

### Expected
- Both orders should complete successfully
- Each order linked to correct agent
- Each agent gets correct commission

## Test Scenario 5: Payment Pending State

### Steps
1. During order payment, if webhook takes time to process:
   - Frontend shows "Verifying Payment"
   - After webhook processes: "Payment Successful"
   - After refresh button: "Payment Successful"

### Backend Check
Webhook processes within 10 seconds:
```
[WEBHOOK] Processing
[WEBHOOK] Order completed: {orderId}
```

## Verification Checklist

### Frontend Verification Endpoint
```bash
# Test the new endpoint directly
curl -X GET "http://localhost:8080/transactions/order/ORDER_12345abc" \
  -H "Content-Type: application/json"

# Expected response (if order exists and paid):
{
  "status": "success",
  "amount": 25.5,
  "reference": "ORDER_12345abc",
  "channel": "paystack",
  "orderId": "12345abc"
}
```

### Order Status Transitions
```
1. Order created: status = CREATED
2. Payment initialized: status = PENDING_PAYMENT
3. Webhook received: status = PAID
4. Bundle processing: status = PROCESSING
5. Bundle delivered: status = COMPLETED
```

### Agent Commission Verification
```javascript
db.orders.findOne({agentId: {$exists: true, $ne: null}})

// Expected:
// baseAmount: 25.00
// customerAmount: 30.00 (25.00 + 5.00 commission)
// commissionAmount: 5.00
// fee: 0.60 (2% of 30.00)
// total: 30.60
// Agent receives: 5.00 (NOT including fee)
// Platform receives: 25.60 (baseAmount + fee)
```

## Debugging Commands

### Check Backend Logs
```bash
# View last 50 lines of logs
tail -50 space_bundle.log

# Filter for payment errors
grep -i "payment\|webhook\|verify" space_bundle.log

# Filter for agent/commission
grep -i "agent\|commission" space_bundle.log
```

### Check Frontend Network Tab (Browser DevTools)
1. Open DevTools → Network tab
2. Place order
3. Look for requests:
   - `POST /orders` → 200 OK (order created)
   - `GET /transactions/order/ORDER_xxx` → 200 OK (verification)
4. Check response body matches expected format

### Database Queries
```javascript
// Check order with agent
db.orders.findOne({agentId: {$exists: true}})

// Check order status history
db.orders.find({status: "COMPLETED"}).limit(5).pretty()

// Check agent's commission
db.orders.aggregate([
  {$match: {agentId: "AGENT_ID"}},
  {$group: {
    _id: "$agentId",
    totalOrders: {$sum: 1},
    totalCommission: {$sum: "$commissionAmount"}
  }}
])
```

## Known Issues & Solutions

### Issue 1: Verification returns 404
**Cause:** Order ID format mismatch
**Solution:** Ensure reference is `ORDER_{id}` or just `{id}`, endpoint accepts both

### Issue 2: Order created but webhook doesn't process
**Cause:** Paystack webhook not sent (network/config issue)
**Solution:** Manually check order status in database, test webhook endpoint directly

### Issue 3: Agent commission is zero
**Cause:** Agent code not provided or invalid
**Solution:** Verify agent code in URL, check agent exists in database

### Issue 4: Paystack popup closes without payment
**Cause:** User cancelled or network timeout
**Solution:** Order stays in PENDING_PAYMENT, user can retry

## Success Criteria ✅

- [ ] Agent link orders show correct commission
- [ ] Payment verification succeeds without "Verification Failed" error
- [ ] No agent orders have zero commission
- [ ] Paystack fee (2%) not deducted from agent commission
- [ ] Wallet payments work without Paystack
- [ ] Concurrent orders from different agents work correctly
- [ ] Order status correctly transitions: CREATED → PENDING_PAYMENT → PAID → PROCESSING → COMPLETED
- [ ] Backend compiles without errors: `BUILD SUCCESS`
- [ ] Frontend compiles without TypeScript errors
- [ ] Browser console shows no JavaScript errors


