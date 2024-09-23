package app.ishizaki.dragon.freeperiodshare

import android.content.Intent
import android.icu.text.CaseMap.Title
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.GridLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import app.ishizaki.dragon.freeperiodshare.databinding.ActivityMainBinding
import app.ishizaki.dragon.freeperiodshare.databinding.ActivityProfileBinding
import app.ishizaki.dragon.freeperiodshare.databinding.ActivityProfileSettingBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private val db = FirebaseFirestore.getInstance()
    private lateinit var auth: FirebaseAuth
    private val rows = 6
    private val columns = 6
    private val gridState = Array(rows) { BooleanArray(columns) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        val currentUid = auth.currentUser?.uid

        if (currentUid != null) {
            db.collection("users").document(currentUid).get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        val userName = document.getString("userName")
                        val userId = document.getString("userId")

                        binding.usernameTextView.text = userName
                        binding.useridTextView.text = "@${userId}"


                        binding.editTimetableButton.setOnClickListener {
                            val intent = Intent(this, SetTimetableActivity::class.java)
                            startActivity(intent)
                        }

                        binding.editProfileButton.setOnClickListener{
                            val intent = Intent(this, ProfileSettingActivity::class.java)
                            intent.putExtra("isEdit", true)
                            startActivity(intent)
                            finish()
                        }

                        binding.signoutButton.setOnClickListener {
                            auth.signOut()

                            val intent = Intent(this, TitleActivity::class.java)
                            startActivity(intent)
                            finish()
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.w("ProfileSetting", "Error getting document", e)
                }
        }

        val gridLayout = binding.gridLayout
        gridLayout.columnCount = columns

        for (row in 0 until rows) {
            for (column in 0 until columns){
                val cellButton = Button(this)
                val params = GridLayout.LayoutParams().apply {
                    width = 0
                    height = 80
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

                            for (cell in gridStateList) {
                                val row = cell["periodIndex"]?.toInt() ?: 0
                                val column = cell["dayOfWeek"]?.toInt() ?: 0 //firebaseのデータを反映する
                                gridState[row][column] = true
                            }

                            updateGridLayout()
                        }
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
}