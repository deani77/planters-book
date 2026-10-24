package com.selendang3.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test

class NavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun detailsShowsPanelsAndPhoneDashboardOpensSummary() {
        compose.onNodeWithText("Crop Data (mt/ha)").assertDoesNotExist()
        compose.onNodeWithText("Estate Details").performScrollTo().performClick()
        compose.onNodeWithText("ESTATE DETAILS · Your Estate").assertIsDisplayed()
        compose.onNodeWithText("Age Profile").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Rainfall").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("OER (%)").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Checkroll Average").assertDoesNotExist()
        compose.onNodeWithText("Cost Breakdown").assertDoesNotExist()
        compose.onNodeWithText("Actual versus Budget · YTD").assertDoesNotExist()
        compose.onNodeWithText("KER (%)").assertDoesNotExist()
        compose.onNodeWithText("External Supply (t)").assertDoesNotExist()
        compose.onNodeWithText("LF").assertDoesNotExist()
        compose.onNodeWithText("‹  Back").performClick()
        compose.onNodeWithContentDescription("Open Estate Dashboard").performScrollTo().performClick()
        compose.onNodeWithText("Crop Production").assertIsDisplayed()
        compose.onNodeWithText("Manuring").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Estate Cost").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Age Profile").assertDoesNotExist()
        compose.onNodeWithText("‹  Back").performClick()
        compose.onAllNodesWithText("Planters Book").assertCountEquals(2)
    }
    @Test fun homeModulesAndAboutInSettings() {
        compose.onNodeWithText("Reports").assertDoesNotExist()
        compose.onNodeWithText("About").assertDoesNotExist()
        compose.onNodeWithText("Field Details").performScrollTo().performClick()
        compose.onNodeWithText("Field Dashboard").assertIsDisplayed()
        compose.onNodeWithText("Production by field").assertDoesNotExist()
        compose.onNodeWithText("‹  Back").performClick()
        compose.onNodeWithText("Planter Tools").performScrollTo().performClick()
        compose.onNodeWithText("Plant Density").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Chemical Dosage per Pump").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Fertilizer Requirement").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Nozzle Calibration").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Work Costing").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Unit Conversion").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("‹  Back").performClick()
        compose.onNodeWithText("Settings").performScrollTo().performClick()
        compose.onNodeWithText("Suggestions").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Estate Data Management").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Import Estate File").performScrollTo().assertIsDisplayed()
        compose.onAllNodesWithText("Read PDF with AI").assertCountEquals(2)
        compose.onNodeWithText("Copy AI Instructions").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Paste AI Result").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Server update address").assertDoesNotExist()
        compose.onNodeWithText("Access token").assertDoesNotExist()
        compose.onNodeWithText("About").performScrollTo().performClick()
        compose.onNodeWithText("Android estate information application. Initial Alpha interface.").assertIsDisplayed()
        compose.onNodeWithText("‹ Settings").performScrollTo().performClick()
        compose.onNodeWithText("User Profile").performScrollTo().assertIsDisplayed()
    }
    @Test fun cropProductionIsServerManagedAndHasNoDataEntry() {
        compose.onNodeWithText("Crop Production").performScrollTo().performClick()
        compose.onNodeWithText("Crop Production Dashboard").assertIsDisplayed()
        compose.onNodeWithText("Field Crop Records").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Save Draft").assertDoesNotExist()
        compose.onNodeWithText("Crop sent to mill (t)").assertDoesNotExist()
        compose.onNodeWithText("Today Crop Production").assertDoesNotExist()
        compose.onNodeWithText("Upload PDF · coming next").assertDoesNotExist()
    }

    @Test fun estateNameCanBeChangedInSettings() {
        try {
            compose.onNodeWithText("Settings").performScrollTo().performClick()
            compose.onNodeWithText("Estate name").performScrollTo().performTextClearance()
            compose.onNodeWithText("Estate name").performTextInput("Example Estate")
            compose.onNodeWithText("Save Estate Name").performScrollTo().performClick()
            compose.onNodeWithText("Estate name saved on this device.").performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("‹  Back").performClick()
            compose.onNodeWithText("Example Estate").assertIsDisplayed()
            compose.onNodeWithText("Estate Details").performScrollTo().performClick()
            compose.onNodeWithText("ESTATE DETAILS · Example Estate").assertIsDisplayed()
        } finally {
            compose.activity.getSharedPreferences("user_settings", 0).edit().remove("estate_name").apply()
        }
    }

    @Test fun suggestionsKeepsDraftWithoutSending() {
        try {
            compose.onNodeWithText("Settings").performScrollTo().performClick()
            compose.onNodeWithText("Suggestions").performScrollTo().performClick()
            compose.onNodeWithText("Your suggestion").performTextClearance()
            compose.onNodeWithText("Your suggestion").performTextInput("Please add a rainfall summary.")
            compose.onNodeWithText("To: deani77@gmail.com").performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("Email Suggestion").performScrollTo().assertIsEnabled()
            compose.onNodeWithText("‹ Settings").performScrollTo().performClick()
            compose.onNodeWithText("Suggestions").performScrollTo().performClick()
            compose.onNodeWithText("Please add a rainfall summary.").assertIsDisplayed()
            compose.onNodeWithText("Clear Draft").performScrollTo().performClick()
            compose.onNodeWithText("Email Suggestion").performScrollTo().assertIsNotEnabled()
        } finally {
            compose.activity.getSharedPreferences("suggestions", 0).edit().remove("draft").apply()
        }
    }
}
