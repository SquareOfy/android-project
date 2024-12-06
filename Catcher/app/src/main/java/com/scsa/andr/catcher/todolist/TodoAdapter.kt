import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import com.scsa.andr.catcher.databinding.TodolistItemBinding
import com.scsa.andr.catcher.todolist.ToDoDto
import java.time.format.DateTimeFormatter

class TodoAdapter(private var context: Context, private var todoList: List<ToDoDto>) : BaseAdapter() {

    override fun getCount(): Int {
        return todoList.size
    }

    override fun getItem(position: Int): Any {
        return todoList[position]
    }

    override fun getItemId(position: Int): Long {
        return todoList[position].id
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val binding: TodolistItemBinding
        val view: View

        if (convertView == null) {
            binding = TodolistItemBinding.inflate(LayoutInflater.from(context), parent, false)
            view = binding.root
            view.tag = binding
        } else {
            binding = convertView.tag as TodolistItemBinding
            view = convertView
        }

        val todo = todoList[position]

        binding.tvTaskTitle.text = todo.title
        binding.tvTaskContent.text = todo.todoContent
        val formatter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        } else {
            TODO("VERSION.SDK_INT < O")
        }

        val dateText = StringBuilder()
        dateText.append(todo.startDate?.format(formatter) ?: "N/A")
        if (todo.endDate != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                dateText.append(" - ").append(todo.endDate!!.format(formatter))
            }
        }

        binding.tvTaskDate.text = dateText.toString()

        return view
    }
}
