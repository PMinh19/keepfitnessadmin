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
import com.example.keepyfitnessadmin.model.Workout
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class AdminWorkoutsFragment : Fragment() {

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
        val view = inflater.inflate(R.layout.fragment_admin_workouts, container, false)
        db = FirebaseFirestore.getInstance()
        progressBar = view.findViewById(R.id.progressBarWorkouts)
        tableLayout = view.findViewById(R.id.workoutTable)
        btnReload = view.findViewById(R.id.btnReload)

        if (tableLayout == null || progressBar == null || btnReload == null) {
            Log.e("AdminWorkouts", "Không tìm thấy workoutTable, progressBarWorkouts hoặc btnReload trong giao diện")
            view?.let {
                Snackbar.make(it, "Lỗi giao diện, kiểm tra file fragment_admin_workouts.xml", Snackbar.LENGTH_LONG).show()
            }
            return view
        }

        btnReload?.setOnClickListener {
            loadWorkouts()
            Snackbar.make(it, "Đã tải lại trang", Snackbar.LENGTH_SHORT).show()
        }

        loadWorkouts()
        return view
    }

    private fun loadWorkouts() {
        progressBar?.visibility = View.VISIBLE
        Log.d("AdminWorkouts", "Starting query for workouts, UserId: $userId")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val documents = if (userId.isNullOrEmpty()) {
                    db.collectionGroup("workouts").get().await()
                } else {
                    db.collection("users").document(userId!!).collection("workouts").get().await()
                }
                val workoutList = mutableListOf<Workout>()
                for (document in documents) {
                    try {
                        val workout = document.toObject(Workout::class.java) ?: Workout()
                        workoutList.add(workout.copy(id = document.id))
                        Log.d("AdminWorkouts", "Bài tập: ${workout.exerciseName}, Thời gian: ${workout.duration}, Calo: ${workout.caloriesBurned}, Ngày: ${workout.date}, Số lần: ${workout.count}, Hoàn thành: ${workout.completed}, Mục tiêu: ${workout.targetCount}, ID bài tập: ${workout.exerciseId}")
                    } catch (e: Exception) {
                        Log.e("AdminWorkouts", "Error deserializing workout document ${document.id}: ${e.message}")
                    }
                }
                activity?.runOnUiThread {
                    progressBar?.visibility = View.GONE
                    Log.d("AdminWorkouts", "Loaded ${workoutList.size} workouts")
                    if (workoutList.isEmpty()) {
                        view?.let {
                            Snackbar.make(it, "Không có bài tập nào được tải", Snackbar.LENGTH_LONG).show()
                        }
                    }
                    updateTable(workoutList)
                }
            } catch (e: Exception) {
                activity?.runOnUiThread {
                    progressBar?.visibility = View.GONE
                    Log.e("AdminWorkouts", "Error loading workouts: ${e.message}")
                    view?.let {
                        Snackbar.make(it, "Lỗi tải bài tập: ${e.message}", Snackbar.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun updateTable(workouts: List<Workout>) {
        val table = tableLayout ?: return
        while (table.childCount > 1) {
            table.removeViewAt(1)
        }

        for ((index, workout) in workouts.withIndex()) {
            val tableRow = TableRow(context).apply {
                layoutParams = TableLayout.LayoutParams(
                    TableLayout.LayoutParams.MATCH_PARENT,
                    TableLayout.LayoutParams.WRAP_CONTENT
                )
                setPadding(8, 8, 8, 8)
                setBackgroundColor(if (index % 2 == 0) 0xCCFFFFFF.toInt() else 0xCCF5F5F5.toInt())
            }

            val nameTextView = TextView(context).apply {
                layoutParams = TableRow.LayoutParams(
                    0,
                    TableRow.LayoutParams.WRAP_CONTENT,
                    1f
                )
                text = workout.exerciseName.takeIf { it.isNotEmpty() } ?: "N/A"
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
                text = workout.duration.takeIf { it > 0 }?.toString() ?: "N/A"
                textSize = 16f
                setTextColor(0xFF000000.toInt())
                setTypeface(null, Typeface.BOLD)
                setPadding(8, 8, 8, 8)
                gravity = android.view.Gravity.CENTER
            }

            val caloriesTextView = TextView(context).apply {
                layoutParams = TableRow.LayoutParams(
                    0,
                    TableRow.LayoutParams.WRAP_CONTENT,
                    1f
                )
                text = workout.caloriesBurned.takeIf { it > 0 }?.toString() ?: "N/A"
                textSize = 16f
                setTextColor(0xFF000000.toInt())
                setTypeface(null, Typeface.BOLD)
                setPadding(8, 8, 8, 8)
                gravity = android.view.Gravity.CENTER
            }

            val dateTextView = TextView(context).apply {
                layoutParams = TableRow.LayoutParams(
                    0,
                    TableRow.LayoutParams.WRAP_CONTENT,
                    1f
                )
                text = try {
                    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(workout.date))
                } catch (e: Exception) {
                    "N/A"
                }
                textSize = 16f
                setTextColor(0xFF000000.toInt())
                setTypeface(null, Typeface.BOLD)
                setPadding(8, 8, 8, 8)
                gravity = android.view.Gravity.CENTER
            }

            val countTextView = TextView(context).apply {
                layoutParams = TableRow.LayoutParams(
                    0,
                    TableRow.LayoutParams.WRAP_CONTENT,
                    1f
                )
                text = workout.count.takeIf { it > 0 }?.toString() ?: "N/A"
                textSize = 16f
                setTextColor(0xFF000000.toInt())
                setTypeface(null, Typeface.BOLD)
                setPadding(8, 8, 8, 8)
                gravity = android.view.Gravity.CENTER
            }

            val completedTextView = TextView(context).apply {
                layoutParams = TableRow.LayoutParams(
                    0,
                    TableRow.LayoutParams.WRAP_CONTENT,
                    1f
                )
                text = if (workout.completed) "Có" else "Không"
                textSize = 16f
                setTextColor(0xFF000000.toInt())
                setTypeface(null, Typeface.BOLD)
                setPadding(8, 8, 8, 8)
                gravity = android.view.Gravity.CENTER
            }

            val targetCountTextView = TextView(context).apply {
                layoutParams = TableRow.LayoutParams(
                    0,
                    TableRow.LayoutParams.WRAP_CONTENT,
                    1f
                )
                text = workout.targetCount.takeIf { it > 0 }?.toString() ?: "N/A"
                textSize = 16f
                setTextColor(0xFF000000.toInt())
                setTypeface(null, Typeface.BOLD)
                setPadding(8, 8, 8, 8)
                gravity = android.view.Gravity.CENTER
            }

            tableRow.addView(nameTextView)
            tableRow.addView(durationTextView)
            tableRow.addView(caloriesTextView)
            tableRow.addView(dateTextView)
            tableRow.addView(countTextView)
            tableRow.addView(completedTextView)
            tableRow.addView(targetCountTextView)
            table.addView(tableRow)
        }
    }
}