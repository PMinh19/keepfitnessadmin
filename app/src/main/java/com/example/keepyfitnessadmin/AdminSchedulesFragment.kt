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
import com.example.keepyfitnessadmin.model.Schedule
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AdminSchedulesFragment : Fragment() {

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
        val view = inflater.inflate(R.layout.fragment_admin_schedules, container, false)
        db = FirebaseFirestore.getInstance()
        progressBar = view.findViewById(R.id.progressBarSchedules)
        tableLayout = view.findViewById(R.id.scheduleTable)
        btnReload = view.findViewById(R.id.btnReload)

        if (tableLayout == null || progressBar == null || btnReload == null) {
            Log.e("AdminSchedules", "Không tìm thấy scheduleTable, progressBarSchedules hoặc btnReload trong giao diện")
            view?.let {
                Snackbar.make(it, "Lỗi giao diện, kiểm tra file fragment_admin_schedules.xml", Snackbar.LENGTH_LONG).show()
            }
            return view
        }

        btnReload?.setOnClickListener {
            loadSchedules()
            Snackbar.make(it, "Đã tải lại trang", Snackbar.LENGTH_SHORT).show()
        }

        loadSchedules()
        return view
    }

    private fun loadSchedules() {
        progressBar?.visibility = View.VISIBLE
        Log.d("AdminSchedules", "Starting query for schedules, UserId: $userId")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val documents = if (userId.isNullOrEmpty()) {
                    db.collectionGroup("schedules").get().await()
                } else {
                    db.collection("users").document(userId!!).collection("schedules").get().await()
                }
                val scheduleList = mutableListOf<Schedule>()
                for (document in documents) {
                    try {
                        val schedule = document.toObject(Schedule::class.java) ?: Schedule()
                        scheduleList.add(schedule)
                        Log.d("AdminSchedules", "Lịch: ${schedule.exercise}, Thời gian: ${schedule.time}, Ngày: ${schedule.days}, Số lượng: ${schedule.quantity}, ID: ${document.id}")
                    } catch (e: Exception) {
                        Log.e("AdminSchedules", "Error deserializing schedule document ${document.id}: ${e.message}")
                    }
                }
                activity?.runOnUiThread {
                    progressBar?.visibility = View.GONE
                    Log.d("AdminSchedules", "Loaded ${scheduleList.size} schedules")
                    if (scheduleList.isEmpty()) {
                        view?.let {
                            Snackbar.make(it, "Không có lịch nào được tải", Snackbar.LENGTH_LONG).show()
                        }
                    }
                    updateTable(scheduleList)
                }
            } catch (e: Exception) {
                activity?.runOnUiThread {
                    progressBar?.visibility = View.GONE
                    Log.e("AdminSchedules", "Error loading schedules: ${e.message}")
                    view?.let {
                        Snackbar.make(it, "Lỗi tải lịch: ${e.message}", Snackbar.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun updateTable(schedules: List<Schedule>) {
        val table = tableLayout ?: return
        while (table.childCount > 1) {
            table.removeViewAt(1)
        }

        for ((index, schedule) in schedules.withIndex()) {
            val tableRow = TableRow(context).apply {
                layoutParams = TableLayout.LayoutParams(
                    TableLayout.LayoutParams.MATCH_PARENT,
                    TableLayout.LayoutParams.WRAP_CONTENT
                )
                setPadding(8, 8, 8, 8)
                setBackgroundColor(if (index % 2 == 0) 0xCCFFFFFF.toInt() else 0xCCF5F5F5.toInt())
            }

            val exerciseTextView = TextView(context).apply {
                layoutParams = TableRow.LayoutParams(
                    0,
                    TableRow.LayoutParams.WRAP_CONTENT,
                    1f
                )
                text = schedule.exercise.takeIf { it.isNotEmpty() } ?: "N/A"
                textSize = 16f
                setTextColor(0xFF000000.toInt())
                setTypeface(null, Typeface.BOLD)
                setPadding(8, 8, 8, 8)
                gravity = android.view.Gravity.CENTER
            }

            val timeTextView = TextView(context).apply {
                layoutParams = TableRow.LayoutParams(
                    0,
                    TableRow.LayoutParams.WRAP_CONTENT,
                    1f
                )
                text = schedule.time.takeIf { it.isNotEmpty() } ?: "N/A"
                textSize = 16f
                setTextColor(0xFF000000.toInt())
                setTypeface(null, Typeface.BOLD)
                setPadding(8, 8, 8, 8)
                gravity = android.view.Gravity.CENTER
            }

            val daysTextView = TextView(context).apply {
                layoutParams = TableRow.LayoutParams(
                    0,
                    TableRow.LayoutParams.WRAP_CONTENT,
                    1f
                )
                text = schedule.days.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: "N/A"
                textSize = 16f
                setTextColor(0xFF000000.toInt())
                setTypeface(null, Typeface.BOLD)
                setPadding(8, 8, 8, 8)
                gravity = android.view.Gravity.CENTER
            }

            val quantityTextView = TextView(context).apply {
                layoutParams = TableRow.LayoutParams(
                    0,
                    TableRow.LayoutParams.WRAP_CONTENT,
                    1f
                )
                text = schedule.quantity.takeIf { it > 0 }?.toString() ?: "N/A"
                textSize = 16f
                setTextColor(0xFF000000.toInt())
                setTypeface(null, Typeface.BOLD)
                setPadding(8, 8, 8, 8)
                gravity = android.view.Gravity.CENTER
            }

            tableRow.addView(exerciseTextView)
            tableRow.addView(timeTextView)
            tableRow.addView(daysTextView)
            tableRow.addView(quantityTextView)
            table.addView(tableRow)
        }
    }
}