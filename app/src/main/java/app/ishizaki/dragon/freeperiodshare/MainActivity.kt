package app.ishizaki.dragon.freeperiodshare

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import app.ishizaki.dragon.freeperiodshare.databinding.ActivityMainBinding
import app.ishizaki.dragon.freeperiodshare.databinding.ActivitySetTimetableBinding
import app.ishizaki.dragon.freeperiodshare.databinding.MoveToInstagramBinding
import com.google.api.Distribution.BucketOptions.Linear
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private val rows = 6
    private val columns = 6
    private val gridState = Array(rows) { BooleanArray(columns) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        val currentUid = auth.currentUser?.uid

        val gridLayout = binding.gridLayout
        gridLayout.columnCount = columns

//        val daysOfWeek = listOf("月", "火", "水", "木", "金", "土")
//        val blankCell = TextView(this)
//        gridLayout.addView(blankCell)

//        for (day in daysOfWeek){
//            val dayTextView = TextView(this).apply {
//                text = day
//                textSize = 16f
//                setPadding(8, 8, 8, 8)
//            }
//            gridLayout.addView(dayTextView)
//        }

        for (row in 0 until rows) {
            for (column in 0 until columns){
                val cellButton = Button(this)
                val params = GridLayout.LayoutParams().apply {
                    width = 0
                    height = 200
//                    height = GridLayout.LayoutParams.WRAP_CONTENT
                    columnSpec = GridLayout.spec(column, 1f)
                    rowSpec = GridLayout.spec(row, 1f)
                    setMargins(8, 8, 8, 8)
                }
                cellButton.layoutParams = params
                cellButton.setBackgroundColor(getColor(R.color.unselected_gray))
                gridLayout.addView(cellButton)
            }
        }

        if (currentUid != null) {
            db.collection("users")
                .document(currentUid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()){
                        val gridStateList = document.get("gridState") as? List<Map<String, Long>>

                        if (gridStateList != null) {
                            // gridStateListに保存されたデータを使ってgridStateを更新
                            for (cell in gridStateList) {
                                val row = cell["periodIndex"]?.toInt() ?: 0
                                val column = cell["dayOfWeek"]?.toInt() ?: 0 //firebaseのデータを反映する
                                gridState[row][column] = true
                            }

                            // GridLayoutのボタンを更新
                            loadFriendsUserTimetables(currentUid)
                        }
                    }
                }
        }

        binding.openProfileActivityButton.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }


    }

    fun loadFriendsUserTimetables(currentUid: String) {


        db.collection("users").document(currentUid).collection("following")
            .addSnapshotListener { snapshot, e ->
                if (e != null){
                    Log.w("Firestore", "リスニング中にエラーが発生しました", e)
                    return@addSnapshotListener
                }
                if (snapshot != null && !snapshot.isEmpty){
                    val friendsUidList = mutableListOf<String>()
                    for (document in snapshot.documents){
                        val friendUid = document.getString("uid")
                        if (friendUid != null) {
                            friendsUidList.add(friendUid)
                        }
                    }

                    if (friendsUidList.isNotEmpty()){
                        db.collection("users")
//            .whereNotEqualTo("uid", currentUid)
                            .whereIn("uid", friendsUidList)
                            .get()
                            .addOnSuccessListener { documents ->
                                val friendsGridState = Array(rows) { BooleanArray(columns) }

                                for (document in documents) {
                                    val gridStateList = document.get("gridState") as? List<Map<String, Long>>
                                    Log.d("testtest", gridStateList.toString())

                                    gridStateList?.let {
                                        for (cell in it) {
                                            val row = cell["periodIndex"]?.toInt() ?: 0
                                            val column = cell["dayOfWeek"]?.toInt() ?: 0
                                            friendsGridState[row][column] = true
                                        }
                                    }
                                }
//                                Toast.makeText(this, "正常に処理中", Toast.LENGTH_SHORT).show()
                                updateGridLayout(friendsGridState, currentUid)

                            }
                            .addOnFailureListener { e ->
//                                Toast.makeText(this, "データの取得中にエラーが発生しました", Toast.LENGTH_SHORT).show()
                                updateGridLayout(Array(6) { BooleanArray(6) }, currentUid)
                            }
                    }else{
//                        Toast.makeText(this, "フォローしているユーザーがいません", Toast.LENGTH_SHORT).show()
                        updateGridLayout(Array(6) { BooleanArray(6) }, currentUid)
                    }
                }else{
//                    Toast.makeText(this, "フォローしているユーザーがいません", Toast.LENGTH_SHORT).show()
                    updateGridLayout(Array(6) { BooleanArray(6) }, currentUid)
                }
            }
    }

    private fun updateGridLayout(otherUserGridState: Array<BooleanArray>, currentUid: String) {

        val gridLayout = binding.gridLayout

        for (row in 0 until rows) {
            for (column in 0 until columns) {
                val cellButton = gridLayout.getChildAt(row * columns + column) as Button

                cellButton?.let {
                    when{
                        otherUserGridState[row][column] && gridState[row][column] -> {
                            it.setBackgroundColor(getColor(R.color.has_friends_red))

                            it.setOnClickListener(null)
                            it.setOnClickListener {
                                showUsersForCell(row, column, currentUid)
                            }
                        }
                        gridState[row][column] -> {
                            it.setBackgroundColor(getColor(R.color.selected_blue))
                        }
                        else -> {
                            it.setBackgroundColor(getColor(R.color.unselected_gray))
                        }
                    }
                }
            }
        }
    }

    private fun showUsersForCell(row: Int, column: Int, currentUid: String) {

        val friendsUidList = mutableListOf<String>()

        db.collection("users").document(currentUid).collection("following").get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val friendUid = document.getString("uid")
                    if (friendUid != null) {
                        friendsUidList.add(friendUid)
                    }
                }

                db.collection("users")
                    .whereIn("uid", friendsUidList)
                    .whereArrayContains("gridState", mapOf("periodIndex" to row, "dayOfWeek" to column))
                    .get()
                    .addOnSuccessListener { documents ->
                        val userNames = mutableListOf<List<String>>()
                        for (document in documents){
                            val uid = document.getString("uid")
                            if (uid != currentUid) {
                                val userName = document.getString("userName") ?: "ユーザー不明"
                                val userId = document.getString("userId") ?: "ユーザー不明"
                                val instagramId = document.getString("instagramId") ?: "ユーザー不明"

                                userNames.add(mutableListOf(userName, userId, instagramId))
                            }
                        }
                        Log.d("空きコマ共通配列", userNames.toString())
                        showUserDialog(userNames, row, column)
                    }
                    .addOnFailureListener{
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

        val dialog = builder.create()
        dialog.show()
    }
}