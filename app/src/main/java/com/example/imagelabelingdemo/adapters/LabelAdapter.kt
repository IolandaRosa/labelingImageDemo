package com.example.imagelabelingdemo.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.imagelabelingdemo.R
import com.example.imagelabelingdemo.models.Label
import kotlin.math.roundToInt

class LabelAdapter(var labels: ArrayList<Label>) : RecyclerView.Adapter<LabelAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent)
    }

    override fun getItemCount(): Int {
        return labels.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = labels[position]
        holder.bind(item)
    }

    class ViewHolder private constructor(view: View) : RecyclerView.ViewHolder(view) {
        private val label: TextView = view.findViewById(R.id.labelTextView)
        private val conf: TextView = view.findViewById(R.id.confidenceTextView)

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val view = layoutInflater.inflate(R.layout.list_item, parent, false)
                return ViewHolder(view)
            }
        }

        fun bind(
            item: Label
        ) {
            label.text = item.label
            conf.text = "${(item.confidence * 100).roundToInt()} %"
        }
    }
}