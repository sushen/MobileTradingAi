package com.shaplachottor.lab.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.shaplachottor.lab.databinding.ItemLessonBinding
import com.shaplachottor.lab.models.Lesson

class LessonAdapter(
    private val lessons: List<Lesson>,
    private val onLessonClick: (Lesson) -> Unit,
    private val onCompleteToggle: (Lesson, Boolean) -> Unit
) : RecyclerView.Adapter<LessonAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemLessonBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLessonBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val lesson = lessons[position]
        val isLocked = position > 0 && !lessons[position - 1].isCompleted

        holder.binding.apply {
            tvLessonTitle.text = lesson.title
            
            // Fix: Remove listener before setting checked state to avoid recursion
            cbLessonComplete.setOnCheckedChangeListener(null)
            cbLessonComplete.isChecked = lesson.isCompleted
            
            // Sequential locking UI
            val alpha = if (isLocked) 0.5f else 1.0f
            root.alpha = alpha
            cbLessonComplete.isEnabled = !isLocked

            tvLessonType.text = if (isLocked) "Locked" else "Available"

            cbLessonComplete.setOnCheckedChangeListener { _, isChecked ->
                onCompleteToggle(lesson, isChecked)
            }

            root.setOnClickListener { 
                if (!isLocked) {
                    onLessonClick(lesson)
                }
            }
        }
    }

    override fun getItemCount() = lessons.size
}
