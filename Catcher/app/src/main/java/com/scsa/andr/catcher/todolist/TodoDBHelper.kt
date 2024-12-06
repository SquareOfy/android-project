package com.scsa.andr.catcher.common

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Build
import android.util.Log
import com.scsa.andr.catcher.todolist.ToDoDto
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class TodoDBHelper(context: Context?, name: String?, factory: SQLiteDatabase.CursorFactory?, version: Int) :
    SQLiteOpenHelper(context, name, factory, version) {

    companion object {
        private const val TAG = "DBHelper"
        private const val TABLE_NAME = "to_do_list"
        private val COLUMNS = arrayOf("_id", "title", "todo_content", "start_date", "end_date", "status")
        private const val DATE_PATTERN = "yyyy-MM-dd HH:mm" // 고정된 날짜/시간 형식
    }

    private lateinit var db: SQLiteDatabase
    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern(DATE_PATTERN)

    override fun onCreate(db: SQLiteDatabase?) {
        val sql = """
            CREATE TABLE IF NOT EXISTS $TABLE_NAME (
                ${COLUMNS[0]} INTEGER PRIMARY KEY AUTOINCREMENT,
                ${COLUMNS[1]} TEXT NOT NULL,
                ${COLUMNS[2]} TEXT NOT NULL,
                ${COLUMNS[3]} TEXT NOT NULL,
                ${COLUMNS[4]} TEXT NOT NULL,
                ${COLUMNS[5]} INTEGER NOT NULL DEFAULT 0
            );
        """
        db?.execSQL(sql)
        Log.d(TAG, "onCreate: 테이블 생성 완료")
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        val sql = "DROP TABLE IF EXISTS $TABLE_NAME"
        db?.execSQL(sql)
        onCreate(db)
        Log.d(TAG, "onUpgrade: 테이블 업그레이드 완료")
    }

    fun open() {
        db = writableDatabase
        Log.d(TAG, "open: 데이터베이스 준비 완료")
    }

    // LocalDateTime -> String 변환
    private fun dateToString(date: LocalDateTime?): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            date?.format(dateFormatter)
        } else {
            TODO("VERSION.SDK_INT < O")
        }
    }

    // String -> LocalDateTime 변환
    private fun stringToDate(dateStr: String?): LocalDateTime? {
        return dateStr?.let { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LocalDateTime.parse(it, dateFormatter)
        } else {
            TODO("VERSION.SDK_INT < O")
        }
        }
    }

    fun insert(toDo: ToDoDto) {
        val values = ContentValues().apply {
            put(COLUMNS[1], toDo.title)
            put(COLUMNS[2], toDo.todoContent)
            put(COLUMNS[3], dateToString(toDo.startDate))
            put(COLUMNS[4], dateToString(toDo.endDate))
            put(COLUMNS[5], toDo.status)
        }

        db.beginTransaction()
        try {
            val result = db.insert(TABLE_NAME, null, values)
            if (result > 0) {
                db.setTransactionSuccessful()
                Log.d(TAG, "insert: 데이터 삽입 성공")
            }
        } finally {
            db.endTransaction()
        }
    }

    fun selectAll(): List<ToDoDto> {
        val toDoList = mutableListOf<ToDoDto>()
        val cursor: Cursor = db.rawQuery("SELECT * FROM $TABLE_NAME", null)

        cursor.use {
            while (it.moveToNext()) {
                val toDo = mapCursorToDto(it)
                toDoList.add(toDo)
            }
        }
        return toDoList
    }

    fun selectByStatus(status: Int): List<ToDoDto> {
        val filteredList = mutableListOf<ToDoDto>()
        val cursor: Cursor = db.query(
            TABLE_NAME, COLUMNS, "${COLUMNS[5]} = ?", arrayOf(status.toString()),
            null, null, null
        )

        cursor.use {
            while (it.moveToNext()) {
                val toDo = mapCursorToDto(it)
                filteredList.add(toDo)
            }
        }
        return filteredList
    }

    fun selectById(todoId: Long): ToDoDto? {
        val cursor: Cursor = db.query(
            TABLE_NAME, COLUMNS, "${COLUMNS[0]} = ?", arrayOf(todoId.toString()),
            null, null, null
        )

        cursor.use {
            if (it.moveToFirst()) {
                return mapCursorToDto(it)
            }
        }
        return null
    }

    fun update(toDo: ToDoDto) {
        val values = ContentValues().apply {
            put(COLUMNS[1], toDo.title)
            put(COLUMNS[2], toDo.todoContent)
            put(COLUMNS[3], dateToString(toDo.startDate))
            put(COLUMNS[4], dateToString(toDo.endDate))
            put(COLUMNS[5], toDo.status)
        }

        db.beginTransaction()
        try {
            val result = db.update(TABLE_NAME, values, "${COLUMNS[0]} = ?", arrayOf(toDo.id.toString()))
            if (result > 0) {
                db.setTransactionSuccessful()
                Log.d(TAG, "update: 데이터 업데이트 성공")
            }
        } finally {
            db.endTransaction()
        }
    }

    fun delete(id: Long) {
        db.beginTransaction()
        try {
            val result = db.delete(TABLE_NAME, "${COLUMNS[0]} = ?", arrayOf(id.toString()))
            if (result > 0) {
                db.setTransactionSuccessful()
                Log.d(TAG, "delete: 데이터 삭제 성공")
            }
        } finally {
            db.endTransaction()
        }
    }

    private fun mapCursorToDto(cursor: Cursor): ToDoDto {
        val id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMNS[0]))
        val title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMNS[1]))
        val todoContent = cursor.getString(cursor.getColumnIndexOrThrow(COLUMNS[2]))
        val startDateStr = cursor.getString(cursor.getColumnIndexOrThrow(COLUMNS[3]))
        val endDateStr = cursor.getString(cursor.getColumnIndexOrThrow(COLUMNS[4]))
        val status = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMNS[5]))

        return ToDoDto(
            id = id,
            title = title,
            todoContent = todoContent,
            startDate = stringToDate(startDateStr),
            endDate = stringToDate(endDateStr),
            status = status
        )
    }
}
