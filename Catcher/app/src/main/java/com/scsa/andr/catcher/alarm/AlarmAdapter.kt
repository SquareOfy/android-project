package com.scsa.andr.catcher.alarm

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.scsa.andr.catcher.R

class AlarmAdapter(private var alarmList: List<AlarmDto>,
                   private val onItemClick: (AlarmDto) -> Unit,
                   private val onItemLongClick: (AlarmDto, View) -> Unit
):
    RecyclerView.Adapter<AlarmAdapter.AlarmViewHolder>() {

        inner class AlarmViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
            val titleTextView: TextView = itemView.findViewById(R.id.timer_title)
            val timeTextView: TextView = itemView.findViewById(R.id.timer_time)

            init {
                itemView.setOnLongClickListener{
                    onItemLongClick(alarmList[adapterPosition], it)
                    true
                }
                itemView.setOnClickListener{
                    onItemClick(alarmList[adapterPosition])
                }
            }
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlarmViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_timer, parent, false)
        return AlarmViewHolder(itemView)
    }

    override fun getItemCount(): Int {
        return alarmList.size
    }

    override fun onBindViewHolder(holder: AlarmViewHolder, position: Int) {
        val alarm = alarmList[position]
        holder.titleTextView.text = alarm.title

        holder.timeTextView.text = String.format("%02d:%02d:%02d", alarm.hour, alarm.minute, alarm.second)
    }


    fun updateData(newAlarmList: List<AlarmDto>){
        alarmList = newAlarmList
        notifyDataSetChanged()
    }

}