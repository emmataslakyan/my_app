# Backend Architecture - Complete Explanation

## Overview

The backend consists of two main components working together:
1. **Supabase Database** - PostgreSQL database with security policies
2. **Supabase Edge Function** - Serverless function that populates data

## Part 1: Database Schema

### File: `supabase/migrations/20260324100845_create_opportunities_table.sql`

This migration sets up everything needed for the opportunities feature in PostgreSQL.

### 1.1 Table Creation

```sql
CREATE TABLE IF NOT EXISTS opportunities (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  title text NOT NULL,
  description text DEFAULT '',
  url text NOT NULL,
  source text NOT NULL,
  format text DEFAULT 'Online',
  category text DEFAULT 'Other',
  cost text DEFAULT 'Paid',
  deadline timestamptz,
  location text DEFAULT '',
  image_url text DEFAULT '',
  created_at timestamptz DEFAULT now(),
  updated_at timestamptz DEFAULT now()
);
```

**Column Breakdown:**

| Column | Type | Purpose |
|--------|------|---------|
| `id` | uuid | Unique auto-generated identifier for each opportunity |
| `title` | text NOT NULL | Opportunity name (required) |
| `description` | text | Full description of the opportunity |
| `url` | text NOT NULL | Link to external website (required, acts as unique key) |
| `source` | text | Which website it came from (greenwich.am or borderless.so) |
| `format` | text | Online, In-Person, or Hybrid (for filtering) |
| `category` | text | Type of opportunity (for filtering) |
| `cost` | text | Free or Paid (for filtering) |
| `deadline` | timestamptz | When the application deadline is (nullable) |
| `location` | text | Geographic location if applicable |
| `image_url` | text | URL to opportunity banner image |
| `created_at` | timestamptz | When record was created (auto-set) |
| `updated_at` | timestamptz | When record was last updated |

**Key Points:**
- `DEFAULT gen_random_uuid()` - Automatically generates unique IDs
- `DEFAULT now()` - Automatically sets current timestamp
- `NOT NULL` - Fields that must have values
- `IF NOT EXISTS` - Won't error if table already exists

### 1.2 Row Level Security (RLS)

```sql
ALTER TABLE opportunities ENABLE ROW LEVEL SECURITY;
```

This locks down the table. By default, NO ONE can access it. You must explicitly grant permissions.

### 1.3 Security Policies

#### Policy 1: Authenticated Users Can Read

```sql
CREATE POLICY "Authenticated users can view opportunities"
  ON opportunities
  FOR SELECT
  TO authenticated
  USING (true);
```

**Breakdown:**
- `FOR SELECT` - This policy applies to READ operations only
- `TO authenticated` - Only users logged into the app can use this
- `USING (true)` - Show all rows (no filtering, everyone sees everything)
- Result: Any logged-in user can view all opportunities

#### Policy 2: Service Role Can Insert

```sql
CREATE POLICY "Service role can insert opportunities"
  ON opportunities
  FOR INSERT
  TO service_role
  WITH CHECK (true);
```

**Breakdown:**
- `FOR INSERT` - Applies to CREATE operations only
- `TO service_role` - Special backend role (only Edge Function can use this)
- `WITH CHECK (true)` - Allow any data to be inserted
- Result: Only the backend Edge Function can add opportunities

#### Policy 3: Service Role Can Update

```sql
CREATE POLICY "Service role can update opportunities"
  ON opportunities
  FOR UPDATE
  TO service_role
  USING (true)
  WITH CHECK (true);
```

**Breakdown:**
- `FOR UPDATE` - Applies to UPDATE operations only
- `TO service_role` - Only backend Edge Function
- `USING (true)` and `WITH CHECK (true)` - Allow updates on any rows
- Result: Only backend can update existing opportunities

**Why This Setup?**
- Users can only read data, can't modify it
- Only the backend server can write data
- Prevents malicious users from inserting fake opportunities
- Ensures data integrity

### 1.4 Performance Indexes

```sql
CREATE INDEX IF NOT EXISTS idx_opportunities_format ON opportunities(format);
CREATE INDEX IF NOT EXISTS idx_opportunities_category ON opportunities(category);
CREATE INDEX IF NOT EXISTS idx_opportunities_cost ON opportunities(cost);
CREATE INDEX IF NOT EXISTS idx_opportunities_created_at ON opportunities(created_at DESC);
```

**What are indexes?**

Indexes are like a book's table of contents. Instead of reading every opportunity to find "Online" ones, the database looks up the index:

- `idx_opportunities_format` - Speed up filtering by format
- `idx_opportunities_category` - Speed up filtering by category
- `idx_opportunities_cost` - Speed up filtering by cost
- `idx_opportunities_created_at` - Speed up sorting by date

**Performance Impact:**
- Without indexes: Need to scan all 10,000+ opportunities (slow)
- With indexes: Lookup is instant (fast)

---

## Part 2: Edge Function

### File: `supabase/functions/fetch-opportunities/index.ts`

This is a serverless function running on Supabase's infrastructure. It's written in TypeScript/Deno.

### 2.1 Imports and Types

```typescript
import { createClient } from "npm:@supabase/supabase-js@2";

interface Opportunity {
  title: string;
  description: string;
  url: string;
  source: string;
  format: string;
  category: string;
  cost: string;
  deadline?: string;
  location?: string;
  image_url?: string;
}
```

**Breakdown:**
- Import Supabase client library to connect to database
- Define TypeScript interface for type safety
- `deadline?` and `location?` are optional (note the `?`)

### 2.2 CORS Headers

```typescript
const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
  "Access-Control-Allow-Headers": "Content-Type, Authorization, X-Client-Info, Apikey",
};
```

**What is CORS?**

CORS (Cross-Origin Resource Sharing) allows your Android app (running on the phone) to talk to the backend (running on Supabase servers).

**Each Header:**
- `Access-Control-Allow-Origin: "*"` - Allow requests from anywhere
- `Access-Control-Allow-Methods` - Allow GET, POST, OPTIONS requests
- `Access-Control-Allow-Headers` - Allow these header types in requests

### 2.3 Main Function Handler

```typescript
Deno.serve(async (req: Request) => {
  // Handle preflight requests
  if (req.method === "OPTIONS") {
    return new Response(null, { status: 200, headers: corsHeaders });
  }
```

**How it works:**

1. Browser does "preflight" check before sending real request
2. Sends OPTIONS request to see if backend allows it
3. Function returns headers saying "yes, I allow it"
4. Browser then sends actual request

### 2.4 Authentication

```typescript
const supabaseUrl = Deno.env.get("SUPABASE_URL")!;
const supabaseKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const supabase = createClient(supabaseUrl, supabaseKey);
```

**Breakdown:**
- `Deno.env.get()` - Read environment variables (never hardcode these!)
- `SUPABASE_URL` - The database server address
- `SUPABASE_SERVICE_ROLE_KEY` - Secret key for backend-only operations
- `createClient()` - Create connection to Supabase

**Security Note:**
- `SERVICE_ROLE_KEY` is more powerful than `ANON_KEY`
- Only available to backend functions, not exposed to frontend
- Can insert/update data bypassing certain RLS policies
- Android app uses `ANON_KEY` which is safe to expose (can only read)

### 2.5 Sample Data Array

```typescript
const opportunities: Opportunity[] = [
  {
    title: "MIT Summer Research Program",
    description: "Engage in cutting-edge research...",
    url: "https://greenwich.am/programs",
    source: "greenwich.am",
    format: "In-Person",
    category: "Summer Programs",
    cost: "Paid",
    location: "Cambridge, MA",
    image_url: "",
  },
  // ... 14 more opportunities
];
```

**What this does:**
- Defines 15 example opportunities
- Each with all required fields
- Ready to be inserted into database

**Future Enhancement:**
- Replace this with real web scraping
- Use Deno's fetch API to download HTML
- Parse HTML to extract opportunity details
- Dynamic data instead of hardcoded samples

### 2.6 Database Insert with Upsert

```typescript
const { data, error } = await supabase.from("opportunities").upsert(
  opportunities.map((opp) => ({
    ...opp,
    updated_at: new Date().toISOString(),
  })),
  { onConflict: "url" }
);
```

**Breaking it down:**

1. **`supabase.from("opportunities")`** - Select the opportunities table

2. **`.upsert()`** - "Insert or update" operation:
   - If URL doesn't exist → INSERT (create new)
   - If URL exists → UPDATE (replace existing)

3. **`opportunities.map()`** - Transform each opportunity:
   - Spread existing fields with `...opp`
   - Add/update `updated_at` field with current time

4. **`{ onConflict: "url" }`** - Use URL as unique identifier:
   - If two opportunities have same URL, treat as duplicate
   - Update the existing one instead of creating duplicate

**Why Upsert?**
- Function might run multiple times
- First run: Creates all 15 opportunities
- Second run: Updates them with same data
- No duplicates created

### 2.7 Error Handling

```typescript
if (error) throw error;
```

If database operation fails, throw error to be caught later.

### 2.8 Success Response

```typescript
return new Response(
  JSON.stringify({
    success: true,
    message: `Successfully loaded ${opportunities.length} opportunities`,
    count: opportunities.length,
  }),
  {
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  }
);
```

**What gets sent back:**
- HTTP status 200 (OK)
- JSON with success flag
- Message indicating how many opportunities loaded
- CORS headers included

### 2.9 Error Response

```typescript
} catch (error) {
  return new Response(
    JSON.stringify({ success: false, error: error.message }),
    {
      status: 500,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    }
  );
}
```

**What gets sent on failure:**
- HTTP status 500 (Server Error)
- JSON with error message
- CORS headers so Android can read error

---

## Part 3: Data Flow

### When User Opens Opportunities Screen

```
1. Android App Launches OpportunitiesActivity
   ↓
2. Activity fetches from REST API: GET /rest/v1/opportunities
   (Using ANON_KEY - can read, cannot write)
   ↓
3. Supabase REST API
   ↓
4. Checks RLS Policy "Authenticated users can view opportunities"
   ↓
5. Returns all opportunities as JSON array
   ↓
6. Android app receives data, displays in RecyclerView with filtering
```

### When Opportunities Table is Empty

```
1. OpportunitiesActivity gets empty array
   ↓
2. Calls Edge Function: POST /functions/v1/fetch-opportunities
   (Using ANON_KEY from Android)
   ↓
3. Edge Function receives request
   ↓
4. Creates Supabase client with SERVICE_ROLE_KEY
   ↓
5. Inserts 15 sample opportunities into database
   (Using SERVICE_ROLE_KEY - can bypass some RLS checks)
   ↓
6. Returns success response
   ↓
7. Android app automatically reloads
   ↓
8. Now opportunities are visible
```

---

## Part 4: Security Breakdown

### What Can Android App Do?

Using `SUPABASE_ANON_KEY` from BuildConfig:
- ✅ Read all opportunities (SELECT allowed by RLS)
- ❌ Cannot create opportunities (INSERT policy requires service_role)
- ❌ Cannot modify opportunities (UPDATE policy requires service_role)
- ❌ Cannot delete opportunities (no DELETE policy)

### What Can Backend Function Do?

Using `SUPABASE_SERVICE_ROLE_KEY`:
- ✅ Read all data
- ✅ Create opportunities (INSERT policy allows service_role)
- ✅ Update opportunities (UPDATE policy allows service_role)
- ✅ Delete opportunities (no DELETE policy, but could add)

### Attack Prevention

**Scenario: Malicious user reverse-engineers app, gets ANON_KEY**
- They can only read opportunities
- Cannot inject fake data
- Cannot delete opportunities
- Cannot take down service

**Result:** Even if security is compromised, damage is minimal

---

## Part 5: How to Extend Backend

### Add Real Web Scraping

Current implementation uses hardcoded data. To scrape real websites:

```typescript
// Example: Scrape greenwich.am
const response = await fetch('https://greenwich.am/programs');
const html = await response.text();

// Parse HTML (would need HTML parser library)
// Extract opportunity data
// Transform to Opportunity[] format
// Insert same way as current code
```

### Connect to External APIs

Instead of hardcoded data or web scraping:

```typescript
// Example: Use a job board API
const apiResponse = await fetch('https://api.example.com/opportunities', {
  headers: { 'Authorization': `Bearer ${API_KEY}` }
});
const externalData = await apiResponse.json();

// Transform external format to Opportunity format
const transformed = externalData.map(item => ({
  title: item.name,
  description: item.details,
  // ... map other fields
}));

// Insert transformed data
await supabase.from("opportunities").upsert(transformed);
```

### Schedule Periodic Updates

Use Supabase's cron to run function automatically:

```yaml
# In supabase.yaml
functions:
  - name: fetch-opportunities
    cron: "0 0 * * *"  # Run daily at midnight
```

---

## Part 6: Database Monitoring

### View Data in Supabase Dashboard

1. Go to supabase.com dashboard
2. Select your project
3. Click "Table Editor"
4. Click "opportunities" table
5. See all 15 opportunities
6. Click any row to view/edit details

### Query Data Directly

```sql
-- View all opportunities
SELECT * FROM opportunities;

-- Count by category
SELECT category, COUNT(*) FROM opportunities GROUP BY category;

-- Find free opportunities
SELECT * FROM opportunities WHERE cost = 'Free';

-- Find online opportunities
SELECT * FROM opportunities WHERE format = 'Online';
```

### Monitor Performance

```sql
-- Check index usage
EXPLAIN SELECT * FROM opportunities WHERE format = 'Online';

-- View table size
SELECT pg_size_pretty(pg_total_relation_size('opportunities'));

-- Check RLS policies
SELECT * FROM pg_policies WHERE tablename = 'opportunities';
```

---

## Summary

### Database (Migration)
- Creates `opportunities` table with 12 fields
- Enables Row Level Security
- Defines 3 policies (read for users, write for backend only)
- Creates 4 performance indexes

### Edge Function
- Runs serverless on Supabase infrastructure
- Uses SERVICE_ROLE_KEY for backend privileges
- Provides 15 sample opportunities
- Implements CORS for Android app
- Error handling with proper HTTP status codes
- Can be extended for real web scraping or API integration

### Security Model
- Android app uses read-only ANON_KEY
- Edge Function uses powerful SERVICE_ROLE_KEY
- RLS policies prevent unauthorized data modification
- Even if frontend is compromised, backend is safe

### Data Flow
- Android app → REST API → Database
- Empty database → Triggers Edge Function → Populates data
- Automatic retry and reload
