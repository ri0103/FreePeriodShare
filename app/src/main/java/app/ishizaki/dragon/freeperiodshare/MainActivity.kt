package app.ishizaki.dragon.freeperiodshare

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Window
import android.widget.Button
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import app.ishizaki.dragon.freeperiodshare.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private val rows = 6
    private val columns = 6
    private val gridState = Array(rows) { BooleanArray(columns) }
    private val followingUidList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        val currentUid = auth.currentUser?.uid

        val gridLayout = binding.gridLayout
        gridLayout.columnCount = columns

        for (row in 0 until rows) {
            for (column in 0 until columns){
                val cellButton = Button(this)
                val params = GridLayout.LayoutParams().apply {
                    width = 0
                    height = 160
                    columnSpec = GridLayout.spec(column, 1f)
                    rowSpec = GridLayout.spec(row, 1f)
                    setMargins(8, 8, 8, 8)
                }
                cellButton.layoutParams = params
//                cellButton.setBackgroundResource(R.drawable.timetable_grid_unselected)
                gridLayout.addView(cellButton)
            }
        }

        if (currentUid != null) {
            db.collection("users")
                .addSnapshotListener { snapshot, e ->
                    db.collection("users")
                        .document(currentUid)
                        .get()
                        .addOnSuccessListener { document ->
                            if (document != null && document.exists()){
                                val gridStateList = document.get("gridState") as? List<Map<String, Long>>


                                if (gridStateList != null) {

                                    //一度初期化
                                    for (row in 0 until rows){
                                        for(column in 0 until columns){
                                            gridState[row][column] = false
                                        }

                                    }

                                    //それから描画
                                    for (cell in gridStateList) {
                                        val row = cell["periodIndex"]?.toInt() ?: 0
                                        val column = cell["dayOfWeek"]?.toInt() ?: 0 //firebaseのデータを反映する
                                        gridState[row][column] = true
                                    }
//                                    Toast.makeText(this, "test", Toast.LENGTH_SHORT).show()
                                    loadFriendsTimetables(currentUid)
                                }
                            }
                        }
                }

            binding.openProfileActivityButton.setOnClickListener {
                val intent = Intent(this, ProfileActivity::class.java)
                startActivity(intent)
            }
        }
    }

    fun loadFriendsTimetables(currentUid: String) {
        db.collection("users").document(currentUid).collection("following").get()
            .addOnSuccessListener { documents ->

                followingUidList.clear()
                for (document in documents){
                    val friendUid = document.getString("uid")
                    if (friendUid != null) {
                        followingUidList.add(friendUid)
                    }
                }

                if (followingUidList.isNotEmpty()){
                    db.collection("users")
                        .whereIn("uid", followingUidList)
                        .get()
                        .addOnSuccessListener { documentsFollowing ->
                            val followingGridState = Array(rows) { BooleanArray(columns) }
                            for (document in documentsFollowing) {
                                val gridStateList = document.get("gridState") as? List<Map<String, Long>>
                                gridStateList?.let {
                                    for (cell in it) {
                                        val row = cell["periodIndex"]?.toInt() ?: 0
                                        val column = cell["dayOfWeek"]?.toInt() ?: 0
                                        followingGridState[row][column] = true
                                    }
                                }
                            }
                            updateGridLayout(followingGridState, currentUid)
                        }
                        .addOnFailureListener { e ->
                            updateGridLayout(Array(6) { BooleanArray(6) }, currentUid)
                        }
                }else{
                    updateGridLayout(Array(6) { BooleanArray(6) }, currentUid)
                }

            }

    }


    private fun updateGridLayout(followingGridState: Array<BooleanArray>, currentUid: String) {

        val gridLayout = binding.gridLayout
        for (row in 0 until rows) {
            for (column in 0 until columns) {
                val cellButton = gridLayout.getChildAt(row * columns + column) as Button
                cellButton?.let {
                    when{
                        followingGridState[row][column] && gridState[row][column] -> {
                            it.setBackgroundResource(R.drawable.timetable_grid_has_friend)
                            it.setOnClickListener {
                                showUsersForCell(row, column, currentUid)
                            }
                        }
                        gridState[row][column] -> {
                            it.setBackgroundResource(R.drawable.timetable_grid_selected)
                            it.setOnClickListener(null)
                        }
                        else -> {
                            it.setBackgroundResource(R.drawable.timetable_grid_unselected)
                            it.setOnClickListener(null)
                        }
                    }
                }
            }
        }
    }

    private fun showUsersForCell(row: Int, column: Int, currentUid: String) {

        db.collection("users").document(currentUid).collection("following").get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val friendUid = document.getString("uid")
                    if (friendUid != null) {
                        followingUidList.add(friendUid)
                    }
                }

                db.collection("users")
                    .whereIn("uid", followingUidList)
                    .whereArrayContains("gridState", mapOf("periodIndex" to row, "dayOfWeek" to column))
                    .get()
                    .addOnSuccessListener { documents ->
                        val userNames = mutableListOf<List<String>>()
                        for (document in documents){
                            val uid = document.getString("uid")
                            if (uid != currentUid) {
                                val userName = document.getString("userName") ?: "ユーザー不明"
                                val userId = document.getString("userId") ?: "ユーザー不明"
                                val instagramId = document.getString("instagramId") ?: ""

                                userNames.add(mutableListOf(userName, userId, instagramId))
                            }
                        }
                        showUserDialog(userNames, row, column)
                    }
            }
    }

    private fun showUserDialog(userNames: List<List<String>>, row: Int, column: Int) {
        val builder = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.dialog_user_list, null)
        val userListContainer = dialogView.findViewById<LinearLayout>(R.id.user_list_container)

        val daysOfWeek = listOf("月", "火", "水", "木", "金", "土")

        for (user in userNames){
            val userName = user[0]
            val userId = user[1]
            val instagramId = user[2]

            val userLayout = layoutInflater.inflate(R.layout.move_to_instagram, userListContainer, false) as LinearLayout
            val userNameTextView = userLayout.findViewById<TextView>(R.id.username_text_view)
            val userIdTextView = userLayout.findViewById<TextView>(R.id.userid_text_view)
            val moveToInstagramButton = userLayout.findViewById<Button>(R.id.move_to_instagram_button)

            userNameTextView.text = userName
            userIdTextView.text = "@" + userId
            moveToInstagramButton.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/${instagramId}/"))
              startActivity(intent)
            }
            userListContainer.addView(userLayout)
        }

        builder.setTitle("${daysOfWeek[column]}曜日 ${row + 1}限の空きコマ")
        builder.setView(dialogView)
        builder.setPositiveButton("閉じる") {dialog, _ ->
            dialog.dismiss()
        }

        val dialog = builder.setView(dialogView).create()
        dialog.window?.setBackgroundDrawableResource(R.drawable.rounded_dialog_background)

        dialog.show()
    }
}