package com.automarket.app.ui.detail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.automarket.app.databinding.ItemPhotoSliderBinding
import com.automarket.app.util.ImageUtils

class PhotoSliderAdapter(
    private val photos: List<String>
) : RecyclerView.Adapter<PhotoSliderAdapter.PhotoViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val binding = ItemPhotoSliderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PhotoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.bind(photos.getOrNull(position))
    }

    override fun getItemCount(): Int {
        return if (photos.isEmpty()) 1 else photos.size
    }

    inner class PhotoViewHolder(private val binding: ItemPhotoSliderBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(photoPathOrUri: String?) {
            ImageUtils.loadImage(
                imageView = binding.ivSliderImage,
                placeholderView = binding.sliderPlaceholder,
                pathOrUri = photoPathOrUri,
                targetWidth = 1080,
                targetHeight = 720
            )
        }
    }
}
