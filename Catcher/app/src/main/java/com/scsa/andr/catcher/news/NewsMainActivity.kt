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
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.scsa.andr.catcher.R

class NewsMainActivity : AppCompatActivity() {

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
        setContentView(R.layout.activity_news_main)

        tabLayout = findViewById(R.id.tabLayout)
        viewPager = findViewById(R.id.viewPager)
        webView = findViewById(R.id.webView)
        val adapter = NewsPagerAdapter(this, rssUrls)
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = tabs[position]
        }.attach()

        // Set up WebView
        webView.webViewClient = WebViewClient()

        // Bottom Sheet Behavior setup
        val bottomSheet = findViewById<View>(R.id.bottomSheet)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)

        // Initial Bottom Sheet state
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        // Set WebView client
        webView.webViewClient = WebViewClient()


        bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_EXPANDED -> {
                        // WebView fully expanded
                    }
                    BottomSheetBehavior.STATE_COLLAPSED -> {
                        // WebView collapsed to its peek height
                    }
                    BottomSheetBehavior.STATE_HIDDEN -> {
                        // WebView hidden
                    }
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                bottomSheet.translationY = (1 - slideOffset) * 500 // Adjust the translation dynamically
                bottomSheet.alpha = 0.5f + (slideOffset / 2) // Adjust transparency dynamically
            }
        })

        viewPager.post {
            val bottomSheetBehavior = BottomSheetBehavior.from(findViewById(R.id.bottomSheet))
            val peekHeight = bottomSheetBehavior.peekHeight
            val tabHeight = tabLayout.measuredHeight

            val availableHeight = window.decorView.height - tabHeight - peekHeight
            val layoutParams = viewPager.layoutParams
            layoutParams.height = availableHeight
            viewPager.layoutParams = layoutParams
        }

    }

    fun toggleWebView(url: String) {
        if (webView.visibility == View.GONE) {
//            webView.visibility = View.VISIBLE
//            webView.loadUrl(url)
            showWebView(url)
        } else {
//            webView.visibility = View.GONE
            hideWebView()
        }
    }



    fun showWebView(url: String) {
        Log.d(TAG)
        webView.loadUrl(url)
        webView.visibility = View.VISIBLE
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    fun hideWebView() {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
    }



}
