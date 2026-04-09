package com.example.xiaosuparent.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.xiaosuparent.R
import com.example.xiaosuparent.model.VideoFile
import java.text.DecimalFormat

class VideoAdapter(private val onItemClick: (VideoFile) -> Unit) : 
    RecyclerView.Adapter<VideoAdapter.VideoViewHolder>() {

    private var fileList: List<VideoFile> = listOf()

    fun updateFiles(newFiles: List<VideoFile>) {
        this.fileList = newFiles
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_video, parent, false)
        return VideoViewHolder(view)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val file = fileList[position]
        holder.bind(file)
    }

    override fun getItemCount(): Int = fileList.size

    inner class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val fileIconImageView: ImageView = itemView.findViewById(R.id.imageView_fileIcon)
        private val fileNameTextView: TextView = itemView.findViewById(R.id.textView_fileName)
        private val fileSizeTextView: TextView = itemView.findViewById(R.id.textView_fileSize)
        private val lastModifiedTextView: TextView = itemView.findViewById(R.id.textView_lastModified)

        fun bind(file: VideoFile) {
            fileNameTextView.text = file.fileName
            
            if (file.isDirectory) {
                // 设置文件夹图标
                fileIconImageView.setImageResource(R.drawable.ic_folder)
                fileSizeTextView.text = "文件夹"
            } else {
                // 设置文件图标（根据文件类型可以设置不同图标）
                if (file.fileType.startsWith("video/")) {
                    fileIconImageView.setImageResource(R.drawable.ic_video)
                } else {
                    fileIconImageView.setImageResource(R.drawable.ic_file)
                }
                fileSizeTextView.text = formatFileSize(file.fileSize)
            }
            
            lastModifiedTextView.text = file.lastModified

            itemView.setOnClickListener {
                onItemClick(file)
            }
        }

        private fun formatFileSize(size: Long): String {
            if (size <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
            return DecimalFormat("#,##0.#").format(size / Math.pow(1024.0, digitGroups.toDouble())) + " " + units[digitGroups]
        }
    }
}