package com.example.mapapp

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.mapapp.databinding.ItemRvBinding

class AdapterPlace(private val list: ArrayList<String>) :
    RecyclerView.Adapter<AdapterPlace.PlaceViewHolder>() {

    var onClick: ((String) -> Unit)? = null

    inner class PlaceViewHolder(private val binding: ItemRvBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun onBind(value: String) {
            binding.tvName.text = value
            itemView.setOnClickListener {
                onClick?.invoke(absoluteAdapterPosition.toString())
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaceViewHolder {
        return PlaceViewHolder(
            ItemRvBinding.inflate(LayoutInflater.from(parent.context),
                parent,
                false
            ))
    }

    override fun onBindViewHolder(holder: PlaceViewHolder, position: Int) {
        return holder.onBind(list[position])
    }

    override fun getItemCount(): Int = list.size

}