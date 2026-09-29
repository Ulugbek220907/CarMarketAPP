package com.automarket.app.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.automarket.app.R
import com.automarket.app.data.model.Car
import com.automarket.app.databinding.ItemCarCardBinding
import com.automarket.app.util.ImageUtils

class CarAdapter(
    private val onCarClick: (Car) -> Unit,
    private val onBookmarkClick: (Car, Boolean) -> Unit
) : ListAdapter<Car, CarAdapter.CarViewHolder>(CarDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarViewHolder {
        val binding = ItemCarCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CarViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CarViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CarViewHolder(private val binding: ItemCarCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(car: Car) {
            binding.tvCarTitle.text = car.displayTitle
            binding.tvPrice.text = car.formattedPrice
            binding.tvMonthlyEstimate.text = car.monthlyEstimate
            val shortMileage = if (car.mileage >= 1000) "${car.mileage / 1000}k mi" else "${car.mileage} mi"
            binding.tvChipMileage.text = shortMileage
            binding.tvChipTransmission.text = car.transmission.take(10)
            binding.tvChipBody.text = car.bodyStyle
            binding.tvCarSpecs.text = "${car.make} ${car.model}"
            binding.tvLocation.text = if (car.location.isNotBlank()) car.location else "Available"
            binding.tvSellerName.text = if (car.sellerName.isNotBlank()) "★ 4.9 • ${car.sellerName}" else "Verified Seller"

            val count = car.photoCount
            binding.tvPhotoCount.text = "$count"
            binding.photoBadgeContainer.visibility = if (count > 0) View.VISIBLE else View.GONE

            // Deal Badge: Highlight good value
            if (car.price in 1.0..35000.0) {
                binding.badgeDealContainer.visibility = View.VISIBLE
                binding.tvBadgeDeal.text = "Great Deal"
            } else if (car.price > 70000.0) {
                binding.badgeDealContainer.visibility = View.VISIBLE
                binding.tvBadgeDeal.text = "Verified Luxury"
            } else {
                binding.badgeDealContainer.visibility = View.VISIBLE
                binding.tvBadgeDeal.text = "Verified"
            }

            ImageUtils.loadImage(
                imageView = binding.ivCarImage,
                placeholderView = binding.placeholderContainer,
                pathOrUri = car.primaryPhotoUrl
            )

            updateBookmarkIcon(car.isFavorite)

            binding.btnBookmark.setOnClickListener {
                val newStatus = !car.isFavorite
                updateBookmarkIcon(newStatus)
                onBookmarkClick(car, newStatus)
            }

            binding.root.setOnClickListener {
                onCarClick(car)
            }
        }

        private fun updateBookmarkIcon(isFav: Boolean) {
            if (isFav) {
                binding.ivBookmark.setImageResource(R.drawable.ic_bookmark_filled)
                binding.ivBookmark.setColorFilter(
                    ContextCompat.getColor(binding.root.context, R.color.deal_emerald)
                )
            } else {
                binding.ivBookmark.setImageResource(R.drawable.ic_bookmark)
                binding.ivBookmark.setColorFilter(
                    ContextCompat.getColor(binding.root.context, R.color.white)
                )
            }
        }
    }

    class CarDiffCallback : DiffUtil.ItemCallback<Car>() {
        override fun areItemsTheSame(oldItem: Car, newItem: Car): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Car, newItem: Car): Boolean {
            return oldItem == newItem
        }
    }
}
