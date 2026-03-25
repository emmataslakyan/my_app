# Backend Quick Reference - Copy & Paste Guide

## Database Schema

```sql
CREATE TABLE opportunities (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  title text NOT NULL,
  description text DEFAULT '',
  url text NOT NULL UNIQUE,
  source text NOT NULL,
  format text DEFAULT 'Online',      -- Online | In-Person | Hybrid
  category text DEFAULT 'Other',     -- 12 categories
  cost text DEFAULT 'Paid',          -- Free | Paid
  deadline timestamptz,
  location text DEFAULT '',
  image_url text DEFAULT '',
  created_at timestamptz DEFAULT now(),
  updated_at timestamptz DEFAULT now()
);

-- Enable Row Level Security
ALTER TABLE opportunities ENABLE ROW LEVEL SECURITY;

-- Policies
CREATE POLICY "Authenticated users can view opportunities"
  ON opportunities FOR SELECT TO authenticated USING (true);

CREATE POLICY "Service role can insert opportunities"
  ON opportunities FOR INSERT TO service_role WITH CHECK (true);

CREATE POLICY "Service role can update opportunities"
  ON opportunities FOR UPDATE TO service_role USING (true) WITH CHECK (true);

-- Indexes for performance
CREATE INDEX idx_opportunities_format ON opportunities(format);
CREATE INDEX idx_opportunities_category ON opportunities(category);
CREATE INDEX idx_opportunities_cost ON opportunities(cost);
CREATE INDEX idx_opportunities_created_at ON opportunities(created_at DESC);
```

---

## SQL Queries

### Read All
```sql
SELECT * FROM opportunities;
```

### Read with Filters
```sql
-- Online only
SELECT * FROM opportunities WHERE format = 'Online';

-- Free only
SELECT * FROM opportunities WHERE cost = 'Free';

-- Summer programs that are free
SELECT * FROM opportunities
WHERE category = 'Summer Programs' AND cost = 'Free';

-- Sort by newest first
SELECT * FROM opportunities
ORDER BY created_at DESC;
```

### Count by Category
```sql
SELECT category, COUNT(*) as count
FROM opportunities
GROUP BY category
ORDER BY count DESC;
```

### Check Data Integrity
```sql
-- All required fields filled?
SELECT * FROM opportunities WHERE title IS NULL OR url IS NULL;

-- Any duplicates?
SELECT url, COUNT(*) FROM opportunities GROUP BY url HAVING COUNT(*) > 1;

-- Check RLS is enabled
SELECT tablename FROM pg_tables
WHERE tablename = 'opportunities'
AND schemaname = 'public';
```

---

## REST API Calls

### JavaScript/Fetch
```javascript
// Read opportunities
const response = await fetch(
  'https://xxx.supabase.co/rest/v1/opportunities',
  {
    headers: {
      'Authorization': `Bearer ${ANON_KEY}`,
      'apikey': ANON_KEY,
    }
  }
);
const data = await response.json();
console.log(data);
```

### cURL
```bash
# Read opportunities
curl -X GET 'https://xxx.supabase.co/rest/v1/opportunities' \
  -H 'Authorization: Bearer YOUR_ANON_KEY' \
  -H 'apikey: YOUR_ANON_KEY'

# Call Edge Function
curl -X POST 'https://xxx.supabase.co/functions/v1/fetch-opportunities' \
  -H 'Authorization: Bearer YOUR_ANON_KEY' \
  -H 'apikey: YOUR_ANON_KEY' \
  -d '{}'
```

### Java/OkHttp (Android)
```java
// Read opportunities
OkHttpClient client = new OkHttpClient();
Request request = new Request.Builder()
    .url("https://xxx.supabase.co/rest/v1/opportunities")
    .header("Authorization", "Bearer " + SUPABASE_ANON_KEY)
    .header("apikey", SUPABASE_ANON_KEY)
    .get()
    .build();

client.newCall(request).enqueue(new Callback() {
    @Override
    public void onResponse(Call call, Response response) throws IOException {
        String body = response.body().string();
        JSONArray array = new JSONArray(body);
        // Parse...
    }
});
```

---

## Edge Function Template

```typescript
import { createClient } from "npm:@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
  "Access-Control-Allow-Headers": "Content-Type, Authorization, X-Client-Info, Apikey",
};

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") {
    return new Response(null, { status: 200, headers: corsHeaders });
  }

  try {
    const supabaseUrl = Deno.env.get("SUPABASE_URL")!;
    const supabaseKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
    const supabase = createClient(supabaseUrl, supabaseKey);

    // TODO: Add your logic here

    return new Response(
      JSON.stringify({ success: true }),
      { headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  } catch (error) {
    return new Response(
      JSON.stringify({ success: false, error: error.message }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  }
});
```

---

## Configuration

### app/build.gradle.kts
```kotlin
android {
    defaultConfig {
        buildConfigField("String", "SUPABASE_URL", "\"https://your-project.supabase.co\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"your-anon-key\"")
    }

    buildFeatures {
        buildConfig = true
    }
}
```

### AndroidManifest.xml
```xml
<activity
    android:name=".OpportunitiesActivity"
    android:exported="false" />
```

---

## Debugging

### Check Database Status
```sql
-- Is RLS enabled?
SELECT tablename, rowsecurity
FROM pg_tables
WHERE tablename = 'opportunities';

-- How many opportunities?
SELECT COUNT(*) FROM opportunities;

-- Check all policies
SELECT policyname, cmd, qual
FROM pg_policies
WHERE tablename = 'opportunities';

-- Check indexes
SELECT indexname, indexdef
FROM pg_indexes
WHERE tablename = 'opportunities';

-- Check table size
SELECT pg_size_pretty(pg_total_relation_size('opportunities'));
```

### Monitor Edge Function
```bash
# View recent logs
supabase functions logs fetch-opportunities

# Test function locally
supabase functions serve fetch-opportunities

# Deploy function
supabase functions deploy fetch-opportunities
```

### Android Logcat
```bash
# Show all app logs
adb logcat | grep OpportunitiesActivity

# Show only errors
adb logcat *:S *:E | grep OpportunitiesActivity

# Clear logs
adb logcat -c

# Save logs to file
adb logcat > logs.txt
```

---

## Data Model

### Opportunity JSON Object
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "title": "MIT Summer Research Program",
  "description": "Engage in cutting-edge research alongside MIT faculty",
  "url": "https://greenwich.am/programs",
  "source": "greenwich.am",
  "format": "In-Person",
  "category": "Summer Programs",
  "cost": "Paid",
  "deadline": null,
  "location": "Cambridge, MA",
  "image_url": "",
  "created_at": "2024-03-25T10:00:00+00:00",
  "updated_at": "2024-03-25T10:00:00+00:00"
}
```

### Categories
```
Summer Programs
Competitions
Essay Contests
Olympiads
Research
Accelerators
Fellowships
Model UN
Internships
Volunteering
Exchange Programs
Other
```

### Formats
```
Online
In-Person
Hybrid
```

### Costs
```
Free
Paid
```

---

## Common Tasks

### Insert Opportunity Manually
```sql
INSERT INTO opportunities (
    title, description, url, source,
    format, category, cost, location
) VALUES (
    'New Program',
    'Description here',
    'https://example.com/program',
    'example.com',
    'Online',
    'Competitions',
    'Free',
    'Remote'
);
```

### Update Opportunity
```sql
UPDATE opportunities
SET description = 'New description',
    updated_at = now()
WHERE url = 'https://example.com/program';
```

### Delete Opportunity
```sql
DELETE FROM opportunities
WHERE url = 'https://example.com/program';
```

### Upsert (Insert or Update)
```sql
INSERT INTO opportunities (
    title, description, url, source,
    format, category, cost
) VALUES (
    'Program', 'Desc', 'https://example.com',
    'example.com', 'Online', 'Competitions', 'Free'
)
ON CONFLICT (url)
DO UPDATE SET
    description = 'Desc',
    updated_at = now();
```

---

## Environment Variables

### Local (.env)
```
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_ANON_KEY=your-anon-key-here
SUPABASE_SERVICE_ROLE_KEY=your-service-role-key-here
```

### Supabase Edge Function (Automatic)
```
SUPABASE_URL (auto)
SUPABASE_ANON_KEY (auto)
SUPABASE_SERVICE_ROLE_KEY (auto)
```

### Android BuildConfig (from gradle)
```kotlin
BuildConfig.SUPABASE_URL
BuildConfig.SUPABASE_ANON_KEY
```

---

## Performance Tips

### Optimize Queries
```sql
-- SLOW: Scanning all rows
SELECT * FROM opportunities WHERE title LIKE '%research%';

-- FASTER: Use index on created_at
SELECT * FROM opportunities
WHERE created_at > NOW() - INTERVAL '30 days'
ORDER BY created_at DESC;
```

### Batch Operations
```typescript
// SLOW: 15 separate inserts
for (const opp of opportunities) {
    await supabase.from("opportunities").insert(opp);
}

// FAST: One batch insert
await supabase.from("opportunities").insert(opportunities);
```

### Pagination
```sql
-- Load 20 at a time
SELECT * FROM opportunities
LIMIT 20 OFFSET 0;  -- Page 1

SELECT * FROM opportunities
LIMIT 20 OFFSET 20;  -- Page 2

SELECT * FROM opportunities
LIMIT 20 OFFSET 40;  -- Page 3
```

---

## Security Checklist

- [ ] RLS is enabled on opportunities table
- [ ] All policies are in place (SELECT, INSERT, UPDATE)
- [ ] Android app uses ANON_KEY (read-only)
- [ ] Backend uses SERVICE_ROLE_KEY (write)
- [ ] No hardcoded keys in source code
- [ ] All API calls use HTTPS
- [ ] Validate input on backend
- [ ] Handle errors gracefully
- [ ] Log security events
- [ ] Regular backups enabled

---

## Useful Commands

### Supabase CLI
```bash
# Login
supabase login

# Link to project
supabase link --project-ref abc123

# Deploy Edge Function
supabase functions deploy fetch-opportunities

# View logs
supabase functions logs fetch-opportunities

# Run migrations
supabase db push
supabase db pull

# List functions
supabase functions list
```

### PostgreSQL
```bash
# Connect to database
psql -U postgres -h localhost -d postgres

# List tables
\dt

# Show table structure
\d opportunities

# Show policies
\d opportunities
```

### Android Studio
```bash
# Clean project
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# Run tests
./gradlew test

# View logs
adb logcat
```

---

## Response Codes

| Code | Meaning | Action |
|------|---------|--------|
| 200 | Success | Show data |
| 201 | Created | Resource inserted |
| 204 | No Content | Success, no data |
| 400 | Bad Request | Fix request format |
| 401 | Unauthorized | Check API key |
| 403 | Forbidden | RLS policy denied |
| 404 | Not Found | Check URL |
| 500 | Server Error | Backend issue |
| 503 | Unavailable | Server down |

---

## Contacts & Resources

**Supabase Docs:** https://supabase.com/docs
**PostgreSQL Docs:** https://www.postgresql.org/docs/
**Android Docs:** https://developer.android.com/docs
**OkHttp Docs:** https://square.github.io/okhttp/
