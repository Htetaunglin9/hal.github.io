package com.hal.carlistmanager

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var containerBlack: LinearLayout
    private lateinit var containerDuty: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        containerBlack = findViewById(R.id.containerPagesBlack)
        containerDuty = findViewById(R.id.containerPagesDuty)

        val btnAddEntry = findViewById<Button>(R.id.btnAddEntry)
        val btnSummary = findViewById<Button>(R.id.btnSummary)
        val btnBatchEntry = findViewById<Button>(R.id.btnBatchEntry)

        // ✅ Individual Entry
        btnAddEntry.setOnClickListener {
            val intent = Intent(this, AddEntryActivity::class.java)
            startActivity(intent)
        }

        // ✅ Summary Page
        btnSummary.setOnClickListener {
            val intent = Intent(this, SummaryActivity::class.java)
            startActivity(intent)
        }

        // ✅ Batch Entry
        btnBatchEntry.setOnClickListener {
            val intent = Intent(this, BatchAddEntryActivity::class.java)
            startActivity(intent)
        }

        val db = AppDatabase.getDatabase(this)
        lifecycleScope.launch {
            generatePagination(containerBlack, db, "Black")
            generatePagination(containerDuty, db, "ဂျူတီ")
        }
    }

    private fun generatePagination(container: LinearLayout, db: AppDatabase, listType: String) {
        lifecycleScope.launch {
            val count = db.carEntryDao().getCountByListType(listType)
            val totalPages = (count + 39) / 40 // 40 per page

            container.removeAllViews()

            for (page in 1..totalPages) {
                val button = Button(this@MainActivity).apply {
                    text = "$listType $page"
                    setOnClickListener {
                        val intent = Intent(this@MainActivity, ListActivity::class.java)
                        intent.putExtra("listType", listType)
                        intent.putExtra("pageNumber", page)
                        startActivity(intent)
                    }
                }
                container.addView(button)
            }
        }
    }
    override fun onResume() {
        super.onResume()
        val db = AppDatabase.getDatabase(this)
        lifecycleScope.launch {
            generatePagination(containerBlack, db, "Black")
            generatePagination(containerDuty, db, "ဂျူတီ")
        }
    }
}
