# Quick Reference - Admin Order Management Features

## 🎯 What Was Built

### For Admins
An enhanced admin dashboard with:
1. **Order Management Tab** - View, filter, and mark orders as complete
2. **Commission Tracking Tab** - View and analyze agent commissions

---

## 📌 Quick Links to Key Features

### Orders Tab
| Feature | Location | How to Use |
|---------|----------|-----------|
| Search | Top search box | Type phone/bundle/order ID |
| Filter Status | Dropdown | Select order status |
| Filter Network | Dropdown | Select MTN/TELECEL/AIRTELTIGO |
| Date Range | Calendar pickers | Select From and To dates |
| Mark as Done | Action column | Click for PROCESSING orders only |
| Statistics | Top cards | View real-time metrics |

### Commissions Tab
| Feature | Location | How to Use |
|---------|----------|-----------|
| Date Filter | Calendar pickers | Select date range |
| Summary | Top cards | View totals and averages |
| Daily Breakdown | Table | See commission by date |

---

## 🔌 API Endpoints

### Mark Order Complete
```
POST /admin/orders/{orderId}/mark-complete
Body: { "providerReference": "string" } (optional)
Response: { "data": AdminOrderView, "message": "Order marked as complete" }
```

### Get Commissions Summary
```
GET /admin/analytics/agent-commissions?from=2026-06-01&to=2026-06-30
Query Parameters:
  - agentId (optional): Filter by agent
  - from (optional): Start date YYYY-MM-DD
  - to (optional): End date YYYY-MM-DD
Response: {
  "data": {
    "dailySummary": [...],
    "totalCommissions": 1000,
    "totalOrders": 50,
    "fromDate": "2026-06-01",
    "toDate": "2026-06-30",
    "agentId": "all"
  }
}
```

---

## 💻 Code Locations

### Backend
```
AdminController.java (Lines 298-463)
  - getAllOrders() - Line 300
  - markOrderComplete() - Line 335
  - getAgentCommissionsSummary() - Line 406

CommissionRepository.java
  - findByCreatedAtBetween() - New method
```

### Frontend
```
OrdersPage.tsx (485 lines)
  - Component imports - Line 1-11
  - StatusBadge component - Line 19-31
  - MarkCompleteDialog component - Line 33-73
  - Main OrdersPage component - Line 75-300
  - CommissionsTab component - Line 302-485

adminService.ts
  - markOrderComplete() - Line 110-113
  - getAgentCommissionsSummary() - Line 120-128
```

---

## 🚀 How to Test

### Test Marking Order Complete
1. Navigate to Orders page
2. Filter for status = PROCESSING
3. Find a PROCESSING order
4. Click "Mark Done" button
5. Confirm in dialog
6. Watch order status change to COMPLETED
7. Verify in table

### Test Commission Viewing
1. Navigate to Orders page
2. Click "Commissions" tab
3. View default (last 30 days)
4. Try filtering with date range
5. Verify daily breakdown shows
6. Check calculations

---

## ⚠️ Important Notes

### Constraints
- Only PROCESSING orders can be marked complete
- Marking complete is not reversible without direct DB access
- Date filters are inclusive (from inclusive, to inclusive)
- Commission data is read-only from admin view

### Validation
- Status must be exactly "PROCESSING" (case-sensitive)
- Dates must be in YYYY-MM-DD format
- Agent ID must exist in database
- Admin role required for all endpoints

---

## 🐛 Troubleshooting

### Mark Complete Button Not Showing
**Cause:** Order is not in PROCESSING status
**Solution:** Filter for PROCESSING status to see eligible orders

### Commission Data Empty
**Cause:** No commissions in selected date range
**Solution:** Expand date range or check if orders exist with agent IDs

### API 403 Error
**Cause:** User doesn't have ADMIN role
**Solution:** Ensure user has ROLE_ADMIN assigned

### Filters Not Working
**Cause:** Browser cache issue
**Solution:** Clear browser cache and reload page

---

## 📈 Performance Tips

### For Large Order Volumes
1. Use date filters to limit dataset
2. Search by specific criteria rather than viewing all
3. Refresh only when needed (use manual refresh button)

### For Commission Analysis
1. Limit date range to relevant period
2. Use specific agent filter if available
3. Daily breakdown is auto-calculated

---

## 🎨 UI Reference

### Status Badge Colors
```
✓ COMPLETED    → Green (bg-green-100, text-green-800)
⟳ PROCESSING   → Blue (bg-blue-100, text-blue-800)
⏳ PENDING      → Yellow (bg-yellow-100, text-yellow-800)
💳 PAID        → Blue (bg-blue-100, text-blue-800)
+ CREATED      → Gray (bg-gray-100, text-gray-800)
✕ FAILED       → Red (bg-red-100, text-red-800)
↺ REFUNDED     → Orange (bg-orange-100, text-orange-800)
```

### Responsive Breakpoints
```
Mobile   < 640px   : Single column, essential info only
Tablet   640-1024px: Two columns, some details hidden
Desktop  > 1024px  : Full layout, all columns visible
```

---

## 📱 Device Support

| Screen | Orders Tab | Commissions Tab |
|--------|-----------|-----------------|
| Mobile | ✅ Full | ✅ Full |
| Tablet | ✅ Full | ✅ Full |
| Desktop | ✅ Full | ✅ Full |

---

## 🔄 Data Refresh Strategy

- **Automatic:** After marking order complete
- **Manual:** Click refresh button
- **Query Cache:** 5 minutes default (React Query)
- **Force Refresh:** Clear browser cache and reload

---

## 💡 Common Use Cases

### Use Case 1: Mark Daily Orders as Complete
1. Go to Orders tab
2. Filter status = PROCESSING
3. Filter date = today
4. Click "Mark Done" for each
5. Verify in completed count

### Use Case 2: Review Agent Commissions
1. Go to Commissions tab
2. Set date range to last month
3. View total commissions
4. Check daily breakdown
5. Identify highest commission days

### Use Case 3: Audit Orders for Specific Date
1. Go to Orders tab
2. Set date range (from = to = specific date)
3. View all orders for that day
4. Filter by status if needed
5. Check revenue and profit

---

## 🔗 Related Documentation

- `ADMIN_ORDER_FEATURES.md` - Complete feature documentation
- `IMPLEMENTATION_CHECKLIST.md` - Testing and deployment steps
- `IMPLEMENTATION_SUMMARY.md` - Detailed implementation overview

---

## 📞 Support Matrix

| Issue | Check | Solution |
|-------|-------|----------|
| Button not showing | Order status | Must be PROCESSING |
| Data not loading | Network tab | Check API response |
| Calculations wrong | Input data | Verify order amounts |
| Date filter broken | Format | Must be YYYY-MM-DD |
| Permission denied | Role | User needs ROLE_ADMIN |

---

**Last Updated:** June 16, 2026
**Version:** 1.0
**Status:** Production Ready


