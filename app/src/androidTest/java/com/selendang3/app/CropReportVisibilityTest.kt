package com.selendang3.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.selendang3.app.data.*
import com.selendang3.app.features.production.FieldCropReport
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CropReportVisibilityTest {
    @get:Rule val compose = createComposeRule()
    @Test fun populatedFieldShowsActualsBudgetsDateAndProductivity() {
        val input = EstatePackage(1, "Fictional", "", listOf(EstateTable("fields", listOf("Field code", "Area (ha)"), listOf(listOf("USER-A", "10")))), """{
            "cropProductionReport":{"reportDate":"2026-01-10","fields":[{"field":"USER-A","sentToMillMtdMt":20,"sentToMillYtdMt":30,"roundsMtd":2,"mtPerHarvesterDayMtd":1.5,"haPerHarvesterDayMtd":3}]},
            "cropBudget":{"year":2026,"monthlyBudget":[{"field":"USER-A","year":2026,"month":1,"tonnes":10}]}}
        """)
        compose.setContent { MaterialTheme { FieldCropReport(input, "USER-A") {} } }
        compose.onNodeWithText("Actual MTD (mt): 20.00").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Budget This Month (mt): 10.00").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Tonnes per mandays (mt/manday): 1.50").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Hectare per mandays (ha/manday): 3.00").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Crop report: 2026-01-10. Budgets use the same report month.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Average Bunch Weight").assertDoesNotExist()
    }
}
