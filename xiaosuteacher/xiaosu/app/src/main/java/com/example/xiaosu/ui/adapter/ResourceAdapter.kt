package com.example.xiaosu.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.xiaosu.R
import com.example.xiaosu.model.Resource

class ResourceAdapter(private val resources: List<Resource>) : 
    RecyclerView.Adapter<ResourceAdapter.ResourceViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResourceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_resource, parent, false)
        return ResourceViewHolder(view)
    }

    override fun onBindViewHolder(holder: ResourceViewHolder, position: Int) {
        val resource = resources[position]
        holder.bind(resource)
    }

    override fun getItemCount(): Int = resources.size

    inner class ResourceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.resourceTitleTextView)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.resourceDescriptionTextView)
        private val dateTextView: TextView = itemView.findViewById(R.id.resourceDateTextView)
        private val downloadButton: Button = itemView.findViewById(R.id.resourceDownloadButton)

        fun bind(resource: Resource) {
            titleTextView.text = resource.title
            descriptionTextView.text = resource.description
            dateTextView.text = resource.date

            downloadButton.setOnClickListener {
                // TODO: 实现下载功能
                Toast.makeText(itemView.context, "开始下载: ${resource.title}", Toast.LENGTH_SHORT).show()
            }

            itemView.setOnClickListener {
                // TODO: 实现点击查看详情功能
                Toast.makeText(itemView.context, "查看详情: ${resource.title}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}