package com.notivas.ui.navigation

sealed class Screen(val route: String) {
    object UniversityInput : Screen("university_input")
    object TokenInput : Screen("token_input")
    object Verification : Screen("verification")
    object Dashboard : Screen("dashboard")
    object Foros : Screen("foros")
    object Notas : Screen("notas")
    object Profile : Screen("profile")
}
