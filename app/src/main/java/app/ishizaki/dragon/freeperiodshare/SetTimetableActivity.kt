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
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import app.ishizaki.dragon.freeperiodshare.databinding.ActivitySetTimetableBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

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
        val currentUid = auth.currentUser?.uid

        val isEdit = intent.getBooleanExtra("isEdit", false)

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

                cellButton.setBackgroundResource(R.drawable.timetable_grid_unselected)

                gridState[row][column] = false

                val unselectedDrawable = ContextCompat.getDrawable(this, R.drawable.timetable_grid_unselected)

                cellButton.setOnClickListener {
                    val currentDrawable = cellButton.background
                    if (currentDrawable.constantState == unselectedDrawable?.constantState) {
                        cellButton.setBackgroundResource(R.drawable.timetable_grid_selected)
                        gridState[row][column] = true
                    } else {
                        cellButton.setBackgroundResource(R.drawable.timetable_grid_unselected)
                        gridState[row][column] = false
                    }
                }

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

                            for (cell in gridStateList) {
                                val row = cell["periodIndex"]?.toInt() ?: 0
                                val column = cell["dayOfWeek"]?.toInt() ?: 0
                                gridState[row][column] = true
                            }
                            updateGridLayout()
                        }
                    }
                }
        }

        binding.nextButton.setOnClickListener {

                if (currentUid != null) {

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
                        "gridState" to gridStateList
                    )

                    db.collection("users").document(currentUid)
                        .set(data, SetOptions.merge())
                        .addOnSuccessListener {
                            if (isEdit){
                                finish()
                            }else{
                                val intent = Intent(this, MainActivity::class.java)
                                startActivity(intent)
                                finish()
                            }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "エラーが発生しました $e", Toast.LENGTH_SHORT).show()
                        }
                }
            }
        }

    private fun updateGridLayout() {
        val gridLayout = binding.gridLayout

        for (row in 0 until rows) {
            for (column in 0 until columns) {
                val cellButton = gridLayout.getChildAt(row * columns + column) as Button

                cellButton?.let {
                    when{
                        gridState[row][column] -> {
                            it.setBackgroundResource(R.drawable.timetable_grid_selected)
                        }
                        else -> {
                            it.setBackgroundResource(R.drawable.timetable_grid_unselected)
                        }
                    }
                }
            }
        }
    }

}

