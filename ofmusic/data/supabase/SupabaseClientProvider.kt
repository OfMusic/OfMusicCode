package com.haziel.ofmusic.data.supabase

import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

object SupabaseClientProvider {

    private const val TAG = "SupabaseClient"

    private const val SUPABASE_URL = "https://duuytgmsmvagjlvixqxv.supabase.co"
    private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImR1dXl0Z21zbXZhZ2psdml4cXh2Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTIwNzA5MjIsImV4cCI6MjEwNzY0NjkyMn0.iC4pYqO2mMyuHCJxim0QZZXN6AcpFfdOrGIpbqR1Tuk"

    @Volatile
    private var instance: SupabaseClient? = null

    fun getInstance(): SupabaseClient {
        return instance ?: synchronized(this) {
            instance ?: createClient().also { instance = it }
        }
    }

    private fun createClient(): SupabaseClient {
        Log.d(TAG, "Inicializando Supabase client...")

        return createSupabaseClient(
            supabaseUrl = SUPABASE_URL,
            supabaseKey = SUPABASE_ANON_KEY
        ) {
            install(Auth) {
                autoLoadFromStorage = true
                alwaysAutoRefresh = true
            }
            install(Postgrest)
            install(Storage)
        }
    }
}