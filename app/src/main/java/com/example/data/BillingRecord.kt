package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "billing_records")
data class BillingRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val billingMonth: String,
    val billingDate: String,
    
    // Main Meter Readings
    val prevMmr: Int,
    val currMmr: Int,
    
    // Room Readings
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
    
    // Motor Readings
    val prevMotor: Int,
    val currMotor: Int,
    
    // Monetary Parameters
    val rate: Double,
    val sweeperCharge: Double = 0.0, // kept for backward compatibility if any
    
    // Room-specific sweeper charges
    val sweeper1: Double = 100.0,
    val sweeper2: Double = 0.0,
    val sweeper3: Double = 150.0,
    val sweeper4: Double = 0.0,
    val sweeper5: Double = 50.0,
    val sweeper6: Double = 0.0,
    
    // Room-specific rents
    val rent1: Double,
    val rent2: Double,
    val rent3: Double,
    val rent4: Double,
    val rent5: Double,
    val rent6: Double,
    
    // Room-specific custom charges
    val custom1: Double = 0.0,
    val custom2: Double = 0.0,
    val custom3: Double = 0.0,
    val custom4: Double = 0.0,
    val custom5: Double = 0.0,
    val custom6: Double = 0.0,
    
    // Room-specific tenant names
    val tenant1: String = "VACANT",
    val tenant2: String = "VACANT",
    val tenant3: String = "VACANT",
    val tenant4: String = "VACANT",
    val tenant5: String = "VACANT",
    val tenant6: String = "VACANT",
    
    // Photo paths for verification records
    val photoMmr: String = "",
    val photoR1: String = "",
    val photoR2: String = "",
    val photoR3: String = "",
    val photoR4: String = "",
    val photoR5: String = "",
    val photoR6: String = "",

    // Date/Timestamp of audit creation
    val timestamp: Long = System.currentTimeMillis()
)
