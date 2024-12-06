package com.scsa.andr.catcher.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.marginTop
import com.scsa.andr.catcher.R
import com.scsa.andr.catcher.databinding.ActivityAlarmMainBinding

class AlarmMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlarmMainBinding


    private var timer: CountDownTimer? = null

    private var mediaPlayer: MediaPlayer? = null
    private lateinit var alarmManager: AlarmManager
    private lateinit var pendingIntent: PendingIntent


    private var isAlarmRunning = false
    private var isPaused = false
    private var remainingTimeInMillis: Long = 0L
    private var initialTimeInMillis: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAlarmMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNumberPickers()
        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        binding.startButton.setOnClickListener {
            when{
                timer == null && !isAlarmRunning -> startTimer()
                isPaused -> resumeTimer()
                isAlarmRunning -> stopAlarm()
                else -> pauseTimer()
            }
        }


        binding.cancelButton.setOnClickListener {
            cancelTimer()
        }



        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun requestOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
            Toast.makeText(this, "권한을 허용해 주세요.", Toast.LENGTH_SHORT).show()
        } else {
            navigateToAlarmActivity()
        }
    }

    private fun navigateToAlarmActivity() {
        val intent = Intent(this, AlarmMainActivity::class.java)
        startActivity(intent)
    }



    private fun setupNumberPickers() {
        binding.hourPicker.apply {
            minValue = 0
            maxValue = 23
            setFormatter { String.format("%02d", it) }
        }

        binding.minutePicker.apply {
            minValue = 0
            maxValue = 59
            setFormatter { String.format("%02d", it) }
        }

        binding.secondPicker.apply {
            minValue = 0
            maxValue = 59
            setFormatter { String.format("%02d", it) }
        }
    }


    private fun startTimer() {
        // 시간 계산
        val hours = binding.hourPicker.value
        val minutes = binding.minutePicker.value
        val seconds = binding.secondPicker.value
        initialTimeInMillis = (hours * 3600 + minutes * 60 + seconds) * 1000L

        if (initialTimeInMillis == 0L) {
            Toast.makeText(this, "시간을 설정하세요.", Toast.LENGTH_SHORT).show()
            return
        }

        remainingTimeInMillis = initialTimeInMillis
        toggleTimerViews(showCountdown = true)

        timer = object : CountDownTimer(initialTimeInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                remainingTimeInMillis = millisUntilFinished
                updateNumberPickers(millisUntilFinished)
            }

            override fun onFinish() {
                startAlarm()
                Toast.makeText(this@AlarmMainActivity, "타이머 종료", Toast.LENGTH_SHORT).show()
            }
        }.start()

        binding.startButton.text = "일시정지"
        isPaused = false
    }


    private fun pauseTimer() {
        timer?.cancel()
        binding.startButton.text = "재시작"
        isPaused = true
    }

    private fun resumeTimer() {
        timer = object : CountDownTimer(remainingTimeInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                remainingTimeInMillis = millisUntilFinished
                updateNumberPickers(millisUntilFinished)
            }

            override fun onFinish() {
                Toast.makeText(this@AlarmMainActivity, "타이머 종료", Toast.LENGTH_SHORT).show()
                resetTimer()
            }
        }.start()

        binding.startButton.text = "일시정지"
        isPaused = false
    }

    private fun startAlarm() {
        isAlarmRunning = true
        binding.startButton.text = "종료"

        mediaPlayer = MediaPlayer.create(this, R.raw.sound1) // 알람음 리소스 설정
        mediaPlayer?.start()
    }

    private fun stopAlarm() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null

        resetTimer()
    }
    private fun cancelTimer() {
        timer?.cancel()
        resetTimer()
    }

    private fun resetTimer() {
        timer = null
        isPaused = false
        binding.startButton.text = "시작"

        // 타이머 초기화
        val hours = (initialTimeInMillis / 3600000).toInt()
        val minutes = ((initialTimeInMillis % 3600000) / 60000).toInt()
        val seconds = ((initialTimeInMillis % 60000) / 1000).toInt()

        toggleTimerViews(showCountdown = false)
        binding.hourPicker.value = hours
        binding.minutePicker.value = minutes
        binding.secondPicker.value = seconds

    }

    private fun updateNumberPickers(millis: Long) {
        val hours = (millis / 3600000).toInt()
        val minutes = ((millis % 3600000) / 60000).toInt()
        val seconds = ((millis % 60000) / 1000).toInt()


        binding.timerCountdownHour.text = hours.toString()
        binding.timerCountdownMinute.text = minutes.toString()
        binding.timerCountdownSecond.text = seconds.toString()
    }

    private fun toggleTimerViews(showCountdown: Boolean) {
        var title: ConstraintLayout.LayoutParams =
            binding.timerTitle.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams

        var startBtn:  ConstraintLayout.LayoutParams =
            binding.startButton.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
        var cancleBtn:  ConstraintLayout.LayoutParams =
            binding.cancelButton.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
        if (showCountdown) {
            // Countdown 보이기
            binding.countdownContainer.visibility=View.VISIBLE

            // NumberPicker 숨기기
            binding.timePickerContainer.visibility = View.GONE

            title.bottomMargin = 50
            startBtn.topMargin = 50
            cancleBtn.topMargin = 50

        } else {
            // Countdown 숨기기
            binding.countdownContainer.visibility=View.GONE


            // NumberPicker 보이기
            binding.timePickerContainer.visibility = View.VISIBLE
            title.bottomMargin = 0
            startBtn.topMargin = 30
            cancleBtn.topMargin = 30
        }
    }



}