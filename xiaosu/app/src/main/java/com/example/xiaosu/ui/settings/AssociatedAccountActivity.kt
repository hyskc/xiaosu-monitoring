package com.example.xiaosu.ui.settings

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.xiaosu.R
import com.example.xiaosu.network.ApiClient
import com.example.xiaosu.network.ApiResponse
import com.example.xiaosu.network.ParentResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AssociatedAccountActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var noDataLayout: LinearLayout
    private lateinit var loadingLayout: LinearLayout
    private lateinit var parentAdapter: ParentAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_associated_account)

        // 设置工具栏
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        // 初始化视图
        recyclerView = findViewById(R.id.recyclerView)
        noDataLayout = findViewById(R.id.noDataLayout)
        loadingLayout = findViewById(R.id.loadingLayout)

        // 设置RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        parentAdapter = ParentAdapter()
        recyclerView.adapter = parentAdapter

        // 设置工具栏返回按钮点击事件
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        // 加载关联账号数据
        loadAssociatedAccounts()
    }

    private fun loadAssociatedAccounts() {
        // 显示加载中布局
        loadingLayout.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        noDataLayout.visibility = View.GONE

        // 从SharedPreferences获取学生ID
        val sharedPreferences = getSharedPreferences("login_prefs", Context.MODE_PRIVATE)
        val studentId = sharedPreferences.getInt("user_id", -1)

        if (studentId == -1) {
            // 未找到学生ID，显示错误信息
            Toast.makeText(this, "未找到学生信息，请重新登录", Toast.LENGTH_SHORT).show()
            loadingLayout.visibility = View.GONE
            noDataLayout.visibility = View.VISIBLE
            return
        }

        // 调用API获取关联账号信息
        ApiClient.apiService.getAssociatedParents(studentId).enqueue(object : Callback<ApiResponse<List<ParentResponse>>> {
            override fun onResponse(call: Call<ApiResponse<List<ParentResponse>>>, response: Response<ApiResponse<List<ParentResponse>>>) {
                loadingLayout.visibility = View.GONE

                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse != null && apiResponse.success && apiResponse.data != null && apiResponse.data.isNotEmpty()) {
                        // 显示关联账号列表
                        parentAdapter.updateData(apiResponse.data)
                        recyclerView.visibility = View.VISIBLE
                    } else {
                        // 没有关联账号，显示无数据布局
                        noDataLayout.visibility = View.VISIBLE
                    }
                } else {
                    // API调用失败，显示错误信息
                    Toast.makeText(this@AssociatedAccountActivity, "获取关联账号失败，请稍后重试", Toast.LENGTH_SHORT).show()
                    noDataLayout.visibility = View.VISIBLE
                }
            }

            override fun onFailure(call: Call<ApiResponse<List<ParentResponse>>>, t: Throwable) {
                // 网络请求失败，显示错误信息
                loadingLayout.visibility = View.GONE
                noDataLayout.visibility = View.VISIBLE
                Toast.makeText(this@AssociatedAccountActivity, "网络请求失败：${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}