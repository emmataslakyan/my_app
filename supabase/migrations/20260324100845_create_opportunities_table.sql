/*
  # Create Opportunities Table

  ## Overview
  This migration creates a table to store career and educational opportunities 
  scraped from external sources with comprehensive filtering capabilities.

  ## New Tables
  - `opportunities`
    - `id` (uuid, primary key) - Unique identifier
    - `title` (text) - Opportunity title/name
    - `description` (text) - Full description
    - `url` (text) - Link to opportunity details
    - `source` (text) - Origin website (greenwich.am or borderless.so)
    - `format` (text) - Delivery format (Online, In-Person, Hybrid)
    - `category` (text) - Type of opportunity
    - `cost` (text) - Free or Paid
    - `deadline` (timestamptz) - Application deadline if available
    - `location` (text) - Geographic location if applicable
    - `image_url` (text) - Banner/thumbnail image
    - `created_at` (timestamptz) - Record creation timestamp
    - `updated_at` (timestamptz) - Last update timestamp

  ## Security
  - Enable RLS on opportunities table
  - Allow all authenticated users to read opportunities
  - Only service role can insert/update (for scraping function)
*/

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

-- Enable Row Level Security
ALTER TABLE opportunities ENABLE ROW LEVEL SECURITY;

-- Policy: All authenticated users can view opportunities
CREATE POLICY "Authenticated users can view opportunities"
  ON opportunities
  FOR SELECT
  TO authenticated
  USING (true);

-- Policy: Only service role can insert opportunities
CREATE POLICY "Service role can insert opportunities"
  ON opportunities
  FOR INSERT
  TO service_role
  WITH CHECK (true);

-- Policy: Only service role can update opportunities
CREATE POLICY "Service role can update opportunities"
  ON opportunities
  FOR UPDATE
  TO service_role
  USING (true)
  WITH CHECK (true);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_opportunities_format ON opportunities(format);
CREATE INDEX IF NOT EXISTS idx_opportunities_category ON opportunities(category);
CREATE INDEX IF NOT EXISTS idx_opportunities_cost ON opportunities(cost);
CREATE INDEX IF NOT EXISTS idx_opportunities_created_at ON opportunities(created_at DESC);