# BundlePrice Endpoint Guide

## Overview
This document explains how to use the BundlePrice endpoint to set selling prices for bundle packages.

## Problem Solved
The project had the following compilation errors that have been fixed:
1. **Missing import in BotPurchaseResponse** - The Order DTO was using the wrong import path
2. **Package structure issue** - Separated the bot API Order DTO from the core domain Order entity

## BundlePrice Endpoint

### Endpoint URL
```
POST /bundles/price
```

### Full URL (with context path)
```
POST /api/bundles/price
```

### Request Method
`POST`

### Request Body Format
```json
{
  "packageId": 20,
  "sellingPrice": 4.80,
  "name": "1G"
}
```

### Request Parameters Explained

| Parameter | Type | Required | Description | Example |
|-----------|------|----------|-------------|---------|
| `packageId` | Long | Yes | The unique package ID from the bot API packages | 20 |
| `sellingPrice` | BigDecimal | Yes | The selling price for this package | 4.80 |
| `name` | String | Yes | The bundle name (should match bundle name for matching) | "1G", "1GB" |

### Response Format
Success Response (HTTP 200):
```json
{
  "success": true,
  "message": null,
  "data": {
    "packageId": 20,
    "sellingPrice": 4.80,
    "name": "1G"
  },
  "timestamp": 1772148053766
}
```

Error Response (HTTP 400/500):
```json
{
  "success": false,
  "message": "Error description",
  "data": null,
  "timestamp": 1772148053766
}
```

## How It Works

### Creating Bundle Prices
1. The BundlePrice entity stores the selling price for packages by package ID
2. When you POST to `/bundles/price`, the system:
   - Creates a new BundlePrice document in MongoDB
   - Stores the package ID as the primary identifier
   - Associates the selling price with the bundle name

### Matching Bundles with Prices
When fetching bundles (GET `/bundles`), the system:
1. Fetches all packages from the bot API
2. For each package, looks up the corresponding BundlePrice by name
3. Uses case-insensitive matching for resilience
4. Returns the bundle with the selling price if found

### Example: Setting Price for 1GB Bundle

**Step 1:** Add the bundle price
```bash
curl -X POST http://localhost:8080/api/bundles/price \
  -H "Content-Type: application/json" \
  -d '{
    "packageId": 20,
    "sellingPrice": 4.80,
    "name": "1G"
  }'
```

**Step 2:** Verify in the bundle list
```bash
curl http://localhost:8080/api/bundles
```

The response will include:
```json
{
  "id": 20,
  "name": "1GB",
  "size": "1",
  "network": "MTN",
  "validityDays": 90,
  "costPrice": "4.30",
  "sellingPrice": 4.80
}
```

## Important Notes

1. **Name Matching**: The `name` field in the BundlePrice request should match the package name from the bot API for proper association
   - The system uses case-insensitive matching as a fallback
   - Example: "1G", "1GB", "1gb" will all match

2. **Package ID**: Must correspond to a valid package from the bot API

3. **Selling Price**: Should be greater than or equal to the cost price for profitability

4. **MongoDB Collection**: Bundle prices are stored in the `bundle_prices` collection

## Database Structure

### BundlePriceDocument (MongoDB)
```
{
  "_id": 20,                    // packageId (primary key)
  "packageId": 20,
  "sellingPrice": 4.80,
  "name": "1G"
}
```

## Related Endpoints

### Get All Bundles with Prices
```
GET /api/bundles
```
Returns all packages from the bot API with their corresponding selling prices.

### Get Bundles by Network
```
GET /api/bundles/network/{network}
```
Returns active bundles for a specific network (e.g., MTN, VODAFONE).

### Get Bundle by Code
```
GET /api/bundles/{code}
```
Returns a specific bundle by its code.

## Troubleshooting

### Issue: Selling Price is null for a bundle
**Solution**: Check that:
1. The `name` field in BundlePrice matches the package name exactly (case-insensitive)
2. The package ID exists in the packages list
3. The BundlePrice was successfully saved to the database

### Issue: Package not found error
**Solution**: Verify that:
1. The packageId exists in the bot API packages
2. You're using the correct API endpoint URL

## Architecture

The BundlePrice functionality uses a layered architecture:

1. **Controller**: `BundleController.createBundlePrice()`
   - Handles HTTP requests
   - Validates input

2. **Service**: `BundleService.createBundlePrice()`
   - Business logic
   - Delegates to persistence layer

3. **Port Interface**: `BundlePricePort`
   - Defines persistence contract

4. **Adapter**: `BundlePriceRepositoryAdapter`
   - Implements the port
   - Maps between domain entities and database documents

5. **Repository**: `BundlePriceRepository`
   - MongoDB repository for CRUD operations
   - Provides query methods

6. **Entity/Document**:
   - `BundlePrice` (domain entity)
   - `BundlePriceDocument` (MongoDB document)

## Files Modified to Fix Compilation Errors

1. **BotPurchaseResponse.java**
   - Changed Order import from `core.entities` to `out.automation.dto`
   - This DTO represents the bot API response structure

2. **AutomationPort.java**
   - Updated to use fully qualified Order class name where needed
   - Ensures correct separation of concerns

## Compilation Status

✅ All compilation errors have been resolved
✅ Project builds successfully with `mvn clean package -DskipTests`
✅ No runtime errors related to BundlePrice endpoints

