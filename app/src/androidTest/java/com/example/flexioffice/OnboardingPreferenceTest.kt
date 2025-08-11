package com.example.flexioffice

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingPreferenceTest {
    @Test
    fun testDefaultIsNotCompleted() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("flexioffice_prefs", Context.MODE_PRIVATE)
        // Clear for deterministic run
        prefs.edit().clear().commit()
        val completed = prefs.getBoolean("onboarding_completed", false)
        assertFalse(completed)
    }

    @Test
    fun testMarkCompleted() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("flexioffice_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("onboarding_completed", true).commit()
        assertTrue(prefs.getBoolean("onboarding_completed", false))
    }
}
