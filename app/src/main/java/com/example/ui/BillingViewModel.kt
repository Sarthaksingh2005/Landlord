package com.example.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BillingCalculator
import com.example.data.BillingDao
import com.example.data.BillingDatabase
import com.example.data.BillingInput
import com.example.data.BillingRecord
import com.example.data.BillingResult
import com.example.data.RoomBillingResult
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject


class BillingViewModel(application: Application) : AndroidViewModel(application) {
    private val billingDao: BillingDao = BillingDatabase.getDatabase(application).billingDao

    // Historical Records from Database Flow
    val savedRecords: StateFlow<List<BillingRecord>> = billingDao.getAllRecords()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Form inputs as strings to allow intuitive numeric typing
    var billingMonth by mutableStateOf("")
    var billingDate by mutableStateOf("")

    var prevMmr by mutableStateOf("7882")
    var currMmr by mutableStateOf("8291")

    var prevR1 by mutableStateOf("650")
    var currR1 by mutableStateOf("687")

    var prevR2 by mutableStateOf("1203")
    var currR2 by mutableStateOf("1248")

    var prevR3 by mutableStateOf("3484")
    var currR3 by mutableStateOf("3515")

    var prevR4 by mutableStateOf("4058")
    var currR4 by mutableStateOf("4141")

    var prevR5 by mutableStateOf("1104")
    var currR5 by mutableStateOf("1186")

    var prevR6 by mutableStateOf("990")
    var currR6 by mutableStateOf("1084")

    var prevMotor by mutableStateOf("2297")
    var currMotor by mutableStateOf("2320")

    var rate by mutableStateOf("8.0")
    var sweeperCharge by mutableStateOf("") // Legacy, unused now. We use sweeper1-6 instead.

    // Room Tenants
    var tenant1 by mutableStateOf("John Doe")
    var tenant2 by mutableStateOf("Jane Smith")
    var tenant3 by mutableStateOf("Alice Johnson")
    var tenant4 by mutableStateOf("Bob Brown")
    var tenant5 by mutableStateOf("Charlie Davis")
    var tenant6 by mutableStateOf("VACANT")

    // Sweeper charges
    var sweeper1 by mutableStateOf("100.0")
    var sweeper2 by mutableStateOf("0.0")
    var sweeper3 by mutableStateOf("150.0")
    var sweeper4 by mutableStateOf("0.0")
    var sweeper5 by mutableStateOf("50.0")
    var sweeper6 by mutableStateOf("0.0")

    // Rents
    var rent1 by mutableStateOf("4100.0")
    var rent2 by mutableStateOf("4100.0")
    var rent3 by mutableStateOf("4100.0")
    var rent4 by mutableStateOf("4100.0")
    var rent5 by mutableStateOf("4100.0")
    var rent6 by mutableStateOf("4100.0")

    // Room photo verification paths
    var photoMmr by mutableStateOf("")
    var photoR1 by mutableStateOf("")
    var photoR2 by mutableStateOf("")
    var photoR3 by mutableStateOf("")
    var photoR4 by mutableStateOf("")
    var photoR5 by mutableStateOf("")
    var photoR6 by mutableStateOf("")

    // Custom Charges
    var custom1 by mutableStateOf("0.0")
    var custom2 by mutableStateOf("0.0")
    var custom3 by mutableStateOf("0.0")
    var custom4 by mutableStateOf("0.0")
    var custom5 by mutableStateOf("0.0")
    var custom6 by mutableStateOf("0.0")

    // Validation or syntax error messages
    var errorMessage by mutableStateOf<String?>(null)

    // Current active calculation result
    var calculationResult by mutableStateOf<BillingResult?>(null)
        private set

    // List of Transfer Event Logs recorded in real-time
    val transferEvents = mutableStateListOf<String>()

    // Real-time calculated summary statistics
    val totalElectricityUsageDiscrepancy: Int
        get() {
            val pMmr = prevMmr.toIntOrNull() ?: 0
            val cMmr = if (currMmr.isBlank()) pMmr else (currMmr.toIntOrNull() ?: pMmr)
            val mainMeterMMR = cMmr - pMmr

            val pR1 = prevR1.toIntOrNull() ?: 0
            val cR1 = if (currR1.isBlank()) pR1 else (currR1.toIntOrNull() ?: pR1)
            val diffR1 = cR1 - pR1

            val pR2 = prevR2.toIntOrNull() ?: 0
            val cR2 = if (currR2.isBlank()) pR2 else (currR2.toIntOrNull() ?: pR2)
            val diffR2 = cR2 - pR2

            val pR3 = prevR3.toIntOrNull() ?: 0
            val cR3 = if (currR3.isBlank()) pR3 else (currR3.toIntOrNull() ?: pR3)
            val diffR3 = cR3 - pR3

            val pR4 = prevR4.toIntOrNull() ?: 0
            val cR4 = if (currR4.isBlank()) pR4 else (currR4.toIntOrNull() ?: pR4)
            val diffR4 = cR4 - pR4

            val pR5 = prevR5.toIntOrNull() ?: 0
            val cR5 = if (currR5.isBlank()) pR5 else (currR5.toIntOrNull() ?: pR5)
            val diffR5 = cR5 - pR5

            val pR6 = prevR6.toIntOrNull() ?: 0
            val cR6 = if (currR6.isBlank()) pR6 else (currR6.toIntOrNull() ?: pR6)
            val diffR6 = cR6 - pR6

            val pMotor = prevMotor.toIntOrNull() ?: 0
            val cMotor = if (currMotor.isBlank()) pMotor else (currMotor.toIntOrNull() ?: pMotor)
            val diffMotor = cMotor - pMotor

            val totalIndividualK = diffR1 + diffR2 + diffR3 + diffR4 + diffR5 + diffR6 + diffMotor
            return mainMeterMMR - totalIndividualK
        }

    val totalRentCollected: Double
        get() {
            var sum = 0.0
            if (tenant1.isNotBlank() && !tenant1.equals("VACANT", ignoreCase = true)) {
                sum += rent1.toDoubleOrNull() ?: 0.0
            }
            if (tenant2.isNotBlank() && !tenant2.equals("VACANT", ignoreCase = true)) {
                sum += rent2.toDoubleOrNull() ?: 0.0
            }
            if (tenant3.isNotBlank() && !tenant3.equals("VACANT", ignoreCase = true)) {
                sum += rent3.toDoubleOrNull() ?: 0.0
            }
            if (tenant4.isNotBlank() && !tenant4.equals("VACANT", ignoreCase = true)) {
                sum += rent4.toDoubleOrNull() ?: 0.0
            }
            if (tenant5.isNotBlank() && !tenant5.equals("VACANT", ignoreCase = true)) {
                sum += rent5.toDoubleOrNull() ?: 0.0
            }
            if (tenant6.isNotBlank() && !tenant6.equals("VACANT", ignoreCase = true)) {
                sum += rent6.toDoubleOrNull() ?: 0.0
            }
            return sum
        }

    // Latest saved record from database
    var latestSavedRecord: com.example.data.BillingRecord? by mutableStateOf(null)
        private set

    fun transferTenant(fromRoom: Int, toRoom: Int): Boolean {
        if (fromRoom == toRoom) return false
        val occupant = when (fromRoom) {
            1 -> tenant1; 2 -> tenant2; 3 -> tenant3; 4 -> tenant4; 5 -> tenant5; else -> tenant6
        }
        if (occupant.isBlank() || occupant.equals("vacant", ignoreCase = true)) return false

        val targetOccupant = when (toRoom) {
            1 -> tenant1; 2 -> tenant2; 3 -> tenant3; 4 -> tenant4; 5 -> tenant5; else -> tenant6
        }

        // Swapping occupant names between the two rooms of the transfer
        when (fromRoom) {
            1 -> tenant1 = targetOccupant; 2 -> tenant2 = targetOccupant; 3 -> tenant3 = targetOccupant
            4 -> tenant4 = targetOccupant; 5 -> tenant5 = targetOccupant; else -> tenant6 = targetOccupant
        }
        when (toRoom) {
            1 -> tenant1 = occupant; 2 -> tenant2 = occupant; 3 -> tenant3 = occupant
            4 -> tenant4 = occupant; 5 -> tenant5 = occupant; else -> tenant6 = occupant
        }

        // Record a professional event log
        val targetLabel = if (targetOccupant.isBlank() || targetOccupant.equals("vacant", ignoreCase = true)) "empty Room $toRoom" else "Room $toRoom (swapping with $targetOccupant)"
        val eventMessage = "Transferred '$occupant' from Room $fromRoom to $targetLabel on ${SimpleDateFormat("dd-MMM HH:mm", Locale.getDefault()).format(Date())}"
        transferEvents.add(0, eventMessage) // add at top of list
        
        // Refresh active calculation
        performCalculation()
        return true
    }

    init {
        // Compute default date elements
        val calendar = Calendar.getInstance()
        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        val dateFormat = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault())
        
        billingMonth = monthFormat.format(calendar.time)
        billingDate = dateFormat.format(calendar.time)

        // Attempt to pre-populate with the latest record from database
        loadLatestRecordDefaults()
    }

    private fun loadLatestRecordDefaults() {
        viewModelScope.launch {
            try {
                val latest = billingDao.getLatestRecord()
                latestSavedRecord = latest
                if (latest != null) {
                    // Pre-populate previous readings with previous calculation's CURRENT readings
                    prevMmr = latest.currMmr.toString()
                    prevR1 = latest.currR1.toString()
                    prevR2 = latest.currR2.toString()
                    prevR3 = latest.currR3.toString()
                    prevR4 = latest.currR4.toString()
                    prevR5 = latest.currR5.toString()
                    prevR6 = latest.currR6.toString()
                    prevMotor = latest.currMotor.toString()

                    // Clear curr readings to let users fill them out
                    currMmr = ""
                    currR1 = ""
                    currR2 = ""
                    currR3 = ""
                    currR4 = ""
                    currR5 = ""
                    currR6 = ""
                    currMotor = ""

                    // Keep defaults
                    rate = latest.rate.toString()
                    
                    tenant1 = latest.tenant1
                    tenant2 = latest.tenant2
                    tenant3 = latest.tenant3
                    tenant4 = latest.tenant4
                    tenant5 = latest.tenant5
                    tenant6 = latest.tenant6

                    sweeper1 = latest.sweeper1.toString()
                    sweeper2 = latest.sweeper2.toString()
                    sweeper3 = latest.sweeper3.toString()
                    sweeper4 = latest.sweeper4.toString()
                    sweeper5 = latest.sweeper5.toString()
                    sweeper6 = latest.sweeper6.toString()

                    rent1 = latest.rent1.toString()
                    rent2 = latest.rent2.toString()
                    rent3 = latest.rent3.toString()
                    rent4 = latest.rent4.toString()
                    rent5 = latest.rent5.toString()
                    rent6 = latest.rent6.toString()

                    custom1 = latest.custom1.toString()
                    custom2 = latest.custom2.toString()
                    custom3 = latest.custom3.toString()
                    custom4 = latest.custom4.toString()
                    custom5 = latest.custom5.toString()
                    custom6 = latest.custom6.toString()
                    
                    // Automatically increment month fields for convenience
                    val cal = Calendar.getInstance()
                    val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                    billingMonth = monthFormat.format(cal.time)
                }
            } catch (e: Exception) {
                // Fail-safe
            }
        }
    }

    private var lastCheckedMonth = ""

    fun getNextMonthName(currentMonth: String): String {
        if (currentMonth.isBlank()) return ""
        return try {
            val formats = listOf(
                SimpleDateFormat("MMMM yyyy", Locale.getDefault()),
                SimpleDateFormat("MMM yyyy", Locale.getDefault()),
                SimpleDateFormat("MMMM", Locale.getDefault())
            )
            var parsedDate: java.util.Date? = null
            var matchedFormat: SimpleDateFormat? = null
            for (f in formats) {
                try {
                    parsedDate = f.parse(currentMonth)
                    matchedFormat = f
                    if (parsedDate != null) break
                } catch (e: Exception) {}
            }
            if (parsedDate != null && matchedFormat != null) {
                val cal = Calendar.getInstance()
                cal.time = parsedDate
                cal.add(Calendar.MONTH, 1)
                matchedFormat.format(cal.time)
            } else {
                "Next Month"
            }
        } catch (e: Exception) {
            "Next Month"
        }
    }

    fun checkAndAutofillNextMonth(newMonth: String) {
        val latest = latestSavedRecord ?: return
        if (newMonth.isNotBlank() && !newMonth.equals(latest.billingMonth, ignoreCase = true)) {
            if (lastCheckedMonth.equals(latest.billingMonth, ignoreCase = true) || lastCheckedMonth.isBlank()) {
                prevMmr = latest.currMmr.toString()
                prevR1 = latest.currR1.toString()
                prevR2 = latest.currR2.toString()
                prevR3 = latest.currR3.toString()
                prevR4 = latest.currR4.toString()
                prevR5 = latest.currR5.toString()
                prevR6 = latest.currR6.toString()
                prevMotor = latest.currMotor.toString()

                currMmr = ""
                currR1 = ""
                currR2 = ""
                currR3 = ""
                currR4 = ""
                currR5 = ""
                currR6 = ""
                currMotor = ""

                rate = latest.rate.toString()

                tenant1 = latest.tenant1
                tenant2 = latest.tenant2
                tenant3 = latest.tenant3
                tenant4 = latest.tenant4
                tenant5 = latest.tenant5
                tenant6 = latest.tenant6

                sweeper1 = latest.sweeper1.toString()
                sweeper2 = latest.sweeper2.toString()
                sweeper3 = latest.sweeper3.toString()
                sweeper4 = latest.sweeper4.toString()
                sweeper5 = latest.sweeper5.toString()
                sweeper6 = latest.sweeper6.toString()

                rent1 = latest.rent1.toString()
                rent2 = latest.rent2.toString()
                rent3 = latest.rent3.toString()
                rent4 = latest.rent4.toString()
                rent5 = latest.rent5.toString()
                rent6 = latest.rent6.toString()

                custom1 = latest.custom1.toString()
                custom2 = latest.custom2.toString()
                custom3 = latest.custom3.toString()
                custom4 = latest.custom4.toString()
                custom5 = latest.custom5.toString()
                custom6 = latest.custom6.toString()

                performCalculation()
            }
        }
        lastCheckedMonth = newMonth
    }

    // Reset fields to original dummy examples
    fun resetToExampleData() {
        billingMonth = "June 2026"
        billingDate = "06-Jun-2026"
        
        prevMmr = "7882"
        currMmr = "8291"
        
        prevR1 = "650"
        currR1 = "687"
        
        prevR2 = "1203"
        currR2 = "1248"
        
        prevR3 = "3484"
        currR3 = "3515"
        
        prevR4 = "4058"
        currR4 = "4141"
        
        prevR5 = "1104"
        currR5 = "1186"
        
        prevR6 = "990"
        currR6 = "1084"
        
        prevMotor = "2297"
        currMotor = "2320"
        
        rate = "8.0"
        sweeperCharge = ""
        
        tenant1 = "John Doe"
        tenant2 = "Jane Smith"
        tenant3 = "Alice Johnson"
        tenant4 = "Bob Brown"
        tenant5 = "Charlie Davis"
        tenant6 = "VACANT"

        sweeper1 = "100.0"
        sweeper2 = "0.0"
        sweeper3 = "150.0"
        sweeper4 = "0.0"
        sweeper5 = "50.0"
        sweeper6 = "0.0"

        rent1 = "4100.0"
        rent2 = "4100.0"
        rent3 = "4100.0"
        rent4 = "4100.0"
        rent5 = "4100.0"
        rent6 = "4100.0"

        custom1 = "0.0"
        custom2 = "0.0"
        custom3 = "0.0"
        custom4 = "0.0"
        custom5 = "120.0"
        custom6 = "0.0"
        
        photoMmr = ""
        photoR1 = ""
        photoR2 = ""
        photoR3 = ""
        photoR4 = ""
        photoR5 = ""
        photoR6 = ""
        
        errorMessage = null
        calculationResult = null
    }

    // Create a complete BillingInput, validate values, and calculate
    fun performCalculation(onSuccess: (() -> Unit)? = null): Boolean {
        errorMessage = null
        try {
            // Unify month and date input validations
            if (billingMonth.isBlank()) throw IllegalArgumentException("Billing Month cannot be blank.")
            if (billingDate.isBlank()) throw IllegalArgumentException("Billing Date cannot be blank.")

            // Trim inputs and parse
            val pMmr = prevMmr.toIntOrNull() ?: throw IllegalArgumentException("Previous MMR must be a valid integer.")
            val cMmr = currMmr.toIntOrNull() ?: throw IllegalArgumentException("Current MMR must be a valid integer.")
            
            val pR1 = prevR1.toIntOrNull() ?: throw IllegalArgumentException("Previous R1 must be an integer.")
            val cR1 = currR1.toIntOrNull() ?: throw IllegalArgumentException("Current R1 must be an integer.")
            
            val pR2 = prevR2.toIntOrNull() ?: throw IllegalArgumentException("Previous R2 must be an integer.")
            val cR2 = currR2.toIntOrNull() ?: throw IllegalArgumentException("Current R2 must be an integer.")
            
            val pR3 = prevR3.toIntOrNull() ?: throw IllegalArgumentException("Previous R3 must be an integer.")
            val cR3 = currR3.toIntOrNull() ?: throw IllegalArgumentException("Current R3 must be an integer.")
            
            val pR4 = prevR4.toIntOrNull() ?: throw IllegalArgumentException("Previous R4 must be an integer.")
            val cR4 = currR4.toIntOrNull() ?: throw IllegalArgumentException("Current R4 must be an integer.")
            
            val pR5 = prevR5.toIntOrNull() ?: throw IllegalArgumentException("Previous R5 must be an integer.")
            val cR5 = currR5.toIntOrNull() ?: throw IllegalArgumentException("Current R5 must be an integer.")
            
            val pR6 = prevR6.toIntOrNull() ?: throw IllegalArgumentException("Previous R6 must be an integer.")
            val cR6 = currR6.toIntOrNull() ?: throw IllegalArgumentException("Current R6 must be an integer.")
            
            val pMotor = prevMotor.toIntOrNull() ?: throw IllegalArgumentException("Previous Motor must be an integer.")
            val cMotor = currMotor.toIntOrNull() ?: throw IllegalArgumentException("Current Motor must be an integer.")

            val eleRate = rate.toDoubleOrNull() ?: throw IllegalArgumentException("Electricity Rate must be a valid number.")

            // Room Sweeper Parser
            val sw1 = sweeper1.toDoubleOrNull() ?: throw IllegalArgumentException("Room 1 Sweeper Charge must be a numeric value.")
            val sw2 = sweeper2.toDoubleOrNull() ?: throw IllegalArgumentException("Room 2 Sweeper Charge must be a numeric value.")
            val sw3 = sweeper3.toDoubleOrNull() ?: throw IllegalArgumentException("Room 3 Sweeper Charge must be a numeric value.")
            val sw4 = sweeper4.toDoubleOrNull() ?: throw IllegalArgumentException("Room 4 Sweeper Charge must be a numeric value.")
            val sw5 = sweeper5.toDoubleOrNull() ?: throw IllegalArgumentException("Room 5 Sweeper Charge must be a numeric value.")
            val sw6 = sweeper6.toDoubleOrNull() ?: throw IllegalArgumentException("Room 6 Sweeper Charge must be a numeric value.")

            // Rents
            val r1 = rent1.toDoubleOrNull() ?: throw IllegalArgumentException("Room 1 Rent must be a numeric value.")
            val r2 = rent2.toDoubleOrNull() ?: throw IllegalArgumentException("Room 2 Rent must be a numeric value.")
            val r3 = rent3.toDoubleOrNull() ?: throw IllegalArgumentException("Room 3 Rent must be a numeric value.")
            val r4 = rent4.toDoubleOrNull() ?: throw IllegalArgumentException("Room 4 Rent must be a numeric value.")
            val r5 = rent5.toDoubleOrNull() ?: throw IllegalArgumentException("Room 5 Rent must be a numeric value.")
            val r6 = rent6.toDoubleOrNull() ?: throw IllegalArgumentException("Room 6 Rent must be a numeric value.")

            // Customs
            val cust1 = custom1.toDoubleOrNull() ?: throw IllegalArgumentException("Room 1 Custom Charge must be a numeric value.")
            val cust2 = custom2.toDoubleOrNull() ?: throw IllegalArgumentException("Room 2 Custom Charge must be a numeric value.")
            val cust3 = custom3.toDoubleOrNull() ?: throw IllegalArgumentException("Room 3 Custom Charge must be a numeric value.")
            val cust4 = custom4.toDoubleOrNull() ?: throw IllegalArgumentException("Room 4 Custom Charge must be a numeric value.")
            val cust5 = custom5.toDoubleOrNull() ?: throw IllegalArgumentException("Room 5 Custom Charge must be a numeric value.")
            val cust6 = custom6.toDoubleOrNull() ?: throw IllegalArgumentException("Room 6 Custom Charge must be a numeric value.")

            // Strict Non-Negative Values Rejections
            if (pMmr < 0 || cMmr < 0) throw IllegalArgumentException("MMR readings cannot be negative.")
            if (pR1 < 0 || cR1 < 0 || pR2 < 0 || cR2 < 0 || pR3 < 0 || cR3 < 0 || pR4 < 0 || cR4 < 0 || pR5 < 0 || cR5 < 0 || pR6 < 0 || cR6 < 0) {
                throw IllegalArgumentException("Room readings cannot be negative.")
            }
            if (pMotor < 0 || cMotor < 0) throw IllegalArgumentException("Motor readings cannot be negative.")
            if (eleRate < 0) throw IllegalArgumentException("Electricity Rate cannot be negative.")
            if (sw1 < 0 || sw2 < 0 || sw3 < 0 || sw4 < 0 || sw5 < 0 || sw6 < 0) throw IllegalArgumentException("Sweeper charges cannot be negative.")
            if (r1 < 0 || r2 < 0 || r3 < 0 || r4 < 0 || r5 < 0 || r6 < 0) throw IllegalArgumentException("Room rents cannot be negative.")
            if (cust1 < 0 || cust2 < 0 || cust3 < 0 || cust4 < 0 || cust5 < 0 || cust6 < 0) throw IllegalArgumentException("Custom charges cannot be negative.")

            // Semantic checks: Current readings must be greater than or equal to previous readings
            if (cMmr < pMmr) throw IllegalArgumentException("Current MMR ($cMmr) cannot be less than Previous MMR ($pMmr).")
            if (cR1 < pR1) throw IllegalArgumentException("Current R1 ($cR1) cannot be less than Previous R1 ($pR1).")
            if (cR2 < pR2) throw IllegalArgumentException("Current R2 ($cR2) cannot be less than Previous R2 ($pR2).")
            if (cR3 < pR3) throw IllegalArgumentException("Current R3 ($cR3) cannot be less than Previous R3 ($pR3).")
            if (cR4 < pR4) throw IllegalArgumentException("Current R4 ($cR4) cannot be less than Previous R4 ($pR4).")
            if (cR5 < pR5) throw IllegalArgumentException("Current R5 ($cR5) cannot be less than Previous R5 ($pR5).")
            if (cR6 < pR6) throw IllegalArgumentException("Current R6 ($cR6) cannot be less than Previous R6 ($pR6).")
            if (cMotor < pMotor) throw IllegalArgumentException("Current Motor ($cMotor) cannot be less than Previous Motor ($pMotor).")

            val input = BillingInput(
                prevMmr = pMmr, currMmr = cMmr,
                prevR1 = pR1, currR1 = cR1,
                prevR2 = pR2, currR2 = cR2,
                prevR3 = pR3, currR3 = cR3,
                prevR4 = pR4, currR4 = cR4,
                prevR5 = pR5, currR5 = cR5,
                prevR6 = pR6, currR6 = cR6,
                prevMotor = pMotor, currMotor = cMotor,
                rate = eleRate,
                sweeper1 = sw1, sweeper2 = sw2, sweeper3 = sw3, sweeper4 = sw4, sweeper5 = sw5, sweeper6 = sw6,
                rent1 = r1, rent2 = r2, rent3 = r3, rent4 = r4, rent5 = r5, rent6 = r6,
                custom1 = cust1, custom2 = cust2, custom3 = cust3, custom4 = cust4, custom5 = cust5, custom6 = cust6,
                tenant1 = tenant1.ifBlank { "VACANT" },
                tenant2 = tenant2.ifBlank { "VACANT" },
                tenant3 = tenant3.ifBlank { "VACANT" },
                tenant4 = tenant4.ifBlank { "VACANT" },
                tenant5 = tenant5.ifBlank { "VACANT" },
                tenant6 = tenant6.ifBlank { "VACANT" }
            )

            val result = BillingCalculator.calculate(input)
            calculationResult = result
            onSuccess?.invoke()
            return true
        } catch (e: Exception) {
            errorMessage = e.message ?: "An unknown validation error occurred."
            return false
        }
    }

    // Save calculation result into database
    fun saveRecord() {
        val result = calculationResult ?: return
        viewModelScope.launch {
            try {
                val record = BillingRecord(
                    billingMonth = billingMonth.ifBlank { "Unspecified Month" },
                    billingDate = billingDate.ifBlank { "Unspecified Date" },
                    prevMmr = result.input.prevMmr,
                    currMmr = result.input.currMmr,
                    prevR1 = result.input.prevR1,
                    currR1 = result.input.currR1,
                    prevR2 = result.input.prevR2,
                    currR2 = result.input.currR2,
                    prevR3 = result.input.prevR3,
                    currR3 = result.input.currR3,
                    prevR4 = result.input.prevR4,
                    currR4 = result.input.currR4,
                    prevR5 = result.input.prevR5,
                    currR5 = result.input.currR5,
                    prevR6 = result.input.prevR6,
                    currR6 = result.input.currR6,
                    prevMotor = result.input.prevMotor,
                    currMotor = result.input.currMotor,
                    rate = result.input.rate,
                    sweeperCharge = 0.0, // Legacy
                    rent1 = result.input.rent1,
                    rent2 = result.input.rent2,
                    rent3 = result.input.rent3,
                    rent4 = result.input.rent4,
                    rent5 = result.input.rent5,
                    rent6 = result.input.rent6,
                    tenant1 = result.input.tenant1,
                    tenant2 = result.input.tenant2,
                    tenant3 = result.input.tenant3,
                    tenant4 = result.input.tenant4,
                    tenant5 = result.input.tenant5,
                    tenant6 = result.input.tenant6,
                    sweeper1 = result.input.sweeper1,
                    sweeper2 = result.input.sweeper2,
                    sweeper3 = result.input.sweeper3,
                    sweeper4 = result.input.sweeper4,
                    sweeper5 = result.input.sweeper5,
                    sweeper6 = result.input.sweeper6,
                    custom1 = result.input.custom1,
                    custom2 = result.input.custom2,
                    custom3 = result.input.custom3,
                    custom4 = result.input.custom4,
                    custom5 = result.input.custom5,
                    custom6 = result.input.custom6,
                    photoMmr = photoMmr,
                    photoR1 = photoR1,
                    photoR2 = photoR2,
                    photoR3 = photoR3,
                    photoR4 = photoR4,
                    photoR5 = photoR5,
                    photoR6 = photoR6
                )
                billingDao.insertRecord(record)
                latestSavedRecord = record
            } catch (e: Exception) {
                // Fail-safe
            }
        }
    }

    // Load custom saved record from database back into the calculator inputs
    fun restoreRecord(record: BillingRecord) {
        latestSavedRecord = record
        billingMonth = record.billingMonth
        billingDate = record.billingDate
        
        prevMmr = record.prevMmr.toString()
        currMmr = record.currMmr.toString()
        
        prevR1 = record.prevR1.toString()
        currR1 = record.currR1.toString()
        prevR2 = record.prevR2.toString()
        currR2 = record.currR2.toString()
        prevR3 = record.prevR3.toString()
        currR3 = record.currR3.toString()
        prevR4 = record.prevR4.toString()
        currR4 = record.currR4.toString()
        prevR5 = record.prevR5.toString()
        currR5 = record.currR5.toString()
        prevR6 = record.prevR6.toString()
        currR6 = record.currR6.toString()
        
        prevMotor = record.prevMotor.toString()
        currMotor = record.currMotor.toString()
        
        rate = record.rate.toString()
        
        tenant1 = record.tenant1
        tenant2 = record.tenant2
        tenant3 = record.tenant3
        tenant4 = record.tenant4
        tenant5 = record.tenant5
        tenant6 = record.tenant6

        sweeper1 = record.sweeper1.toString()
        sweeper2 = record.sweeper2.toString()
        sweeper3 = record.sweeper3.toString()
        sweeper4 = record.sweeper4.toString()
        sweeper5 = record.sweeper5.toString()
        sweeper6 = record.sweeper6.toString()

        rent1 = record.rent1.toString()
        rent2 = record.rent2.toString()
        rent3 = record.rent3.toString()
        rent4 = record.rent4.toString()
        rent5 = record.rent5.toString()
        rent6 = record.rent6.toString()

        custom1 = record.custom1.toString()
        custom2 = record.custom2.toString()
        custom3 = record.custom3.toString()
        custom4 = record.custom4.toString()
        custom5 = record.custom5.toString()
        custom6 = record.custom6.toString()
        
        photoMmr = record.photoMmr
        photoR1 = record.photoR1
        photoR2 = record.photoR2
        photoR3 = record.photoR3
        photoR4 = record.photoR4
        photoR5 = record.photoR5
        photoR6 = record.photoR6
        
        errorMessage = null
        // Re-execute calculations with direct frozen restore values
        performCalculation()
    }

    // Delete a record
    fun deleteRecord(record: BillingRecord) {
        viewModelScope.launch {
            billingDao.deleteRecord(record)
        }
    }

    // Helper text formatter that constructs the exact text layout specified by the user!
    fun generateShareableTextReport(result: BillingResult): String {
        return buildString {
            append("## BILLING AUDITOR REPORT\n\n")
            append("Month: ${billingMonth}\n")
            append("Date: ${billingDate}\n")
            append("Rate: ₹${result.input.rate}/unit\n\n")
            append("---\n\n")
            append("## STEP-BY-STEP CALCULATIONS\n\n")
            
            append("### Room Consumption Calculation\n")
            append("R1 = ${result.input.currR1} - ${result.input.prevR1} = ${result.diffR1} (Tenant: ${result.room1.tenantName})\n")
            append("R2 = ${result.input.currR2} - ${result.input.prevR2} = ${result.diffR2} (Tenant: ${result.room2.tenantName})\n")
            append("R3 = ${result.input.currR3} - ${result.input.prevR3} = ${result.diffR3} (Tenant: ${result.room3.tenantName})\n")
            append("R4 = ${result.input.currR4} - ${result.input.prevR4} = ${result.diffR4} (Tenant: ${result.room4.tenantName})\n")
            append("R5 = ${result.input.currR5} - ${result.input.prevR5} = ${result.diffR5} (Tenant: ${result.room5.tenantName})\n")
            append("R6 = ${result.input.currR6} - ${result.input.prevR6} = ${result.diffR6} (Tenant: ${result.room6.tenantName})\n")
            append("Motor = ${result.input.currMotor} - ${result.input.prevMotor} = ${result.diffMotor}\n\n")
            
            append("### Total Individual Consumption\n")
            append("Formula: K = R1 + R2 + R3 + R4 + R5 + R6 + Motor\n")
            append("K = ${result.diffR1} + ${result.diffR2} + ${result.diffR3} + ${result.diffR4} + ${result.diffR5} + ${result.diffR6} + ${result.diffMotor} = ${result.totalIndividualK}\n\n")
            
            append("### Main Meter Consumption\n")
            append("Formula: MMR = Current MMR - Previous MMR\n")
            append("MMR = ${result.input.currMmr} - ${result.input.prevMmr} = ${result.mainMeterMMR}\n\n")
            
            append("### Discrepancy Calculation\n")
            append("Formula: L = MMR - K\n")
            append("L = ${result.mainMeterMMR} - ${result.totalIndividualK} = ${result.discrepancyL}\n\n")
            
            append("### Discrepancy Distribution\n")
            append("Distributed equally among R1, R2, R3 (ascending original consumption sort order logic)\n")
            append("Calculated allocation: R1 += ${result.discDistR1}, R2 += ${result.discDistR2}, R3 += ${result.discDistR3}\n")
            append("Total distributed discrepancy = ${result.discDistR1 + result.discDistR2 + result.discDistR3}\n\n")
            
            append("### Motor Distribution\n")
            append("Distributed among all 6 rooms. Formula: Motor Difference ÷ 6\n")
            append("Calculated allocation:\n")
            append("R1 += ${result.motorDistR1}, R2 += ${result.motorDistR2}, R3 += ${result.motorDistR3}, R4 += ${result.motorDistR4}, R5 += ${result.motorDistR5}, R6 += ${result.motorDistR6}\n")
            append("Total distributed motor units = ${result.motorDistR1 + result.motorDistR2 + result.motorDistR3 + result.motorDistR4 + result.motorDistR5 + result.motorDistR6}\n\n")
            
            append("### Updated Unit Verification\n")
            append("R1: ${result.diffR1} + ${result.discDistR1} + ${result.motorDistR1} = ${result.room1.updatedUnits}\n")
            append("R2: ${result.diffR2} + ${result.discDistR2} + ${result.motorDistR2} = ${result.room2.updatedUnits}\n")
            append("R3: ${result.diffR3} + ${result.discDistR3} + ${result.motorDistR3} = ${result.room3.updatedUnits}\n")
            append("R4: ${result.diffR4} + ${result.motorDistR4} = ${result.room4.updatedUnits}\n")
            append("R5: ${result.diffR5} + ${result.motorDistR5} = ${result.room5.updatedUnits}\n")
            append("R6: ${result.diffR6} + ${result.motorDistR6} = ${result.room6.updatedUnits}\n")
            append("Verification Sum = ${result.room1.updatedUnits} + ${result.room2.updatedUnits} + ${result.room3.updatedUnits} + ${result.room4.updatedUnits} + ${result.room5.updatedUnits} + ${result.room6.updatedUnits} = ${result.totalUpdatedUnits}\n")
            append("MMR Difference = ${result.mainMeterMMR}\n")
            append("Status = ${if (result.verificationPassed) "VERIFIED ✓" else "FAILED ❌"}\n\n")
            
            append("---\n\n")
            append("## UPDATED UNIT TABLE\n\n")
            append("| Unit Meter | Original Diff | Final Updated Diff | Tenant Status |\n")
            append("| ---------- | ------------- | ------------------ | ------------- |\n")
            append("| R1         | ${result.diffR1}             | ${result.room1.updatedUnits}                  | ${result.room1.tenantName} |\n")
            append("| R2         | ${result.diffR2}             | ${result.room2.updatedUnits}                  | ${result.room2.tenantName} |\n")
            append("| R3         | ${result.diffR3}             | ${result.room3.updatedUnits}                  | ${result.room3.tenantName} |\n")
            append("| R4         | ${result.diffR4}             | ${result.room4.updatedUnits}                  | ${result.room4.tenantName} |\n")
            append("| R5         | ${result.diffR5}             | ${result.room5.updatedUnits}                  | ${result.room5.tenantName} |\n")
            append("| R6         | ${result.diffR6}             | ${result.room6.updatedUnits}                  | ${result.room6.tenantName} |\n\n")
            
            append("---\n\n")
            append("## ROOM-WISE BILL REPORT\n\n")
            
            // Format room template helper
            fun appendRoom(roomNum: Int, res: RoomBillingResult) {
                val tenantStr = if (res.tenantName.isBlank() || res.tenantName.equals("vacant", ignoreCase = true)) "VACANT" else res.tenantName
                append("### Room $roomNum ($tenantStr)\n\n")
                append("• Rent: ₹${res.rent.toInt()}\n")
                append("• Electricity (${res.updatedUnits} × ₹${result.input.rate}): ₹${res.electricityCharge.toInt()}\n")
                append("• Sweeper: ₹${res.sweeper.toInt()}\n")
                append("• Custom Charges: ₹${res.custom.toInt()}\n")
                append("• TOTAL DUE: ₹${res.totalBill.toInt()}\n\n")
                append("---\n\n")
            }
            
            appendRoom(1, result.room1)
            appendRoom(2, result.room2)
            appendRoom(3, result.room3)
            appendRoom(4, result.room4)
            appendRoom(5, result.room5)
            appendRoom(6, result.room6)
            
            append("## FINAL VERIFICATION SUMMARY\n\n")
            append("Total Allocated Units = ${result.totalUpdatedUnits}\n")
            append("MMR Difference = ${result.mainMeterMMR}\n")
            append("Status = ${if (result.verificationPassed) "VERIFIED ✓" else "FAILED ❌"}\n")
        }
    }

    fun analyzeMeterPhotoAndFill(photoPath: String, onValueRead: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val file = File(photoPath)
                if (!file.exists()) return@launch
                
                val options = BitmapFactory.Options().apply {
                    inSampleSize = 2 // scale down for speed
                }
                val bitmap = BitmapFactory.decodeFile(file.absolutePath, options) ?: return@launch
                
                val result = extractMeterReadingFromImage(bitmap)
                if (result != "ERROR" && !result.startsWith("Error")) {
                    onValueRead(result)
                    performCalculation()
                }
            } catch (e: Exception) {
                // Fail-safe
            }
        }
    }

    private suspend fun extractMeterReadingFromImage(bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        val apiKey = com.example.BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "API key not configured"
        }
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
        
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
        val base64Image = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
        
        val partText = JSONObject().put("text", "You are an expert utility meter reader. Output ONLY the numeric reading shown on this electricity meter dial/display. Do not write any explanations, units, or other text. Just the raw number, as an integer. If you see multiple numbers, return the main cumulative kilowatt-hour (kWh) reading. If you cannot read it, reply with exactly 'ERROR'.")
        val partImage = JSONObject().put("inlineData", JSONObject()
            .put("mimeType", "image/jpeg")
            .put("data", base64Image)
        )
        
        val content = JSONObject().put("parts", JSONArray().put(partText).put(partImage))
        val requestBodyJson = JSONObject().put("contents", JSONArray().put(content))
        
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = requestBodyJson.toString().toRequestBody(mediaType)
        
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()
            
        val client = OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build()
            
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext "Error: ${response.code}"
                }
                val responseString = response.body?.string() ?: ""
                val jsonResponse = JSONObject(responseString)
                val candidates = jsonResponse.getJSONArray("candidates")
                val firstCandidate = candidates.getJSONObject(0)
                val parts = firstCandidate.getJSONObject("content").getJSONArray("parts")
                val text = parts.getJSONObject(0).getString("text").trim()
                val digits = text.replace(Regex("[^0-9]"), "")
                if (digits.isNotEmpty()) {
                    digits
                } else {
                    "ERROR"
                }
            }
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
}

