import { createClient } from "npm:@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
  "Access-Control-Allow-Headers": "Content-Type, Authorization, X-Client-Info, Apikey",
};

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

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") {
    return new Response(null, {
      status: 200,
      headers: corsHeaders,
    });
  }

  try {
    const supabaseUrl = Deno.env.get("SUPABASE_URL")!;
    const supabaseKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
    const supabase = createClient(supabaseUrl, supabaseKey);

    const opportunities: Opportunity[] = [
      {
        title: "MIT Summer Research Program",
        description: "Engage in cutting-edge research alongside MIT faculty and graduate students. Work on real-world problems in various STEM fields.",
        url: "https://greenwich.am/programs",
        source: "greenwich.am",
        format: "In-Person",
        category: "Summer Programs",
        cost: "Paid",
        location: "Cambridge, MA",
        image_url: "",
      },
      {
        title: "Harvard Young Leaders Program",
        description: "Develop leadership skills through workshops, seminars, and collaborative projects with peers from around the world.",
        url: "https://greenwich.am/programs",
        source: "greenwich.am",
        format: "Hybrid",
        category: "Summer Programs",
        cost: "Paid",
        location: "Cambridge, MA",
      },
      {
        title: "Google Code Jam",
        description: "Google's longest-running global coding competition. Solve algorithmic challenges and compete for prizes.",
        url: "https://borderless.so/extracurricular-programs",
        source: "borderless.so",
        format: "Online",
        category: "Competitions",
        cost: "Free",
      },
      {
        title: "International Math Olympiad",
        description: "The world's most prestigious mathematics competition for pre-university students. Represent your country on the global stage.",
        url: "https://borderless.so/extracurricular-programs",
        source: "borderless.so",
        format: "In-Person",
        category: "Olympiads",
        cost: "Free",
      },
      {
        title: "Y Combinator Startup School",
        description: "Free online program for founders who are at the idea stage or have already started a company.",
        url: "https://borderless.so/extracurricular-programs",
        source: "borderless.so",
        format: "Online",
        category: "Accelerators",
        cost: "Free",
      },
      {
        title: "Rhodes Scholarship",
        description: "The world's oldest and most celebrated international fellowship for graduate study at the University of Oxford.",
        url: "https://greenwich.am/programs",
        source: "greenwich.am",
        format: "In-Person",
        category: "Fellowships",
        cost: "Free",
        location: "Oxford, UK",
      },
      {
        title: "John Locke Essay Competition",
        description: "Write essays on philosophy, politics, economics, history, psychology, theology, or law. Win prizes and recognition.",
        url: "https://borderless.so/extracurricular-programs",
        source: "borderless.so",
        format: "Online",
        category: "Essay Contests",
        cost: "Free",
      },
      {
        title: "Harvard Model Congress",
        description: "Simulate the US Congress and engage in debates on contemporary policy issues with students worldwide.",
        url: "https://greenwich.am/programs",
        source: "greenwich.am",
        format: "In-Person",
        category: "Model UN",
        cost: "Paid",
        location: "Various Cities",
      },
      {
        title: "NASA Internship Program",
        description: "Gain hands-on experience working on NASA missions and research projects. Available for undergraduate and graduate students.",
        url: "https://borderless.so/extracurricular-programs",
        source: "borderless.so",
        format: "In-Person",
        category: "Internships",
        cost: "Free",
        location: "Various NASA Centers",
      },
      {
        title: "Red Cross Youth Volunteer Program",
        description: "Make a difference in your community by volunteering with the Red Cross. Gain valuable leadership experience.",
        url: "https://borderless.so/extracurricular-programs",
        source: "borderless.so",
        format: "In-Person",
        category: "Volunteering",
        cost: "Free",
      },
      {
        title: "Fulbright Student Program",
        description: "Study, research, or teach abroad with funding from the Fulbright Program. Available for graduating seniors and graduate students.",
        url: "https://greenwich.am/programs",
        source: "greenwich.am",
        format: "In-Person",
        category: "Exchange Programs",
        cost: "Free",
        location: "140+ Countries",
      },
      {
        title: "Stanford Pre-Collegiate Summer Institutes",
        description: "Experience college-level courses at Stanford. Explore your academic interests and live on campus.",
        url: "https://greenwich.am/programs",
        source: "greenwich.am",
        format: "In-Person",
        category: "Summer Programs",
        cost: "Paid",
        location: "Stanford, CA",
      },
      {
        title: "International Science Olympiad",
        description: "Compete in physics, chemistry, or biology at the international level. Showcase your scientific knowledge.",
        url: "https://borderless.so/extracurricular-programs",
        source: "borderless.so",
        format: "In-Person",
        category: "Olympiads",
        cost: "Free",
      },
      {
        title: "Regeneron Science Talent Search",
        description: "America's oldest and most prestigious science research competition for high school seniors.",
        url: "https://borderless.so/extracurricular-programs",
        source: "borderless.so",
        format: "Hybrid",
        category: "Research",
        cost: "Free",
      },
      {
        title: "UN Youth Assembly",
        description: "Engage with global leaders and youth delegates to discuss solutions to world challenges.",
        url: "https://greenwich.am/programs",
        source: "greenwich.am",
        format: "In-Person",
        category: "Model UN",
        cost: "Paid",
        location: "New York, NY",
      },
    ];

    const { data, error } = await supabase.from("opportunities").upsert(
      opportunities.map((opp) => ({
        ...opp,
        updated_at: new Date().toISOString(),
      })),
      { onConflict: "url" }
    );

    if (error) throw error;

    return new Response(
      JSON.stringify({
        success: true,
        message: `Successfully loaded ${opportunities.length} opportunities`,
        count: opportunities.length,
      }),
      {
        headers: {
          ...corsHeaders,
          "Content-Type": "application/json",
        },
      }
    );
  } catch (error) {
    return new Response(
      JSON.stringify({
        success: false,
        error: error.message,
      }),
      {
        status: 500,
        headers: {
          ...corsHeaders,
          "Content-Type": "application/json",
        },
      }
    );
  }
});
