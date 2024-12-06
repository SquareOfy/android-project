package com.scsa.andr.catcher.news

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.scsa.andr.catcher.R

class NewsAdapter(
    private val context: Context,
    private val newsList: List<NewsItem>
) : BaseAdapter() {

    override fun getCount(): Int = newsList.size

    override fun getItem(position: Int): Any = newsList[position]

    override fun getItemId(position: Int): Long = newsList[position].id

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View
        val viewHolder: ViewHolder

        if (convertView == null) {
            view = LayoutInflater.from(context).inflate(R.layout.item_news, parent, false)
            viewHolder = ViewHolder(view)
            view.tag = viewHolder
        } else {
            view = convertView
            viewHolder = view.tag as ViewHolder
        }

        val newsItem = newsList[position]
        viewHolder.title.text = newsItem.title
        viewHolder.summary.text = newsItem.description
        viewHolder.date.text = newsItem.pubDate

        return view
    }

    private class ViewHolder(view: View) {
        val title: TextView = view.findViewById(R.id.newsTitle)
        val summary: TextView = view.findViewById(R.id.newsSummary)
        val date: TextView = view.findViewById(R.id.newsDate)
    }
}
