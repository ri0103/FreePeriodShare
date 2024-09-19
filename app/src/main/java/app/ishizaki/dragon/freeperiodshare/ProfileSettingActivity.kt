package app.ishizaki.dragon.freeperiodshare

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import app.ishizaki.dragon.freeperiodshare.databinding.ActivityProfileSettingBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileSettingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileSettingBinding
    private val db = FirebaseFirestore.getInstance()
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileSettingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid


        if (userId != null) {
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        val userName = document.getString("userName")
                        val userId = document.getString("userId")
                        val instagramId = document.getString("instagramId")

                        binding.usernameInputText.setText(userName)
                        binding.useridInputText.setText(userId)
                        binding.instagramidInputText.setText(instagramId)
                    }
                }
                .addOnFailureListener { e ->
                    Log.w("ProfileSetting", "Error getting document", e)
                }
        }

        binding.saveUserProfileButton.setOnClickListener {
            val updatedUsername = binding.usernameInputText.text.toString()
            val updatedUserid = binding.useridInputText.text.toString()
            val updatedInstagramid = binding.instagramidInputText.text.toString()

            if (userId != null) {
                val userData = hashMapOf(
                    "userName" to updatedUsername,
                    "userId" to updatedUserid,
                    "instagramId" to updatedInstagramid
                )

                val docRef = db.collection("users").document(userId)
                docRef
                    .update(userData as Map<String, Any>)
                    .addOnSuccessListener { document ->
                        docRef.get()
                            .addOnSuccessListener { document ->
                                if (document != null && document.exists()) {
                                    val timetableStatus = document.getBoolean("timetableStatus")
                                    if (timetableStatus == true) {
                                        val intent = Intent(this, MainActivity::class.java)
                                        startActivity(intent)
                                        finish()
                                    }else{
                                        val intent = Intent(this, SetTimetableActivity::class.java)
                                        startActivity(intent)
                                        finish()
                                    }
                                }
                            }
                        startActivity(intent)
                    }
                    .addOnFailureListener { e ->
                        Log.w("ProfileSetting", "Error updating document", e)
                    }
            }
        }


    }
}