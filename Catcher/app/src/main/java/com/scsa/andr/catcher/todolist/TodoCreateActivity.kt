package com.scsa.andr.catcher.todolist

import android.os.Bundle
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast

import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.scsa.andr.catcher.R
import com.scsa.andr.catcher.common.TodoDBHelper
import com.scsa.andr.catcher.databinding.ActivityTodoCreateBinding
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar

class TodoCreateActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTodoCreateBinding

    private val TAG: String = "CATCHER_YY"
    private lateinit var todoDBHelper: TodoDBHelper

    private lateinit var etTitle: EditText
    private lateinit var etContent: EditText
    private lateinit var btnStartDate: Button
    private lateinit var btnEndDate: Button
    private lateinit var btnSave: Button

    private var selectedStartDateTime: String? = null
    private var selectedEndDateTime: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate called")
        enableEdgeToEdge()
        setContentView(R.layout.activity_todo_create)


        binding = ActivityTodoCreateBinding.inflate(layoutInflater)
        setContentView(binding.root)

        etTitle = findViewById(R.id.et_title)
        etContent = findViewById(R.id.et_content)
        btnStartDate = findViewById(R.id.btn_start_date)
        btnEndDate = findViewById(R.id.btn_end_date)
        btnSave = findViewById(R.id.btn_save)

        todoDBHelper = TodoDBHelper(this, "ToDoDatabase", null, 1)
        todoDBHelper.open()


        val todoId = intent.getLongExtra("TODO_ID", -1L)
        if (todoId != -1L) {
            val todo = todoDBHelper.selectById(todoId)
            if (todo != null) {
                etTitle.setText(todo.title)
                etContent.setText(todo.todoContent)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val startDate = todo.startDate?.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                    val endDate = todo.endDate?.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                    btnStartDate.text = startDate
                    btnEndDate.text = endDate
                    selectedStartDateTime = startDate // 초기화
                    selectedEndDateTime = endDate // 초기화
                }

                btnSave.text = "수정" // 버튼 텍스트 변경
            }
        }

        binding.btnStartDate.setOnClickListener {
            Log.d(TAG, "Start date button clicked")
            showDateTimePicker { selectedDateTime ->
                selectedStartDateTime = selectedDateTime
                binding.btnStartDate.text = selectedDateTime // Display selected start date-time
                Log.d(TAG, "Selected start date-time: $selectedDateTime")
            }
        }

        binding.btnEndDate.setOnClickListener {
            Log.d(TAG, "End date button clicked")
            showDateTimePicker { selectedDateTime ->
                selectedEndDateTime = selectedDateTime
                binding.btnEndDate.text = selectedDateTime // Display selected end date-time
                Log.d(TAG, "Selected end date-time: $selectedDateTime")
            }
        }

        binding.btnSave.setOnClickListener {
            Log.d(TAG, "Save button clicked")
            if (validateInputs()) {
                Log.d(TAG, "Inputs validated successfully")
                val title = etTitle.text.toString()
                val content = etContent.text.toString()
                val startDate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    LocalDateTime.parse(selectedStartDateTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                } else {
                    TODO("VERSION.SDK_INT < O")
                }
                val endDate = LocalDateTime.parse(selectedEndDateTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))

                val toDo = ToDoDto(
                    title = title,
                    todoContent = content,
                    startDate = startDate,
                    endDate = endDate,
                    status = 0 // Default status
                )

                if (todoId != -1L) {
                    todoDBHelper.update(toDo)
                    Toast.makeText(this, "일정이 수정되었습니다!", Toast.LENGTH_SHORT).show()
                } else {
                    todoDBHelper.insert(toDo)
                    Toast.makeText(this, "일정이 등록되었습니다!", Toast.LENGTH_SHORT).show()
                }

                // 브로드캐스트 전송
                val intent = Intent("com.scsa.andr.catcher.NEW_TODO")
                sendBroadcast(intent)


                finish()
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun showDateTimePicker(onDateTimeSelected: (String) -> Unit) {
        Log.d(TAG, "showDateTimePicker called")
        val calendar = Calendar.getInstance()

        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                Log.d(TAG, "Date selected: $year-$month-$dayOfMonth")
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                TimePickerDialog(
                    this,
                    { _, hourOfDay, minute ->
                        Log.d(TAG, "Time selected: $hourOfDay:$minute")
                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        calendar.set(Calendar.MINUTE, minute)

                        val selectedDateTime = String.format(
                            "%04d-%02d-%02d %02d:%02d",
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH) + 1,
                            calendar.get(Calendar.DAY_OF_MONTH),
                            calendar.get(Calendar.HOUR_OF_DAY),
                            calendar.get(Calendar.MINUTE)
                        )
                        Log.d(TAG, "Full date-time selected: $selectedDateTime")
                        onDateTimeSelected(selectedDateTime)
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true // 24-hour format
                ).show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun validateInputs(): Boolean {
        Log.d(TAG, "validateInputs called")
        val title = binding.etTitle.text.toString().trim()
        val content = binding.etContent.text.toString().trim()

        if (title.isEmpty()) {
            Log.d(TAG, "Title is empty")
            Toast.makeText(this, "제목을 입력해주세요.", Toast.LENGTH_SHORT).show()
            return false
        }

        if (content.isEmpty()) {
            Log.d(TAG, "Content is empty")
            Toast.makeText(this, "내용을 입력해주세요.", Toast.LENGTH_SHORT).show()
            return false
        }

        if (selectedStartDateTime == null) {
            Log.d(TAG, "Start date-time is not selected")
            Toast.makeText(this, "시작 일시를 선택해주세요.", Toast.LENGTH_SHORT).show()
            return false
        }

        if (selectedEndDateTime == null) {
            Log.d(TAG, "End date-time is not selected")
            Toast.makeText(this, "종료 일시를 선택해주세요.", Toast.LENGTH_SHORT).show()
            return false
        }

        Log.d(TAG, "All inputs are valid")
        return true
    }


}
