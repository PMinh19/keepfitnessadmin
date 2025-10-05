
package com.example.keepyfitnessadmin

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.example.keepyfitnessadmin.model.Exercise
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AddEditExerciseActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private var exerciseId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit_exercise)

        db = FirebaseFirestore.getInstance()

        val edtTitle = findViewById<EditText>(R.id.edtExerciseTitle)
        val edtCategory = findViewById<EditText>(R.id.edtExerciseCategory)
        val edtDescription = findViewById<EditText>(R.id.edtExerciseDescription)
        val btnSave = findViewById<Button>(R.id.btnSaveExercise)

        exerciseId = intent.getStringExtra("EXERCISE_ID")
        if (exerciseId != null) {
            edtTitle.setText(intent.getStringExtra("EXERCISE_TITLE"))
            edtCategory.setText(intent.getStringExtra("EXERCISE_CATEGORY"))
            edtDescription.setText(intent.getStringExtra("EXERCISE_DESCRIPTION"))
        }

        btnSave.setOnClickListener {
            val title = edtTitle.text.toString()
            val category = edtCategory.text.toString()
            val description = edtDescription.text.toString()

            if (title.isEmpty()) {
                Snackbar.make(it, "Vui lòng nhập tên bài tập", Snackbar.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val exercise = Exercise(title = title, id = 0, category = category, description = description)

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    if (exerciseId != null) {
                        db.collection("exercises").document(exerciseId!!).set(exercise).await()
                        runOnUiThread {
                            Snackbar.make(it, "Đã sửa bài tập", Snackbar.LENGTH_LONG).show()
                            finish()
                        }
                    } else {
                        db.collection("exercises").add(exercise).await()
                        runOnUiThread {
                            Snackbar.make(it, "Đã thêm bài tập", Snackbar.LENGTH_LONG).show()
                            finish()
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        Snackbar.make(it, "Lỗi lưu bài tập: ${e.message}", Snackbar.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}
