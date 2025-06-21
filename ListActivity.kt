package com.hal.carlistmanager

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ListActivity : AppCompatActivity() {

    private lateinit var adapter: CarEntryAdapter
    private lateinit var rvEntries: RecyclerView
    private lateinit var btnExportImage: Button
    private lateinit var exportContainer: View
    private lateinit var tvPageTitle: TextView

    private var pageNumber = 1
    private var offset = 0
    private lateinit var listType: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)

        // Initialize views
        rvEntries = findViewById(R.id.rvEntries)
        btnExportImage = findViewById(R.id.btnExportImage)
        exportContainer = findViewById(R.id.exportContainer)
        tvPageTitle = findViewById(R.id.tvPageTitle)

        // Get intent extras
        listType = intent.getStringExtra("listType") ?: "Black"
        pageNumber = intent.getIntExtra("pageNumber", 1)
        offset = (pageNumber - 1) * 40

        // Set up UI
        tvPageTitle.text = "$listType စာရင်း - စာမျက်နှာ $pageNumber"

        // Set up RecyclerView with corrected adapter initialization
        rvEntries.layoutManager = LinearLayoutManager(this)
        adapter = CarEntryAdapter() // No parameter needed now
        rvEntries.adapter = adapter

        // Load data
        loadEntries()

        // Set up export button
        btnExportImage.setOnClickListener {
            exportToImageCustom()
        }
    }

    private fun loadEntries() {
        val db = AppDatabase.getDatabase(this)
        lifecycleScope.launch {
            try {
                val entries = db.carEntryDao().getEntriesByTypePaged(listType, 40, offset)
                adapter.submitList(entries)
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@ListActivity,
                        "ဒေတာများမရှိပါ: ${e.message}",
                        Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun exportToImageCustom() {
        try {
            btnExportImage.visibility = View.INVISIBLE

            exportContainer.measure(
                View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.UNSPECIFIED
            )

            val bitmap = Bitmap.createBitmap(
                1080,
                exportContainer.measuredHeight,
                Bitmap.Config.ARGB_8888
            )

            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)
            exportContainer.layout(0, 0, 1080, exportContainer.measuredHeight)
            exportContainer.draw(canvas)

            saveToGallery(bitmap)

        } catch (e: Exception) {
            runOnUiThread {
                Toast.makeText(this@ListActivity,
                    "ပုံထုတ်ရာတွင်အမှားအယွင်းရှိပါသည်: ${e.localizedMessage}",
                    Toast.LENGTH_LONG).show()
            }
        } finally {
            runOnUiThread {
                btnExportImage.visibility = View.VISIBLE
            }
        }
    }

    private fun saveToGallery(bitmap: Bitmap) {
        val dateFormat = SimpleDateFormat("dd-MM-yyyy_HHmmss", Locale.getDefault())
        val filename = "${listType}_Page${pageNumber}_${dateFormat.format(Date())}.jpg"

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH,
                Environment.DIRECTORY_PICTURES + "/CarlistManager")
        }

        try {
            contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )?.let { uri ->
                contentResolver.openOutputStream(uri)?.use { out ->
                    if (bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)) {
                        Toast.makeText(this,
                            "Pictures/CarlistManager တွင် သိမ်းဆည်းပြီးပါပြီ",
                            Toast.LENGTH_LONG).show()
                    }
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this,
                "သိမ်းဆည်းရာတွင် အမှားအယွင်းရှိပါသည်: ${e.localizedMessage}",
                Toast.LENGTH_LONG).show()
        }
    }
}