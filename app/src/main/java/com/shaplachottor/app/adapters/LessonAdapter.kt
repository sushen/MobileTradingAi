package com.shaplachottor.app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.shaplachottor.app.databinding.ItemLessonBinding
import com.shaplachottor.app.models.Lesson

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
        holder.binding.apply {
            tvLessonTitle.text = lesson.title
            tvLessonType.text = lesson.type.replaceFirstChar { it.uppercase() }
            cbLessonComplete.isChecked = lesson.isCompleted

            cbLessonComplete.setOnCheckedChangeListener { _, isChecked ->
                onCompleteToggle(lesson, isChecked)
            }

            root.setOnClickListener { onLessonClick(lesson) }
        }
    }

    override fun getItemCount() = lessons.size
}
