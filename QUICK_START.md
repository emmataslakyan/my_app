# Quick Start Guide - Discover Opportunities Feature

## Prerequisites
- Android Studio installed
- Supabase account (free tier works)
- Internet connection

## Setup Steps

### 1. Create Supabase Project (5 minutes)
1. Go to [supabase.com](https://supabase.com)
2. Click "Start your project"
3. Create new organization (if first time)
4. Click "New Project"
5. Fill in:
   - Name: SkillSpark (or any name)
   - Database Password: (generate strong password)
   - Region: Choose closest to you
6. Click "Create new project"
7. Wait 2-3 minutes for setup

### 2. Get API Credentials
1. In your Supabase dashboard, click "Settings" (gear icon)
2. Click "API" in the left sidebar
3. Copy two values:
   - **Project URL** (looks like: `https://abc123xyz.supabase.co`)
   - **anon/public key** (long string starting with `eyJ...`)

### 3. Configure Android App
1. Open the project in Android Studio
2. Open `app/build.gradle.kts`
3. Find these lines (around line 21-22):
   ```kotlin
   buildConfigField("String", "SUPABASE_URL", "\"https://your-project.supabase.co\"")
   buildConfigField("String", "SUPABASE_ANON_KEY", "\"your-anon-key\"")
   ```
4. Replace with your actual values:
   ```kotlin
   buildConfigField("String", "SUPABASE_URL", "\"https://abc123xyz.supabase.co\"")
   buildConfigField("String", "SUPABASE_ANON_KEY", "\"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...\"")
   ```
5. Click "Sync Now" in Android Studio

### 4. Deploy Edge Function
1. Install Supabase CLI:
   ```bash
   # Mac/Linux
   brew install supabase/tap/supabase

   # Windows
   scoop install supabase
   ```

2. Login to Supabase:
   ```bash
   supabase login
   ```

3. Link your project:
   ```bash
   supabase link --project-ref YOUR_PROJECT_REF
   ```
   (Project ref is the part before `.supabase.co` in your URL)

4. Deploy the function:
   ```bash
   supabase functions deploy fetch-opportunities
   ```

### 5. Run the App
1. Connect your Android device or start emulator
2. Click "Run" in Android Studio
3. Log in to the app
4. Tap "Discover Opportunities" on the home screen
5. Wait for opportunities to load

## Verification

✅ You should see 15 sample opportunities
✅ Filter chips should be clickable
✅ Tapping an opportunity opens its website
✅ Filters should work instantly

## Troubleshooting

### "Failed to load opportunities"
- Check internet connection
- Verify Supabase URL is correct
- Verify anon key is correct
- Check Logcat for specific error

### Empty list with no error
- Edge Function might not have run
- Open Supabase dashboard
- Go to Table Editor
- Check if `opportunities` table exists and has data
- If empty, call the Edge Function manually:
  ```bash
  curl -X POST 'https://YOUR_PROJECT.supabase.co/functions/v1/fetch-opportunities' \
    -H 'Authorization: Bearer YOUR_ANON_KEY'
  ```

### Build errors
- Clean project: Build > Clean Project
- Rebuild: Build > Rebuild Project
- Sync Gradle: File > Sync Project with Gradle Files

### Edge Function deployment issues
- Ensure you're logged in: `supabase login`
- Check you're linked: `supabase projects list`
- Try deploying with `--no-verify-jwt false` flag if authentication issues

## Alternative: Manual Database Population

If Edge Function deployment is difficult, you can manually insert data:

1. Go to Supabase dashboard
2. Click "SQL Editor"
3. Run the migration from the project:
   ```sql
   -- Copy from supabase/migrations/create_opportunities_table
   ```
4. Then insert sample data:
   ```sql
   INSERT INTO opportunities (title, description, url, source, format, category, cost, location)
   VALUES
   ('MIT Summer Research Program', 'Engage in cutting-edge research...',
    'https://greenwich.am/programs', 'greenwich.am', 'In-Person',
    'Summer Programs', 'Paid', 'Cambridge, MA'),
   -- Add more rows...
   ```

## Need Help?

- Check `OPPORTUNITIES_SETUP.md` for detailed documentation
- Check `IMPLEMENTATION_SUMMARY.md` for technical details
- Review Logcat in Android Studio for error messages
- Verify RLS policies in Supabase dashboard under Authentication > Policies
