package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.util.AppSecurityManager
import com.example.util.AppStorageHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AppSecurityTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
        AppSecurityManager.init(context)
    }

    @Test
    fun testSetAndVerify6DigitPin() {
        val pin = "123456"
        val setResult = AppSecurityManager.set6DigitPin(context, pin)
        assertTrue("PIN setup should succeed", setResult)

        val config = AppSecurityManager.securityConfig.value
        assertTrue("PIN should be enabled in config", config.isPinEnabled)
        assertTrue("PIN salt should not be empty", config.pinSalt.isNotEmpty())
        assertTrue("PIN hash should not be empty", config.pinHash.isNotEmpty())

        // Verify correct PIN
        assertTrue("Correct PIN verification should succeed", AppSecurityManager.verifyPin("123456", context))

        // Verify wrong PIN
        assertFalse("Wrong PIN verification should fail", AppSecurityManager.verifyPin("654321", context))

        // Check security directory files
        val securityDir = AppStorageHelper.getSecurityDir(context)
        assertTrue("Security directory must exist", securityDir.exists())
        val pinFile = File(securityDir, "pin_credential.dat")
        val configFile = File(securityDir, "security_config.json")
        assertTrue("pin_credential.dat must exist in security folder", pinFile.exists())
        assertTrue("security_config.json must exist in security folder", configFile.exists())
    }

    @Test
    fun testRejectInvalidPinLength() {
        assertFalse("PIN with 5 digits should be rejected", AppSecurityManager.set6DigitPin(context, "12345"))
        assertFalse("PIN with 7 digits should be rejected", AppSecurityManager.set6DigitPin(context, "1234567"))
        assertFalse("Non-digit PIN should be rejected", AppSecurityManager.set6DigitPin(context, "12345a"))
    }

    @Test
    fun testChangePin() {
        AppSecurityManager.set6DigitPin(context, "112233")

        // Fail with wrong old PIN
        val (failResult, _) = AppSecurityManager.changePin(context, "000000", "998877")
        assertFalse("Change PIN should fail with wrong old PIN", failResult)

        // Succeed with correct old PIN
        val (successResult, _) = AppSecurityManager.changePin(context, "112233", "998877")
        assertTrue("Change PIN should succeed with correct old PIN", successResult)
        assertTrue("New PIN should verify successfully", AppSecurityManager.verifyPin("998877", context))
    }

    @Test
    fun testFingerprintTokenGeneration() {
        AppSecurityManager.set6DigitPin(context, "556677")

        val (ok, _) = AppSecurityManager.setFingerprintEnabled(context, true)
        assertTrue("Enabling fingerprint should succeed", ok)

        val securityDir = AppStorageHelper.getSecurityDir(context)
        val bioFile = File(securityDir, "biometric_token.dat")
        assertTrue("biometric_token.dat should exist in security folder", bioFile.exists())
        assertTrue("Biometric token should not be empty", bioFile.readText().isNotEmpty())
    }

    @Test
    fun testAntiScreenshotToggle() {
        AppSecurityManager.setAntiScreenshotEnabled(context, true)
        assertTrue(AppSecurityManager.securityConfig.value.isAntiScreenshotEnabled)

        AppSecurityManager.setAntiScreenshotEnabled(context, false)
        assertFalse(AppSecurityManager.securityConfig.value.isAntiScreenshotEnabled)
    }

    @Test
    fun testDisablePin() {
        AppSecurityManager.set6DigitPin(context, "987654")
        val (disabled, _) = AppSecurityManager.disablePin(context, "987654")
        assertTrue("Disabling PIN with correct password should succeed", disabled)
        assertFalse("PIN should no longer be enabled", AppSecurityManager.securityConfig.value.isPinEnabled)
    }
}
