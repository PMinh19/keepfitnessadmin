package com.example.keepyfitnessadmin

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.keepyfitnessadmin.model.User
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AdminUsersFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private var tableLayout: TableLayout? = null
    private var progressBar: ProgressBar? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_admin_users, container, false)

        db = FirebaseFirestore.getInstance()
        tableLayout = view.findViewById(R.id.userTable)
        progressBar = view.findViewById(R.id.progressBarUsers)

        if (tableLayout == null || progressBar == null) {
            Log.e("AdminUsers", "Failed to find userTable or progressBarUsers in layout")
            view?.let {
                Snackbar.make(it, "Lỗi giao diện, kiểm tra file fragment_admin_users.xml", Snackbar.LENGTH_LONG).show()
            }
            return view
        }

        loadUsers()

        return view
    }

    private fun loadUsers() {
        progressBar?.visibility = View.VISIBLE
        Log.d("AdminUsers", "Starting query for users")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val documents = db.collection("users").get().await()
                val userList = mutableListOf<User>()
                for (document in documents) {
                    try {
                        val user = document.toObject(User::class.java) ?: User()
                        user.id = document.id
                        Log.d("AdminUsers", "User: ${user.email}, Role: ${user.role}, ID: ${user.id}")
                        userList.add(user)
                    } catch (e: Exception) {
                        Log.e("AdminUsers", "Error deserializing user document ${document.id}: ${e.message}")
                    }
                }
                activity?.runOnUiThread {
                    progressBar?.visibility = View.GONE
                    Log.d("AdminUsers", "Loaded ${userList.size} users")
                    if (userList.isEmpty()) {
                        view?.let {
                            Snackbar.make(it, "Không có người dùng nào được tải", Snackbar.LENGTH_LONG).show()
                        }
                    }
                    updateTable(userList)
                }
            } catch (e: Exception) {
                activity?.runOnUiThread {
                    progressBar?.visibility = View.GONE
                    Log.e("AdminUsers", "Error loading users: ${e.message}")
                    view?.let {
                        Snackbar.make(it, "Lỗi tải người dùng: ${e.message}", Snackbar.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun updateTable(users: List<User>) {
        val table = tableLayout ?: return
        // Clear existing rows except the header
        while (table.childCount > 1) {
            table.removeViewAt(1)
        }

        // Add user rows dynamically
        for ((index, user) in users.withIndex()) {
            val tableRow = TableRow(context).apply {
                layoutParams = TableLayout.LayoutParams(
                    TableLayout.LayoutParams.MATCH_PARENT,
                    TableLayout.LayoutParams.WRAP_CONTENT
                )
                setPadding(8, 8, 8, 8)
                // Alternating semi-transparent backgrounds for contrast
                setBackgroundColor(if (index % 2 == 0) 0xCCFFFFFF.toInt() else 0xCCF5F5F5.toInt())
                setOnClickListener {
                    try {
                        val intent = Intent(activity, UserDetailsActivity::class.java).apply {
                            putExtra("USER_ID", user.id)
                            putExtra("USER_EMAIL", user.email)
                        }
                        startActivity(intent)
                    } catch (e: Exception) {
                        Log.e("AdminUsers", "Error starting UserDetailsActivity: ${e.message}")
                        view?.let {
                            Snackbar.make(it, "Lỗi khi mở chi tiết người dùng", Snackbar.LENGTH_LONG).show()
                        }
                    }
                }
            }

            val emailTextView = TextView(context).apply {
                layoutParams = TableRow.LayoutParams(
                    0,
                    TableRow.LayoutParams.WRAP_CONTENT,
                    1f
                )
                text = user.email.takeIf { it.isNotEmpty() } ?: "N/A"
                textSize = 16f
                setTextColor(0xFF000000.toInt())
                setTypeface(null, Typeface.BOLD)
                setPadding(8, 8, 8, 8)
                gravity = android.view.Gravity.CENTER
            }

            val roleTextView = TextView(context).apply {
                layoutParams = TableRow.LayoutParams(
                    0,
                    TableRow.LayoutParams.WRAP_CONTENT,
                    1f
                )
                text = user.role.takeIf { it.isNotEmpty() } ?: "N/A"
                textSize = 16f
                setTextColor(0xFF000000.toInt())
                setTypeface(null, Typeface.BOLD)
                setPadding(8, 8, 8, 8)
                gravity = android.view.Gravity.CENTER
            }

            tableRow.addView(emailTextView)
            tableRow.addView(roleTextView)
            table.addView(tableRow)
        }
    }
}