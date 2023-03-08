package com.example.songbook.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.songbook.data.Band
import com.example.songbook.data.relations.BandWithSongs
import com.example.songbook.databinding.BandItemBinding

class UserHomeBandsListAdapter (private val listener: OnItemClickListener)
    : ListAdapter<BandWithSongs, UserHomeBandsListAdapter.UserBandsViewHolder>(DiffBandsCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserBandsViewHolder {
        val binding = BandItemBinding.inflate(LayoutInflater.from(parent.context), parent,false)
        return UserBandsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserBandsViewHolder, position: Int) {
        val currentItem = getItem(position)
        holder.bind(currentItem)
    }

    inner class UserBandsViewHolder(private val binding: BandItemBinding) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.apply {
                root.setOnClickListener {
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val band = getItem(position)
                        listener.onItemClick(band)
                    }
                }
            }
        }

        fun bind(bandWithSongs: BandWithSongs) {
            binding.textViewBandItem.text = bandWithSongs.band.bandName
        }
    }

    interface OnItemClickListener {
        fun onItemClick(bandWithSongs: BandWithSongs)
    }

    class DiffBandsCallback : DiffUtil.ItemCallback<BandWithSongs>() {
        override fun areItemsTheSame(oldItem: BandWithSongs, newItem: BandWithSongs) =
            oldItem.band.bandName == newItem.band.bandName

        override fun areContentsTheSame(oldItem: BandWithSongs, newItem: BandWithSongs) =
            oldItem == newItem
    }

}