# Android-Backend Communication Guide

## Overview

This document explains how the Android app communicates with Supabase backend using HTTP requests.

---

## Communication Stack

```
┌─────────────────────────────────────────────────────────┐
│           ANDROID APPLICATION                           │
│  OpportunitiesActivity.java                             │
└────────────────────┬────────────────────────────────────┘
                     │
                     │ Uses OkHttp library
                     ▼
┌─────────────────────────────────────────────────────────┐
│           HTTP PROTOCOL (HTTPS)                         │
│  Secure encrypted connection                            │
└────────────────────┬────────────────────────────────────┘
                     │
                     │ Port 443 (secure)
                     ▼
┌─────────────────────────────────────────────────────────┐
│           SUPABASE REST API                             │
│  https://your-project.supabase.co/rest/v1/             │
│  https://your-project.supabase.co/functions/v1/        │
└─────────────────────────────────────────────────────────┘
```

---

## API Endpoints

### Endpoint 1: Read Opportunities

**Type:** GET (retrieve data)
**Purpose:** Fetch opportunities from database

```
GET https://your-project.supabase.co/rest/v1/opportunities
```

**Headers Required:**
```
Authorization: Bearer YOUR_ANON_KEY
apikey: YOUR_ANON_KEY
```

**Response (Success - HTTP 200):**
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "title": "MIT Summer Research Program",
    "description": "Engage in cutting-edge research...",
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
  },
  {
    "id": "550e8400-e29b-41d4-a716-446655440001",
    "title": "Google Code Jam",
    "description": "Google's longest-running...",
    ...
  }
]
```

**Response (Error - HTTP 403 Forbidden):**
```json
{
  "code": "PGRST301",
  "message": "new row violates row level security policy"
}
```

---

### Endpoint 2: Populate Opportunities

**Type:** POST (send data to backend)
**Purpose:** Call Edge Function to populate database with sample data

```
POST https://your-project.supabase.co/functions/v1/fetch-opportunities
```

**Headers Required:**
```
Authorization: Bearer YOUR_ANON_KEY
apikey: YOUR_ANON_KEY
Content-Type: application/json
```

**Request Body:**
```json
{}
```
(Empty body - function uses internal data)

**Response (Success - HTTP 200):**
```json
{
  "success": true,
  "message": "Successfully loaded 15 opportunities",
  "count": 15
}
```

**Response (Error - HTTP 500):**
```json
{
  "success": false,
  "error": "Database connection failed"
}
```

---

## Android Implementation

### File: OpportunitiesActivity.java

#### 1. Load Opportunities from Database

```java
private void loadOpportunities() {
    progressBar.setVisibility(View.VISIBLE);

    // Get config values from BuildConfig (set in build.gradle.kts)
    String supabaseUrl = BuildConfig.SUPABASE_URL;
    String supabaseKey = BuildConfig.SUPABASE_ANON_KEY;

    // Create HTTP request
    Request request = new Request.Builder()
            .url(supabaseUrl + "/rest/v1/opportunities?select=*")
            .header("apikey", supabaseKey)
            .header("Authorization", "Bearer " + supabaseKey)
            .get()
            .build();

    // Execute request asynchronously
    client.newCall(request).enqueue(new Callback() {
        @Override
        public void onFailure(Call call, IOException e) {
            // Network error
            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(OpportunitiesActivity.this,
                        "Failed to load opportunities", Toast.LENGTH_SHORT).show();
                Log.e("OpportunitiesActivity", "Error loading", e);
            });
        }

        @Override
        public void onResponse(Call call, Response response) throws IOException {
            String responseBody = response.body().string();

            if (response.isSuccessful()) {
                try {
                    JSONArray jsonArray = new JSONArray(responseBody);
                    List<Opportunity> newOpportunities = new ArrayList<>();

                    // Parse each JSON object into Opportunity
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject obj = jsonArray.getJSONObject(i);
                        Opportunity opp = new Opportunity();
                        opp.setId(obj.optString("id"));
                        opp.setTitle(obj.optString("title"));
                        opp.setDescription(obj.optString("description"));
                        opp.setUrl(obj.optString("url"));
                        opp.setSource(obj.optString("source"));
                        opp.setFormat(obj.optString("format"));
                        opp.setCategory(obj.optString("category"));
                        opp.setCost(obj.optString("cost"));
                        opp.setLocation(obj.optString("location"));
                        opp.setImageUrl(obj.optString("image_url"));
                        newOpportunities.add(opp);
                    }

                    // Update UI on main thread
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        opportunityList.clear();
                        opportunityList.addAll(newOpportunities);
                        adapter.updateData(newOpportunities);

                        // If empty, populate database
                        if (newOpportunities.isEmpty()) {
                            fetchAndPopulateOpportunities();
                        }
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(OpportunitiesActivity.this,
                                "Error parsing data", Toast.LENGTH_SHORT).show();
                        Log.e("OpportunitiesActivity", "Parse error", e);
                    });
                }
            } else {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Log.e("OpportunitiesActivity", "Response error: " + responseBody);
                });
            }
        }
    });
}
```

**Step-by-step explanation:**

1. **Get Config Values**
   ```java
   String supabaseUrl = BuildConfig.SUPABASE_URL;
   String supabaseKey = BuildConfig.SUPABASE_ANON_KEY;
   ```
   - Retrieves values set in `app/build.gradle.kts`
   - Not hardcoded (secure practice)

2. **Build Request**
   ```java
   Request request = new Request.Builder()
           .url(supabaseUrl + "/rest/v1/opportunities?select=*")
           .header("apikey", supabaseKey)
           .header("Authorization", "Bearer " + supabaseKey)
           .get()
           .build();
   ```
   - Creates HTTP GET request
   - Adds authentication headers
   - `?select=*` means get all columns

3. **Async Execution**
   ```java
   client.newCall(request).enqueue(new Callback() { ... });
   ```
   - Sends request without blocking UI
   - Callbacks execute when response arrives

4. **Parse Response**
   ```java
   JSONArray jsonArray = new JSONArray(responseBody);
   for (int i = 0; i < jsonArray.length(); i++) {
       JSONObject obj = jsonArray.getJSONObject(i);
       Opportunity opp = new Opportunity();
       opp.setTitle(obj.optString("title"));
       // ... set other fields
   }
   ```
   - Converts JSON to Java objects
   - `optString()` safely gets values (returns "" if missing)

5. **Update UI**
   ```java
   runOnUiThread(() -> {
       adapter.updateData(newOpportunities);
       progressBar.setVisibility(View.GONE);
   });
   ```
   - Must run UI updates on main thread
   - Shows data, hides loading indicator

---

#### 2. Populate Database

```java
private void fetchAndPopulateOpportunities() {
    String supabaseUrl = BuildConfig.SUPABASE_URL;
    String supabaseKey = BuildConfig.SUPABASE_ANON_KEY;

    // Create POST request to Edge Function
    Request request = new Request.Builder()
            .url(supabaseUrl + "/functions/v1/fetch-opportunities")
            .header("apikey", supabaseKey)
            .header("Authorization", "Bearer " + supabaseKey)
            .post(okhttp3.RequestBody.create("", null))
            .build();

    client.newCall(request).enqueue(new Callback() {
        @Override
        public void onFailure(Call call, IOException e) {
            Log.e("OpportunitiesActivity", "Failed to populate", e);
        }

        @Override
        public void onResponse(Call call, Response response) throws IOException {
            if (response.isSuccessful()) {
                // Edge Function succeeded, reload data
                runOnUiThread(() -> loadOpportunities());
            }
        }
    });
}
```

**Explanation:**

1. **POST Request**
   ```java
   .post(okhttp3.RequestBody.create("", null))
   ```
   - Sends POST (not GET)
   - Empty body (function doesn't need input)

2. **Success Handler**
   ```java
   if (response.isSuccessful()) {
       runOnUiThread(() -> loadOpportunities());
   }
   ```
   - If Edge Function succeeded, fetch data again
   - Database now has 15 opportunities

3. **Automatic Flow**
   ```
   1. User opens screen
   2. loadOpportunities() → empty
   3. fetchAndPopulateOpportunities() → triggers Edge Function
   4. Edge Function inserts 15 opportunities
   5. Automatically calls loadOpportunities() again
   6. Now shows all 15 items
   ```

---

## Request/Response Lifecycle

### Complete Example: What Happens When Screen Opens

```
T+0ms: onCreate() called
       ↓
T+10ms: setupFilters() - Setup chip listeners
       ↓
T+20ms: loadOpportunities() - Start HTTP request
       │
       └─ Show ProgressBar
       └─ Build HTTP request:
          GET /rest/v1/opportunities
          Headers: apikey, Authorization
       │
       ▼
T+100ms: Network packet sent to Supabase
        Internet latency: ~80ms
       │
       ▼
T+200ms: Supabase receives request
        ├─ Check auth: ANON_KEY valid? ✓
        ├─ Check RLS: Can user read? ✓
        ├─ Execute: SELECT * FROM opportunities
        └─ Check database indexes for speed

T+210ms: Supabase prepares response
        ├─ If 15 opportunities found:
        │  └─ Convert to JSON array
        │  └─ Return HTTP 200 + JSON
        │
        └─ If 0 opportunities found:
           └─ Return HTTP 200 + empty array []

T+290ms: Network packet returned to Android
        Internet latency: ~80ms

T+300ms: Android receives response
        ├─ onResponse() callback triggered
        ├─ response.isSuccessful()? ✓
        ├─ response.body().string() → JSON
        ├─ Parse JSON → create Opportunity objects
        └─ newOpportunities.isEmpty()?
           │
           ├─ YES → fetchAndPopulateOpportunities()
           │        POST /functions/v1/fetch-opportunities
           │        (repeat process for Edge Function)
           │
           └─ NO → Update adapter with data

T+310ms: If data found:
        └─ runOnUiThread() updates UI:
           ├─ Hide ProgressBar
           ├─ Update adapter.data
           ├─ adapter.notifyDataSetChanged()
           └─ RecyclerView redraws with items

T+320ms: User sees opportunities on screen!

Total time: ~320ms (0.3 seconds)
User experience: Smooth loading with progress indicator
```

---

## Error Handling

### Common Errors and Solutions

#### Error 1: Invalid API Key

```
HTTP 401 Unauthorized
Response: { "message": "Unauthorized" }
```

**Cause:**
- Wrong `SUPABASE_ANON_KEY` in BuildConfig
- API key is malformed

**Solution:**
1. Go to Supabase dashboard
2. Settings > API
3. Copy correct `anon/public` key
4. Update `app/build.gradle.kts`
5. Clean and rebuild project

---

#### Error 2: RLS Policy Violation

```
HTTP 403 Forbidden
Response: { "code": "PGRST301",
            "message": "new row violates row level security policy" }
```

**Cause:**
- Trying to INSERT without service_role
- Android app doesn't have write permission
- Only Edge Function (with service_role_key) can write

**Solution:**
- This is normal! Android should only read
- Don't try to POST to `/rest/v1/opportunities`
- Use Edge Function for writing data

---

#### Error 3: Network Timeout

```
IOException: timeout
```

**Cause:**
- No internet connection
- Network is slow
- Supabase server unreachable

**Solution:**
```java
@Override
public void onFailure(Call call, IOException e) {
    Toast.makeText(context, "Network error. Check internet?",
                   Toast.LENGTH_SHORT).show();
}
```

---

#### Error 4: JSON Parse Error

```
JSONException: Value [] of type JSONArray
                cannot be converted to JSONObject
```

**Cause:**
- Response is empty array `[]`
- Code expects object `{}`

**Solution:**
```java
// CORRECT: Handle both array and empty responses
if (response.isSuccessful()) {
    JSONArray jsonArray = new JSONArray(responseBody);

    if (jsonArray.length() == 0) {
        // Handle empty case
        fetchAndPopulateOpportunities();
    } else {
        // Parse opportunities
    }
}
```

---

## Performance Optimization

### Request Time Breakdown

```
Total: ~320ms

Breakdown:
- Android HTTP setup: 20ms
- Network latency (up): 80ms
- Supabase processing: 10ms
- Database query: 5ms (thanks to indexes!)
- Network latency (down): 80ms
- Android JSON parsing: 120ms
- UI update: 5ms

Optimization opportunities:
1. Reduce JSON parsing: Use streaming parser
2. Cache data locally: Room database
3. Pagination: Load 20 at a time, not all
4. Compression: Gzip responses
```

### Network Analysis

```java
// Add timing to measure performance
long startTime = System.currentTimeMillis();

Request request = new Request.Builder()
        .url(url)
        .build();

client.newCall(request).enqueue(new Callback() {
    @Override
    public void onResponse(Call call, Response response)
            throws IOException {
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        Log.d("Performance", "Request took: " + duration + "ms");
        // Log to Supabase for monitoring
    }
});
```

---

## Security Best Practices

### What's Secure ✓

```java
// ✓ Good: Keys in BuildConfig (built into APK, not in code)
String key = BuildConfig.SUPABASE_ANON_KEY;

// ✓ Good: Using ANON_KEY for app (limited permissions)
String key = BuildConfig.SUPABASE_ANON_KEY;

// ✓ Good: HTTPS only (automatically with Supabase URLs)
.url("https://xxx.supabase.co/...")
```

### What's NOT Secure ✗

```java
// ✗ Bad: Hardcoded in code (visible in source)
String key = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";

// ✗ Bad: SERVICE_ROLE_KEY in app (can write to database)
String key = BuildConfig.SUPABASE_SERVICE_ROLE_KEY;

// ✗ Bad: HTTP (unencrypted)
.url("http://xxx.supabase.co/...")
```

---

## Future Enhancements

### Real-time Updates

Use Supabase RealtimeClient instead of polling:

```typescript
// In Edge Function or on backend
const channel = supabase
    .channel('opportunities')
    .on('postgres_changes',
         { event: 'INSERT', schema: 'public',
           table: 'opportunities' },
         payload => {
             // Broadcast to Android app
         }
    )
    .subscribe()
```

### Caching Strategy

```java
// Local cache in Room database
@Entity
public class OpportunityCached {
    @PrimaryKey
    public String id;
    public String title;
    public long syncedAt;
}

// Check local first, network second
List<Opportunity> cached = opportunityDao.getAll();
if (cached != null && !isCacheExpired()) {
    showData(cached);
} else {
    loadFromNetwork();
}
```

### Pagination

```java
// Load 20 at a time instead of all
.url(supabaseUrl + "/rest/v1/opportunities" +
     "?select=*" +
     "&limit=20" +
     "&offset=" + (page * 20))
```
