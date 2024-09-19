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
import app.ishizaki.dragon.freeperiodshare.databinding.ActivitySetTimetableBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SetTimetableActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetTimetableBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private val rows = 6
    private val columns = 6
    private val gridState = Array(rows) { BooleanArray(columns) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetTimetableBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid

        val selectedBlue = Color.parseColor("#6884B7")
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

                gridState[row][column] = false


                cellButton.setOnClickListener {
                    val currentColor = (it.background as? ColorDrawable)?.color
                    if (currentColor == unselectedGray) {
                        cellButton.setBackgroundColor(selectedBlue)
                        gridState[row][column] = true
                    } else {
                        cellButton.setBackgroundColor(unselectedGray)
                        gridState[row][column] = false
                    }
                }
                // グリッドレイアウトに追加
                gridLayout.addView(cellButton)
            }
        }

        binding.nextButton.setOnClickListener {

                if (userId != null) {

                    val gridStateList = mutableListOf<Map<String, Any>>()

                    for (row in 0 until rows) {
                        for (column in 0 until columns) {
                            if (gridState[row][column]) {
                                gridStateList.add(mapOf("periodIndex" to row, "dayOfWeek" to column))
                            }
                        }
                    }
                    Log.d("時間割配列", gridStateList.toString())
                    val data = hashMapOf<String, Any>(
                        "uid" to userId,
                        "gridState" to gridStateList
                    )

                    db.collection("timetables").document(userId)
                        .set(data)
                        .addOnSuccessListener {

//                            val timetableStatus = hashMapOf<String, Any>(
//                                "timetableStatus" to true
//                            )

//                            db.collection("users")
//                                .document(userId)
//                                .update(timetableStatus)
//                                .addOnSuccessListener {
                            val intent = Intent(this, MainActivity::class.java)
                            startActivity(intent)
                            finish()
//                                }

                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "エラーが発生しました $e", Toast.LENGTH_SHORT).show()
                        }
                }
            }
        }
    }
