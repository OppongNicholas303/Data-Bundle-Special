# BundlePrice System Architecture & Flow Diagrams

## System Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                        REST API Client                           │
│                    (curl, Postman, App)                          │
└───────────────────────────┬─────────────────────────────────────┘
                            │
                            │ POST /api/bundles/price
                            │ {packageId, sellingPrice, name}
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                  WEB/PRESENTATION LAYER                          │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  BundleController                                         │  │
│  │  - @PostMapping("/price")                                │  │
│  │  - createBundlePrice(CreateBundlePriceRequest)           │  │
│  │  - Returns: ApiResponse<BundlePrice>                     │  │
│  └───────────────────┬──────────────────────────────────────┘  │
└──────────────────────┼──────────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────────┐
│                   SERVICE LAYER                                  │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  BundleService                                            │  │
│  │  - createBundlePrice(packageId, sellingPrice, name)      │  │
│  │  - Builds BundlePrice entity                             │  │
│  │  - Delegates to bundlePriceRepository.save()             │  │
│  └───────────────────┬──────────────────────────────────────┘  │
└──────────────────────┼──────────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────────┐
│              PORT/ADAPTER LAYER                                  │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  BundlePricePort (Interface)                             │  │
│  │  ├─ save(BundlePrice): BundlePrice                       │  │
│  │  ├─ findById(Long): Optional<BundlePrice>                │  │
│  │  ├─ findByName(String): Optional<BundlePrice>            │  │
│  │  └─ findAll(): List<BundlePrice>                         │  │
│  └──────────────────────┬─────────────────────────────────────┘  │
│                         │                                        │
│  ┌──────────────────────▼─────────────────────────────────────┐ │
│  │  BundlePriceRepositoryAdapter (Implementation)            │ │
│  │  - Implements BundlePricePort                             │ │
│  │  - Converts BundlePrice ↔ BundlePriceDocument            │ │
│  │  - Uses BundlePriceMapper for conversion                 │ │
│  │  - Delegates to BundlePriceRepository                    │ │
│  └──────────────────────┬─────────────────────────────────────┘ │
└──────────────────────┼──────────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────────┐
│          PERSISTENCE LAYER                                       │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  BundlePriceMapper                                        │  │
│  │  - toDocument(BundlePrice): BundlePriceDocument          │  │
│  │  - toDomain(BundlePriceDocument): BundlePrice            │  │
│  └───────────────────┬──────────────────────────────────────┘  │
│                      │                                          │
│  ┌──────────────────▼─────────────────────────────────────────┐ │
│  │  BundlePriceRepository (Spring Data MongoRepository)      │ │
│  │  - Extends MongoRepository<BundlePriceDocument, Long>    │ │
│  │  - save(): Saves to MongoDB                              │ │
│  │  - findByName(): Queries by name                         │ │
│  │  - findByNameIgnoreCase(): Case-insensitive query        │ │
│  └───────────────────┬──────────────────────────────────────┘  │
└──────────────────────┼──────────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────────┐
│                  DATABASE LAYER                                  │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  MongoDB                                                  │  │
│  │  Collection: bundle_prices                                │  │
│  │                                                           │  │
│  │  Document Structure:                                      │  │
│  │  {                                                        │  │
│  │    "_id": 20,         (packageId)                         │  │
│  │    "packageId": 20,                                       │  │
│  │    "sellingPrice": 4.80,                                  │  │
│  │    "name": "1G"                                           │  │
│  │  }                                                        │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

---

## Request/Response Flow Diagram

```
┌──────────────────────────────────────────────────────────┐
│                    CLIENT REQUEST                        │
│                                                          │
│  POST /api/bundles/price                                │
│  Content-Type: application/json                         │
│                                                          │
│  {                                                       │
│    "packageId": 20,                                      │
│    "sellingPrice": 4.80,                                │
│    "name": "1G"                                          │
│  }                                                       │
└──────────┬───────────────────────────────────────────────┘
           │
           ▼
┌──────────────────────────────────────────────────────────┐
│     ① BundleController.createBundlePrice()              │
│     - Receives CreateBundlePriceRequest                 │
│     - Validates JSON structure                          │
│     - Logs request: "Create bundle price request..."    │
└──────────┬───────────────────────────────────────────────┘
           │
           ▼
┌──────────────────────────────────────────────────────────┐
│     ② BundleService.createBundlePrice()                 │
│     - Creates BundlePrice entity:                       │
│       {packageId: 20, sellingPrice: 4.80, name: "1G"}  │
│     - Calls bundlePriceRepository.save(entity)          │
└──────────┬───────────────────────────────────────────────┘
           │
           ▼
┌──────────────────────────────────────────────────────────┐
│     ③ BundlePriceRepositoryAdapter.save()               │
│     - Receives BundlePrice domain entity                │
│     - Calls BundlePriceMapper.toDocument()              │
│     - Converts to BundlePriceDocument                   │
│     - Calls bundlePriceRepository.save(document)        │
└──────────┬───────────────────────────────────────────────┘
           │
           ▼
┌──────────────────────────────────────────────────────────┐
│     ④ BundlePriceRepository.save()                      │
│     - Spring Data MongoRepository method                │
│     - Performs MongoDB upsert operation                 │
│     - Inserts or updates document in collection         │
└──────────┬───────────────────────────────────────────────┘
           │
           ▼
┌──────────────────────────────────────────────────────────┐
│     ⑤ MongoDB Insert/Update                              │
│     Collection: bundle_prices                           │
│     Document inserted/updated                           │
└──────────┬───────────────────────────────────────────────┘
           │
           ▼
┌──────────────────────────────────────────────────────────┐
│     ⑥ Return BundlePriceDocument from Repository        │
│     - MongoDB returns saved document                    │
│     - Spring Data wraps in Optional/Response            │
└──────────┬───────────────────────────────────────────────┘
           │
           ▼
┌──────────────────────────────────────────────────────────┐
│     ⑦ BundlePriceMapper.toDomain()                      │
│     - Converts BundlePriceDocument back to entity       │
│     - Extracts: packageId, sellingPrice, name           │
└──────────┬───────────────────────────────────────────────┘
           │
           ▼
┌──────────────────────────────────────────────────────────┐
│     ⑧ Return to BundleService                           │
│     - Receives BundlePrice entity                       │
│     - Logs: "Bundle price saved: packageId=20"          │
└──────────┬───────────────────────────────────────────────┘
           │
           ▼
┌──────────────────────────────────────────────────────────┐
│     ⑨ Return to BundleController                        │
│     - Controller wraps in ApiResponse                   │
│     - Sets success=true                                 │
│     - Sets data to saved BundlePrice                    │
│     - Sets timestamp                                    │
└──────────┬───────────────────────────────────────────────┘
           │
           ▼
┌──────────────────────────────────────────────────────────┐
│                  HTTP RESPONSE (200 OK)                  │
│                                                          │
│  Content-Type: application/json                         │
│                                                          │
│  {                                                       │
│    "success": true,                                      │
│    "message": null,                                      │
│    "data": {                                             │
│      "packageId": 20,                                    │
│      "sellingPrice": 4.80,                              │
│      "name": "1G"                                        │
│    },                                                    │
│    "timestamp": 1645947908000                           │
│  }                                                       │
└──────────────────────────────────────────────────────────┘
```

---

## Data Flow Diagram

```
CreateBundlePriceRequest (JSON)
├─ packageId: 20
├─ sellingPrice: 4.80
└─ name: "1G"
    │
    ▼ (Jackson deserializes)
CreateBundlePriceRequest (Java Object)
    │
    ▼ (BundleService)
BundlePrice Entity (Domain Model)
├─ packageId: 20
├─ sellingPrice: BigDecimal(4.80)
└─ name: "1G"
    │
    ▼ (BundlePriceMapper.toDocument)
BundlePriceDocument (MongoDB Document)
├─ _id: 20
├─ packageId: 20
├─ sellingPrice: 4.80
└─ name: "1G"
    │
    ▼ (MongoDB)
MongoDB Collection "bundle_prices"
│
├─ Document {_id: 1, ...}
├─ Document {_id: 2, ...}
└─ Document {_id: 20, packageId: 20, sellingPrice: 4.80, name: "1G"} ◄── NEW
    │
    ▼ (Returned from Repository)
BundlePriceDocument (Retrieved)
    │
    ▼ (BundlePriceMapper.toDomain)
BundlePrice Entity (Domain Model)
    │
    ▼ (ApiResponse wrapping)
ApiResponse<BundlePrice>
├─ success: true
├─ data: BundlePrice {...}
└─ timestamp: 1645947908000
    │
    ▼ (Jackson serializes)
JSON Response
```

---

## Class Relationship Diagram

```
┌─────────────────────────────────────────────────────────┐
│                 REST Layer                              │
│  ┌────────────────────────────────────────────────────┐ │
│  │ BundleController                                   │ │
│  │  - createBundlePrice(CreateBundlePriceRequest)    │ │
│  │  - Uses: BundleService                            │ │
│  │  - Uses: CreateBundlePriceRequest DTO             │ │
│  │  - Returns: ApiResponse<BundlePrice>              │ │
│  └────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
                         │
                         │ depends on
                         ▼
┌─────────────────────────────────────────────────────────┐
│                Service Layer                            │
│  ┌────────────────────────────────────────────────────┐ │
│  │ BundleService                                      │ │
│  │  - createBundlePrice(Long, BigDecimal, String)   │ │
│  │  - Uses: BundlePricePort                         │ │
│  │  - Uses: BundlePrice entity                      │ │
│  │  - Builds: BundlePrice from parameters           │ │
│  └────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
                         │
                         │ uses
                         ▼
┌─────────────────────────────────────────────────────────┐
│            Port/Adapter Layer                           │
│  ┌────────────────────────────────────────────────────┐ │
│  │ BundlePricePort (Interface)                        │ │
│  │  + save(BundlePrice): BundlePrice                 │ │
│  │  + findById(Long): Optional<BundlePrice>          │ │
│  │  + findByName(String): Optional<BundlePrice>      │ │
│  └────────────────────────────────────────────────────┘ │
│                    ▲                                     │
│                    │ implements                          │
│  ┌────────────────┴─────────────────────────────────┐  │
│  │ BundlePriceRepositoryAdapter                     │  │
│  │  - bundlePriceRepository: BundlePriceRepository  │  │
│  │  - Uses: BundlePriceMapper                       │  │
│  │  - Converts: BundlePrice ↔ BundlePriceDocument │  │
│  └────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
                         │
                         │ uses
                         ▼
┌─────────────────────────────────────────────────────────┐
│         Persistence/Data Access Layer                   │
│  ┌────────────────────────────────────────────────────┐ │
│  │ BundlePriceRepository                              │ │
│  │  extends MongoRepository<BundlePriceDocument, Long>│ │
│  │  - save(BundlePriceDocument)                      │ │
│  │  - findByName(String)                             │ │
│  │  - findByNameIgnoreCase(String)                   │ │
│  └────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
                         │
                         │ maps
                         ▼
┌─────────────────────────────────────────────────────────┐
│         Entity/Document Layer                           │
│  ┌──────────────────┐      ┌────────────────────────┐   │
│  │ BundlePrice      │      │ BundlePriceDocument    │   │
│  │ (Domain Entity)  │◄────►│ (MongoDB Document)     │   │
│  │ - packageId      │ (via)│ - _id                  │   │
│  │ - sellingPrice   │      │ - packageId            │   │
│  │ - name           │      │ - sellingPrice         │   │
│  │                  │      │ - name                 │   │
│  └──────────────────┘      └────────────────────────┘   │
│                    (BundlePriceMapper)                   │
└─────────────────────────────────────────────────────────┘
                         │
                         │ persisted in
                         ▼
┌─────────────────────────────────────────────────────────┐
│                 Database Layer                          │
│  ┌────────────────────────────────────────────────────┐ │
│  │ MongoDB - Collection: bundle_prices                │ │
│  │  {                                                  │ │
│  │    "_id": 20,                                       │ │
│  │    "packageId": 20,                                 │ │
│  │    "sellingPrice": 4.80,                           │ │
│  │    "name": "1G"                                     │ │
│  │  }                                                  │ │
│  └────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
```

---

## Data Transfer Object (DTO) Diagram

```
API Request
    │
    ▼
┌──────────────────────────────┐
│ CreateBundlePriceRequest     │
│ ├─ packageId: Long           │
│ ├─ sellingPrice: BigDecimal  │
│ └─ name: String              │
└──────────────────────────────┘
    │
    ▼ (Service layer)
┌──────────────────────────────┐
│ BundlePrice (Entity)         │
│ ├─ packageId: Long           │
│ ├─ sellingPrice: BigDecimal  │
│ └─ name: String              │
└──────────────────────────────┘
    │
    ▼ (Mapper)
┌──────────────────────────────┐
│ BundlePriceDocument          │
│ ├─ _id: Long                 │
│ ├─ packageId: Long           │
│ ├─ sellingPrice: BigDecimal  │
│ └─ name: String              │
└──────────────────────────────┘
    │
    ▼ (MongoDB)
┌──────────────────────────────┐
│ Persisted in Database        │
└──────────────────────────────┘
    │
    ▼ (Mapper)
┌──────────────────────────────┐
│ BundlePrice (Entity)         │
├─ packageId: Long             │
├─ sellingPrice: BigDecimal    │
└─ name: String                │
└──────────────────────────────┘
    │
    ▼ (ApiResponse wrapper)
┌──────────────────────────────┐
│ ApiResponse<BundlePrice>     │
├─ success: boolean            │
├─ message: String             │
├─ data: BundlePrice           │
└─ timestamp: long             │
└──────────────────────────────┘
    │
    ▼ (JSON serialization)
API Response (JSON)
```

---

## State Management Diagram

```
START
  │
  ▼
┌──────────────────────────────┐
│ Receive POST Request         │
│ /api/bundles/price           │
│ with JSON body               │
└─────────────┬────────────────┘
              │
              ▼
┌──────────────────────────────┐
│ Validate Request             │
│ ✓ JSON format valid          │
│ ✓ All required fields present│
│ ✓ Data types correct         │
└─────────────┬────────────────┘
              │
              ▼
┌──────────────────────────────┐
│ Create BundlePrice Entity    │
│ with provided values         │
└─────────────┬────────────────┘
              │
              ▼
┌──────────────────────────────┐
│ Save to MongoDB              │
│ - Convert to Document        │
│ - Insert/Update in DB        │
│ - Return saved object        │
└─────────────┬────────────────┘
              │
              ▼
┌──────────────────────────────┐
│ Convert back to Entity       │
│ (if needed for response)     │
└─────────────┬────────────────┘
              │
              ▼
┌──────────────────────────────┐
│ Wrap in ApiResponse          │
│ - success: true              │
│ - data: BundlePrice          │
│ - timestamp: now             │
└─────────────┬────────────────┘
              │
              ▼
┌──────────────────────────────┐
│ Serialize to JSON            │
│ and Send Response            │
└─────────────┬────────────────┘
              │
              ▼
END (HTTP 200 OK)
```

---

## Bundle Matching Diagram

```
GET /api/bundles
    │
    ▼
┌────────────────────────────────────┐
│ Get Packages from Bot API          │
│ [                                   │
│   {id: 20, name: "1GB", ...},     │
│   {id: 21, name: "2GB", ...},     │
│   {id: 22, name: "5GB", ...}      │
│ ]                                   │
└────────────────────────────────────┘
    │
    ▼
┌────────────────────────────────────┐
│ For each package:                  │
│                                    │
│ 1. Try exact match:                │
│    findByName("1GB")               │
│    → Found: BundlePrice with price │
│                                    │
│    NOT FOUND → Fallback:           │
│                                    │
│ 2. Try case-insensitive match:    │
│    findByNameIgnoreCase("1gb")     │
│    → Found: BundlePrice with price │
│                                    │
│    STILL NOT FOUND:                │
│    → Return bundle without price   │
│    (sellingPrice: null)            │
└────────────────────────────────────┘
    │
    ▼
┌────────────────────────────────────┐
│ Return bundles with prices:        │
│ [                                   │
│   {                                │
│     id: 20,                        │
│     name: "1GB",                   │
│     sellingPrice: 4.80 ◄── Matched │
│   },                               │
│   {                                │
│     id: 21,                        │
│     name: "2GB",                   │
│     sellingPrice: null ◄── No match│
│   }                                │
│ ]                                   │
└────────────────────────────────────┘
```

---

## Error Handling Flow

```
API Request
    │
    ▼
┌──────────────────────────┐
│ Request Validation       │
└──────────────────────────┘
    │
    ├─ Valid ──────────────────────────┐
    │                                   │
    └─ Invalid                          │
        │                               │
        ▼                               │
    ┌──────────────────────┐            │
    │ 400 Bad Request      │            │
    │ - Missing field      │            │
    │ - Invalid type       │            │
    │ - Malformed JSON     │            │
    └──────────────────────┘            │
                                        │
                                        ▼
                            ┌──────────────────────┐
                            │ Database Operation   │
                            └──────────────────────┘
                                        │
                                        ├─ Success
                                        │    │
                                        │    ▼
                                        │  ┌──────────────────┐
                                        │  │ 200 OK           │
                                        │  │ Return data      │
                                        │  └──────────────────┘
                                        │
                                        └─ Failure
                                             │
                                             ▼
                                         ┌──────────────────┐
                                         │ 500 Server Error │
                                         │ - DB connection  │
                                         │ - Constraint     │
                                         │ - Other errors   │
                                         └──────────────────┘
```

---

## Summary

This architecture provides:
- ✅ **Clean Separation of Concerns** - Each layer has clear responsibilities
- ✅ **Port & Adapter Pattern** - Database is abstracted via interfaces
- ✅ **Testability** - Each layer can be tested independently
- ✅ **Maintainability** - Easy to understand and modify
- ✅ **Scalability** - Can add new features without affecting existing code
- ✅ **Flexibility** - Can swap MongoDB for another database

