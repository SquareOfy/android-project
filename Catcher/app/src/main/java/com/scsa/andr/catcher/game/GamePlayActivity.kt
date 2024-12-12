package com.scsa.andr.catcher.game

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.LayerDrawable
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.util.TypedValueCompat
import com.scsa.andr.catcher.R
import java.util.Random

class GamePlayActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "GamePlayActivity_YY"
    }

    private lateinit var frameLayout: FrameLayout
    private lateinit var params: FrameLayout.LayoutParams
    private lateinit var scoreTextView: TextView
    private lateinit var timerProgressBar: ProgressBar

    private var score = 0
    private var count = 0
    private var gameSpeed = 1000
    private var timeLeft = 5 * 1000L // 30 seconds
    private var threadEndFlag = true
    private var mouseTask: MouseTask?=null
    private lateinit var countDownTimer: CountDownTimer

    private var myWidth = 0
    private var myHeight =0
    private val imgWidth = 150
    private val imgHeight = 150

    private val random = Random()

    private lateinit var soundPool: SoundPool
    private var killSound = 0
    private lateinit var mediaPlayer: MediaPlayer

    private var x = 200
    private var y = 200
    private lateinit var imageViews: Array<ImageView>

    private var level = 1
    private var howManyMouse = 7


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_game_play)
        frameLayout = findViewById(R.id.frame)
        params = FrameLayout.LayoutParams(1, 1)
        scoreTextView = findViewById(R.id.score_text_view)
        timerProgressBar = findViewById(R.id.timer_progress_bar)
//        clockIcon = findViewById(R.id.clock_icon)

        val finishDialog = findViewById<LinearLayout>(R.id.finish_dialog)
        Log.d(TAG, "finishDialog: $finishDialog") // 디버깅 로그 추가

        params = FrameLayout.LayoutParams(1,1)


        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        myWidth = metrics.widthPixels
        myHeight = metrics.heightPixels
        Log.d(TAG, "My Window $myWidth : $myHeight")
        Log.d(TAG, "My Window $myWidth : $myHeight")

        // 사운드 셋팅
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setLegacyStreamType(AudioManager.STREAM_MUSIC)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(attributes)
            .build()
        killSound = soundPool.load(this, R.raw.mouse_scream, 1)
        mediaPlayer = MediaPlayer.create(this, R.raw.bgm).apply {
            isLooping = true
        }



        init(howManyMouse)
    }

    private fun init(nums: Int){
        Log.d(TAG, "Init Games!!!!!!!!!!")
        count = 0
        timeLeft = 5 * 1000L
        threadEndFlag = true
        howManyMouse = nums
        gameSpeed = (gameSpeed * (10 - level) / 10.0).toInt()
        Log.d(TAG, "count : $count")
        Log.d(TAG, "timeLeft : $timeLeft")
        Log.d(TAG, "howManyMouse : $howManyMouse")

        if (::imageViews.isInitialized) {
            imageViews.forEach { frameLayout.removeView(it) }
        }

        val images = arrayOf(
            R.drawable.hat_mouse,
            R.drawable.standing_mouse,
            R.drawable.running_mouse,
            R.drawable.cat_pink_ball,
            R.drawable.cat_big_eyes
        )
        // 이미지 담을 배열 생성과 이미지 담기
        imageViews = Array(images.size) { index ->
            ImageView(this).apply {
                setImageResource(images[index])
                params = FrameLayout.LayoutParams(imgWidth, imgHeight)
                visibility = View.VISIBLE
                frameLayout.addView(this, params)
                setOnClickListener(mouseClickListener)
            }
        }
        Log.d(TAG, "imageViews size : ${imageViews.size}")

        startTimer()
        mouseTask?.cancel(true)
        mouseTask = MouseTask().apply { execute() }
    }

    private fun resetGame() {
        val intent = Intent(this, GamePlayActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }

    private fun startTimer() {


        timerProgressBar.max = timeLeft.toInt()
        timerProgressBar.setProgress(timeLeft.toInt())

        timerProgressBar.visibility = View.VISIBLE
        countDownTimer = object : CountDownTimer(timeLeft, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeft = millisUntilFinished
                updateProgressBarAndIcons(millisUntilFinished.toInt())
                timerProgressBar.setProgress(millisUntilFinished.toInt())
            }

            override fun onFinish() {
                threadEndFlag = false
                mouseTask?.cancel(true)
                showEndGameDialog()
            }
        }.start()
    }
    private fun updateProgressBarAndIcons(progress: Int) {
        val color = when {
            progress <= timerProgressBar.max / 3 -> Color.RED
            progress <= 2 * timerProgressBar.max / 3 -> Color.parseColor("#FF8000") // 주황색
            else -> Color.GREEN
        }

        val progressDrawable = timerProgressBar.progressDrawable
        if (progressDrawable is LayerDrawable) {
            val progressBarLayer = progressDrawable.findDrawableByLayerId(android.R.id.progress)
            if (progressBarLayer != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    progressBarLayer.setTint(color)
                } else {
                    progressBarLayer.setColorFilter(color, PorterDuff.Mode.SRC_IN)
                }
            }
        }


    }


    private fun showEndGameDialog() {

        setContentView(R.layout.activity_game_play)
        val finishDialog = findViewById<LinearLayout>(R.id.finish_dialog)
        val scoreText: TextView = findViewById(R.id.final_score_text_view)
        val replayButton: Button = findViewById(R.id.replay_button)
        val homeButton: Button = findViewById(R.id.home_button)

        // finish_dialog 전체를 보이도록 설정
        finishDialog?.visibility = View.VISIBLE


        scoreText.text = "SCORE: $score"



        replayButton.setOnClickListener {
            finishDialog?.visibility = View.GONE
            resetGame()
        }

        homeButton?.setOnClickListener {
            finish()
        }


    }

    override fun onResume() {
        super.onResume()
        mediaPlayer.start()
    }

    override fun onPause() {
        super.onPause()
        if (mediaPlayer.isPlaying) {
            mediaPlayer.pause()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.stop()
        mediaPlayer.release()
        mouseTask?.cancel(true)
        threadEndFlag = false
    }

    private val mouseClickListener = View.OnClickListener { view ->
        count++
        val iv = view as ImageView
        soundPool.play(killSound, 1f, 1f, 0, 0, 1f)

        when (iv.drawable.constantState) {
            resources.getDrawable(R.drawable.hat_mouse).constantState -> score += 5
            resources.getDrawable(R.drawable.standing_mouse).constantState, resources.getDrawable(R.drawable.running_mouse).constantState -> score++
            resources.getDrawable(R.drawable.cat_pink_ball).constantState -> score -= 3
            resources.getDrawable(R.drawable.cat_big_eyes).constantState -> score -= 3
        }
        scoreTextView.text = "Score: $score"
        iv.visibility = View.INVISIBLE
        gameSpeed = (1000 * (30 - score.coerceAtLeast(1)) / 30).coerceAtLeast(200)

    }
    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun update() {
        if (!threadEndFlag) return
        Log.d(TAG, "update:")
        val topOffsetPx = dpToPx(70)
        val selectedImages = imageViews.toList().shuffled().take(5)
        imageViews.forEach { it.visibility = View.INVISIBLE }
        selectedImages.forEach { img ->
            x = random.nextInt(myWidth - imgWidth)
            y = random.nextInt(myHeight - imgHeight-topOffsetPx)+topOffsetPx
            img.layout(x, y, x + imgWidth, y + imgHeight)
            img.visibility = View.VISIBLE
            img.invalidate()
        }
    }



    private inner class MouseTask : AsyncTask<Void, Void, Void>() {
        override fun doInBackground(vararg params: Void?): Void? {
            while (threadEndFlag) {
                publishProgress()
                try {
                    Thread.sleep(gameSpeed.toLong())
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
            return null
        }

        override fun onProgressUpdate(vararg values: Void?) {
            update()
        }
    }
}