package com.scsa.andr.catcher

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.ui.AppBarConfiguration
import com.scsa.andr.catcher.alarm.AlarmMainActivity
import com.scsa.andr.catcher.databinding.ActivityMainBinding
import com.scsa.andr.catcher.game.GameMainActivity
import com.scsa.andr.catcher.news.NewsMainActivity
import com.scsa.andr.catcher.todolist.TodoMainActivity

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        // View Binding 초기화
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // TO DO 버튼 클릭 이벤트
        binding.btnTodo.setOnClickListener {
            val intent = Intent(this, TodoMainActivity::class.java)
            startActivity(intent)
        }

        // ALARM 버튼 클릭 이벤트
        binding.btnAlarm.setOnClickListener {
            val intent = Intent(this, AlarmMainActivity::class.java)
            startActivity(intent)
        }

        // NEWS 버튼 클릭 이벤트
        binding.btnNews.setOnClickListener {
            val intent = Intent(this, NewsMainActivity::class.java)
            startActivity(intent)
        }

        // CATCH 버튼 클릭 이벤트
        binding.btnCatch.setOnClickListener {
            val intent = Intent(this, GameMainActivity::class.java)
            startActivity(intent)
        }
    }

}