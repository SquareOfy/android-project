package com.scsa.andr.catcher.todolist

import java.time.LocalDateTime

class ToDoDto (var id: Long = 0L, var title: String="", var todoContent: String="",
               var startDate: LocalDateTime?=null, var endDate: LocalDateTime?=null, var status:Int = 0)