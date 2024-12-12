package com.scsa.andr.catcher.alarm

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class AlarmDBHelper(context: Context?, name: String?, factory: SQLiteDatabase.CursorFactory?, version: Int) :
    SQLiteOpenHelper(context, name, factory, version) {

    companion object {
        private const val TAG = "AlarmDBHelper"
        private const val TABLE_NAME = "alarm_list"
        private val COLUMNS = arrayOf("_id", "title", "hour", "minute", "second")
    }

    private lateinit var db: SQLiteDatabase

    override fun onCreate(db: SQLiteDatabase?) {
        val sql = """
            CREATE TABLE IF NOT EXISTS $TABLE_NAME (
                ${COLUMNS[0]} INTEGER PRIMARY KEY AUTOINCREMENT,
                ${COLUMNS[1]} TEXT NOT NULL,
                ${COLUMNS[2]} INTEGER NOT NULL,
                ${COLUMNS[3]} INTEGER NOT NULL,
                ${COLUMNS[4]} INTEGER NOT NULL
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

    fun insert(alarm: AlarmDto):Long {
        val values = ContentValues().apply {
            put(COLUMNS[1], alarm.title)
            put(COLUMNS[2], alarm.hour)
            put(COLUMNS[3], alarm.minute)
            put(COLUMNS[4], alarm.second)
        }

        db.beginTransaction()
        try {
            val result = db.insert(TABLE_NAME, null, values)
            if (result > 0) {
                db.setTransactionSuccessful()
                Log.d(TAG, "insert: 데이터 삽입 성공")
            }
            return result
        } finally {
            db.endTransaction()
        }
    }

    fun selectAll(): List<AlarmDto> {
        val alarmList = mutableListOf<AlarmDto>()
        val cursor: Cursor = db.rawQuery("SELECT * FROM $TABLE_NAME", null)

        cursor.use {
            while (it.moveToNext()) {
                val alarm = mapCursorToDto(it)
                alarmList.add(alarm)
            }
        }
        return alarmList
    }

    fun selectById(alarmId: Long): AlarmDto? {
        val cursor: Cursor = db.query(
            TABLE_NAME, COLUMNS, "${COLUMNS[0]} = ?", arrayOf(alarmId.toString()),
            null, null, null
        )

        cursor.use {
            if (it.moveToFirst()) {
                return mapCursorToDto(it)
            }
        }
        return null
    }

    fun update(alarm: AlarmDto) {
        val values = ContentValues().apply {
            put(COLUMNS[1], alarm.title)
            put(COLUMNS[2], alarm.hour)
            put(COLUMNS[3], alarm.minute)
            put(COLUMNS[4], alarm.second)
        }

        db.beginTransaction()
        try {
            val result = db.update(TABLE_NAME, values, "${COLUMNS[0]} = ?", arrayOf(alarm.id.toString()))
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

    private fun mapCursorToDto(cursor: Cursor): AlarmDto {
        val id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMNS[0]))
        val title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMNS[1]))
        val hour = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMNS[2]))
        val minute = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMNS[3]))
        val second = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMNS[4]))

        return AlarmDto(id, title, hour, minute, second)
    }
}
