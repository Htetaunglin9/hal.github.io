package com.hal.carlistmanager

import android.app.DatePickerDialog
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Environment
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class SummaryActivity : AppCompatActivity() {

    private lateinit var etFromDate: EditText
    private lateinit var etToDate: EditText
    private lateinit var etCustomName: EditText
    private lateinit var etCustomAmount: EditText
    private lateinit var btnAddCustomRow: Button
    private lateinit var btnToggleRemoveMode: Button
    private lateinit var btnExportJPEG: Button
    private lateinit var btnGenerateSummary: Button
    private lateinit var containerSummary: LinearLayout
    private lateinit var summaryTableContainer: LinearLayout

    private var lastGrandTotal = 0
    private var currentManualTotal = 0
    private var removeMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_summary)

        etFromDate = findViewById(R.id.etFromDate)
        etToDate = findViewById(R.id.etToDate)
        etCustomName = findViewById(R.id.etCustomName)
        etCustomAmount = findViewById(R.id.etCustomAmount)
        btnAddCustomRow = findViewById(R.id.btnAddCustomRow)
        btnToggleRemoveMode = findViewById(R.id.btnToggleRemoveMode)
        btnExportJPEG = findViewById(R.id.btnExportJPEG)
        btnGenerateSummary = findViewById(R.id.btnGenerateSummary)
        containerSummary = findViewById(R.id.containerSummary)
        summaryTableContainer = findViewById(R.id.summaryTableContainer)

        setupDatePickers()

        btnAddCustomRow.setOnClickListener {
            val name = etCustomName.text.toString()
            val amount = etCustomAmount.text.toString().toIntOrNull() ?: 0

            if (name.isNotBlank() && amount > 0) {
                val row = layoutInflater.inflate(R.layout.item_summary_row, summaryTableContainer, false)
                row.findViewById<TextView>(R.id.tvLabel).apply {
                    text = name
                    setTypeface(null, Typeface.BOLD)
                }
                row.findViewById<TextView>(R.id.tvAmount).apply {
                    text = String.format("%,d", amount)
                    setTypeface(null, Typeface.BOLD)
                }

                row.tag = "manual_row"
                row.setTag(R.id.tvAmount, amount)

                val btnDelete = row.findViewById<Button>(R.id.btnDelete)
                btnDelete?.visibility = if (removeMode) Button.VISIBLE else Button.GONE
                btnDelete?.setOnClickListener {
                    summaryTableContainer.removeView(row)
                    currentManualTotal -= amount
                    updateTotalRow()
                }

                val totalIndex = summaryTableContainer.indexOfChild(summaryTableContainer.findViewWithTag("total_row"))
                val insertIndex = if (totalIndex != -1) totalIndex else summaryTableContainer.childCount
                summaryTableContainer.addView(row, insertIndex)

                currentManualTotal += amount
                updateTotalRow()

                etCustomName.text.clear()
                etCustomAmount.text.clear()
            }
        }

        btnToggleRemoveMode.setOnClickListener {
            removeMode = !removeMode
            btnToggleRemoveMode.text = if (removeMode) "✔ Ok" else "🗑 ဖျက်မည်"
            toggleDeleteButtons()
        }

        btnExportJPEG.setOnClickListener {
            exportSummaryAsJPEG()
        }

        btnGenerateSummary.setOnClickListener {
            if (etFromDate.text.isNullOrBlank() || etToDate.text.isNullOrBlank()) {
                Toast.makeText(this, "နေ့စွဲ ၂ ခုလုံးရွေးပါ", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            generateSummary()
        }
    }

    private fun toggleDeleteButtons() {
        for (i in 0 until summaryTableContainer.childCount) {
            val view = summaryTableContainer.getChildAt(i)
            if (view.tag == "manual_row") {
                view.findViewById<Button>(R.id.btnDelete)?.visibility = if (removeMode) Button.VISIBLE else Button.GONE
            }
        }
    }

    private fun setupDatePickers() {
        etFromDate.setOnClickListener { showDatePicker { date -> etFromDate.setText(date) } }
        etToDate.setOnClickListener { showDatePicker { date -> etToDate.setText(date) } }
    }

    private fun showDatePicker(onSelected: (String) -> Unit) {
        val cal = Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, d ->
            val formatted = String.format("%04d-%02d-%02d", y, m + 1, d)
            onSelected(formatted)
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun formatDateDisplay(input: String): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val formatter = SimpleDateFormat("d.M.yyyy", Locale.getDefault())
            formatter.format(parser.parse(input) ?: Date())
        } catch (e: Exception) {
            input
        }
    }

    private fun generateSummary() {
        val fromDate = etFromDate.text.toString()
        val toDate = etToDate.text.toString()
        val displayFrom = formatDateDisplay(fromDate)
        val displayTo = formatDateDisplay(toDate)

        val db = AppDatabase.getDatabase(this)
        val inflater = LayoutInflater.from(this)

        summaryTableContainer.removeAllViews()
        lastGrandTotal = 0
        currentManualTotal = 0

        lifecycleScope.launch {
            val types = listOf("Black", "ဂျူတီ")

            val tvTitle = TextView(this@SummaryActivity).apply {
                text = "ရန်ကုန်အဝင် စာရင်း"
                textSize = 30f
                setTypeface(null, Typeface.BOLD)
                alpha = 1.0f
                gravity = Gravity.CENTER
                setPadding(0, 24, 0, 24)
            }
            summaryTableContainer.addView(tvTitle)

            val tvHeader = TextView(this@SummaryActivity).apply {
                text = "($displayFrom) မှ ($displayTo)"
                textSize = 30f
                setTypeface(null, Typeface.BOLD)
                alpha = 1.0f
                gravity = Gravity.CENTER
                setPadding(0, 8, 0, 16)
            }
            summaryTableContainer.addView(tvHeader)

            for (type in types) {
                val entries = db.carEntryDao().getByTypeAndDate(type, fromDate, toDate)
                val pages = entries.chunked(40)
                for ((index, page) in pages.withIndex()) {
                    val totalFee = page.sumOf { it.fee }
                    lastGrandTotal += totalFee

                    val row = inflater.inflate(R.layout.item_summary_row, summaryTableContainer, false)
                    row.findViewById<TextView>(R.id.tvLabel).apply {
                        text = if (type == "ဂျူတီ") "ဂျူတီ စာရင်း ${index + 1}" else "Black စာရင်း ${index + 1}"
                        setTypeface(null, Typeface.BOLD)
                    }
                    row.findViewById<TextView>(R.id.tvAmount).apply {
                        text = String.format("%,d", totalFee)
                        setTypeface(null, Typeface.BOLD)
                    }
                    row.findViewById<Button>(R.id.btnDelete)?.visibility = Button.GONE
                    summaryTableContainer.addView(row)
                }
            }
            updateTotalRow()
        }
    }

    private fun updateTotalRow() {
        val total = lastGrandTotal + currentManualTotal
        val existing = summaryTableContainer.findViewWithTag<LinearLayout>("total_row")

        val row = existing ?: layoutInflater.inflate(R.layout.item_summary_row, summaryTableContainer, false).apply {
            tag = "total_row"
            summaryTableContainer.addView(this)
        }

        row.findViewById<TextView>(R.id.tvLabel).apply {
            text = "Total"
            setTypeface(null, Typeface.BOLD)
        }

        row.findViewById<TextView>(R.id.tvAmount).apply {
            text = String.format("%,d", total)
            setTypeface(null, Typeface.BOLD)
        }

        row.findViewById<Button>(R.id.btnDelete)?.visibility = Button.GONE
    }

    private fun exportSummaryAsJPEG() {
        val bitmap = getBitmapFromView(summaryTableContainer)

        val toDateRaw = etToDate.text.toString()
        val displayDate = try {
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val formatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            formatter.format(parser.parse(toDateRaw) ?: Date())
        } catch (e: Exception) {
            "unknown"
        }

        val fileName = "စာရင်းချုပ် $displayDate.jpg"
        val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val appDir = File(picturesDir, "BlackCarApp")
        if (!appDir.exists()) appDir.mkdirs()

        val file = File(appDir, fileName)
        try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            }
            Toast.makeText(this, "Saved: ${file.absolutePath}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to save JPEG", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getBitmapFromView(view: View): Bitmap {
        val widthSpec = View.MeasureSpec.makeMeasureSpec(view.width, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        view.measure(widthSpec, heightSpec)
        val width = view.measuredWidth
        val height = view.measuredHeight

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        view.layout(0, 0, width, height)
        view.draw(canvas)
        return bitmap
    }
}