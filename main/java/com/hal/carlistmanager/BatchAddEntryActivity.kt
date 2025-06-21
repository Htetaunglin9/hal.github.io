package com.hal.carlistmanager

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import java.util.*
import android.util.Log
import android.view.View

class BatchAddEntryActivity : AppCompatActivity() {

    private lateinit var spnListType: Spinner
    private lateinit var etDate: EditText
    private lateinit var spnRoad: Spinner
    private lateinit var etMultiEntries: EditText
    private lateinit var btnSaveBatch: Button
    private lateinit var rvPreview: RecyclerView

    private var parsedEntries = mutableListOf<CarEntry>()
    private var inMemoryDuplicates = setOf<String>()
    private var existingCarNos = setOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_batch_add_entry)

        spnListType = findViewById(R.id.spnListType)
        etDate = findViewById(R.id.etDate)
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        etDate.setText(currentYear.toString())

        spnRoad = findViewById(R.id.spnRoad)
        spnRoad.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            listOf("လမ်းရွေးပါ", "2", "3")
        )
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            listOf("လမ်းရွေးပါ", "2", "3")
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spnRoad.adapter = adapter
        etMultiEntries = findViewById(R.id.etMultiEntries)
        btnSaveBatch = findViewById(R.id.btnSaveBatch)
        rvPreview = findViewById(R.id.rvPreview)

        rvPreview.layoutManager = LinearLayoutManager(this)
        spnListType.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, listOf("Black", "ဂျူတီ"))
        val listTypeAdapter = ArrayAdapter(
            this,
            R.layout.spinner_item,
            listOf("Black", "ဂျူတီ")
        )
        listTypeAdapter.setDropDownViewResource(R.layout.spinner_item)
        spnListType.adapter = listTypeAdapter


        etDate.setOnClickListener {
            showYearPicker()
        }

        etMultiEntries.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = updatePreview()
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        btnSaveBatch.setOnClickListener {
            if (parsedEntries.isEmpty()) {
                Toast.makeText(this, "စာရင်းများ မရှိသေးပါ", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val duplicates = parsedEntries
                .map { it.carNo }
                .groupingBy { it }
                .eachCount()
                .filterValues { it > 1 }

            val alreadyExist = parsedEntries.map { it.carNo }.filter { it in existingCarNos }

            if (duplicates.isNotEmpty() || alreadyExist.isNotEmpty()) {
                val warningMsg = buildString {
                    if (duplicates.isNotEmpty()) append("🔁 ထပ်နေတဲ့ CarNo: ${duplicates.keys.joinToString(", ")}\n")
                    if (alreadyExist.isNotEmpty()) append("📂 DB ထဲမှာရှိပြီးသား: ${alreadyExist.joinToString(", ")}\n")
                    append("\nသိမ်းချင်ပါသလား?")
                }

                AlertDialog.Builder(this)
                    .setTitle("⚠️ သတိပေးချက်")
                    .setMessage(warningMsg)
                    .setPositiveButton("Save") { _, _ -> saveToDatabase() }
                    .setNegativeButton("မသိမ်းပါ", null)
                    .show()

                return@setOnClickListener
            }

            saveToDatabase()
        }
    }

    private fun saveToDatabase() {
        val db = AppDatabase.getDatabase(this)
        lifecycleScope.launch {
            db.carEntryDao().insertAll(parsedEntries)
            runOnUiThread {
                Toast.makeText(this@BatchAddEntryActivity, "စာရင်း ${parsedEntries.size} ခု သိမ်းပြီးပါပြီ", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun updatePreview() {
        val listType = spnListType.selectedItem.toString()
        val year = etDate.text.toString().trim().filter { it.isDigit() }.toIntOrNull() ?: return
        val roadSelection = spnRoad.selectedItem.toString()
        val road = if (roadSelection == "လမ်းရွေးပါ") "" else roadSelection
        val lines = etMultiEntries.text.toString().trim().lines()

        parsedEntries.clear()
        val carNoSet = mutableSetOf<String>()

        // FIXED regex
        val regex = Regex("""([A-Z0-9.]+)\((\d{1,2})w\)""")
        val datePattern = Regex("""(\d{1,2})/(\d{1,2})""")

        var currentDate = ""
        val db = AppDatabase.getDatabase(this)

        lifecycleScope.launch {
            existingCarNos = db.carEntryDao().getAllCarNos().toSet()

            for (line in lines) {
                val dateMatch = datePattern.find(line)
                if (dateMatch != null) {
                    val day = dateMatch.groupValues[1].padStart(2, '0')
                    val month = dateMatch.groupValues[2].padStart(2, '0')
                    currentDate = "$year-$month-$day"
                }

                val match = regex.find(line)
                if (match != null && currentDate.isNotEmpty()) {
                    val carNo = match.groupValues[1]
                    val wheels = match.groupValues[2].toInt()
                    val fee = calculateFee(wheels, listType)

                    val entry = CarEntry(
                        carNo = carNo,
                        wheels = wheels,
                        road = road,
                        fee = fee,
                        listType = listType,
                        date = currentDate

                    )
                    Log.d("DebugParse", "Matched: carNo=$carNo, wheels=$wheels, date=$currentDate")


                    parsedEntries.add(entry)

                }
            }

            inMemoryDuplicates = parsedEntries
                .map { it.carNo }
                .groupingBy { it }
                .eachCount()
                .filterValues { it > 1 }
                .keys

            rvPreview.adapter = BatchPreviewAdapter(parsedEntries, inMemoryDuplicates, existingCarNos)
        }
    }


    private fun calculateFee(wheels: Int, listType: String): Int {
        return when (listType) {
            "Black" -> when (wheels) {
                6 -> 90000
                10 -> 130000
                12, 22 -> 150000
                else -> 0
            }
            "ဂျူတီ" -> when (wheels) {
                6 -> 70000
                10 -> 100000
                12, 22 -> 120000
                else -> 0
            }
            else -> 0
        }
    }
    private fun showYearPicker() {
        val calendar = Calendar.getInstance()
        val thisYear = calendar.get(Calendar.YEAR)

        val yearPicker = NumberPicker(this).apply {
            minValue = 2000
            maxValue = thisYear + 10
            value = thisYear
            wrapSelectorWheel = false
        }

        AlertDialog.Builder(this)
            .setTitle("ခုနှစ် ရွေးပါ")
            .setView(yearPicker)
            .setPositiveButton("OK") { _, _ ->
                etDate.setText(yearPicker.value.toString())
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

}