package com.scsa.andr.catcher.news

import android.animation.ValueAnimator
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.shape.ShapeAppearanceModel
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.scsa.andr.catcher.R
import com.scsa.andr.catcher.databinding.ActivityNewsMainBinding

class NewsMainActivity : AppCompatActivity() {
    private val TAG = "CATCHER_YY"
    private lateinit var binding: ActivityNewsMainBinding
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var webView: WebView
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>


    private val tabs = listOf("연합뉴스 TV", "JTBC", "매일경제", "한국경제", "NYTimes")
    private val rssUrls = listOf(

        "https://www.yonhapnewstv.co.kr/browse/feed/",
        "https://news-ex.jtbc.co.kr/v1/get/rss/issue",
        "https://www.mk.co.kr/rss/30000001/",
        "https://www.hankyung.com/feed/all-news",
        "https://rss.nytimes.com/services/xml/rss/nyt/HomePage.xml"


    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityNewsMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tabLayout = binding.tabLayout
        viewPager = binding.viewPager
        webView = binding.webView
        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet)

        setWebViewPager()

        setupWebView()

        // 3. BottomSheet 설정 개선: 초기화를 별도 메서드로 분리하여 관리
        setupBottomSheet()

    }

    fun setWebViewPager(){
        val adapter = NewsPagerAdapter(this, rssUrls)
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = tabs[position]
        }.attach()

        // 1.1 ViewPager 높이 동적 조정
        viewPager.post {
            adjustViewPagerHeight()
        }
    }

    private fun setupWebView() {
        webView.apply {
            webViewClient = WebViewClient() // 중복 설정 제거
            setBackgroundColor(android.graphics.Color.TRANSPARENT) // 투명 배경 설정
            visibility = View.GONE // 초기 상태를 명확히 설정
        }
    }

    // 3. BottomSheet 설정: 둥근 모서리 및 상태 콜백 설정
    private fun setupBottomSheet() {
        applyRoundedCorners(binding.bottomSheet)

        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                Log.d(TAG, "onStateChanged: New state = $newState")
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                Log.d(TAG, "onSlide: Slide offset = $slideOffset")
            }
        })
    }
    // 1.1 ViewPager 높이 동적 조정: 중복된 코드 제거
    private fun adjustViewPagerHeight() {
        val peekHeight = bottomSheetBehavior.peekHeight
        val tabHeight = tabLayout.measuredHeight
        val availableHeight = window.decorView.height - tabHeight - peekHeight

        viewPager.layoutParams = viewPager.layoutParams.apply {
            height = availableHeight
        }
    }

    fun applyRoundedCorners(bottomSheet: View){
        bottomSheet.background = getDrawable(R.drawable.bottom_sheet_round)
    }

    fun toggleWebView(url: String) {
        showWebView(url)
    }



    fun showWebView(url: String) {
        applyRoundedCorners(binding.bottomSheet);
        applyRoundedCorners(binding.webView);
        webView.loadUrl(url)
        webView.visibility = View.VISIBLE
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    fun hideWebView() {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        webView.visibility = View.GONE
    }



}
