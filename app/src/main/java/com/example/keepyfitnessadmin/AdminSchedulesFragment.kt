
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
import com.example.keepyfitnessadmin.model.Schedule
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AdminSchedulesFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var scheduleAdapter: ScheduleAdapter
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
        val view = inflater.inflate(R.layout.fragment_admin_schedules, container, false)

        db = FirebaseFirestore.getInstance()
        recyclerView = view.findViewById(R.id.scheduleRecyclerView)
        progressBar = view.findViewById(R.id.progressBarSchedules)
        recyclerView.layoutManager = LinearLayoutManager(context)

        scheduleAdapter = ScheduleAdapter(mutableListOf()) { schedule, userId, scheduleId ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    db.collection("users").document(userId).collection("schedules").document(scheduleId)
                        .delete().await()
                    activity?.runOnUiThread {
                        view.findViewById<View>(android.R.id.content)?.let {
                            Snackbar.make(it, "Đã xóa lịch: ${schedule.exercise}", Snackbar.LENGTH_LONG).show()
                        }
                        loadSchedules()
                    }
                } catch (e: Exception) {
                    activity?.runOnUiThread {
                        view.findViewById<View>(android.R.id.content)?.let {
                            Snackbar.make(it, "Lỗi xóa lịch: ${e.message}", Snackbar.LENGTH_LONG).show()
                        }
                        Log.e("AdminSchedules", "Delete error: ${e.message}")
                    }
                }
            }
        }
        recyclerView.adapter = scheduleAdapter

        view.findViewById<Button>(R.id.btnRefreshSchedules)?.setOnClickListener {
            loadSchedules()
        }

        loadSchedules()

        return view
    }

    private fun loadSchedules() {
        progressBar.visibility = View.VISIBLE
        Log.d("AdminSchedules", "Starting query for schedules, UserId: $userId")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val documents = if (userId.isNullOrEmpty()) {
                    db.collectionGroup("schedules").get().await()
                } else {
                    db.collection("users").document(userId!!).collection("schedules").get().await()
                }
                val scheduleList = mutableListOf<ScheduleItem>()
                for (document in documents) {
                    try {
                        val schedule = document.toObject(Schedule::class.java) ?: Schedule()
                        val docUserId = document.reference.parent.parent?.id ?: "Unknown"
                        val scheduleId = document.id
                        Log.d("AdminSchedules", "Schedule: ${schedule.exercise}, Time: ${schedule.time}, Days: ${schedule.days}, Quantity: ${schedule.quantity}, User: $docUserId, ID: $scheduleId")
                        scheduleList.add(ScheduleItem(schedule, docUserId, scheduleId))
                    } catch (e: Exception) {
                        Log.e("AdminSchedules", "Error deserializing schedule document ${document.id}: ${e.message}")
                    }
                }
                activity?.runOnUiThread {
                    progressBar.visibility = View.GONE
                    Log.d("AdminSchedules", "Loaded ${scheduleList.size} schedules")
                    if (scheduleList.isEmpty()) {
                        view?.findViewById<View>(android.R.id.content)?.let {
                            Snackbar.make(it, "Không có lịch nào được tải", Snackbar.LENGTH_LONG).show()
                        }
                    }
                    scheduleAdapter.updateSchedules(scheduleList)
                }
            } catch (e: Exception) {
                activity?.runOnUiThread {
                    progressBar.visibility = View.GONE
                    Log.e("AdminSchedules", "Error loading schedules: ${e.message}")
                    view?.findViewById<View>(android.R.id.content)?.let {
                        Snackbar.make(it, "Lỗi tải lịch: ${e.message}", Snackbar.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    data class ScheduleItem(val schedule: Schedule, val userId: String, val scheduleId: String)

    inner class ScheduleAdapter(
        private val schedules: MutableList<ScheduleItem>,
        private val onLongClick: (Schedule, String, String) -> Unit
    ) : RecyclerView.Adapter<ScheduleAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val scheduleNameText: TextView = itemView.findViewById(R.id.scheduleNameText)
            val userEmailText: TextView = itemView.findViewById(R.id.userEmailText)

            fun bind(scheduleItem: ScheduleItem) {
                scheduleNameText.text = scheduleItem.schedule.exercise
                userEmailText.text = buildString {
                    append("Email: $userEmail")
                    append(", Time: ${scheduleItem.schedule.time}")
                    append(", Days: ${scheduleItem.schedule.days.joinToString()}")
                    append(", Quantity: ${scheduleItem.schedule.quantity}")
                }
                itemView.setOnLongClickListener {
                    onLongClick(scheduleItem.schedule, scheduleItem.userId, scheduleItem.scheduleId)
                    true
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_schedule_admin, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(schedules[position])
        }

        override fun getItemCount(): Int = schedules.size

        fun updateSchedules(newSchedules: List<ScheduleItem>) {
            schedules.clear()
            schedules.addAll(newSchedules)
            notifyDataSetChanged()
        }
    }
}
