package za.ac.dut.campuspro.data

/**
 * Supabase project settings.
 *
 * ANON_KEY: Supabase dashboard › Project Settings › API Keys ›
 * the "anon public" key (starts with eyJ...) or the "publishable" key (starts with sb_publishable_).
 * It is safe to ship in the app: Row Level Security and the database functions
 * decide what each signed-in user may read and change.
 * NEVER put the service_role / secret key here.
 */
object SupabaseConfig {
    const val URL = "https://knosgzraytvdwhtjxstk.supabase.co"
    const val ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imtub3NnenJheXR2ZHdodGp4c3RrIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTA3NzA5NTYsImV4cCI6MjEwNjM0Njk1Nn0.kEwc9QcGeYw_Wsz9gIQY1bcmjWdhOA06RLU18oJzbUs"

    val isConfigured: Boolean get() = !ANON_KEY.startsWith("PASTE_")
}
