package com.example.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.data.BillingResult
import com.example.data.RoomBillingResult
import java.io.File
import java.io.FileOutputStream

object PdfGenerator {

    /**
     * Category A: Master Billing Report PDF
     */
    fun generateAndSaveMasterPdf(
        context: Context,
        result: BillingResult,
        month: String,
        date: String
    ): Uri? {
        val pdfDocument = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842

        // A single page portrait layout
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        // Common Paints
        val titlePaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 15f
            color = Color.parseColor("#4F378B") // Brand Purple
        }
        val subtitlePaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 10f
            color = Color.parseColor("#21005D")
        }
        val sectionPaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 11f
            color = Color.parseColor("#6750A4")
        }
        val bodyBoldPaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 8.5f
            color = Color.parseColor("#1D1B20")
        }
        val bodyPaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textSize = 8f
            color = Color.parseColor("#49454F")
        }
        val blueUrlPaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 5.2f
            color = Color.parseColor("#1A73E8") // Clickable link color
            isUnderlineText = true
        }
        val footerPaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            textSize = 7.5f
            color = Color.parseColor("#7A7A7A")
        }
        val gridHeaderPaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 8.5f
            color = Color.parseColor("#21005D")
        }
        val gridSubHeaderPaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 7.5f
            color = Color.parseColor("#49454F")
        }
        val dividerPaint = Paint().apply {
            color = Color.parseColor("#CAC4D0")
            strokeWidth = 1f
        }
        val borderPaint = Paint().apply {
            color = Color.parseColor("#7A7A7A")
            style = Paint.Style.STROKE
            strokeWidth = 1.2f
        }
        val highlightPaint = Paint().apply {
            color = Color.parseColor("#F7F2FA")
        }
        val headerBgPaint = Paint().apply {
            color = Color.parseColor("#EADDFF")
        }
        val successBadgePaint = Paint().apply {
            color = Color.parseColor("#C8E6C9")
        }
        val successTextPaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 8.5f
            color = Color.parseColor("#1B5E20")
        }

        // 1. Draw Header Bar
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 65f, Paint().apply { color = Color.parseColor("#FEF7FF") })
        canvas.drawText("MASTER BILLING AUDIT REPORT", 30f, 32f, titlePaint)
        canvas.drawText("Generated: $date  |  Month: $month  |  Rate: ₹${result.input.rate}/unit", 30f, 48f, bodyPaint)
        canvas.drawLine(0f, 65f, pageWidth.toFloat(), 65f, dividerPaint)

        // 2. Audit Verification Status summary box
        var y = 75f
        canvas.drawRoundRect(30f, y, (pageWidth - 30).toFloat(), y + 36f, 10f, 10f, highlightPaint)
        canvas.drawText("AUDIT VERIFICATION SUMMARY", 45f, y + 21f, subtitlePaint)
        
        val isPassed = result.verificationPassed
        val statusText = if (isPassed) "VERIFIED" else "MISMATCH"
        val badgeColor = if (isPassed) successBadgePaint else Paint().apply { color = Color.parseColor("#F9DEDC") }
        val badgeTextColor = if (isPassed) successTextPaint else Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 8.5f
            color = Color.parseColor("#B3261E")
        }
        canvas.drawRoundRect((pageWidth - 145).toFloat(), y + 8f, (pageWidth - 45).toFloat(), y + 28f, 6f, 6f, badgeColor)
        canvas.drawText("STATUS: $statusText", (pageWidth - 135).toFloat(), y + 21f, badgeTextColor)

        canvas.drawText("MMR Gauge: ${result.mainMeterMMR} un", 215f, y + 21f, bodyBoldPaint)
        canvas.drawText("Total Allocations: ${result.totalUpdatedUnits} un", 355f, y + 21f, bodyBoldPaint)

        y += 55f

        // 3. TABLE 1: Main Meter & Summation Overview (Units)
        canvas.drawText("1. MAIN BALANCE SHEET (ENERGY METRICS)", 30f, y, sectionPaint)
        canvas.drawLine(30f, y + 4f, (pageWidth - 30).toFloat(), y + 4f, dividerPaint)
        y += 15f

        // Header Background for Table 1
        canvas.drawRect(30f, y, (pageWidth - 30).toFloat(), y + 20f, headerBgPaint)
        canvas.drawText("Main Meter Reading (Prev / Curr / Diff)", 35f, y + 14f, gridHeaderPaint)
        canvas.drawText("Motor Reading (A)", 215f, y + 14f, gridHeaderPaint)
        canvas.drawText("Summation Unit (B)", 310f, y + 14f, gridHeaderPaint)
        canvas.drawText("Utility Reading (C)", 415f, y + 14f, gridHeaderPaint)
        canvas.drawText("Total (A+B+C)", 500f, y + 14f, gridHeaderPaint)

        // Draw Table 1 Outer Borders
        canvas.drawRect(30f, y, (pageWidth - 30).toFloat(), y + 45f, borderPaint)
        canvas.drawLine(30f, y + 20f, (pageWidth - 30).toFloat(), y + 20f, dividerPaint)

        // Column grid lines for Table 1
        canvas.drawLine(210f, y, 210f, y + 45f, dividerPaint)
        canvas.drawLine(305f, y, 305f, y + 45f, dividerPaint)
        canvas.drawLine(410f, y, 410f, y + 45f, dividerPaint)
        canvas.drawLine(495f, y, 495f, y + 45f, dividerPaint)

        // Drawing Table 1 values
        val summationRooms = result.diffR1 + result.diffR2 + result.diffR3 + result.diffR4 + result.diffR5 + result.diffR6
        val utilityDispStyle = "${result.discDistR1}+${result.discDistR2}+${result.discDistR3}"
        
        y += 20f
        canvas.drawText("Prev: ${result.input.prevMmr} | Curr: ${result.input.currMmr} | Diff: ${result.mainMeterMMR} un", 35f, y + 16f, bodyBoldPaint)
        canvas.drawText("+ ${result.diffMotor} un", 215f, y + 16f, bodyPaint)
        canvas.drawText("+ $summationRooms un", 310f, y + 16f, bodyPaint)
        canvas.drawText("$utilityDispStyle = ${result.discrepancyL} un", 415f, y + 16f, bodyPaint)
        canvas.drawText("${result.mainMeterMMR} un", 500f, y + 16f, bodyBoldPaint)

        y += 45f

        // 4. TABLE 2: Detailed Room Billing Assessment Grid
        canvas.drawText("2. CONSOLIDATED ROOM BILLING & ASSESSMENT LEDGER", 30f, y, sectionPaint)
        canvas.drawLine(30f, y + 4f, (pageWidth - 30).toFloat(), y + 4f, dividerPaint)
        y += 15f

        val sumOrigDiff = result.room1.originalDiff + result.room2.originalDiff + result.room3.originalDiff + result.room4.originalDiff + result.room5.originalDiff + result.room6.originalDiff
        val sumSharedMotor = result.room1.sharedMotor + result.room2.sharedMotor + result.room3.sharedMotor + result.room4.sharedMotor + result.room5.sharedMotor + result.room6.sharedMotor
        val sumSharedDisc = result.room1.sharedDiscrepancy + result.room2.sharedDiscrepancy + result.room3.sharedDiscrepancy + result.room4.sharedDiscrepancy + result.room5.sharedDiscrepancy + result.room6.sharedDiscrepancy
        val sumUpdatedUnits = result.room1.updatedUnits + result.room2.updatedUnits + result.room3.updatedUnits + result.room4.updatedUnits + result.room5.updatedUnits + result.room6.updatedUnits
        val sumElecCharge = result.room1.electricityCharge + result.room2.electricityCharge + result.room3.electricityCharge + result.room4.electricityCharge + result.room5.electricityCharge + result.room6.electricityCharge
        val sumRent = result.room1.rent + result.room2.rent + result.room3.rent + result.room4.rent + result.room5.rent + result.room6.rent
        val sumSweeper = result.room1.sweeper + result.room2.sweeper + result.room3.sweeper + result.room4.sweeper + result.room5.sweeper + result.room6.sweeper
        val sumTotalBill = result.room1.totalBill + result.room2.totalBill + result.room3.totalBill + result.room4.totalBill + result.room5.totalBill + result.room6.totalBill

        // Table 2 Header Row 1 (Group Columns)
        canvas.drawRect(30f, y, (pageWidth - 30).toFloat(), y + 20f, headerBgPaint)
        canvas.drawText("Room", 34f, y + 14f, gridHeaderPaint)
        canvas.drawText("Tenant Name", 69f, y + 14f, gridHeaderPaint)
        
        // Group Header: "Electric Unit" with subheader bounding box
        canvas.drawText("Electric Unit", 205f, y + 14f, gridHeaderPaint)
        
        canvas.drawText("Elec Charge", 314f, y + 14f, gridHeaderPaint)
        canvas.drawText("Rent", 379f, y + 14f, gridHeaderPaint)
        canvas.drawText("Sweeper", 434f, y + 14f, gridHeaderPaint)
        canvas.drawText("Total Amount", 489f, y + 14f, gridHeaderPaint)

        canvas.drawLine(30f, y + 20f, (pageWidth - 30).toFloat(), y + 20f, dividerPaint)
        y += 20f

        // Table 2 Header Row 2 (Subheaders under "Electric Unit")
        canvas.drawRect(30f, y, (pageWidth - 30).toFloat(), y + 15f, highlightPaint)
        canvas.drawText("Meter", 164f, y + 11f, gridSubHeaderPaint)
        canvas.drawText("Motor", 199f, y + 11f, gridSubHeaderPaint)
        canvas.drawText("Utility", 236f, y + 11f, gridSubHeaderPaint)
        canvas.drawText("Total", 274f, y + 11f, gridSubHeaderPaint)

        canvas.drawLine(30f, y + 15f, (pageWidth - 30).toFloat(), y + 15f, dividerPaint)
        y += 15f

        // Store start of data records
        val dataStartY = y - 35f // includes both header rows height (20f + 15f)
        
        val roomsDataList = listOf(
            Pair("Room 1", result.room1),
            Pair("Room 2", result.room2),
            Pair("Room 3", result.room3),
            Pair("Room 4", result.room4),
            Pair("Room 5", result.room5),
            Pair("Room 6", result.room6)
        )

        for ((index, item) in roomsDataList.withIndex()) {
            val roomName = item.first
            val roomRes = item.second
            
            // Background zebra shading
            if (index % 2 == 1) {
                canvas.drawRect(30f, y, (pageWidth - 30).toFloat(), y + 25f, highlightPaint)
            }

            // Draw texts formatted
            canvas.drawText(roomName, 34f, y + 16f, bodyBoldPaint)
            val nameStr = if (roomRes.tenantName.isBlank() || roomRes.tenantName.equals("vacant", ignoreCase = true)) "VACANT" else roomRes.tenantName
            val tenantPaint = if (nameStr == "VACANT") {
                Paint(bodyPaint).apply { color = Color.parseColor("#B3261E"); typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
            } else {
                bodyPaint
            }
            canvas.drawText(nameStr, 69f, y + 16f, tenantPaint)

            // Electric units columns
            canvas.drawText("${roomRes.originalDiff}", 164f, y + 16f, bodyPaint)
            canvas.drawText("+${roomRes.sharedMotor}", 199f, y + 16f, bodyPaint)
            canvas.drawText("+${roomRes.sharedDiscrepancy}", 236f, y + 16f, bodyPaint)
            canvas.drawText("${roomRes.updatedUnits}", 274f, y + 16f, bodyBoldPaint)

            // Money ledger columns
            canvas.drawText("₹${String.format("%.1f", roomRes.electricityCharge)}", 314f, y + 16f, bodyPaint)
            canvas.drawText("₹${String.format("%.1f", roomRes.rent)}", 379f, y + 16f, bodyPaint)
            canvas.drawText("₹${String.format("%.1f", roomRes.sweeper)}", 434f, y + 16f, bodyPaint)
            canvas.drawText("₹${String.format("%.1f", roomRes.totalBill)}", 489f, y + 16f, Paint(bodyBoldPaint).apply { color = Color.parseColor("#6750A4") })

            canvas.drawLine(30f, y + 25f, (pageWidth - 30).toFloat(), y + 25f, dividerPaint)
            y += 25f
        }

        // Draw Grand Total Row background
        canvas.drawRect(30f, y, (pageWidth - 30).toFloat(), y + 25f, headerBgPaint)

        // Draw texts for Grand Total
        canvas.drawText("Total", 34f, y + 16f, bodyBoldPaint)
        canvas.drawText("Grand Summary", 69f, y + 16f, bodyBoldPaint)

        // Electric units sums
        canvas.drawText("$sumOrigDiff", 164f, y + 16f, bodyBoldPaint)
        canvas.drawText("+$sumSharedMotor", 199f, y + 16f, bodyBoldPaint)
        canvas.drawText("+$sumSharedDisc", 236f, y + 16f, bodyBoldPaint)
        canvas.drawText("$sumUpdatedUnits", 274f, y + 16f, bodyBoldPaint)

        // Money ledger sums
        canvas.drawText("₹${String.format("%.1f", sumElecCharge)}", 314f, y + 16f, bodyBoldPaint)
        canvas.drawText("₹${String.format("%.1f", sumRent)}", 379f, y + 16f, bodyBoldPaint)
        canvas.drawText("₹${String.format("%.1f", sumSweeper)}", 434f, y + 16f, bodyBoldPaint)
        canvas.drawText("₹${String.format("%.1f", sumTotalBill)}", 489f, y + 16f, Paint(bodyBoldPaint).apply { color = Color.parseColor("#6750A4") })

        canvas.drawLine(30f, y + 25f, (pageWidth - 30).toFloat(), y + 25f, dividerPaint)
        y += 25f

        // Draw Table 2 vertical column gridlines
        val totalGridHeight = 35f + (7 * 25f) // headers + 7 rows (6 rooms + 1 grand total)
        canvas.drawRect(30f, dataStartY, (pageWidth - 30).toFloat(), dataStartY + totalGridHeight, borderPaint)

        canvas.drawLine(65f, dataStartY, 65f, dataStartY + totalGridHeight, dividerPaint)
        canvas.drawLine(160f, dataStartY, 160f, dataStartY + totalGridHeight, dividerPaint)
        
        // Under Electric Unit Subheaders
        canvas.drawLine(195f, dataStartY + 20f, 195f, dataStartY + totalGridHeight, dividerPaint)
        canvas.drawLine(232f, dataStartY + 20f, 232f, dataStartY + totalGridHeight, dividerPaint)
        canvas.drawLine(270f, dataStartY + 20f, 270f, dataStartY + totalGridHeight, dividerPaint)
        canvas.drawLine(310f, dataStartY, 310f, dataStartY + totalGridHeight, dividerPaint)

        canvas.drawLine(375f, dataStartY, 375f, dataStartY + totalGridHeight, dividerPaint)
        canvas.drawLine(430f, dataStartY, 430f, dataStartY + totalGridHeight, dividerPaint)
        canvas.drawLine(485f, dataStartY, 485f, dataStartY + totalGridHeight, dividerPaint)

        y += 25f

        // 5. Grand Sign-Off Summary Card
        canvas.drawRoundRect(30f, y, (pageWidth - 30).toFloat(), y + 75f, 12f, 12f, Paint().apply { color = Color.parseColor("#FEF7FF") })
        canvas.drawRoundRect(30f, y, (pageWidth - 30).toFloat(), y + 75f, 12f, 12f, Paint().apply {
            color = Color.parseColor("#6750A4")
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        })

        val grandTotalSum = result.room1.totalBill + result.room2.totalBill + result.room3.totalBill + result.room4.totalBill + result.room5.totalBill + result.room6.totalBill
        
        canvas.drawText("GRAND SUMMARY SIGN-OFF", 45f, y + 25f, subtitlePaint)
        canvas.drawText("Total Energy Shared: ${result.totalUpdatedUnits} Units Metered", 45f, y + 43f, bodyBoldPaint)
        canvas.drawText("Total Invoice charges: ₹${String.format("%,.2f", grandTotalSum)}", 45f, y + 60f, Paint(titlePaint).apply { textSize = 11.5f })

        canvas.drawText("Audit Clearance signature:", 380f, y + 25f, bodyPaint)
        canvas.drawLine(380f, y + 54f, 530f, y + 54f, dividerPaint)
        canvas.drawText("Authorized Landlord", 412f, y + 66f, bodyBoldPaint)

        // 6. Footer Page 1 of 1
        canvas.drawText("Billing Auditor Utility • Complete Single-Page Audit Summary • Generated Offline", 30f, (pageHeight - 25).toFloat(), footerPaint)

        pdfDocument.finishPage(page)

        // Save PDF to Downloads folder
        val fileName = "Master_Billing_Report_${month.replace(" ", "_")}.pdf"
        val uri = savePdfDocument(context, pdfDocument, fileName)
        pdfDocument.close()
        return uri
    }

    private fun drawRoomCardHelper(
        canvas: Canvas,
        room: RoomInvoiceData,
        x: Float,
        y: Float,
        boldPaint: Paint,
        bodyPaint: Paint,
        dividerPaint: Paint,
        bgPaint: Paint
    ) {
        val width = 250f
        val height = 155f

        // Card shadow/boundary
        canvas.drawRoundRect(x, y, x + width, y + height, 16f, 16f, Paint().apply { color = Color.parseColor("#12000000") })
        canvas.drawRoundRect(x - 2f, y - 2f, x + width - 2f, y + height - 2f, 16f, 16f, Paint().apply { color = Color.WHITE })
        canvas.drawRoundRect(x - 2f, y - 2f, x + width - 2f, y + height - 2f, 16f, 16f, Paint().apply {
            color = Color.parseColor("#CAC4D0")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        })

        // Card Header background
        canvas.drawRoundRect(x - 1f, y - 1f, x + width - 3f, y + 26f, 16f, 16f, bgPaint)
        canvas.drawRect(x - 1f, y + 15f, x + width - 3f, y + 26f, bgPaint)

        // Tenant description
        val tenantNameStr = if (room.res.tenantName.isBlank() || room.res.tenantName.equals("vacant", ignoreCase = true)) "VACANT" else room.res.tenantName
        canvas.drawText("ROOM ${room.number} ($tenantNameStr)", x + 15f, y + 17f, boldPaint)
        canvas.drawLine(x - 2f, y + 26f, x + width - 2f, y + 26f, dividerPaint)

        var itemY = y + 43f
        canvas.drawText("Rent Charge:", x + 15f, itemY, bodyPaint)
        canvas.drawText("₹${String.format("%,.2f", room.res.rent)}", x + 160f, itemY, boldPaint)

        itemY += 17f
        canvas.drawText("Electricity (${room.res.updatedUnits} un):", x + 15f, itemY, bodyPaint)
        canvas.drawText("₹${String.format("%,.2f", room.res.electricityCharge)}", x + 160f, itemY, boldPaint)

        itemY += 17f
        canvas.drawText("Sweeper Charge:", x + 15f, itemY, bodyPaint)
        canvas.drawText("₹${String.format("%,.2f", room.res.sweeper)}", x + 160f, itemY, boldPaint)

        itemY += 17f
        canvas.drawText("Custom Charge:", x + 15f, itemY, bodyPaint)
        canvas.drawText("₹${String.format("%,.2f", room.res.custom)}", x + 160f, itemY, boldPaint)

        canvas.drawLine(x + 15f, itemY + 10f, x + width - 15f, itemY + 10f, dividerPaint)

        itemY += 24f
        val totalPaint = Paint(boldPaint).apply {
            textSize = 10f
            color = Color.parseColor("#6750A4")
        }
        canvas.drawText("TOTAL DUE:", x + 15f, itemY, totalPaint)
        canvas.drawText("₹${String.format("%,.2f", room.res.totalBill)}", x + 160f, itemY, totalPaint)
    }


    /**
     * Category B: Individual Room Bill Invoice PDF (Single Page)
     */
    fun generateAndSaveRoomPdf(
        context: Context,
        result: RoomBillingResult,
        roomNum: Int,
        prevReading: Int,
        currReading: Int,
        month: String,
        date: String,
        rate: Double,
        billNum: String
    ): Uri? {
        val pdfDocument = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842

        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        // Header Background Decor
        val headerBarPaint = Paint().apply { color = Color.parseColor("#6750A4") }
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 110f, headerBarPaint)

        // Typefaces & Colors
        val titlePaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 21f
            color = Color.WHITE
        }
        val whiteBodyPaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textSize = 10f
            color = Color.parseColor("#EADDFF")
        }
        val billNoPaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 13f
            color = Color.parseColor("#FEF7FF")
        }
        val subHeadingPaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 14f
            color = Color.parseColor("#21005D")
        }
        val regularLabelPaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textSize = 10f
            color = Color.parseColor("#49454F")
        }
        val boldValuePaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 11f
            color = Color.parseColor("#1D1B20")
        }
        val dividerPaint = Paint().apply {
            color = Color.parseColor("#CAC4D0")
            strokeWidth = 1f
        }
        val highlightPaint = Paint().apply {
            color = Color.parseColor("#F7F2FA")
        }

        // Draw Header Content
        canvas.drawText("ROOM INVOICE DETAILED BILL", 40f, 45f, titlePaint)
        val occupant = if (result.tenantName.isBlank() || result.tenantName.equals("vacant", ignoreCase = true)) "VACANT" else result.tenantName
        canvas.drawText("Room Number: Room $roomNum ($occupant)  |  Billing Month: $month", 40f, 68f, whiteBodyPaint)
        canvas.drawText("Generated Date: $date", 40f, 85f, whiteBodyPaint)
        canvas.drawText(billNum, pageWidth - 180f, 45f, billNoPaint)

        var y = 145f

        // Tenant Detail & Status section
        canvas.drawRoundRect(40f, y, (pageWidth - 40).toFloat(), y + 65f, 16f, 16f, highlightPaint)
        canvas.drawText("TENANT INFORMATION", 55f, y + 22f, subHeadingPaint)
        canvas.drawText("Occupant Tenant Name: $occupant", 55f, y + 43f, boldValuePaint)
        
        // Status Badge
        val statusBg = Paint().apply {
            color = if (occupant == "VACANT") Color.parseColor("#F9DEDC") else Color.parseColor("#C8E6C9")
        }
        val statusTxt = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 9f
            color = if (occupant == "VACANT") Color.parseColor("#8B0000") else Color.parseColor("#1B5E20")
        }
        canvas.drawRoundRect(pageWidth - 160f, y + 10f, pageWidth - 55f, y + 30f, 10f, 10f, statusBg)
        val statusLabel = if (occupant == "VACANT") "STATUS: VACANT" else "STATUS: OCCUPIED"
        canvas.drawText(statusLabel, pageWidth - 150f, y + 23f, statusTxt)

        // Increment past the Tenant Information card (height is 65f, so start at y + 65f + 35f padding)
        y += 100f

        // Ledger Pricing Table
        canvas.drawText("LEDGER ASSESSMENT BREAKDOWN", 40f, y, subHeadingPaint)
        canvas.drawLine(40f, y + 5f, (pageWidth - 40).toFloat(), y + 5f, dividerPaint)
        y += 25f

        // Paint tables
        val tableHeadPaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 10f
            color = Color.parseColor("#21005D")
        }
        
        canvas.drawRect(40f, y, (pageWidth - 40).toFloat(), y + 24f, highlightPaint)
        canvas.drawText("Assessment Item Description", 50f, y + 16f, tableHeadPaint)
        canvas.drawText("Calculation Reference", 260f, y + 16f, tableHeadPaint)
        canvas.drawText("Total (INR)", pageWidth - 140f, y + 16f, tableHeadPaint)
        
        y += 24f

        val ledgerItems = listOf(
            LedgerItem("Gross Room Rent Assessment", "Fixed monthly covenant rate", result.rent),
            LedgerItem("Electricity Utility Consumption", "${result.updatedUnits} units × ₹$rate", result.electricityCharge),
            LedgerItem("Sweeper Service Charge", "Dedicated room sweep allocation", result.sweeper),
            LedgerItem("Custom Charges / Maintenance", "Special cycle assessment key", result.custom)
        )

        for ((index, item) in ledgerItems.withIndex()) {
            canvas.drawText(item.name, 50f, y + 17f, regularLabelPaint)
            canvas.drawText(item.calc, 260f, y + 17f, regularLabelPaint)
            canvas.drawText("₹${String.format("%,.2f", item.amount)}", pageWidth - 140f, y + 17f, boldValuePaint)
            
            canvas.drawLine(40f, y + 25f, (pageWidth - 40).toFloat(), y + 25f, dividerPaint)
            y += 25f
        }

        y += 25f

        // Huge Grand Total Due Box
        canvas.drawRoundRect(40f, y, (pageWidth - 40).toFloat(), y + 70f, 16f, 16f, Paint().apply { color = Color.parseColor("#FEF7FF") })
        canvas.drawRoundRect(40f, y, (pageWidth - 40).toFloat(), y + 70f, 16f, 16f, Paint().apply {
            color = Color.parseColor("#6750A4")
            style = Paint.Style.STROKE
            strokeWidth = 2f
        })

        val grandTotalTextPaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 15f
            color = Color.parseColor("#21005D")
        }
        val grandTotalValuePaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 20f
            color = Color.parseColor("#6750A4")
        }

        canvas.drawText("TOTAL AMOUNT DUE:", 55f, y + 41f, grandTotalTextPaint)
        canvas.drawText("₹${String.format("%,.2f", result.totalBill)}", pageWidth - 190f, y + 45f, grandTotalValuePaint)

        // DUE label watermarked in red
        val dueLabelPaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 28f
            color = Color.parseColor("#15BA1A1A") // Extremely transparent red watermark
        }
        canvas.drawText("UNPAID / DUE", pageWidth - 210f, y - 10f, dueLabelPaint)

        // Advance past Total Due Box (starts at y, ends at y+70f). Make y change by 70f + 60f margin = 130f
        y += 130f

        // Footer signature
        canvas.drawText("Verified landlord digital summary copy", 40f, y, regularLabelPaint)
        canvas.drawLine(pageWidth - 180f, y, pageWidth - 40f, y, dividerPaint)
        canvas.drawText("Landlord Signature", pageWidth - 160f, y + 14f, boldValuePaint)

        val footerPaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            textSize = 8f
            color = Color.parseColor("#7A7A7A")
        }
        canvas.drawText("Confidential • System Generated Mathematical Audit Receipt Copy", 40f, (pageHeight - 25).toFloat(), footerPaint)

        pdfDocument.finishPage(page)

        // Save PDF to Downloads folder
        val fileName = "Room_${roomNum}_Bill_${month.replace(" ", "_")}.pdf"
        val uri = savePdfDocument(context, pdfDocument, fileName)
        pdfDocument.close()
        return uri
    }

    private fun savePdfDocument(context: Context, doc: PdfDocument, name: String): Uri? {
        var uri: Uri? = null
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val contentResolver = context.contentResolver
                val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                uri = contentResolver.insert(collection, contentValues)
                if (uri != null) {
                    contentResolver.openOutputStream(uri).use { out ->
                        if (out != null) doc.writeTo(out)
                    }
                }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }
                val file = File(downloadsDir, name)
                FileOutputStream(file).use { out ->
                    doc.writeTo(out)
                }
                uri = Uri.fromFile(file)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return uri
    }

    /**
     * Category C: Meter Record PDF with Photo Verification
     */
    fun generateAndSaveMeterRecordsPdf(
        context: Context,
        record: com.example.data.BillingRecord,
        month: String,
        date: String
    ): Uri? {
        val pdfDocument = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842

        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        // Common Paints
        val titlePaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 15f
            color = Color.parseColor("#4F378B") // Brand Purple
        }
        val headerPaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 10f
            color = Color.parseColor("#21005D")
        }
        val cardTitlePaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 9f
            color = Color.parseColor("#6750A4")
        }
        val textPaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textSize = 8f
            color = Color.parseColor("#1D1B20")
        }
        val borderPaint = Paint().apply {
            color = Color.parseColor("#7A7A7A")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val cardBgPaint = Paint().apply {
            color = Color.parseColor("#F7F2FA")
        }
        val dividerPaint = Paint().apply {
            color = Color.parseColor("#CAC4D0")
            strokeWidth = 0.8f
        }

        // Header Background block
        val headerBg = Paint().apply {
            color = Color.parseColor("#EADDFF")
        }
        canvas.drawRect(30f, 30f, (pageWidth - 30).toFloat(), 85f, headerBg)
        canvas.drawText("METER PHOTO RECORDING ARCHIVE", 45f, 52f, titlePaint)
        canvas.drawText("Verification Audit Trail • Month: $month • Date: $date", 45f, 72f, headerPaint)

        // Draw outer bounding box
        canvas.drawRect(30f, 30f, (pageWidth - 30).toFloat(), (pageHeight - 30).toFloat(), borderPaint)

        // Calculate columns for grid: 3 rows, 2 columns layout
        val colWidth = 240f
        val cardHeight = 210f
        
        val roomsData = listOf(
            Triple(1, record.tenant1, Pair(record.prevR1, record.currR1)),
            Triple(2, record.tenant2, Pair(record.prevR2, record.currR2)),
            Triple(3, record.tenant3, Pair(record.prevR3, record.currR3)),
            Triple(4, record.tenant4, Pair(record.prevR4, record.currR4)),
            Triple(5, record.tenant5, Pair(record.prevR5, record.currR5)),
            Triple(6, record.tenant6, Pair(record.prevR6, record.currR6))
        )

        for (index in 0..5) {
            val (roomNum, tenant, readings) = roomsData[index]
            val row = index / 2
            val col = index % 2

            val startX = if (col == 0) 45f else 310f
            val startY = 100f + (row * (cardHeight + 25f))

            // Draw Card background
            canvas.drawRect(startX, startY, startX + colWidth, startY + cardHeight, cardBgPaint)
            canvas.drawRect(startX, startY, startX + colWidth, startY + cardHeight, borderPaint)

            // Room title
            canvas.drawText("Room $roomNum Verification Slip", startX + 10f, startY + 20f, cardTitlePaint)
            // Tenant
            canvas.drawText("Tenant: $tenant", startX + 10f, startY + 36f, textPaint)
            // Readings
            canvas.drawText("Prev Rd: ${readings.first}  |  Curr Rd: ${readings.second}", startX + 10f, startY + 52f, textPaint)

            // Draw divider inside card
            canvas.drawLine(startX + 5f, startY + 60f, startX + colWidth - 5f, startY + 60f, dividerPaint)

            // Photo paths
            val photoPath = when (roomNum) {
                1 -> record.photoR1
                2 -> record.photoR2
                3 -> record.photoR3
                4 -> record.photoR4
                5 -> record.photoR5
                else -> record.photoR6
            }

            val imgX = startX + 25f
            val imgY = startY + 70f
            val imgW = 190f
            val imgH = 125f

            if (photoPath.isNotEmpty() && File(photoPath).exists()) {
                try {
                    val file = File(photoPath)
                    val opts = BitmapFactory.Options().apply {
                        inSampleSize = 4 // downsample for performance
                    }
                    val originalBitmap = BitmapFactory.decodeFile(file.absolutePath, opts)
                    if (originalBitmap != null) {
                        val scaled = Bitmap.createScaledBitmap(originalBitmap, imgW.toInt(), imgH.toInt(), true)
                        canvas.drawBitmap(scaled, imgX, imgY, null)
                        scaled.recycle()
                        originalBitmap.recycle()
                    } else {
                        drawPlaceholderBox(canvas, imgX, imgY, imgW, imgH, "Failed to decode photo")
                    }
                } catch (e: Exception) {
                    drawPlaceholderBox(canvas, imgX, imgY, imgW, imgH, "Decoding Error")
                }
            } else {
                drawPlaceholderBox(canvas, imgX, imgY, imgW, imgH, "No Photo Saved")
            }
        }

        // Draw Footer
        val footerPaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            textSize = 7.5f
            color = Color.parseColor("#7A7A7A")
        }
        canvas.drawText("Meter readings photo records audit sheet for month: $month", 45f, (pageHeight - 15).toFloat(), footerPaint)

        pdfDocument.finishPage(page)

        val outName = "meter_record_${month.replace(" ", "_")}.pdf"
        val uri = savePdfDocument(context, pdfDocument, outName)
        pdfDocument.close()
        return uri
    }

    private fun drawPlaceholderBox(canvas: Canvas, x: Float, y: Float, w: Float, h: Float, message: String) {
        val boxPaint = Paint().apply {
            color = Color.parseColor("#E1E0E5")
        }
        val borderPaint = Paint().apply {
            color = Color.parseColor("#CAC4D0")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val textPaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textSize = 8f
            color = Color.parseColor("#49454F")
            textAlign = Paint.Align.CENTER
        }

        canvas.drawRect(x, y, x + w, y + h, boxPaint)
        canvas.drawRect(x, y, x + w, y + h, borderPaint)
        canvas.drawText(message, x + (w / 2f), y + (h / 2f) + 3f, textPaint)
    }

    // Secondary Class Definitions
    private data class RowData(
        val label: String,
        val prev: Int,
        val curr: Int,
        val origDiff: Int,
        val adjustment: String,
        val finalUnits: Int
    )

    private data class RoomInvoiceData(
        val number: Int,
        val res: RoomBillingResult
    )

    private data class LedgerItem(
        val name: String,
        val calc: String,
        val amount: Double
    )
}
