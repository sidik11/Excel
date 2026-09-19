package com.example.data.local

data class UserProfile(
    val fullName: String = "",
    val dateOfBirth: String = "",
    val phoneNumber: String = "",
    val emailId: String = "",
    val device: String = "",
    val village: String = "",
    val district: String = "",
    val state: String = "",
    val country: String = "",
    val pincode: String = "",
    val profileImageBase64: String = "",
    val profileImagePath: String = "",
    val googleEmail: String = "",
    val googleDisplayName: String = "",
    val googleId: String = "",
    val googleProfilePicUrl: String = "",
    val isGoogleConnected: Boolean = false,
    val updatedAt: Long = 0L
) {
    /**
     * Checks if a Google account is connected to this profile.
     */
    fun isGoogleAccountConnected(): Boolean {
        return (isGoogleConnected || googleEmail.isNotBlank()) && googleEmail.isNotBlank()
    }

    /**
     * Checks if all required fields are filled.
     * All textual profile fields, profile picture, and Google Account connection are strictly mandatory.
     */
    fun isComplete(): Boolean {
        return fullName.isNotBlank() &&
                dateOfBirth.isNotBlank() &&
                phoneNumber.isNotBlank() &&
                emailId.isNotBlank() &&
                device.isNotBlank() &&
                village.isNotBlank() &&
                district.isNotBlank() &&
                state.isNotBlank() &&
                country.isNotBlank() &&
                pincode.isNotBlank() &&
                (profileImageBase64.isNotBlank() || profileImagePath.isNotBlank()) &&
                isGoogleAccountConnected()
    }

    /**
     * Helper to check if any field is empty, returns the name of first missing field if any.
     */
    fun getFirstMissingField(): String? {
        return when {
            profileImageBase64.isBlank() && profileImagePath.isBlank() -> "Profile Picture"
            fullName.isBlank() -> "Full Name"
            dateOfBirth.isBlank() -> "Date of Birth"
            phoneNumber.isBlank() -> "Phone Number"
            emailId.isBlank() -> "Email ID"
            device.isBlank() -> "Device"
            village.isBlank() -> "Village"
            district.isBlank() -> "District"
            state.isBlank() -> "State"
            country.isBlank() -> "Country"
            pincode.isBlank() -> "Pincode"
            !isGoogleAccountConnected() -> "Connect with Google (Mandatory)"
            else -> null
        }
    }
}
