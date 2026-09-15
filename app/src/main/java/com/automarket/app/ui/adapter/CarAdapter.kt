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
            binding.tvCarSubtitle.text = car.displaySubtitle
            binding.tvPrice.text = car.formattedPrice
            binding.tvMonthlyPayment.text = car.formattedMonthlyPayment

            // Deal Rating Badge
            if (car.dealRating.isNotBlank()) {
                binding.tvDealRating.text = car.dealRating
                binding.dealBadgeContainer.visibility = View.VISIBLE
            } else {
                binding.dealBadgeContainer.visibility = View.GONE
            }

            // Specs Matrix
            binding.tvSpecMileage.text = car.formattedMileage
            binding.tvSpecDrive.text = if (car.drivetrain.isNotBlank()) car.drivetrain else car.transmission
            binding.tvSpecFuel.text = car.fuelType

            // Distance & Rating
            binding.tvDistance.text = car.distance
            binding.tvRating.text = "${car.sellerRating}"
            binding.tvReviews.text = "(${car.sellerReviewCount})"

            // Photo count & Image
            val photos = car.getPhotos()
            val count = if (photos.isNotEmpty()) photos.size else 3
            binding.tvPhotoCount.text = "$count"

            ImageUtils.loadImage(
                imageView = binding.ivCarImage,
                placeholderView = binding.placeholderContainer,
                pathOrUri = photos.firstOrNull()
            )

            // Bookmark icon
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
                    ContextCompat.getColor(binding.root.context, R.color.on_surface)
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
