
package com.example.keepyfitnessadmin

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
    private lateinit var recyclerView: RecyclerView
    private lateinit var workoutAdapter: WorkoutAdapter
    private lateinit var progressBar: ProgressBar
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
        recyclerView = view.findViewById(R.id.workoutRecyclerView)
        progressBar = view.findViewById(R.id.progressBarWorkouts)
        recyclerView.layoutManager = LinearLayoutManager(context)

        workoutAdapter = WorkoutAdapter(mutableListOf()) { workout, userId, workoutId ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    db.collection("users").document(userId).collection("workouts").document(workoutId)
                        .delete().await()
                    activity?.runOnUiThread {
                        view.findViewById<View>(android.R.id.content)?.let {
                            Snackbar.make(it, "Đã xóa workout: ${workout.exerciseName}", Snackbar.LENGTH_LONG).show()
                        }
                        loadWorkouts()
                    }
                } catch (e: Exception) {
                    activity?.runOnUiThread {
                        view.findViewById<View>(android.R.id.content)?.let {
                            Snackbar.make(it, "Lỗi xóa workout: ${e.message}", Snackbar.LENGTH_LONG).show()
                        }
                        Log.e("AdminWorkouts", "Delete error: ${e.message}")
                    }
                }
            }
        }
        recyclerView.adapter = workoutAdapter

        view.findViewById<Button>(R.id.btnDeleteAllWorkouts)?.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val documents = db.collectionGroup("workouts").get().await()
                    val deletePromises = documents.documents.map { doc ->
                        db.collection("users").document(doc.reference.parent.parent?.id!!)
                            .collection("workouts").document(doc.id).delete()
                    }
                    deletePromises.forEach { it.await() } // Thay awaitAll bằng vòng lặp
                    activity?.runOnUiThread {
                        view.findViewById<View>(android.R.id.content)?.let {
                            Snackbar.make(it, "Đã xóa tất cả workouts", Snackbar.LENGTH_LONG).show()
                        }
                        loadWorkouts()
                    }
                } catch (e: Exception) {
                    activity?.runOnUiThread {
                        view.findViewById<View>(android.R.id.content)?.let {
                            Snackbar.make(it, "Lỗi xóa tất cả workouts: ${e.message}", Snackbar.LENGTH_LONG).show()
                        }
                        Log.e("AdminWorkouts", "Delete all error: ${e.message}")
                    }
                }
            }
        }

        loadWorkouts()

        return view
    }

    private fun loadWorkouts() {
        progressBar.visibility = View.VISIBLE
        Log.d("AdminWorkouts", "Starting query for workouts, UserId: $userId")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val documents = if (userId.isNullOrEmpty()) {
                    db.collectionGroup("workouts").get().await()
                } else {
                    db.collection("users").document(userId!!).collection("workouts").get().await()
                }
                val workoutList = mutableListOf<WorkoutItem>()
                for (document in documents) {
                    try {
                        val workout = document.toObject(Workout::class.java) ?: Workout()
                        val docUserId = document.reference.parent.parent?.id ?: "Unknown"
                        val workoutId = document.id
                        Log.d("AdminWorkouts", "Workout: ${workout.exerciseName}, Duration: ${workout.duration}, Calories: ${workout.caloriesBurned}, User: $docUserId, ID: $workoutId")
                        workoutList.add(WorkoutItem(workout, docUserId, workoutId))
                    } catch (e: Exception) {
                        Log.e("AdminWorkouts", "Error deserializing workout document ${document.id}: ${e.message}")
                    }
                }
                activity?.runOnUiThread {
                    progressBar.visibility = View.GONE
                    Log.d("AdminWorkouts", "Loaded ${workoutList.size} workouts")
                    if (workoutList.isEmpty()) {
                        view?.findViewById<View>(android.R.id.content)?.let {
                            Snackbar.make(it, "Không có workout nào được tải", Snackbar.LENGTH_LONG).show()
                        }
                    }
                    workoutAdapter.updateWorkouts(workoutList)
                }
            } catch (e: Exception) {
                activity?.runOnUiThread {
                    progressBar.visibility = View.GONE
                    Log.e("AdminWorkouts", "Error loading workouts: ${e.message}")
                    view?.findViewById<View>(android.R.id.content)?.let {
                        Snackbar.make(it, "Lỗi tải workout: ${e.message}", Snackbar.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    data class WorkoutItem(val workout: Workout, val userId: String, val workoutId: String)

    inner class WorkoutAdapter(
        private val workouts: MutableList<WorkoutItem>,
        private val onLongClick: (Workout, String, String) -> Unit
    ) : RecyclerView.Adapter<WorkoutAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val workoutNameText: TextView = itemView.findViewById(R.id.workoutNameText)
            val userEmailText: TextView = itemView.findViewById(R.id.userEmailText)

            fun bind(workoutItem: WorkoutItem) {
                workoutNameText.text = workoutItem.workout.exerciseName
                userEmailText.text = buildString {
                    append("User ID: ${workoutItem.userId}")
                    append(", Duration: ${workoutItem.workout.duration} ms")
                    append(", Calories: ${workoutItem.workout.caloriesBurned}")
                    append(", Date: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(workoutItem.workout.date))}")
                }
                itemView.setOnLongClickListener {
                    onLongClick(workoutItem.workout, workoutItem.userId, workoutItem.workoutId)
                    true
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_workout_admin, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(workouts[position])
        }

        override fun getItemCount(): Int = workouts.size

        fun updateWorkouts(newWorkouts: List<WorkoutItem>) {
            workouts.clear()
            workouts.addAll(newWorkouts)
            notifyDataSetChanged()
        }
    }
}
