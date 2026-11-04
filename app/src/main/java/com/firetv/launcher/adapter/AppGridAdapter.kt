package com.firetv.launcher.adapter

import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.firetv.launcher.databinding.ItemAppTileBinding
import com.firetv.launcher.model.AppItem
import com.firetv.launcher.util.DpadFocusHelper

class AppGridAdapter(
    private val onItemClick: (AppItem) -> Unit,
    private val onItemLongClick: (AppItem) -> Unit,
    private val onItemFocused: ((AppItem, Boolean) -> Unit)? = null
) : RecyclerView.Adapter<AppGridAdapter.AppViewHolder>() {

    private val items = mutableListOf<AppItem>()

    fun submitList(newItems: List<AppItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val binding = ItemAppTileBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AppViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class AppViewHolder(private val binding: ItemAppTileBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            // Attach ATV Launcher Pro style scale & glow animation on D-pad focus
            DpadFocusHelper.attachFocusAnimation(binding.cardView) { hasFocus ->
                // Enable marquee on label only when focused
                binding.tvAppLabel.isSelected = hasFocus
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION && position < items.size) {
                    onItemFocused?.invoke(items[position], hasFocus)
                }
            }

            // Normal click (D-pad Center / Select)
            binding.cardView.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(items[position])
                }
            }

            // Long press click (Hold Center / Select)
            binding.cardView.setOnLongClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemLongClick(items[position])
                    true
                } else {
                    false
                }
            }

            // FireTV remote Menu button key listener
            binding.cardView.setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_MENU && event.action == KeyEvent.ACTION_DOWN) {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        onItemLongClick(items[position])
                        return@setOnKeyListener true
                    }
                }
                false
            }
        }

        fun bind(app: AppItem) {
            binding.ivAppIcon.setImageDrawable(app.icon)
            binding.tvAppLabel.text = app.label
        }
    }
}
