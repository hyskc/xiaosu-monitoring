package com.example.xiaosuparent.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.xiaosuparent.R
import com.example.xiaosuparent.adapters.AppUsageAdapter
import com.example.xiaosuparent.api.RetrofitClient
import com.example.xiaosuparent.model.ApiResponse
import com.example.xiaosuparent.model.AppUsageInfo
import com.example.xiaosuparent.model.Student
import com.example.xiaosuparent.utils.UserSession
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AppMonitorFragment : Fragment() {

    private lateinit var userSession: UserSession
    private lateinit var studentSpinner: Spinner
    private lateinit var timeRangeSpinner: Spinner
    private lateinit var recyclerViewApps: RecyclerView
    private lateinit var emptyTextView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var refreshButton: Button
    private lateinit var sortButton: Button
    private lateinit var categoryButton: Button
    private lateinit var horizontalScrollView: HorizontalScrollView
    private lateinit var chipGroupCategories: ChipGroup
    
    private var studentList = mutableListOf<Student>()
    private var appUsageAdapter: AppUsageAdapter? = null
    private var currentStudentId: Int = -1
    private var currentTimeRange: String = "today" // 默认查询今天的数据
    private var isDescendingOrder: Boolean = true // 默认降序排列（使用时间最长的在前）
    private var originalAppUsageList: List<AppUsageInfo> = listOf()
    private var selectedCategory: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // 加载布局
        return inflater.inflate(R.layout.fragment_app_monitor, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 初始化用户会话
        userSession = UserSession.getInstance(requireContext())

        // 初始化视图
        studentSpinner = view.findViewById(R.id.spinner_students)
        timeRangeSpinner = view.findViewById(R.id.spinner_time_range)
        recyclerViewApps = view.findViewById(R.id.recyclerView_apps)
        emptyTextView = view.findViewById(R.id.textView_empty)
        progressBar = view.findViewById(R.id.progressBar)
        refreshButton = view.findViewById(R.id.button_refresh)
        sortButton = view.findViewById(R.id.button_sort)
        categoryButton = view.findViewById(R.id.button_category)
        horizontalScrollView = view.findViewById(R.id.horizontalScrollView)
        chipGroupCategories = view.findViewById(R.id.chipGroup_categories)

        // 设置RecyclerView
        recyclerViewApps.layoutManager = LinearLayoutManager(requireContext())
        appUsageAdapter = AppUsageAdapter()
        recyclerViewApps.adapter = appUsageAdapter

        // 设置刷新按钮点击事件
        refreshButton.setOnClickListener {
            loadAppUsageData()
        }
        
        // 设置排序按钮点击事件
        sortButton.setOnClickListener {
            isDescendingOrder = !isDescendingOrder
            appUsageAdapter?.sortByUsageDuration(isDescendingOrder)
            sortButton.text = if (isDescendingOrder) getString(R.string.sort_by_duration) else getString(R.string.sort_by_duration_asc)
        }
        
        // 设置分类按钮点击事件
        categoryButton.setOnClickListener {
            if (horizontalScrollView.visibility == View.VISIBLE) {
                horizontalScrollView.visibility = View.GONE
            } else {
                if (originalAppUsageList.isNotEmpty()) {
                    showCategoryChips(originalAppUsageList)
                    horizontalScrollView.visibility = View.VISIBLE
                } else {
                    Toast.makeText(requireContext(), getString(R.string.no_categories_available), Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 设置时间范围选择器
        setupTimeRangeSpinner()

        // 检查用户是否已登录
        if (userSession.isLoggedIn()) {
            // 获取关联的学生列表
            loadAssociatedStudents()
        } else {
            // 显示未登录提示
            showEmptyState("请先登录以查看应用使用情况")
            studentSpinner.visibility = View.GONE
            timeRangeSpinner.visibility = View.GONE
            recyclerViewApps.visibility = View.GONE
            refreshButton.visibility = View.GONE
            sortButton.visibility = View.GONE
            categoryButton.visibility = View.GONE
            horizontalScrollView.visibility = View.GONE
        }

        // 设置学生选择监听器
        studentSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (studentList.isNotEmpty() && position < studentList.size) {
                    val selectedStudent = studentList[position]
                    currentStudentId = selectedStudent.id
                    loadAppUsageData()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // 不做任何操作
            }
        }
        
        // 设置时间范围选择监听器
        timeRangeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val timeRanges = resources.getStringArray(R.array.time_ranges)
                val timeRangeValues = resources.getStringArray(R.array.time_range_values)
                if (position < timeRangeValues.size) {
                    currentTimeRange = timeRangeValues[position]
                    loadAppUsageData()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // 不做任何操作
            }
        }
    }
    
    private fun setupTimeRangeSpinner() {
        val timeRanges = resources.getStringArray(R.array.time_ranges)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, timeRanges)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        timeRangeSpinner.adapter = adapter
    }

    private fun loadAssociatedStudents() {
        val parentId = userSession.getUserId()
        if (parentId <= 0) {
            showError("无法获取用户ID")
            return
        }

        val apiService = RetrofitClient.apiService
        apiService.getAssociatedStudents(parentId).enqueue(object : Callback<ApiResponse<List<Student>>> {
            override fun onResponse(
                call: Call<ApiResponse<List<Student>>>,
                response: Response<ApiResponse<List<Student>>>
            ) {
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse != null && apiResponse.code == 200) {
                        val students = apiResponse.data
                        if (students != null && students.isNotEmpty()) {
                            studentList.clear()
                            studentList.addAll(students)
                            setupStudentSpinner()
                            // 默认加载第一个学生的应用使用情况
                            if (studentList.isNotEmpty()) {
                                currentStudentId = studentList[0].id
                                loadAppUsageData()
                            }
                        } else {
                            showEmptyState("没有关联的学生")
                        }
                    } else {
                        showError(apiResponse?.message ?: "获取学生列表失败")
                    }
                } else {
                    showError("服务器响应错误: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<ApiResponse<List<Student>>>, t: Throwable) {
                showError("网络错误: ${t.message}")
            }
        })
    }

    private fun setupStudentSpinner() {
        if (studentList.isEmpty()) {
            studentSpinner.visibility = View.GONE
            return
        }

        studentSpinner.visibility = View.VISIBLE
        val studentNames = studentList.map { it.username }.toTypedArray()
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, studentNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        studentSpinner.adapter = adapter
    }

    private fun loadAppUsageData() {
        if (currentStudentId <= 0) {
            showError("请先选择学生")
            return
        }
        
        // 显示加载中状态
        showLoading(true)
        
        // 调用API获取应用使用情况数据
        val apiService = RetrofitClient.apiService
        apiService.getAppUsageInfo(currentStudentId, currentTimeRange).enqueue(object : Callback<ApiResponse<List<AppUsageInfo>>> {
            override fun onResponse(
                call: Call<ApiResponse<List<AppUsageInfo>>>,
                response: Response<ApiResponse<List<AppUsageInfo>>>
            ) {
                showLoading(false)
                
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse != null && apiResponse.code == 200) {
                        val appUsageList = apiResponse.data
                        if (appUsageList != null && appUsageList.isNotEmpty()) {
                            showAppUsageData(appUsageList)
                        } else {
                            showEmptyState("暂无应用使用数据")
                        }
                    } else {
                        showError(apiResponse?.message ?: "获取应用使用情况失败")
                    }
                } else {
                    showError("服务器响应错误: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<ApiResponse<List<AppUsageInfo>>>, t: Throwable) {
                showLoading(false)
                showError("网络错误: ${t.message}")
            }
        })
    }

    private fun showAppUsageData(appUsageList: List<AppUsageInfo>) {
        emptyTextView.visibility = View.GONE
        recyclerViewApps.visibility = View.VISIBLE
        sortButton.visibility = View.VISIBLE
        categoryButton.visibility = View.VISIBLE
        
        // 保存原始数据列表
        originalAppUsageList = appUsageList
        
        // 更新适配器数据
        appUsageAdapter?.updateData(appUsageList)
        appUsageAdapter?.sortByUsageDuration(isDescendingOrder)
    }
    
    private fun showCategoryChips(appUsageList: List<AppUsageInfo>) {
        // 清除现有的Chips
        chipGroupCategories.removeAllViews()
        
        // 添加"全部"选项
         val allChip = Chip(requireContext())
         allChip.text = getString(R.string.all_categories)
        allChip.isCheckable = true
        allChip.isChecked = selectedCategory == null
        allChip.setOnClickListener {
            selectedCategory = null
            filterAppsByCategory(null)
            updateChipSelection(allChip)
        }
        chipGroupCategories.addView(allChip)
        
        // 获取所有不重复的应用类别
        val categories = appUsageList
            .mapNotNull { it.category }
            .filter { it.isNotEmpty() }
            .toSet()
            .sorted()
        
        // 为每个类别创建一个Chip
        for (category in categories) {
            val chip = Chip(requireContext())
            chip.text = category
            chip.isCheckable = true
            chip.isChecked = category == selectedCategory
            chip.setOnClickListener {
                selectedCategory = category
                filterAppsByCategory(category)
                updateChipSelection(chip)
            }
            chipGroupCategories.addView(chip)
        }
    }
    
    private fun updateChipSelection(selectedChip: Chip) {
        // 更新Chip的选中状态
        for (i in 0 until chipGroupCategories.childCount) {
            val chip = chipGroupCategories.getChildAt(i) as Chip
            chip.isChecked = chip == selectedChip
        }
    }
    
    private fun filterAppsByCategory(category: String?) {
        if (category == null) {
            // 显示所有应用
            appUsageAdapter?.updateData(originalAppUsageList)
        } else {
            // 按类别筛选应用
            val filteredList = originalAppUsageList.filter { it.category == category }
            appUsageAdapter?.updateData(filteredList)
        }
        
        // 应用当前的排序
        appUsageAdapter?.sortByUsageDuration(isDescendingOrder)
    }

    private fun showEmptyState(message: String) {
        emptyTextView.visibility = View.VISIBLE
        emptyTextView.text = message
        recyclerViewApps.visibility = View.GONE
        sortButton.visibility = View.GONE
        categoryButton.visibility = View.GONE
        horizontalScrollView.visibility = View.GONE
    }

    private fun showError(errorMessage: String) {
        Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
        showEmptyState("加载失败: $errorMessage")
    }

    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        if (isLoading) {
            recyclerViewApps.visibility = View.GONE
            emptyTextView.visibility = View.GONE
            sortButton.visibility = View.GONE
            categoryButton.visibility = View.GONE
            horizontalScrollView.visibility = View.GONE
        }
    }
}