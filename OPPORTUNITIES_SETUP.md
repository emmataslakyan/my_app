# Opportunities Feature Setup Guide

## Overview
The Discover Opportunities feature allows users to browse educational and career opportunities from multiple sources with advanced filtering capabilities.

## Database Setup

The feature uses Supabase as the backend database. The database schema includes:

### Tables
- **opportunities**: Stores all opportunity listings
  - `id`: Unique identifier
  - `title`: Opportunity name
  - `description`: Full description
  - `url`: External link
  - `source`: Origin website (greenwich.am or borderless.so)
  - `format`: Online, In-Person, or Hybrid
  - `category`: Summer Programs, Competitions, Essay Contests, Olympiads, Research, Accelerators, Fellowships, Model UN, Internships, Volunteering, Exchange Programs, Other
  - `cost`: Free or Paid
  - `deadline`: Application deadline
  - `location`: Geographic location
  - `image_url`: Banner image
  - `created_at`: Creation timestamp
  - `updated_at`: Last update timestamp

## Configuration Steps

### 1. Set Up Supabase Project
1. Create a free account at [supabase.com](https://supabase.com)
2. Create a new project
3. Note your project URL and anon key from Project Settings > API

### 2. Configure Android App
Edit `app/build.gradle.kts` and replace the placeholder values:

```kotlin
buildConfigField("String", "SUPABASE_URL", "\"https://your-project.supabase.co\"")
buildConfigField("String", "SUPABASE_ANON_KEY", "\"your-anon-key-here\"")
```

### 3. Initialize Database
The database tables are automatically created via migrations. The Edge Function will populate sample data on first load.

### 4. Populate Opportunities
The app automatically calls the `fetch-opportunities` Edge Function when the opportunities list is empty. Sample data includes 15 diverse opportunities.

## Features

### Filtering System
Users can filter opportunities by:

1. **Format**
   - All Formats
   - Online
   - In-Person
   - Hybrid

2. **Category**
   - All Categories
   - Summer Programs
   - Competitions
   - Essay Contests
   - Olympiads
   - Research
   - Accelerators
   - Fellowships
   - Model UN
   - Internships
   - Volunteering
   - Exchange Programs
   - Other

3. **Cost**
   - All
   - Free
   - Paid

### User Interface
- Clean, modern card-based design
- Material Design 3 chips for filtering
- One-tap filtering with visual feedback
- Direct links to opportunity websites
- Location information when available

## Edge Function

The `fetch-opportunities` function:
- Deployed to Supabase Edge Functions
- Populates the database with curated opportunities
- Can be extended to scrape real-time data from external sources
- Automatically called when the database is empty

## Security

Row Level Security (RLS) is enabled:
- All authenticated users can read opportunities
- Only the service role can insert/update data (via Edge Function)
- Data is protected from unauthorized modifications

## Extending the Feature

### Adding Real Web Scraping
To parse actual data from websites:
1. Update the Edge Function with scraping logic
2. Use Deno's built-in fetch API
3. Parse HTML using a library like `deno-dom`
4. Schedule periodic updates via Supabase cron

### Adding More Filters
1. Add new columns to the `opportunities` table
2. Update the layout XML with new chip groups
3. Modify the filter logic in `OpportunityAdapter`
4. Update the Edge Function sample data

## Troubleshooting

### Build Config Error
If you get `BuildConfig cannot be resolved`, ensure:
```kotlin
buildFeatures {
    buildConfig = true
}
```
is present in `app/build.gradle.kts`

### Empty Opportunities List
- Check internet connection
- Verify Supabase credentials in BuildConfig
- Check Logcat for API errors
- Ensure RLS policies allow reading

### Filtering Not Working
- Verify chip IDs match in layout and activity
- Check filter logic in `OpportunityAdapter.filter()`
- Ensure "All" options are properly handled
