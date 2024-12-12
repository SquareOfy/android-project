package com.scsa.andr.catcher.alarm

import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaPlayer
import android.net.Uri
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.Settings
import android.view.ContextMenu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.scsa.andr.catcher.R
import com.scsa.andr.catcher.databinding.ActivityAlarmMainBinding
import com.scsa.andr.catcher.databinding.DialogAddTimerBinding

class AlarmMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlarmMainBinding
    private lateinit var alarmAdapter: AlarmAdapter
    private lateinit var alarmDBHelper: AlarmDBHelper
    private var selectedAlarm: AlarmDto? = null

    private var timer: CountDownTimer? = null

    private var mediaPlayer: MediaPlayer? = null
    private lateinit var alarmManager: AlarmManager
    private lateinit var pendingIntent: PendingIntent
    private lateinit var intentFilters: Array<IntentFilter>


    private var isAlarmRunning = false
    private var isPaused = false
    private var remainingTimeInMillis: Long = 0L
    private var initialTimeInMillis: Long = 0L

    //NFC
    private val nfcAdapter: NfcAdapter?
            by lazy { NfcAdapter.getDefaultAdapter(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAlarmMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (nfcAdapter == null) {
            Toast.makeText(this, "이 장치는 NFC를 지원하지 않습니다.", Toast.LENGTH_SHORT).show()
        } else if (!nfcAdapter!!.isEnabled) {
            Toast.makeText(this, "NFC를 활성화하세요.", Toast.LENGTH_SHORT).show()
        }

        setupNumberPickers()
        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        alarmDBHelper = AlarmDBHelper(this, "alarm.db", null, 1)
        alarmDBHelper.open()


        alarmAdapter = AlarmAdapter(emptyList(),
            onItemClick = { alarm -> setTimerToNumberPickers(alarm) },
            onItemLongClick = { alarm, view ->
                selectedAlarm = alarm
                registerForContextMenu(view)
                view.showContextMenu()
            }
        )
        binding.timerListRecyclerview.layoutManager = LinearLayoutManager(this)
        binding.timerListRecyclerview.adapter = alarmAdapter

        fetchAndDisplayAlarms()

        binding.startButton.setOnClickListener {
            when {
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

        val fabAdd = binding.fabTimerAdd
        fabAdd.setOnClickListener {
            showAddOrEditTimerDialog()
        }


        //NFC
        // Set up PendingIntent and IntentFilter
        val intent = Intent(this, javaClass).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE)

        val ndefFilter = IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED).apply {
            try {
                addDataType("text/*") // MIME 타입 필터링
            } catch (e: IntentFilter.MalformedMimeTypeException) {
                e.printStackTrace()
            }
        }
        intentFilters = arrayOf(ndefFilter)
    }
    override fun onResume() {
        super.onResume()
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, intentFilters, null)
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        val rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES) ?: return
        val ndefMessage = rawMessages[0] as? NdefMessage ?: return
        val ndefRecord = ndefMessage.records.firstOrNull() ?: return

        val payload = String(ndefRecord.payload)
        val trimmedPayload = if (payload.length > 3) payload.substring(3) else ""
        parseAndSetTimer(trimmedPayload)

    }


    // 00:00:00 형태의 TEXT를 파싱하여 타이머에 설정
    private fun parseAndSetTimer(payload: String) {
        val regex = Regex("""(\d{2}):(\d{2}):(\d{2})""")
        val matchResult = regex.find(payload)

        if (matchResult != null) {
            val (hours, minutes, seconds) = matchResult.destructured

            runOnUiThread {
                binding.hourPicker.value = hours.toInt()
                binding.minutePicker.value = minutes.toInt()
                binding.secondPicker.value = seconds.toInt()

                Toast.makeText(this, "NFC에서 읽은 타이머가 설정되었습니다: $payload", Toast.LENGTH_SHORT).show()
            }
        } else {
            runOnUiThread {
                Toast.makeText(this, "NFC 데이터에서 유효한 시간을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setTimerToNumberPickers(alarm: AlarmDto) {
        // 클릭된 타이머의 데이터를 NumberPicker에 설정
        binding.hourPicker.value = alarm.hour
        binding.minutePicker.value = alarm.minute
        binding.secondPicker.value = alarm.second
        Toast.makeText(this, "타이머 설정됨: ${alarm.title}", Toast.LENGTH_SHORT).show()
    }

    private fun showDeleteConfirmationDialog(alarm: AlarmDto) {
        AlertDialog.Builder(this)
            .setTitle("타이머 삭제")
            .setMessage("정말로 삭제하시겠습니까?")
            .setPositiveButton("삭제") { _, _ ->
                alarmDBHelper.delete(alarm.id)
                fetchAndDisplayAlarms()
                Toast.makeText(this, "타이머가 삭제되었습니다.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("취소", null)
            .create()
            .show()
    }


    // Context menu handling
    override fun onCreateContextMenu(
        menu: ContextMenu,
        v: View,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        super.onCreateContextMenu(menu, v, menuInfo)
        menuInflater.inflate(R.menu.todo_context_menu, menu)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_edit -> {
                selectedAlarm?.let { showAddOrEditTimerDialog(it) }
                true
            }

            R.id.menu_delete -> {
                selectedAlarm?.let { showDeleteConfirmationDialog(it) }
                true
            }

            else -> super.onContextItemSelected(item)
        }
    }

    private fun fetchAndDisplayAlarms() {
        val alarms = alarmDBHelper.selectAll()
        alarmAdapter.updateData(alarms)
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
        // 타이머 초기화
        var hours = (initialTimeInMillis / 3600000).toInt()
        var minutes = ((initialTimeInMillis % 3600000) / 60000).toInt()
        var seconds = ((initialTimeInMillis % 60000) / 1000).toInt()

        timer = null
        isPaused = false
        isAlarmRunning = false // 알람 실행 상태 초기화
        if (!isPaused && binding.startButton.text != "시작") {
            hours = 0
            minutes = 0
            seconds = 0
            toggleTimerViews(showCountdown = false)
        }
        binding.startButton.text = "시작"




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

        var startBtn: ConstraintLayout.LayoutParams =
            binding.startButton.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
        var cancleBtn: ConstraintLayout.LayoutParams =
            binding.cancelButton.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
        if (showCountdown) {
            // Countdown 보이기
            binding.countdownContainer.visibility = View.VISIBLE

            // NumberPicker 숨기기
            binding.timePickerContainer.visibility = View.GONE

            title.bottomMargin += 400
            startBtn.topMargin += 400
            cancleBtn.topMargin += 400


        } else {
            // Countdown 숨기기
            binding.countdownContainer.visibility = View.GONE


            // NumberPicker 보이기
            binding.timePickerContainer.visibility = View.VISIBLE
            title.bottomMargin -= 400
            startBtn.topMargin -= 400
            cancleBtn.topMargin -= 400
        }
        binding.timerTitle.requestLayout()
        binding.startButton.requestLayout()
        binding.cancelButton.requestLayout()
    }


    private fun showAddOrEditTimerDialog(alarm: AlarmDto? = null) {
        val dialogBinding = DialogAddTimerBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()

        // Set initial values if editing
        alarm?.let {
            dialogBinding.timerTitleEdittext.setText(it.title)
            dialogBinding.hourPicker.value = it.hour
            dialogBinding.minutePicker.value = it.minute
            dialogBinding.secondPicker.value = it.second
        }

        dialogBinding.hourPicker.apply { minValue = 0; maxValue = 23 }
        dialogBinding.minutePicker.apply { minValue = 0; maxValue = 59 }
        dialogBinding.secondPicker.apply { minValue = 0; maxValue = 59 }

        // 수정 모드: 기존 데이터 설정
        if (alarm != null) {
            dialogBinding.timerTitleEdittext.setText(alarm.title) // 제목 설정
            dialogBinding.hourPicker.value = alarm.hour          // 시 설정
            dialogBinding.minutePicker.value = alarm.minute      // 분 설정
            dialogBinding.secondPicker.value = alarm.second      // 초 설정
            dialogBinding.startButton.text = "수정"
        } else {
            dialogBinding.startButton.text = "등록"
        }

        dialogBinding.startButton.setOnClickListener {
            val title = dialogBinding.timerTitleEdittext.text.toString()
            val hour = dialogBinding.hourPicker.value
            val minute = dialogBinding.minutePicker.value
            val second = dialogBinding.secondPicker.value

            if (title.isBlank() || (hour == 0 && minute == 0 && second == 0)) {
                Toast.makeText(this, "타이머 내용을 입력하세요.", Toast.LENGTH_SHORT).show()
            } else {
                if (alarm == null) {
                    // New timer
                    val newTimer = AlarmDto(0L, title, hour, minute, second)
                    newTimer.id = alarmDBHelper.insert(newTimer)
                    Toast.makeText(this, "타이머가 저장되었습니다.", Toast.LENGTH_SHORT).show()
                } else {
                    // Update existing timer
                    alarm.title = title
                    alarm.hour = hour
                    alarm.minute = minute
                    alarm.second = second
                    alarmDBHelper.update(alarm)
                    Toast.makeText(this, "타이머가 수정되었습니다.", Toast.LENGTH_SHORT).show()
                }
                fetchAndDisplayAlarms()
                dialog.dismiss()
            }
        }

        dialogBinding.cancelButton.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun saveTimer(timer: AlarmDto) {
        alarmDBHelper.insert(timer)
        // Implement saving logic here
        Toast.makeText(
            this,
            "타이머 저장됨: ${timer.title} (${timer.hour}시간 ${timer.minute}분 ${timer.second}초)",
            Toast.LENGTH_SHORT
        ).show()
    }

}