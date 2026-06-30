package com.must.connect.data.remote

import com.must.connect.BuildConfig
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.storage.Storage

/**
 * Thread-safe Singleton Supabase client for MUST-CONNECT.
 *
 * Credentials are injected at compile time via BuildConfig fields, which are
 * populated from local.properties by the Gradle build script — they are never
 * hardcoded in source and are excluded from version control via .gitignore.
 *
 * Access pattern (from any class):
 *   val client = SupabaseClientProvider.client
 *
 * Thread-safety guarantee:
 *   Kotlin's `object` declaration is initialised by the class-loader exactly
 *   once, in a thread-safe manner, before any thread can access the object.
 *   The `by lazy` delegate on `client` additionally uses the default
 *   LazyThreadSafetyMode.SYNCHRONIZED so the first call from any thread
 *   blocks until initialisation is complete.
 */
object SupabaseClientProvider {

    /**
     * The single Supabase client instance.
     *
     * Lazily initialised on first access and then reused for the lifetime
     * of the process.  Installing [Auth] and [Postgrest] here makes both
     * modules available everywhere via extension properties:
     *   - `client.auth`      → GoTrue / Auth operations
     *   - `client.from(...)` → PostgREST / Database operations
     */
    val client by lazy {
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_KEY,
        ) {
            install(Auth) {
                // Sessions are automatically persisted to SharedPreferences
                // by the Supabase Android SDK — no manual handling required.
            }
            install(Postgrest)
            install(Functions)
            install(Storage)
        }
    }
}
