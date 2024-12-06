package com.scsa.andr.catcher.todolist

import TodoAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.ContextMenu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.scsa.andr.catcher.MainActivity
import com.scsa.andr.catcher.R
import com.scsa.andr.catcher.common.TodoDBHelper

class TodoMainActivity : AppCompatActivity() {

    private lateinit var todoDBHelper: TodoDBHelper
    private lateinit var todoAdapter: TodoAdapter
    private lateinit var todoListView: ListView
    private lateinit var emptyPlaceholder: TextView
    private var todoList: List<ToDoDto> = mutableListOf()
    private val TAG = "CATCHER_TODO_MAIN_yy"
    private var selectedTodoId: Long = -1L //context Menu에서 선택한 항목의 id

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate called")
        enableEdgeToEdge()
        setContentView(R.layout.activity_todo_main)

        // Initialize DBHelper
        todoDBHelper = TodoDBHelper(this, "ToDoDatabase", null, 1)
        todoDBHelper.open()
        Log.d(TAG, "DBHelper initialized and database opened")

        // Initialize ListView and Adapter
        todoListView = findViewById(R.id.todo_list_view)
        emptyPlaceholder = findViewById(R.id.empty_placeholder)
        Log.d(TAG, "ListView initialized")

        //초기엔 시작 전만 띄우기
        filterTodoListByStatus(0)

        var allTextView: TextView = findViewById(R.id.all_view_textview)
        val beforeTextView: TextView = findViewById(R.id.before_textview)
        val ingTextView: TextView = findViewById(R.id.ing_textview)
        val finishTextView: TextView = findViewById(R.id.finish_textview)

        allTextView.setOnClickListener {
            Log.d(TAG, "Before filter clicked")
            loadTodoList() // 시작 전

        }


        // Set click listeners for filtering
        beforeTextView.setOnClickListener {
            Log.d(TAG, "Before filter clicked")
            filterTodoListByStatus(0) // 시작 전

        }

        ingTextView.setOnClickListener {
            Log.d(TAG, "In-progress filter clicked")
            filterTodoListByStatus(1) // 진행 중

        }

        finishTextView.setOnClickListener {
            Log.d(TAG, "Completed filter clicked")
            filterTodoListByStatus(2) // 완료

        }



        //FAB 버튼
        val fabAdd = findViewById<FloatingActionButton>(R.id.fab_add)
        fabAdd.setOnClickListener { view: View? ->
            val intent: Intent =
                Intent(
                    this@TodoMainActivity,
                    TodoCreateActivity::class.java
                )
            startActivity(intent)
            Log.d(TAG, "FAB clicked, navigating to TodoCreateActivity")
        }

        val fabHome = findViewById<FloatingActionButton>(R.id.fab_home)
        fabHome.setOnClickListener { view: View? ->
            val intent: Intent =
                Intent(
                    this@TodoMainActivity,
                    MainActivity::class.java
                )
            startActivity(intent)
            Log.d(TAG, "FAB clicked, navigating to MainActivity")
        }

        //리시버 등록
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(todoReceiver, IntentFilter("com.scsa.andr.catcher.NEW_TODO"),
                RECEIVER_NOT_EXPORTED
            )
        }

        //context menu 등록
        registerForContextMenu(todoListView)


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    // Context Menu 생성
    override fun onCreateContextMenu(menu: ContextMenu?, v: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        if (v?.id == R.id.todo_list_view) {
            menuInflater.inflate(R.menu.todo_context_menu, menu)
            val info = menuInfo as AdapterView.AdapterContextMenuInfo
            selectedTodoId = todoList[info.position].id // 선택된 항목의 ID 저장
        }
    }

    // Context Menu 항목 선택 처리
    override fun onContextItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_edit -> {
                editTodo()
                true
            }
            R.id.menu_delete -> {
                confirmDeleteTodo()
                true
            }
            else -> super.onContextItemSelected(item)
        }
    }

    private fun editTodo() {
        val intent = Intent(this, TodoCreateActivity::class.java)
        intent.putExtra("TODO_ID", selectedTodoId)
        startActivity(intent)
    }

    private fun confirmDeleteTodo() {
        AlertDialog.Builder(this)
            .setTitle("삭제 확인")
            .setMessage("이 일정을 삭제하시겠습니까?")
            .setPositiveButton("확인") { _, _ ->
                deleteTodo()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun deleteTodo() {
        todoDBHelper.delete(selectedTodoId)
        loadTodoList() // 삭제 후 목록 갱신
        Toast.makeText(this, "일정이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume called")
        filterTodoListByStatus(0)
//        loadTodoList() // 수정: Main 화면으로 돌아올 때 리스트 갱신
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy called, unregistering BroadcastReceiver")
        unregisterReceiver(todoReceiver)
    }

    private fun loadTodoList() {
        Log.d(TAG, "loadTodoList called")
        todoList = todoDBHelper.selectAll()
        Log.d(TAG, "Fetched ${todoList.size} items from database")
        todoAdapter = TodoAdapter(this, todoList)
        todoListView.adapter = todoAdapter
        todoAdapter.notifyDataSetChanged()
        Log.d(TAG, "ListView adapter set with all items")

        // 추가: 리스트가 비어 있을 때 Placeholder 표시
        if (todoList.isEmpty()) {
            Log.d(TAG, "Empty todoList")
            emptyPlaceholder.visibility = View.VISIBLE
            todoListView.visibility = View.GONE
        } else {
            emptyPlaceholder.visibility = View.GONE
            todoListView.visibility = View.VISIBLE
        }
    }

    private fun filterTodoListByStatus(status: Int) {
        Log.d(TAG, "filterTodoListByStatus called with status: $status")
        todoList = todoDBHelper.selectByStatus(status)
        Log.d(TAG, "Fetched ${todoList.size} items with status: $status")

        todoAdapter = TodoAdapter(this, todoList)
        todoListView.adapter = todoAdapter
        Log.d(TAG, "ListView adapter set with filtered items")
        todoAdapter.notifyDataSetChanged()
        // 추가: 리스트가 비어 있을 때 Placeholder 표시
        if (todoList.isEmpty()) {
            Log.d(TAG, "Empty todoList : " + todoList.size)
            emptyPlaceholder.visibility = View.VISIBLE
            todoListView.visibility = View.GONE
        } else {
            emptyPlaceholder.visibility = View.GONE
            todoListView.visibility = View.VISIBLE
        }
    }

    private val todoReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG, "Broadcast received for new ToDo")
            // 새로운 데이터를 DB에서 다시 로드
            filterTodoListByStatus(1)
            todoAdapter.notifyDataSetChanged()
        }
    }

}
