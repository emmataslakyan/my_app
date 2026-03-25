# Backend Architecture Diagrams

## System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        ANDROID APP                              │
│  (OpportunitiesActivity + OpportunityAdapter + UI Components)   │
└──────────────────────────────┬──────────────────────────────────┘
                               │ HTTP/REST
                               │ Uses: ANON_KEY (read-only)
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│                    SUPABASE PLATFORM                             │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │           REST API Layer                                │   │
│  │  GET  /rest/v1/opportunities                            │   │
│  │  POST  /functions/v1/fetch-opportunities               │   │
│  └──────────┬───────────────────────────────────────────────┘   │
│             │                                                    │
│             ├─────────────────────┬──────────────────────────┐   │
│             │                     │                          │   │
│  ┌──────────▼─────────┐  ┌────────▼─────────┐   ┌──────────▼──┐│
│  │   PostgreSQL DB    │  │  Edge Function   │   │   Auth      ││
│  │                    │  │  (Deno Runtime)  │   │   System    ││
│  │ ┌────────────────┐ │  │                  │   │             ││
│  │ │ opportunities  │ │  │ ┌──────────────┐ │   │ ┌─────────┐ ││
│  │ │ table:         │ │  │ │ fetch-       │ │   │ │ user    │ ││
│  │ │ - id           │ │  │ │ opportunities│ │   │ │ accounts│ ││
│  │ │ - title        │ │  │ │ function     │ │   │ │ & auth  │ ││
│  │ │ - description  │ │  │ └──────────────┘ │   │ │ tokens  │ ││
│  │ │ - url          │ │  │                  │   │ └─────────┘ ││
│  │ │ - format       │ │  │ Uses:            │   │             ││
│  │ │ - category     │ │  │ SERVICE_ROLE_KEY │   │             ││
│  │ │ - cost         │ │  │                  │   │             ││
│  │ │ - location     │ │  └──────────────────┘   │             ││
│  │ │ - created_at   │ │                         │             ││
│  │ └────────────────┘ │                         │             ││
│  │                    │                         │             ││
│  │  RLS Policies:     │                         │             ││
│  │  ✓ Users: SELECT   │                         │             ││
│  │  ✓ Func: INSERT    │                         │             ││
│  │  ✓ Func: UPDATE    │                         │             ││
│  │                    │                         │             ││
│  │  Performance       │                         │             ││
│  │  Indexes:          │                         │             ││
│  │  - format          │                         │             ││
│  │  - category        │                         │             ││
│  │  - cost            │                         │             ││
│  │  - created_at      │                         │             ││
│  └────────────────────┘                         │             ││
│                                                 └─────────────┘│
└─────────────────────────────────────────────────────────────────┘
```

---

## Request-Response Flow - Data Load

### Scenario 1: Database Already Has Data

```
┌─────────────────────────────────────────────────────────────┐
│              ANDROID APP (OpportunitiesActivity)             │
└────────────────────────────┬────────────────────────────────┘
                             │
                    onCreate() runs
                             │
                             ▼
        ┌────────────────────────────────────┐
        │ loadOpportunities()                │
        │ Shows ProgressBar                  │
        └────────────┬───────────────────────┘
                     │
                     │ HTTP GET Request
                     │ URL: https://xxx.supabase.co/rest/v1/opportunities
                     │ Headers:
                     │   - apikey: ANON_KEY
                     │   - Authorization: Bearer ANON_KEY
                     │
                     ▼
┌────────────────────────────────────────────────────────────┐
│          SUPABASE REST API (PostgreSQL Direct)             │
│                                                            │
│  1. Receive GET request from Android                      │
│  2. Check authentication: Is ANON_KEY valid?  ✓          │
│  3. Check RLS Policy:                                    │
│     "Authenticated users can view opportunities"         │
│  4. Policy check: true (allows all authenticated users)  │
│  5. Execute query: SELECT * FROM opportunities           │
│  6. Database uses indexes to find data                   │
│  7. Return JSON array                                    │
└────────────────────────────────────────────────────────────┘
                     │
                     │ HTTP 200 OK + JSON Array
                     │ [
                     │   {
                     │     "id": "uuid",
                     │     "title": "MIT Summer Research Program",
                     │     "format": "In-Person",
                     │     "category": "Summer Programs",
                     │     ...
                     │   },
                     │   ...
                     │ ]
                     ▼
        ┌─────────────────────────────────────┐
        │ ANDROID (onResponse)                │
        │                                     │
        │ 1. Parse JSON                       │
        │ 2. Create Opportunity objects       │
        │ 3. Update adapter.data              │
        │ 4. adapter.notifyDataSetChanged()   │
        │ 5. Hide ProgressBar                 │
        │ 6. Show RecyclerView with items     │
        └─────────────────────────────────────┘
                     │
                     ▼
        ┌─────────────────────────────────────┐
        │ USER SEES:                          │
        │ - List of 15 opportunities          │
        │ - Filter chips                      │
        │ - Cards with titles, descriptions   │
        └─────────────────────────────────────┘
```

---

### Scenario 2: Database is Empty (First Time)

```
┌──────────────────────────────────────────────────────┐
│  ANDROID APP (OpportunitiesActivity)                 │
└──────────────────┬───────────────────────────────────┘
                   │
                   ▼
        loadOpportunities()
                   │
                   ▼
        ┌─────────────────────────────────┐
        │ GET /rest/v1/opportunities      │
        └────────────┬────────────────────┘
                     │
                     ▼
        ┌─────────────────────────────────────────┐
        │ Database returns: []  (empty array)     │
        └────────────┬────────────────────────────┘
                     │
                     ▼
        ┌─────────────────────────────────────────┐
        │ opportunityList.isEmpty() ? true        │
        │                                         │
        │ YES → Call fetchAndPopulateOpportunities│
        └────────────┬────────────────────────────┘
                     │
                     ▼ HTTP POST Request

        ┌──────────────────────────────────────────────┐
        │ POST https://xxx.supabase.co/functions/v1/   │
        │      fetch-opportunities                     │
        │                                              │
        │ Headers:                                     │
        │   - Authorization: Bearer ANON_KEY           │
        │   - apikey: ANON_KEY                         │
        └───────────┬──────────────────────────────────┘
                    │
                    ▼
┌──────────────────────────────────────────────────────────────────┐
│          SUPABASE EDGE FUNCTION (Deno Runtime)                   │
│                                                                  │
│  1. Receive POST request from Android                          │
│  2. Extract SUPABASE_URL from environment                      │
│  3. Extract SUPABASE_SERVICE_ROLE_KEY from environment         │
│  4. Create Supabase client with SERVICE_ROLE_KEY               │
│  5. Define 15 sample opportunities (JavaScript array)          │
│  6. Call: supabase.from("opportunities").upsert(...)           │
│     - Uses SERVICE_ROLE_KEY (more powerful)                    │
│     - RLS Policy allows INSERT for service_role ✓              │
│  7. Inserts 15 opportunities into database                     │
│  8. Returns JSON response: { success: true, count: 15 }        │
└───────────┬────────────────────────────────────────────────────┘
            │
            ▼ HTTP 200 OK + success response

        ┌──────────────────────────────────────────┐
        │ ANDROID (Edge Function Response)         │
        │                                          │
        │ Response.isSuccessful() ? true          │
        │ Call loadOpportunities() again           │
        └──────────────┬───────────────────────────┘
                       │
                       ▼
        ┌──────────────────────────────────────────┐
        │ GET /rest/v1/opportunities               │
        │ (Same as scenario 1, but now has data!)  │
        └──────────────┬───────────────────────────┘
                       │
                       ▼
        ┌──────────────────────────────────────────┐
        │ Returns 15 opportunities                 │
        └──────────────┬───────────────────────────┘
                       │
                       ▼
        ┌──────────────────────────────────────────┐
        │ Display opportunities on screen          │
        └──────────────────────────────────────────┘
```

---

## Security Policy Evaluation

### Flow: User Tries to Read Opportunities

```
User makes request to READ opportunities
                │
                ▼
    ┌─────────────────────────────┐
    │ Supabase REST API receives: │
    │ GET /rest/v1/opportunities  │
    │ Headers include ANON_KEY    │
    └────────────┬────────────────┘
                 │
                 ▼ Step 1: Authenticate
         ┌───────────────────────────────┐
         │ Is ANON_KEY valid?            │
         │ Check: firebase auth.users    │
         │ Result: ✓ YES (user logged in)│
         └────────┬──────────────────────┘
                  │
                  ▼ Step 2: Identify User
          ┌──────────────────────────────┐
          │ Get auth.uid() from JWT      │
          │ Result: user-uuid-12345      │
          └────────┬─────────────────────┘
                   │
                   ▼ Step 3: Check RLS Policies
       ┌───────────────────────────────────────────┐
       │ Policy: "Authenticated users can view     │
       │          opportunities"                   │
       │                                           │
       │ FOR SELECT    ✓ (matches GET request)     │
       │ TO authenticated  ✓ (user is logged in)   │
       │ USING (true)  ✓ (no row filtering)        │
       │                                           │
       │ RESULT: ✓ POLICY ALLOWS                   │
       └────────┬────────────────────────────────┘
                │
                ▼
         ┌────────────────────────────┐
         │ Execute: SELECT *          │
         │ FROM opportunities         │
         │                            │
         │ Return all 15 rows         │
         └────────┬───────────────────┘
                  │
                  ▼
         ┌────────────────────────────┐
         │ User SUCCEEDS in reading   │
         └────────────────────────────┘
```

### Flow: User Tries to Insert Opportunity

```
User makes request to CREATE opportunity
                │
                ▼
    ┌──────────────────────────────────────┐
    │ Supabase REST API receives:          │
    │ POST /rest/v1/opportunities          │
    │ Headers include ANON_KEY             │
    │ Body: { title: "Hacker Program", ... }
    └────────────┬─────────────────────────┘
                 │
                 ▼ Step 1-2: Authenticate & Identify (same as above)
         ┌────────────────────────────────┐
         │ User: authenticated            │
         │ auth.uid(): user-uuid-12345    │
         └────────┬───────────────────────┘
                  │
                  ▼ Step 3: Check RLS Policies
      ┌────────────────────────────────────────────┐
      │ Looking for INSERT policy...               │
      │                                            │
      │ Policy: "Service role can insert           │
      │          opportunities"                    │
      │                                            │
      │ FOR INSERT      ✓ (matches POST request)   │
      │ TO service_role ✗ (user is NOT service_   │
      │                  role, just regular user) │
      │                                            │
      │ RESULT: ✗ POLICY DENIES                    │
      └────────┬────────────────────────────────┘
               │
               ▼
        ┌─────────────────────────────────────┐
        │ PostgreSQL rejects INSERT:          │
        │ HTTP 403 Forbidden                  │
        │ Message: "new row violates row      │
        │          level security policy"     │
        └─────────────────────────────────────┘
               │
               ▼
        ┌─────────────────────────────────────┐
        │ User FAILS to insert opportunity    │
        │ Data remains protected              │
        └─────────────────────────────────────┘
```

---

## Database Query Examples

### Example 1: Get All Online Opportunities

**SQL Query:**
```sql
SELECT * FROM opportunities
WHERE format = 'Online'
ORDER BY created_at DESC
LIMIT 10;
```

**Android Code:**
```kotlin
// No need to write SQL! Supabase REST API does it:
GET /rest/v1/opportunities?format=eq.Online&order=created_at.desc&limit=10
```

**What happens:**
1. REST API converts query string to SQL
2. Checks RLS policy (ALLOW)
3. Uses `idx_opportunities_format` index for speed
4. Returns JSON array of matching opportunities

---

### Example 2: Get Free Summer Programs

**SQL:**
```sql
SELECT * FROM opportunities
WHERE cost = 'Free'
AND category = 'Summer Programs'
ORDER BY deadline ASC;
```

**Android Code:**
```kotlin
GET /rest/v1/opportunities?cost=eq.Free&category=eq.Summer%20Programs&order=deadline.asc
```

---

### Example 3: Count Opportunities by Category

**SQL:**
```sql
SELECT category, COUNT(*) as count
FROM opportunities
GROUP BY category;
```

**Queries to run in Supabase SQL Editor:**
```
Cost breakdown:
- Free: 7 opportunities
- Paid: 8 opportunities

Format breakdown:
- Online: 5
- In-Person: 7
- Hybrid: 3
```

---

## Edge Function Execution Timeline

```
Timeline of fetch-opportunities execution:

T+0ms
  ├─ Request arrives at Supabase
  ├─ Deno runtime starts Edge Function
  ├─ Import Supabase client library
  └─ Define TypeScript interface

T+10ms
  ├─ Check if req.method === "OPTIONS"
  ├─ No → skip preflight response
  └─ Continue to main logic

T+15ms
  ├─ Fetch SUPABASE_URL from environment
  ├─ Fetch SUPABASE_SERVICE_ROLE_KEY from environment
  └─ Create Supabase client instance

T+20ms
  ├─ Load 15 sample opportunities into array
  └─ Add updated_at timestamp to each

T+25ms
  ├─ Call supabase.from("opportunities").upsert()
  ├─ Send batch INSERT/UPDATE request to PostgreSQL
  └─ PostgreSQL executes within try-catch

T+30ms
  ├─ Check if error occurred
  ├─ No error → continue
  └─ Prepare success response

T+35ms
  ├─ Create JSON response
  ├─ Add CORS headers
  ├─ Return HTTP 200
  └─ Function completes

T+40ms (total)
  Total execution time: ~40ms
  Return: { success: true, count: 15 }
```

---

## Comparison: Before vs After Edge Function

```
BEFORE (Database Empty):
┌──────────────────────────────────┐
│ User Opens Opportunities Screen  │
└────────────────────┬─────────────┘
                     │
                     ▼
         Shows ProgressBar
                     │
                     ▼
      Fetches from database
                     │
                     ▼
      Returns empty array []
                     │
                     ▼
    Screen shows: "No opportunities"
                     │
                     ▼
    User is sad :(


AFTER (Edge Function Populates Data):
┌──────────────────────────────────┐
│ User Opens Opportunities Screen  │
└────────────────────┬─────────────┘
                     │
                     ▼
         Shows ProgressBar
                     │
                     ▼
      Fetches from database
                     │
                     ▼
      Returns empty array []
                     │
                     ▼
      Detects empty: calls Edge Function
                     │
                     ▼
      Edge Function inserts 15 opportunities
                     │
                     ▼
      Automatically reloads data
                     │
                     ▼
      Fetches from database again
                     │
                     ▼
      Returns 15 opportunities
                     │
                     ▼
    Screen shows: 15 opportunity cards
                     │
                     ▼
    User is happy :)
```

---

## Performance Insights

### Query Speed with Indexes

```
Finding all "Online" opportunities:

WITHOUT Index:
  - Scan entire table: 10,000+ rows
  - Check each row: format = 'Online'?
  - Time: ~500ms (slow)

WITH Index (idx_opportunities_format):
  - Look up index: format = 'Online'
  - Get row IDs directly
  - Time: ~5ms (100x faster!)

Index cost: Small amount of disk space
Index benefit: Massive speed improvement
```

### Scaling Example

```
Current: 15 opportunities
- All queries instant
- Simple filtering

Future: 100,000+ opportunities
- WITHOUT indexes: queries timeout
- WITH indexes: still instant

This is why we created 4 indexes!
```
