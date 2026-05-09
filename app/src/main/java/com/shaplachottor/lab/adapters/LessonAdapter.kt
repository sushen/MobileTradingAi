package com.shaplachottor.lab.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.shaplachottor.lab.R
import com.shaplachottor.lab.databinding.ItemLessonBinding
import com.shaplachottor.lab.models.Lesson

class LessonAdapter(
    private val lessons: List<Lesson>,
    private val onLessonClick: (Lesson) -> Unit,
    private val onCompleteToggle: (Lesson, Boolean) -> Unit
) : RecyclerView.Adapter<LessonAdapter.ViewHolder>() {
    init {
        setHasStableIds(true)
    }

    class ViewHolder(val binding: ItemLessonBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLessonBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val lesson = lessons[position]
        // Sequential Logic: First lesson is always unlocked. Others depend on previous lesson completion.
        val isLocked = if (position == 0) false else !lessons[position - 1].isCompleted
        val isCompleted = lesson.isCompleted
        val isCurrent = !isLocked && !isCompleted
        val adapterPosition = holder.adapterPosition

        android.util.Log.d("LessonAdapter", "Binding Lesson: ${lesson.title} (Pos: $position, AdapterPos: $adapterPosition, ID: ${lesson.id}) -> Locked: $isLocked, Completed: $isCompleted, Current: $isCurrent")

        holder.binding.apply {
            // Reset listener and state first to prevent callback triggers from previous state
            cbLessonComplete.setOnCheckedChangeListener(null)
            
            tvLessonTitle.text = lesson.title
            tvLessonType.text = root.context.getString(com.shaplachottor.lab.R.string.lesson_step_format, position + 1, lessons.size)
            
            cbLessonComplete.isChecked = isCompleted
            cbLessonComplete.isEnabled = !isLocked

            // Visual States Reset - ALWAYS reset properties that change
            root.alpha = 1.0f
            root.strokeWidth = 0
            root.setStrokeColor(android.content.res.ColorStateList.valueOf(android.graphics.Color.TRANSPARENT))
            cbLessonComplete.alpha = 1.0f
            ivLessonStatus.setImageResource(android.R.drawable.ic_media_play)
            ivLessonStatus.imageTintList = android.content.res.ColorStateList.valueOf(root.context.getColor(R.color.primary))

            when {
                isLocked -> {
                    root.alpha = 0.5f
                    ivLessonStatus.setImageResource(android.R.drawable.ic_lock_idle_lock)
                    ivLessonStatus.imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.GRAY)
                    cbLessonComplete.alpha = 0.3f
                }
                isCompleted -> {
                    ivLessonStatus.setImageResource(android.R.drawable.ic_notification_overlay)
                    ivLessonStatus.imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#4CAF50"))
                    cbLessonComplete.alpha = 1.0f
                }
                isCurrent -> {
                    root.strokeWidth = 4
                    val primaryColor = root.context.getColor(com.shaplachottor.lab.R.color.primary)
                    root.setStrokeColor(android.content.res.ColorStateList.valueOf(primaryColor))
                    ivLessonStatus.setImageResource(android.R.drawable.ic_media_play)
                    ivLessonStatus.imageTintList = android.content.res.ColorStateList.valueOf(primaryColor)
                    cbLessonComplete.alpha = 1.0f
                }
            }

            cbLessonComplete.setOnCheckedChangeListener { _, isChecked ->
                android.util.Log.d("LessonAdapter", "Checkbox Toggled: ${lesson.title} (ID: ${lesson.id}, AdapterPos: ${holder.adapterPosition}) -> $isChecked")
                onCompleteToggle(lesson, isChecked)
            }

            root.setOnClickListener { 
                if (!isLocked) {
                    android.util.Log.d("LessonAdapter", "Clicked Lesson: ${lesson.title} (ID: ${lesson.id}, AdapterPos: ${holder.adapterPosition})")
                    onLessonClick(lesson)
                } else {
                    android.util.Log.d("LessonAdapter", "Clicked Locked Lesson: ${lesson.title} (ID: ${lesson.id}, AdapterPos: ${holder.adapterPosition})")
                }
            }
        }
    }

    override fun getItemId(position: Int): Long = lessons[position].id.hashCode().toLong()

    override fun getItemCount() = lessons.size
}
