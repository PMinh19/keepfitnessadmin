
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
    private lateinit var recyclerView: RecyclerView
    private lateinit var heartRateAdapter: HeartRateAdapter
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
        val view = inflater.inflate(R.layout.fragment_admin_heart_rate, container, false)

        db = FirebaseFirestore.getInstance()
        recyclerView = view.findViewById(R.id.heartRateRecyclerView)
        progressBar = view.findViewById(R.id.progressBarHeartRate)
        recyclerView.layoutManager = LinearLayoutManager(context)

        heartRateAdapter = HeartRateAdapter(mutableListOf()) { heartRate, userId, heartRateId ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    db.collection("users").document(userId).collection("healthMetrics").document(heartRateId)
                        .delete().await()
                    activity?.runOnUiThread {
                        view.findViewById<View>(android.R.id.content)?.let {
                            Snackbar.make(it, "Đã xóa nhịp tim", Snackbar.LENGTH_LONG).show()
                        }
                        loadHeartRates()
                    }
                } catch (e: Exception) {
                    activity?.runOnUiThread {
                        view.findViewById<View>(android.R.id.content)?.let {
                            Snackbar.make(it, "Lỗi xóa nhịp tim: ${e.message}", Snackbar.LENGTH_LONG).show()
                        }
                        Log.e("AdminHeartRate", "Delete error: ${e.message}")
                    }
                }
            }
        }
        recyclerView.adapter = heartRateAdapter

        view.findViewById<Button>(R.id.btnRefreshHeartRate)?.setOnClickListener {
            loadHeartRates()
        }

        loadHeartRates()

        return view
    }

    private fun loadHeartRates() {
        progressBar.visibility = View.VISIBLE
        Log.d("AdminHeartRate", "Starting query for heart rates, UserId: $userId")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val documents = if (userId.isNullOrEmpty()) {
                    db.collectionGroup("healthMetrics").get().await()
                } else {
                    db.collection("users").document(userId!!).collection("healthMetrics").get().await()
                }
                val heartRateList = mutableListOf<HeartRateItem>()
                for (document in documents) {
                    try {
                        val heartRate = document.toObject(HeartRateData::class.java) ?: HeartRateData()
                        val docUserId = document.reference.parent.parent?.id ?: "Unknown"
                        val heartRateId = document.id
                        Log.d("AdminHeartRate", "HeartRate: ${heartRate.bpm} bpm, Status: ${heartRate.status}, Suggestion: ${heartRate.suggestion}, Time: ${heartRate.timestamp}, Duration: ${heartRate.duration}, User: $docUserId, ID: $heartRateId")
                        heartRateList.add(HeartRateItem(heartRate, docUserId, heartRateId))
                    } catch (e: Exception) {
                        Log.e("AdminHeartRate", "Error deserializing heart rate document ${document.id}: ${e.message}")
                    }
                }
                activity?.runOnUiThread {
                    progressBar.visibility = View.GONE
                    Log.d("AdminHeartRate", "Loaded ${heartRateList.size} heart rates")
                    if (heartRateList.isEmpty()) {
                        view?.findViewById<View>(android.R.id.content)?.let {
                            Snackbar.make(it, "Không có nhịp tim nào được tải", Snackbar.LENGTH_LONG).show()
                        }
                    }
                    heartRateAdapter.updateHeartRates(heartRateList)
                }
            } catch (e: Exception) {
                activity?.runOnUiThread {
                    progressBar.visibility = View.GONE
                    Log.e("AdminHeartRate", "Error loading heart rates: ${e.message}")
                    view?.findViewById<View>(android.R.id.content)?.let {
                        Snackbar.make(it, "Lỗi tải nhịp tim: ${e.message}", Snackbar.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    data class HeartRateItem(val heartRate: HeartRateData, val userId: String, val heartRateId: String)

    inner class HeartRateAdapter(
        private val heartRates: MutableList<HeartRateItem>,
        private val onLongClick: (HeartRateData, String, String) -> Unit
    ) : RecyclerView.Adapter<HeartRateAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val heartRateText: TextView = itemView.findViewById(R.id.heartRateText)
            val userEmailText: TextView = itemView.findViewById(R.id.userEmailText)

            fun bind(heartRateItem: HeartRateItem) {
                heartRateText.text = "Heart Rate: ${heartRateItem.heartRate.bpm} bpm"
                userEmailText.text = buildString {
                    append("Email: $userEmail")
                    append(", Status: ${heartRateItem.heartRate.status}")
                    append(", Suggestion: ${heartRateItem.heartRate.suggestion}")
                    append(", Time: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(heartRateItem.heartRate.timestamp))}")
                    append(", Duration: ${heartRateItem.heartRate.duration} ms")
                }
                itemView.setOnLongClickListener {
                    onLongClick(heartRateItem.heartRate, heartRateItem.userId, heartRateItem.heartRateId)
                    true
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_heart_rate_admin, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(heartRates[position])
        }

        override fun getItemCount(): Int = heartRates.size

        fun updateHeartRates(newHeartRates: List<HeartRateItem>) {
            heartRates.clear()
            heartRates.addAll(newHeartRates)
            notifyDataSetChanged()
        }
    }
}
