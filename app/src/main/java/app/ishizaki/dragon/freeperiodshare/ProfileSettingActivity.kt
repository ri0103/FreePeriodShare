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
import com.google.firebase.firestore.SetOptions

class ProfileSettingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileSettingBinding
    private val db = FirebaseFirestore.getInstance()
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileSettingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        val currentUid = auth.currentUser?.uid

        val isEdit = intent.getBooleanExtra("isEdit", false)

        if (currentUid != null && !isEdit) {
            db.collection("users").document(currentUid).get()
                .addOnSuccessListener { document ->
                    if (document.get("gridState") != null){
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else if (document.getString("userId") != null) {
                        val intent = Intent(this, SetTimetableActivity::class.java)
                        startActivity(intent)
                        finish()
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

            if (currentUid != null) {
                val userData = hashMapOf(
                    "userName" to updatedUsername,
                    "userId" to updatedUserid,
                    "instagramId" to updatedInstagramid
                )

                val docRef = db.collection("users").document(currentUid)

                docRef
                .set(userData as Map<String, Any>, SetOptions.merge())
                .addOnSuccessListener {
                    docRef.get().addOnSuccessListener { document ->
                        if (document.getString("userId") != null) {
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
                .addOnFailureListener { e ->
                    Log.w("ProfileSetting", "Error getting document", e)
                }

            }
        }


    }
}