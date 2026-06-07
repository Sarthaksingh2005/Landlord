package com.example.data

import kotlin.math.abs

data class BillingInput(
    val prevMmr: Int,
    val currMmr: Int,
    val prevR1: Int,
    val currR1: Int,
    val prevR2: Int,
    val currR2: Int,
    val prevR3: Int,
    val currR3: Int,
    val prevR4: Int,
    val currR4: Int,
    val prevR5: Int,
    val currR5: Int,
    val prevR6: Int,
    val currR6: Int,
    val prevMotor: Int,
    val currMotor: Int,
    val rate: Double,
    
    // Room-specific sweeper charges
    val sweeper1: Double,
    val sweeper2: Double,
    val sweeper3: Double,
    val sweeper4: Double,
    val sweeper5: Double,
    val sweeper6: Double,
    
    // Room-specific rents
    val rent1: Double,
    val rent2: Double,
    val rent3: Double,
    val rent4: Double,
    val rent5: Double,
    val rent6: Double,
    
    // Room-specific custom charges
    val custom1: Double,
    val custom2: Double,
    val custom3: Double,
    val custom4: Double,
    val custom5: Double,
    val custom6: Double,
    
    // Room-specific tenant names
    val tenant1: String,
    val tenant2: String,
    val tenant3: String,
    val tenant4: String,
    val tenant5: String,
    val tenant6: String
)

data class RoomBillingResult(
    val originalDiff: Int,
    val sharedDiscrepancy: Int,
    val sharedMotor: Int,
    val updatedUnits: Int,
    val electricityCharge: Double,
    val rent: Double,
    val sweeper: Double,
    val custom: Double,
    val totalBill: Double,
    val tenantName: String
)

data class BillingResult(
    val input: BillingInput,
    
    // Step 1
    val diffR1: Int,
    val diffR2: Int,
    val diffR3: Int,
    val diffR4: Int,
    val diffR5: Int,
    val diffR6: Int,
    val diffMotor: Int,
    
    // Step 2 & 3
    val totalIndividualK: Int,
    val mainMeterMMR: Int,
    
    // Step 4
    val discrepancyL: Int,
    
    // Step 5 & 6
    val discDistR1: Int,
    val discDistR2: Int,
    val discDistR3: Int,
    val motorDistR1: Int,
    val motorDistR2: Int,
    val motorDistR3: Int,
    val motorDistR4: Int,
    val motorDistR5: Int,
    val motorDistR6: Int,
    
    // Room summaries
    val room1: RoomBillingResult,
    val room2: RoomBillingResult,
    val room3: RoomBillingResult,
    val room4: RoomBillingResult,
    val room5: RoomBillingResult,
    val room6: RoomBillingResult,
    
    // Check
    val totalUpdatedUnits: Int,
    val verificationPassed: Boolean
)

object BillingCalculator {
    fun calculate(input: BillingInput): BillingResult {
        // Step 1: Calculate original diffs
        val diffR1 = input.currR1 - input.prevR1
        val diffR2 = input.currR2 - input.prevR2
        val diffR3 = input.currR3 - input.prevR3
        val diffR4 = input.currR4 - input.prevR4
        val diffR5 = input.currR5 - input.prevR5
        val diffR6 = input.currR6 - input.prevR6
        val diffMotor = input.currMotor - input.prevMotor
        
        // Step 2: Calculate K
        val totalIndividualK = diffR1 + diffR2 + diffR3 + diffR4 + diffR5 + diffR6 + diffMotor
        
        // Step 3: Calculate MMR
        val mainMeterMMR = input.currMmr - input.prevMmr
        
        // Step 4: Discrepancy L
        val discrepancyL = mainMeterMMR - totalIndividualK
        
        // Step 5: Distribute discrepancy L among R1, R2, R3 (the selected recipient rooms)
        // Divide discrepancy equally
        val baseDisc = discrepancyL / 3
        val remDisc = discrepancyL % 3
        
        // Sort rooms 1, 2, 3 by original consumption ascending, then room index ascending
        val recipientRooms = listOf(
            Triple(1, diffR1, "Room 1"),
            Triple(2, diffR2, "Room 2"),
            Triple(3, diffR3, "Room 3")
        ).sortedWith(compareBy({ it.second }, { it.first }))
        
        var discDistR1 = baseDisc
        var discDistR2 = baseDisc
        var discDistR3 = baseDisc
        
        // Distribute remainder units to rooms in sorted order
        val absRemDisc = abs(remDisc)
        val incrementDisc = if (remDisc >= 0) 1 else -1
        
        for (i in 0 until absRemDisc) {
            val roomNum = recipientRooms[i].first
            when (roomNum) {
                1 -> discDistR1 += incrementDisc
                2 -> discDistR2 += incrementDisc
                3 -> discDistR3 += incrementDisc
            }
        }
        
        // Step 6: Distribute shared motor consumption among all 6 rooms (Formula is Motor ÷ 6)
        val baseMotor = diffMotor / 6
        val remMotor = diffMotor % 6
        
        // For motor distribution, we apply symmetric rounding / distribution among R1-R6
        // Let's sort all 6 rooms by original consumption to distribute motor remainder fairly!
        val allRooms = listOf(
            Triple(1, diffR1, "Room 1"),
            Triple(2, diffR2, "Room 2"),
            Triple(3, diffR3, "Room 3"),
            Triple(4, diffR4, "Room 4"),
            Triple(5, diffR5, "Room 5"),
            Triple(6, diffR6, "Room 6")
        ).sortedWith(compareBy({ it.second }, { it.first }))
        
        var motorDistR1 = baseMotor
        var motorDistR2 = baseMotor
        var motorDistR3 = baseMotor
        var motorDistR4 = baseMotor
        var motorDistR5 = baseMotor
        var motorDistR6 = baseMotor
        
        val absRemMotor = abs(remMotor)
        val incrementMotor = if (remMotor >= 0) 1 else -1
        
        for (i in 0 until absRemMotor) {
            val roomNum = allRooms[i].first
            when (roomNum) {
                1 -> motorDistR1 += incrementMotor
                2 -> motorDistR2 += incrementMotor
                3 -> motorDistR3 += incrementMotor
                4 -> motorDistR4 += incrementMotor
                5 -> motorDistR5 += incrementMotor
                6 -> motorDistR6 += incrementMotor
            }
        }
        
        // Step 7: Compute updated units per room
        val updatedR1 = diffR1 + discDistR1 + motorDistR1
        val updatedR2 = diffR2 + discDistR2 + motorDistR2
        val updatedR3 = diffR3 + discDistR3 + motorDistR3
        val updatedR4 = diffR4 + motorDistR4
        val updatedR5 = diffR5 + motorDistR5
        val updatedR6 = diffR6 + motorDistR6
        
        val totalUpdatedUnits = updatedR1 + updatedR2 + updatedR3 + updatedR4 + updatedR5 + updatedR6
        val verificationPassed = totalUpdatedUnits == mainMeterMMR
        
        // Step 8 & 9: Calculations per room and total bills
        val elecChargeR1 = updatedR1 * input.rate
        val elecChargeR2 = updatedR2 * input.rate
        val elecChargeR3 = updatedR3 * input.rate
        val elecChargeR4 = updatedR4 * input.rate
        val elecChargeR5 = updatedR5 * input.rate
        val elecChargeR6 = updatedR6 * input.rate
        
        val r1Result = RoomBillingResult(
            originalDiff = diffR1,
            sharedDiscrepancy = discDistR1,
            sharedMotor = motorDistR1,
            updatedUnits = updatedR1,
            electricityCharge = elecChargeR1,
            rent = input.rent1,
            sweeper = input.sweeper1,
            custom = input.custom1,
            totalBill = input.rent1 + elecChargeR1 + input.sweeper1 + input.custom1,
            tenantName = input.tenant1
        )
        
        val r2Result = RoomBillingResult(
            originalDiff = diffR2,
            sharedDiscrepancy = discDistR2,
            sharedMotor = motorDistR2,
            updatedUnits = updatedR2,
            electricityCharge = elecChargeR2,
            rent = input.rent2,
            sweeper = input.sweeper2,
            custom = input.custom2,
            totalBill = input.rent2 + elecChargeR2 + input.sweeper2 + input.custom2,
            tenantName = input.tenant2
        )
        
        val r3Result = RoomBillingResult(
            originalDiff = diffR3,
            sharedDiscrepancy = discDistR3,
            sharedMotor = motorDistR3,
            updatedUnits = updatedR3,
            electricityCharge = elecChargeR3,
            rent = input.rent3,
            sweeper = input.sweeper3,
            custom = input.custom3,
            totalBill = input.rent3 + elecChargeR3 + input.sweeper3 + input.custom3,
            tenantName = input.tenant3
        )
        
        val r4Result = RoomBillingResult(
            originalDiff = diffR4,
            sharedDiscrepancy = 0,
            sharedMotor = motorDistR4,
            updatedUnits = updatedR4,
            electricityCharge = elecChargeR4,
            rent = input.rent4,
            sweeper = input.sweeper4,
            custom = input.custom4,
            totalBill = input.rent4 + elecChargeR4 + input.sweeper4 + input.custom4,
            tenantName = input.tenant4
        )
        
        val r5Result = RoomBillingResult(
            originalDiff = diffR5,
            sharedDiscrepancy = 0,
            sharedMotor = motorDistR5,
            updatedUnits = updatedR5,
            electricityCharge = elecChargeR5,
            rent = input.rent5,
            sweeper = input.sweeper5,
            custom = input.custom5,
            totalBill = input.rent5 + elecChargeR5 + input.sweeper5 + input.custom5,
            tenantName = input.tenant5
        )
        
        val r6Result = RoomBillingResult(
            originalDiff = diffR6,
            sharedDiscrepancy = 0,
            sharedMotor = motorDistR6,
            updatedUnits = updatedR6,
            electricityCharge = elecChargeR6,
            rent = input.rent6,
            sweeper = input.sweeper6,
            custom = input.custom6,
            totalBill = input.rent6 + elecChargeR6 + input.sweeper6 + input.custom6,
            tenantName = input.tenant6
        )
        
        return BillingResult(
            input = input,
            diffR1 = diffR1,
            diffR2 = diffR2,
            diffR3 = diffR3,
            diffR4 = diffR4,
            diffR5 = diffR5,
            diffR6 = diffR6,
            diffMotor = diffMotor,
            totalIndividualK = totalIndividualK,
            mainMeterMMR = mainMeterMMR,
            discrepancyL = discrepancyL,
            discDistR1 = discDistR1,
            discDistR2 = discDistR2,
            discDistR3 = discDistR3,
            motorDistR1 = motorDistR1,
            motorDistR2 = motorDistR2,
            motorDistR3 = motorDistR3,
            motorDistR4 = motorDistR4,
            motorDistR5 = motorDistR5,
            motorDistR6 = motorDistR6,
            room1 = r1Result,
            room2 = r2Result,
            room3 = r3Result,
            room4 = r4Result,
            room5 = r5Result,
            room6 = r6Result,
            totalUpdatedUnits = totalUpdatedUnits,
            verificationPassed = verificationPassed
        )
    }
}
