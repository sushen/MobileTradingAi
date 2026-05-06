package com.shaplachottor.lab.adapters

import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.shaplachottor.lab.databinding.ItemLessonContentBlockBinding
import com.shaplachottor.lab.models.ContentBlockType
import com.shaplachottor.lab.models.LessonContentBlock

class ContentBlockAdapter(
    private val blocks: List<LessonContentBlock>
) : RecyclerView.Adapter<ContentBlockAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemLessonContentBlockBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLessonContentBlockBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val block = blocks[position]
        holder.binding.apply {
            tvBlockTitle.text = block.title
            tvBlockBody.text = block.body

            // Reset defaults to avoid recycling issues
            tvBlockBody.setTypeface(Typeface.DEFAULT)
            tvBlockBody.setTextColor(Color.BLACK)
            cardContentBlock.setCardBackgroundColor(Color.WHITE)
            cardContentBlock.strokeWidth = 0

            // Styling based on type
            when (block.type) {
                ContentBlockType.EXAMPLE -> {
                    tvBlockBody.setTypeface(Typeface.MONOSPACE)
                    cardContentBlock.setCardBackgroundColor(Color.LTGRAY)
                    tvBlockBody.setTextColor(Color.BLACK)
                }
                ContentBlockType.EXERCISE -> {
                    cardContentBlock.strokeWidth = 4
                    cardContentBlock.setStrokeColor(Color.parseColor("#2E7D32")) // shapla_green
                    cardContentBlock.setCardBackgroundColor(Color.parseColor("#E8F5E9")) // primary_container
                }
                ContentBlockType.REFLECTION -> {
                    cardContentBlock.setCardBackgroundColor(Color.parseColor("#E0F2F1")) // secondary_container
                }
                else -> {
                    // CONCEPT or other types
                    cardContentBlock.setCardBackgroundColor(Color.TRANSPARENT)
                }
            }
        }
    }

    override fun getItemCount() = blocks.size
}
