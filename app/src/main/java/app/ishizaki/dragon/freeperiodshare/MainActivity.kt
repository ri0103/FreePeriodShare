package app.ishizaki.dragon.freeperiodshare

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.GridLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import app.ishizaki.dragon.freeperiodshare.databinding.ActivityMainBinding
import app.ishizaki.dragon.freeperiodshare.databinding.ActivitySetTimetableBinding
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
        val userId = auth.currentUser?.uid

//        val selectedBlue = Color.parseColor("#6884B7")
        val unselectedGray = Color.parseColor("#A2A2A2")
        val gridLayout = binding.gridLayout
        gridLayout.columnCount = columns

        for (row in 0 until rows) {
            for (column in 0 until columns){
                val cellButton = Button(this)
                val params = GridLayout.LayoutParams().apply {
                    width = 0
                    height = GridLayout.LayoutParams.WRAP_CONTENT
                    columnSpec = GridLayout.spec(column, 1f) // 重みを1fに設定
                    rowSpec = GridLayout.spec(row, 1f)
                    setMargins(8, 8, 8, 8)
                }
                cellButton.layoutParams = params
                cellButton.setBackgroundColor(unselectedGray)
                gridLayout.addView(cellButton)
            }
        }

        if (userId != null) {
            db.collection("timetables")
                .document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()){
                        val gridStateList = document.get("gridState") as? List<Map<String, Long>>

                        if (gridStateList != null) {
                            // gridStateListに保存されたデータを使ってgridStateを更新
                            for (cell in gridStateList) {
                                val row = cell["periodIndex"]?.toInt() ?: 0
                                val column = cell["dayOfWeek"]?.toInt() ?: 0
                                gridState[row][column] = true // Firestoreのデータに基づいて状態を設定
                            }

                            // GridLayoutのボタンを更新
                            loadOtherUserTimetables(userId)
                        }
                    }
                }
        }

        binding.openProfileActivityButton.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }


    }

    private fun loadOtherUserTimetables(userId: String) {
        db.collection("timetables")
            .whereNotEqualTo("uid", userId)
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
                updateGridLayout(otherUserGridState)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "データ取得中にエラーが発生しました", Toast.LENGTH_SHORT).show()
            }
    }



    private fun updateGridLayout(otherUserGridState: Array<BooleanArray>) {

        val selectedBlue = Color.parseColor("#6884B7")
        val unselectedGray = Color.parseColor("#A2A2A2")
        val hasFriendPeriodRed = Color.parseColor("#D26D6B")


        val gridLayout = binding.gridLayout

        for (row in 0 until rows) {
            for (column in 0 until columns) {
                val cellButton = gridLayout.getChildAt(row * columns + column) as Button
//                Log.d("otheruser", otherUserGridState[row][column].toString())
                cellButton?.let {
                    when{
                        otherUserGridState[row][column] && gridState[row][column] -> {
                            it.setBackgroundColor(hasFriendPeriodRed)
                        }
                        gridState[row][column] -> {
                            it.setBackgroundColor(selectedBlue)
                        }
                        else -> {
                            it.setBackgroundColor(unselectedGray)
                        }
                    }
                }
            }
        }
    }
}