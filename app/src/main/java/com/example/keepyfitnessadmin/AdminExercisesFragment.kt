
package com.example.keepyfitnessadmin

import android.content.Intent
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
import com.example.keepyfitnessadmin.model.Exercise
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AdminExercisesFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var exerciseAdapter: ExerciseAdapter
    private lateinit var progressBar: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_admin_exercises, container, false)

        db = FirebaseFirestore.getInstance()
        recyclerView = view.findViewById(R.id.exerciseRecyclerView)
        progressBar = view.findViewById(R.id.progressBarExercises)
        recyclerView.layoutManager = LinearLayoutManager(context)

        exerciseAdapter = ExerciseAdapter(mutableListOf()) { exercise, exerciseId, action ->
            when (action) {
                "edit" -> {
                    val intent = Intent(activity, AddEditExerciseActivity::class.java).apply {
                        putExtra("EXERCISE_ID", exerciseId)
                        putExtra("EXERCISE_TITLE", exercise.title)
                        putExtra("EXERCISE_CATEGORY", exercise.category)
                        putExtra("EXERCISE_DESCRIPTION", exercise.description)
                    }
                    startActivity(intent)
                }
                "delete" -> {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            db.collection("exercises").document(exerciseId).delete().await()
                            activity?.runOnUiThread {
                                view.findViewById<View>(android.R.id.content)?.let {
                                    Snackbar.make(it, "Đã xóa bài tập: ${exercise.title}", Snackbar.LENGTH_LONG).show()
                                }
                                loadExercises()
                            }
                        } catch (e: Exception) {
                            activity?.runOnUiThread {
                                view.findViewById<View>(android.R.id.content)?.let {
                                    Snackbar.make(it, "Lỗi xóa bài tập: ${e.message}", Snackbar.LENGTH_LONG).show()
                                }
                                Log.e("AdminExercises", "Delete error: ${e.message}")
                            }
                        }
                    }
                }
            }
        }
        recyclerView.adapter = exerciseAdapter

        view.findViewById<Button>(R.id.btnAddExercise)?.setOnClickListener {
            val intent = Intent(activity, AddEditExerciseActivity::class.java)
            startActivity(intent)
        }

        view.findViewById<Button>(R.id.btnDeleteAllExercises)?.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val documents = db.collection("exercises").get().await()
                    val deletePromises = documents.documents.map { doc ->
                        db.collection("exercises").document(doc.id).delete()
                    }
                    deletePromises.forEach { it.await() }
                    activity?.runOnUiThread {
                        view.findViewById<View>(android.R.id.content)?.let {
                            Snackbar.make(it, "Đã xóa tất cả bài tập", Snackbar.LENGTH_LONG).show()
                        }
                        loadExercises()
                    }
                } catch (e: Exception) {
                    activity?.runOnUiThread {
                        view.findViewById<View>(android.R.id.content)?.let {
                            Snackbar.make(it, "Lỗi xóa tất cả bài tập: ${e.message}", Snackbar.LENGTH_LONG).show()
                        }
                        Log.e("AdminExercises", "Delete all error: ${e.message}")
                    }
                }
            }
        }

        loadExercises()

        return view
    }

    private fun loadExercises() {
        progressBar.visibility = View.VISIBLE
        Log.d("AdminExercises", "Starting query for exercises")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val documents = db.collection("exercises").get().await()
                val exerciseList = mutableListOf<ExerciseItem>()
                for (document in documents) {
                    try {
                        val exercise = document.toObject(Exercise::class.java) ?: Exercise()
                        val exerciseId = document.id
                        Log.d("AdminExercises", "Exercise: ${exercise.title}, Category: ${exercise.category}, ID: $exerciseId")
                        exerciseList.add(ExerciseItem(exercise, exerciseId))
                    } catch (e: Exception) {
                        Log.e("AdminExercises", "Error deserializing exercise document ${document.id}: ${e.message}")
                    }
                }
                activity?.runOnUiThread {
                    progressBar.visibility = View.GONE
                    Log.d("AdminExercises", "Loaded ${exerciseList.size} exercises")
                    if (exerciseList.isEmpty()) {
                        view?.findViewById<View>(android.R.id.content)?.let {
                            Snackbar.make(it, "Không có bài tập nào được tải", Snackbar.LENGTH_LONG).show()
                        }
                    }
                    exerciseAdapter.updateExercises(exerciseList)
                }
            } catch (e: Exception) {
                activity?.runOnUiThread {
                    progressBar.visibility = View.GONE
                    Log.e("AdminExercises", "Error loading exercises: ${e.message}")
                    view?.findViewById<View>(android.R.id.content)?.let {
                        Snackbar.make(it, "Lỗi tải bài tập: ${e.message}", Snackbar.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    data class ExerciseItem(val exercise: Exercise, val exerciseId: String)

    inner class ExerciseAdapter(
        private val exercises: MutableList<ExerciseItem>,
        private val onAction: (Exercise, String, String) -> Unit // action: "edit" or "delete"
    ) : RecyclerView.Adapter<ExerciseAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val exerciseNameText: TextView = itemView.findViewById(R.id.exerciseNameText)
            val exerciseInfoText: TextView = itemView.findViewById(R.id.exerciseInfoText)

            fun bind(exerciseItem: ExerciseItem) {
                exerciseNameText.text = exerciseItem.exercise.title
                exerciseInfoText.text = buildString {
                    append("Category: ${exerciseItem.exercise.category}")
                    append(", Description: ${exerciseItem.exercise.description}")
                }
                itemView.setOnClickListener {
                    onAction(exerciseItem.exercise, exerciseItem.exerciseId, "edit")
                }
                itemView.setOnLongClickListener {
                    onAction(exerciseItem.exercise, exerciseItem.exerciseId, "delete")
                    true
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_exercise_admin, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(exercises[position])
        }

        override fun getItemCount(): Int = exercises.size

        fun updateExercises(newExercises: List<ExerciseItem>) {
            exercises.clear()
            exercises.addAll(newExercises)
            notifyDataSetChanged()
        }
    }
}
