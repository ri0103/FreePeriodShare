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
                            loadOtherUserTimetables(currentUid)
                        }
                    }
                }
        }

        binding.openProfileActivityButton.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }


    }

    fun loadOtherUserTimetables(currentUid: String) {
        db.collection("users")
            .whereNotEqualTo("uid", currentUid)
            .get()
            .addOnSuccessListener { documents ->
                val otherUserGridState = Array(rows) { BooleanArray(columns) }

                for (document in documents) {
                    val gridStateList = document.get("gridState") as? List<Map<String, Long>>
                    Log.d("testtest", gridStateList.toString())

                    gridStateList?.let {
                        for (cell in it) {
                            val row = cell["periodIndex"]?.toInt() ?: 0
                            val column = cell["dayOfWeek"]?.toInt() ?: 0
                            otherUserGridState[row][column] = true
                        }
                    }
                }
                updateGridLayout(otherUserGridState, currentUid)
            }
            .addOnFailureListener { e ->
//            Toast.makeText(this, "データの取得中にエラーが発生しました", Toast.LENGTH_SHORT).show()
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
        db.collection("users")
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
                showUserDialog(userNames)
            }
            .addOnFailureListener{

            }
    }

    private fun showUserDialog(userNames: List<List<String>>) {
        val builder = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.dialog_user_list, null)
        val userListContainer = dialogView.findViewById<LinearLayout>(R.id.user_list_container)

        for (user in userNames){
            val userName = user[0]
            val userId = user[1]
            val instagramId = user[2]

            val userLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }

            val userNameTextView = TextView(this).apply {
                text = userName
                textSize = 18f
                setPadding(0, 0, 16, 0)
            }

            val openWebsiteButton = Button(this).apply {
                text = "インスタへ"
                setOnClickListener {
                    // ブラウザでウェブサイトを開く
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/${instagramId}/"))
                    startActivity(intent)
                }
            }

            userLayout.addView(userNameTextView)
            userLayout.addView(openWebsiteButton)

            userListContainer.addView(userLayout)
        }

        builder.setTitle("同じ空きコマの友達")
        builder.setView(dialogView)
        builder.setPositiveButton("閉じる") {dialog, _ ->
            dialog.dismiss()
        }

        val dialog = builder.create()
        dialog.show()
    }
}