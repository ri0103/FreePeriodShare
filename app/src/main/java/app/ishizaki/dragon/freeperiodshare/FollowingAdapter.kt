package app.ishizaki.dragon.freeperiodshare

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FollowingAdapter(
    private var userList: List<ProfileActivity.User>,
    private val deleteFriend: (ProfileActivity.User) -> Unit
    ) : RecyclerView.Adapter<FollowingAdapter.UserViewHolder>() {

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userNameTextView: TextView = itemView.findViewById(R.id.userNameTextView)
        val userIdTextView: TextView = itemView.findViewById(R.id.userIdTextView)

        val deleteFriendButton: ImageButton = itemView.findViewById(R.id.delete_friend_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = userList[position]
        holder.userNameTextView.text = user.userName
        holder.userIdTextView.text = "@${user.userId}"

        holder.deleteFriendButton.setOnClickListener {
            deleteFriend(user)
        }
    }

    override fun getItemCount(): Int {
        return userList.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newUserList: List<ProfileActivity.User>) {
        userList = newUserList
        notifyDataSetChanged()
    }
}