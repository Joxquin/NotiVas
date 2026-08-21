package com.notivas.ui.navigation

sealed class Screen(val route: String) {
    object UniversityInput : Screen("university_input")
    object TokenInput : Screen("token_input")
    object Verification : Screen("verification")
    object Dashboard : Screen("dashboard")
    object Profile : Screen("profile")
}
