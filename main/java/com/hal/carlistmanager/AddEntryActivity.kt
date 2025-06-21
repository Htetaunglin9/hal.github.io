package com.hal.carlistmanager

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.util.*

class AddEntryActivity : AppCompatActivity() {

    private lateinit var etCarNo: EditText
    private lateinit var etDate: EditText
    private lateinit var spnWheels: Spinner
    private lateinit var spnRoad: Spinner
    private lateinit var tvGateFee: TextView
    private lateinit var spnListType: Spinner
    private lateinit var btnSave: Button

    private var currentGateFee = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_entry)

        etCarNo = findViewById(R.id.etCarNo)
        etDate = findViewById(R.id.etDate)
        spnWheels = findViewById(R.id.spnWheels)
        spnRoad = findViewById(R.id.spnRoad)
        tvGateFee = findViewById(R.id.tvGateFee)
        spnListType = findViewById(R.id.spnListType)
        btnSave = findViewById(R.id.btnSave)

        val wheelsOptions = listOf(6, 10, 12, 22)
        val listTypeOptions = listOf("Black", "ဂျူတီ")
        val roadOptions = listOf("2", "3", ) // Spinner 3 options

        spnWheels.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, wheelsOptions)
        spnListType.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, listTypeOptions)
        spnRoad.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, roadOptions)

        // Gate fee update listener
        val updateGateFeeListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                updateGateFee()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
        spnWheels.onItemSelectedListener = updateGateFeeListener
        spnListType.onItemSelectedListener = updateGateFeeListener

        // Date Picker
        val calendar = Calendar.getInstance()
        etDate.setOnClickListener {
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            val datePicker = DatePickerDialog(this, { _, y, m, d ->
                val selectedDate = String.format("%04d-%02d-%02d", y, m + 1, d)
                etDate.setText(selectedDate)
            }, year, month, day)
            datePicker.show()
        }

        // Save Button
        btnSave.setOnClickListener {
            val carNo = etCarNo.text.toString().trim()
            val date = etDate.text.toString().trim()
            val road = spnRoad.selectedItem as String
            val wheels = spnWheels.selectedItem as Int
            val type = spnListType.selectedItem as String

            if (carNo.isEmpty() || date.isEmpty()) {
                Toast.makeText(this, "အချက်အလက်ပြည့်စုံထည့်ပါ", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val entry = CarEntry(
                carNo = carNo,
                date = date,
                wheels = wheels,
                road = road,
                fee = currentGateFee,
                listType = type
            )

            val db = AppDatabase.getDatabase(this)
            lifecycleScope.launch {
                db.carEntryDao().insert(entry)
                runOnUiThread {
                    Toast.makeText(this@AddEntryActivity, "စာရင်း သိမ်းပြီးပါပြီ", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    private fun updateGateFee() {
        val wheels = spnWheels.selectedItem as Int
        val type = spnListType.selectedItem as String

        currentGateFee = when (type) {
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
        tvGateFee.text = "ဂိတ်ကြေး: $currentGateFee"
    }
}
