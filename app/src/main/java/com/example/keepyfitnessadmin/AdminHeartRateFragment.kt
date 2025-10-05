package com.example.keepyfitnessadmin

import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.keepyfitnessadmin.model.HeartRateData
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class AdminHeartRateFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private var progressBar: ProgressBar? = null
    private var tableLayout: TableLayout? = null
    private var btnReload: Button? = null
    private var userId: String? = null
    private var userEmail: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userId = arguments?.getString("USER_ID")
        userEmail = arguments?.getString("USER_EMAIL")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_admin_heart_rate, container, false)
        db = FirebaseFirestore.getInstance()
        progressBar = view.findViewById(R.id.progressBarHeartRate)
        tableLayout = view.findViewById(R.id.heartRateTable)
        btnReload = view.findViewById(R.id.btnReload)

        if (tableLayout == null || progressBar == null || btnReload == null) {
            Log.e("AdminHeartRate", "Không tìm thấy heartRateTable, progressBarHeartRate hoặc btnReload trong giao diện")
            view?.let {
                Snackbar.make(it, "Lỗi giao diện, kiểm tra file fragment_admin_heart_rate.xml", Snackbar.LENGTH_LONG).show()
            }
            return view
        }

        btnReload?.setOnClickListener {
            loadHeartRates()
            Snackbar.make(it, "Đã tải lại trang", Snackbar.LENGTH_SHORT).show()
        }

        loadHeartRates()
        return view
    }

    private fun loadHeartRates() {
        progressBar?.visibility = View.VISIBLE
        Log.d("AdminHeartRate", "Starting query for heart rates, UserId: $userId")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val documents = if (userId.isNullOrEmpty()) {
                    db.collectionGroup("healthMetrics").get().await()
                } else {
                    db.collection("users").document(userId!!).collection("healthMetrics").get().await()
                }
                val heartRateList = mutableListOf<HeartRateData>()
                for (document in documents) {
                    try {
                        val heartRate = document.toObject(HeartRateData::class.java) ?: HeartRateData()
                        heartRateList.add(heartRate.copy(id = document.id))
                        Log.d("AdminHeartRate", "Nhịp tim: ${heartRate.bpm} bpm, Trạng thái: ${heartRate.status}, Gợi ý: ${heartRate.suggestion}, Thời gian: ${heartRate.timestamp}, Thời lượng: ${heartRate.duration}, ID: ${document.id}")
                    } catch (e: Exception) {
                        Log.e("AdminHeartRate", "Error deserializing heart rate document ${document.id}: ${e.message}")
                    }
                }
                activity?.runOnUiThread {
                    progressBar?.visibility = View.GONE
                    Log.d("AdminHeartRate", "Loaded ${heartRateList.size} heart rates")
                    if (heartRateList.isEmpty()) {
                        view?.let {
                            Snackbar.make(it, "Không có nhịp tim nào được tải", Snackbar.LENGTH_LONG).show()
                        }
                    }
                    updateTable(heartRateList)
                }
            } catch (e: Exception) {
                activity?.runOnUiThread {
                    progressBar?.visibility = View.GONE
                    Log.e("AdminHeartRate", "Error loading heart rates: ${e.message}")
                    view?.let {
                        Snackbar.make(it, "Lỗi tải nhịp tim: ${e.message}", Snackbar.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun updateTable(heartRates: List<HeartRateData>) {
        val table = tableLayout ?: return
        while (table.childCount > 1) {
            table.removeViewAt(1)
        }

        for ((index, heartRate) in heartRates.withIndex()) {
            val tableRow = TableRow(context).apply {
                layoutParams = TableLayout.LayoutParams(
                    TableLayout.LayoutParams.MATCH_PARENT,
                    TableLayout.LayoutParams.WRAP_CONTENT
                )
                setPadding(8, 8, 8, 8)
                setBackgroundColor(if (index % 2 == 0) 0xCCFFFFFF.toInt() else 0xCCF5F5F5.toInt())
            }

            val bpmTextView = TextView(context).apply {
                layoutParams = TableRow.LayoutParams(
                    0,
                    TableRow.LayoutParams.WRAP_CONTENT,
                    1f
                )
                text = heartRate.bpm.takeIf { it > 0 }?.toString() ?: "N/A"
                textSize = 16f
                setTextColor(0xFF000000.toInt())
                setTypeface(null, Typeface.BOLD)
                setPadding(8, 8, 8, 8)
                gravity = android.view.Gravity.CENTER
            }

            val statusTextView = TextView(context).apply {
                layoutParams = TableRow.LayoutParams(
                    0,
                    TableRow.LayoutParams.WRAP_CONTENT,
                    1f
                )
                text = heartRate.status.takeIf { it.isNotEmpty() } ?: "N/A"
                textSize = 16f
                setTextColor(0xFF000000.toInt())
                setTypeface(null, Typeface.BOLD)
                setPadding(8, 8, 8, 8)
                gravity = android.view.Gravity.CENTER
            }

            val suggestionTextView = TextView(context).apply {
                layoutParams = TableRow.LayoutParams(
                    0,
                    TableRow.LayoutParams.WRAP_CONTENT,
                    1f
                )
                text = heartRate.suggestion.takeIf { it.isNotEmpty() } ?: "N/A"
                textSize = 16f
                setTextColor(0xFF000000.toInt())
                setTypeface(null, Typeface.BOLD)
                setPadding(8, 8, 8, 8)
                gravity = android.view.Gravity.CENTER
            }

            val timestampTextView = TextView(context).apply {
                layoutParams = TableRow.LayoutParams(
                    0,
                    TableRow.LayoutParams.WRAP_CONTENT,
                    1f
                )
                text = try {
                    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(heartRate.timestamp))
                } catch (e: Exception) {
                    "N/A"
                }
                textSize = 16f
                setTextColor(0xFF000000.toInt())
                setTypeface(null, Typeface.BOLD)
                setPadding(8, 8, 8, 8)
                gravity = android.view.Gravity.CENTER
            }

            val durationTextView = TextView(context).apply {
                layoutParams = TableRow.LayoutParams(
                    0,
                    TableRow.LayoutParams.WRAP_CONTENT,
                    1f
                )
                text = heartRate.duration.takeIf { it > 0 }?.toString() ?: "N/A"
                textSize = 16f
                setTextColor(0xFF000000.toInt())
                setTypeface(null, Typeface.BOLD)
                setPadding(8, 8, 8, 8)
                gravity = android.view.Gravity.CENTER
            }

            tableRow.addView(bpmTextView)
            tableRow.addView(statusTextView)
            tableRow.addView(suggestionTextView)
            tableRow.addView(timestampTextView)
            tableRow.addView(durationTextView)
            table.addView(tableRow)
        }
    }
}