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
    val updatedAt: Long = 0L
) {
    /**
     * Checks if all required fields are filled.
     * All textual profile fields and profile picture are strictly required.
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
                (profileImageBase64.isNotBlank() || profileImagePath.isNotBlank())
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
            else -> null
        }
    }
}
