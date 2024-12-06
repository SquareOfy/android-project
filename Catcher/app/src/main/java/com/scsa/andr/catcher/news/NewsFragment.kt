import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ListView
import androidx.fragment.app.Fragment
import com.scsa.andr.catcher.R
import com.scsa.andr.catcher.news.NewsAdapter
import com.scsa.andr.catcher.news.NewsItem
import com.scsa.andr.catcher.news.NewsMainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.net.URL

class NewsFragment : Fragment() {
    private val newsList = mutableListOf<NewsItem>()
    private val TAG = "CATCHER_YY"
    private lateinit var listView: ListView
    private lateinit var adapter: NewsAdapter

    private var rssUrl: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        Log.d(TAG, "onCreateView: Initializing view for NewsFragment")
        val view = inflater.inflate(R.layout.fragment_news_list, container, false)
        listView = view.findViewById(R.id.listView)

        adapter = NewsAdapter(requireContext(), newsList)
        listView.adapter = adapter

        listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            Log.d(TAG, "onItemClick: Clicked on item: ${newsList[position].title}")
            (activity as? NewsMainActivity)?.toggleWebView(newsList[position].link)
        }

        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        Log.d(TAG, "onActivityCreated: Fragment created")
        rssUrl = arguments?.getString("rssUrl")
        Log.d(TAG, "onActivityCreated: RSS URL = $rssUrl")

        rssUrl?.let {
            CoroutineScope(Dispatchers.IO).launch {
                fetchNews(it)
            }
        } ?: Log.e(TAG, "onActivityCreated: RSS URL is null")
    }

    private fun fetchNews(url: String) {
        Log.d(TAG, "fetchNews: Fetching news from URL: $url")
        try {
            val inputStream = URL(url).openStream()
            val parserFactory = XmlPullParserFactory.newInstance()
            val parser = parserFactory.newPullParser()
            parser.setInput(inputStream, null)

            var eventType = parser.eventType
            var item: NewsItem? = null
            var text = ""
            var id = 0L

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val tagName = parser.name
                when (eventType) {
                    XmlPullParser.START_TAG -> if (tagName == "item") {
                        item = NewsItem(++id, "", "", "", "")
                        Log.d(TAG, "fetchNews: Found <item> tag")
                    }
                    XmlPullParser.TEXT -> text = parser.text
                    XmlPullParser.END_TAG -> {
                        if (item != null) {
                            when (tagName) {
                                "title" -> {
                                    item = item.copy(title = text)
                                    Log.d(TAG, "fetchNews: Parsed title: $text")
                                }
                                "link" -> {
                                    item = item.copy(link = text)
                                    Log.d(TAG, "fetchNews: Parsed link: $text")
                                }
                                "description" -> {
                                    item = item.copy(description = text)
                                    Log.d(TAG, "fetchNews: Parsed description: $text")
                                }
                                "pubDate" -> {
                                    item = item.copy(pubDate = text)
                                    Log.d(TAG, "fetchNews: Parsed pubDate: $text")
                                }
                                "item" -> {
                                    newsList.add(item)
                                    Log.d(TAG, "fetchNews: Added item: ${item.title}")
                                    item = null
                                }
                            }
                        }
                    }
                }
                eventType = parser.next()
            }

            Log.d(TAG, "fetchNews: Parsing complete. Total items: ${newsList.size}")

            CoroutineScope(Dispatchers.Main).launch {
                Log.d(TAG, "fetchNews: Updating adapter with news list")
                adapter.notifyDataSetChanged()
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchNews: Error fetching news: ${e.message}", e)
        }
    }

    companion object {
        fun newInstance(rssUrl: String): NewsFragment {
            val fragment = NewsFragment()
            val args = Bundle()
            args.putString("rssUrl", rssUrl)
            fragment.arguments = args
            return fragment
        }
    }


}
