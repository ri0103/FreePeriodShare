package app.ishizaki.dragon.freeperiodshare

import android.content.Intent
import android.icu.text.CaseMap.Title
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import app.ishizaki.dragon.freeperiodshare.databinding.ActivityProfileBinding
import app.ishizaki.dragon.freeperiodshare.databinding.ActivityProfileSettingBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileBinding
    private val db = FirebaseFirestore.getInstance()
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid

        if (userId != null) {
            db.collection("users").document(userId).get()
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

    }
}