package com.example.xiaosu.ui.settings

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.xiaosu.R
import com.example.xiaosu.network.ParentResponse

/**
 * 家长账号适配器
 * 用于在RecyclerView中显示关联的家长账号列表
 */
class ParentAdapter : RecyclerView.Adapter<ParentAdapter.ParentViewHolder>() {
    
    private var parentList: List<ParentResponse> = emptyList()
    
    /**
     * 更新数据列表
     * @param newList 新的家长账号列表
     */
    fun updateData(newList: List<ParentResponse>) {
        parentList = newList
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_parent, parent, false)
        return ParentViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ParentViewHolder, position: Int) {
        val parent = parentList[position]
        holder.bind(parent)
    }
    
    override fun getItemCount(): Int = parentList.size
    
    /**
     * 家长账号ViewHolder
     */
    class ParentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val usernameTextView: TextView = itemView.findViewById(R.id.parentUsernameTextView)
        private val codeTextView: TextView = itemView.findViewById(R.id.parentCodeTextView)
        
        /**
         * 绑定数据到视图
         * @param parent 家长账号数据
         */
        fun bind(parent: ParentResponse) {
            usernameTextView.text = "家长账号：${parent.username}"
            codeTextView.text = "关联码：${parent.code}"
        }
    }
}