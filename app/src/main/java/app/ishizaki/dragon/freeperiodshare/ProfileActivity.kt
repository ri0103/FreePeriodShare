package app.ishizaki.dragon.freeperiodshare

import android.content.Intent
import android.icu.text.CaseMap.Title
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.GridLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.ishizaki.dragon.freeperiodshare.databinding.ActivityMainBinding
import app.ishizaki.dragon.freeperiodshare.databinding.ActivityProfileBinding
import app.ishizaki.dragon.freeperiodshare.databinding.ActivityProfileSettingBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
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
                        val instagramId = document.getString("instagramId")

                        binding.usernameTextView.text = userName
                        binding.useridTextView.text = "@${userId}"
                        binding.instagramidTextView.text = "@${instagramId}"



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

            val followingUidList = mutableListOf<String>()

            db.collection("users").document(currentUid).collection("following").get()
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                        val friendUid = document.getString("uid")
                        if (friendUid != null) {
                            followingUidList.add(friendUid)
                        }
                    }

                    if (followingUidList.isNotEmpty()) {
                        loadFollowingUsers(followingUidList, currentUid)
                    } else {
//                        Toast.makeText(this, "フォローユーザーがいません", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Log.w("Firestore", "フォローユーザーの取得中にエラーが発生しました", e)
                }
        }

        val gridLayout = binding.gridLayout
        gridLayout.columnCount = columns

        for (row in 0 until rows) {
            for (column in 0 until columns){
                val cellButton = Button(this)
                val params = GridLayout.LayoutParams().apply {
                    width = 0
                    height = 40
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

        binding.addFriendButton.setOnClickListener {

            if (currentUid != null) {
                val friendUserId = binding.friendUsernameInputText.text.toString()

                if (friendUserId.isNotEmpty()){
                    db.collection("users")
                        .whereEqualTo("userId", friendUserId)
                        .get()
                        .addOnSuccessListener { documents ->
                            if (!documents.isEmpty) {
                                val userDocument = documents.first()
                                val friendUid = userDocument.getString("uid").toString()

                                val currentUserRef = db.collection("users").document(currentUid)
                                val friendUserRef = db.collection("users").document(friendUid)

                                currentUserRef.collection("following").document(friendUid)
                                    .set(mapOf("uid" to friendUid))
                                    .addOnSuccessListener { followingDocuments ->


                                        currentUserRef.collection("following").get()
                                            .addOnSuccessListener { followingDocuments ->
                                                val followingUidList = mutableListOf<String>()

                                                for (document in followingDocuments) {
                                                    followingUidList.add(document.id)
                                                }

                                                Log.d("Firestore", "Following list: $followingUidList")

                                                // 必要に応じて、このリストを使って表示を更新する
                                                loadFollowingUsers(followingUidList, currentUid)

                                                currentUserRef.collection("following").get()
                                                    .addOnSuccessListener { followingDocuments ->
                                                        val followingUidList = mutableListOf<String>()

                                                        for (document in followingDocuments) {
                                                            followingUidList.add(document.id) // 各ドキュメントのID（友達のUID）をリストに追加
                                                        }

                                                        // followingUidListが正しく取得できたことを確認する
                                                        Log.d("Firestore", "Following list: $followingUidList")

                                                        // 必要に応じて、このリストを使って表示を更新する
                                                        loadFollowingUsers(followingUidList, currentUid)
                                                    }

                                                friendUserRef.collection("followers").document(currentUid)
                                                    .set(mapOf("uid" to currentUid))
                                                    .addOnSuccessListener {
                                                        Toast.makeText(this, "フォローに成功しました", Toast.LENGTH_SHORT).show()



                                                    }
                                                    .addOnFailureListener {
                                                        Toast.makeText(this, "フォロワーの追加に失敗しました", Toast.LENGTH_SHORT).show()
                                                    }


                                            }



                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(this, "フォロワーの追加に失敗しました", Toast.LENGTH_SHORT).show()
                                    }

                            } else {
                                Toast.makeText(this, "ユーザーが見つかりませんでした", Toast.LENGTH_SHORT).show()
                            }
                        }


                }

            }

        }

    }

    data class User(
        val uid: String = "",
        val userName: String = "",
        val userId: String = ""
    )

    fun loadFollowingUsers(followingUidList: List<String>, currentUid: String) {
        val followingUsersList = mutableListOf<User>()

        db.collection("users")
            .whereIn("uid", followingUidList.take(10))
            .addSnapshotListener { snapshot, e ->
            if (e != null){
                Log.w("Firestore", "リスニング中にエラーが発生しました", e)
                return@addSnapshotListener
            }
            if (snapshot != null && !snapshot.isEmpty){
                followingUsersList.clear()
                for (document in snapshot.documents) {
                    val uid = document.getString("uid") ?: ""
                    val userName = document.getString("userName") ?: ""
                    val userId = document.getString("userId") ?: ""
                    followingUsersList.add(User(uid, userName, userId))
                }
                showFollowingUsers(followingUsersList, currentUid)

            }
        }

    }

    fun showFollowingUsers(userList: List<User>, currentUid: String) {
        val recyclerView = findViewById<RecyclerView>(R.id.followingRecyclerView)
        val adapter = recyclerView.adapter as? FollowingAdapter
//        recyclerView.layoutManager = LinearLayoutManager(this)
//        recyclerView.adapter = FollowingAdapter(userList)
        if (adapter == null){
            recyclerView.layoutManager = LinearLayoutManager(this)
            recyclerView.adapter = FollowingAdapter(userList) { friend ->
                deleteFriend(currentUid, friend)
            }
        }else{
            adapter.updateData(userList)
        }
    }

    fun deleteFriend(currentUid: String, friend: User) {
        val recyclerView = binding.followingRecyclerView
        val adapter = recyclerView.adapter as? FollowingAdapter

        val currentUserRef = db.collection("users").document(currentUid)
        val friendUserRef = db.collection("users").document(friend.uid)

        currentUserRef.collection("following").document(friend.uid)
            .delete()
            .addOnSuccessListener {
                friendUserRef.collection("followers").document(currentUid)
                    .delete()
                    .addOnSuccessListener {

                        currentUserRef.collection("following").get()
                            .addOnSuccessListener { followingDocuments ->
                                if (!followingDocuments.isEmpty){
                                    val followingUidList = mutableListOf<String>()
                                    for (document in followingDocuments) {
                                        followingUidList.add(document.id)
                                    }

                                    loadFollowingUsers(followingUidList, currentUid)
                                    Toast.makeText(this, "${friend.userName}を削除しました", Toast.LENGTH_SHORT).show()

                                }

                            }

                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "フォロワーの削除に失敗しました", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "フォローリストからの削除に失敗しました", Toast.LENGTH_SHORT).show()
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