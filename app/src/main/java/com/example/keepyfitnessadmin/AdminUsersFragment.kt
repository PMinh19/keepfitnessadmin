
package com.example.keepyfitnessadmin

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.keepyfitnessadmin.model.User
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AdminUsersFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var userAdapter: UserAdapter
    private lateinit var progressBar: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_admin_users, container, false)

        db = FirebaseFirestore.getInstance()
        recyclerView = view.findViewById(R.id.userRecyclerView)
        progressBar = view.findViewById(R.id.progressBarUsers)
        recyclerView.layoutManager = LinearLayoutManager(context)

        userAdapter = UserAdapter(mutableListOf()) { user ->
            // Chuyển sang UserDetailsActivity khi click vào user
            val intent = Intent(activity, UserDetailsActivity::class.java).apply {
                putExtra("USER_ID", user.id)
                putExtra("USER_EMAIL", user.email)
            }
            startActivity(intent)
        }
        recyclerView.adapter = userAdapter

        loadUsers()

        return view
    }

    private fun loadUsers() {
        progressBar.visibility = View.VISIBLE
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
                    progressBar.visibility = View.GONE
                    Log.d("AdminUsers", "Loaded ${userList.size} users")
                    if (userList.isEmpty()) {
                        activity?.findViewById<View>(android.R.id.content)?.let {
                            Snackbar.make(it, "Không có người dùng nào được tải", Snackbar.LENGTH_LONG).show()
                        }
                    }
                    userAdapter.updateUsers(userList)
                }
            } catch (e: Exception) {
                activity?.runOnUiThread {
                    progressBar.visibility = View.GONE
                    Log.e("AdminUsers", "Error loading users: ${e.message}")
                    activity?.findViewById<View>(android.R.id.content)?.let {
                        Snackbar.make(it, "Lỗi tải người dùng: ${e.message}", Snackbar.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    inner class UserAdapter(
        private val users: MutableList<User>,
        private val onClick: (User) -> Unit
    ) : RecyclerView.Adapter<UserAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val userEmailText: TextView = itemView.findViewById(R.id.userEmailText)
            val userRoleText: TextView = itemView.findViewById(R.id.userRoleText)

            fun bind(user: User) {
                userEmailText.text = user.email
                userRoleText.text = "${user.role}"
                itemView.setOnClickListener {
                    onClick(user)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_user_admin, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(users[position])
        }

        override fun getItemCount(): Int = users.size

        fun updateUsers(newUsers: List<User>) {
            users.clear()
            users.addAll(newUsers)
            notifyDataSetChanged()
        }
    }
}
