package app.ishizaki.dragon.freeperiodshare

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import app.ishizaki.dragon.freeperiodshare.databinding.ActivityTitleBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions


class TitleActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTitleBinding
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTitleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        if (auth.currentUser != null) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
        auth = FirebaseAuth.getInstance()

        binding.titleContinueButton.setOnClickListener {
            signIn()
        }

    }

    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        launcher.launch(signInIntent)
    }

    // Googleサインインの結果を処理する
    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(Exception::class.java)
            firebaseAuthWithGoogle(account)
        } catch (e: Exception) {
            Log.w(TAG, "Google sign in failed", e)
        }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount?) {
        val credential = GoogleAuthProvider.getCredential(account?.idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.let {
                        val userData = hashMapOf(
                            "uid" to it.uid,
                            "email" to it.email,
                        )

                        db.collection("users").document(it.uid)
                            .set(userData, SetOptions.merge())
                            .addOnSuccessListener {

                                val intent = Intent(this, ProfileSettingActivity::class.java)
                                startActivity(intent)
                                finish()

                            }
                            .addOnFailureListener { e ->
                                Log.w(TAG, "Error adding user to Firestore", e)
                            }
                    }
                } else {
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                }
            }
    }
}