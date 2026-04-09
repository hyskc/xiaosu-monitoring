package com.example.xiaosu.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.xiaosu.R
import com.example.xiaosu.model.Resource
import com.example.xiaosu.ui.adapter.ResourceAdapter

class ResourcesFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var resourceAdapter: ResourceAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_resources, container, false)
        recyclerView = view.findViewById(R.id.resourcesRecyclerView)
        setupRecyclerView()
        return view
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(context)
        resourceAdapter = ResourceAdapter(getSampleResources())
        recyclerView.adapter = resourceAdapter
    }

    private fun getSampleResources(): List<Resource> {
        // 示例数据，实际应用中应该从服务器获取
        return listOf(
            Resource(
                "1",
                "小学语文教学指南",
                "适合小学语文教师使用的教学指南，包含教学方法和案例分析",
                "2023-06-01",
                "https://example.com/resource1"
            ),
            Resource(
                "2",
                "小学数学教学资源包",
                "包含小学数学各年级教学课件、习题和教案",
                "2023-06-05",
                "https://example.com/resource2"
            ),
            Resource(
                "3",
                "小学英语教学视频",
                "小学英语教学示范课视频合集，适合英语教师参考",
                "2023-06-10",
                "https://example.com/resource3"
            ),
            Resource(
                "4",
                "教育心理学指南",
                "帮助教师了解学生心理特点，改进教学方法的指南",
                "2023-06-15",
                "https://example.com/resource4"
            ),
            Resource(
                "5",
                "班级管理技巧",
                "小学班级管理经验分享和技巧总结",
                "2023-06-20",
                "https://example.com/resource5"
            )
        )
    }

    companion object {
        fun newInstance() = ResourcesFragment()
    }
}