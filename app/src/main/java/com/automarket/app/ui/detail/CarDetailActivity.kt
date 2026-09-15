package com.automarket.app.ui.detail

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.automarket.app.AutoMarketApplication
import com.automarket.app.R
import com.automarket.app.data.model.Car
import com.automarket.app.databinding.ActivityCarDetailBinding
import com.automarket.app.ui.chat.ChatOffersActivity

class CarDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CAR_ID = "extra_car_id"

        fun start(context: Context, carId: Long) {
            val intent = Intent(context, CarDetailActivity::class.java)
            intent.putExtra(EXTRA_CAR_ID, carId)
            context.startActivity(intent)
        }
    }

    private lateinit var binding: ActivityCarDetailBinding
    private val repository by lazy { (application as AutoMarketApplication).repository }
    private var carId: Long = -1L
    private var currentCar: Car? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCarDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        carId = intent.getLongExtra(EXTRA_CAR_ID, -1L)
        if (carId == -1L) {
            Toast.makeText(this, "Vehicle not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadCarDetails()
        setupListeners()
    }

    private fun loadCarDetails() {
        val car = repository.getCarById(carId)
        if (car == null) {
            Toast.makeText(this, "Vehicle not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        currentCar = car

        // Title & Price Section
        binding.tvCarTitle.text = car.displayTitle
        binding.tvCarSubtitle.text = car.displaySubtitle
        binding.tvPrice.text = car.formattedPrice
        binding.tvMonthlyEst.text = car.formattedMonthlyPayment
        binding.tvDealTag.text = car.dealRating

        // Quick Specs Bento Grid
        binding.tvBentoMileage.text = car.formattedMileage
        binding.tvBentoTrans.text = car.transmission
        binding.tvBentoDrive.text = if (car.drivetrain.isNotBlank()) car.drivetrain else "All-Wheel Drive"
        binding.tvBentoFuel.text = "${car.fuelType} Drive"

        // Dynamic Highlights & Features
        binding.containerHighlights.removeAllViews()
        val highlights = car.getHighlightsList()
        for (item in highlights) {
            val pill = TextView(this).apply {
                text = "•  $item"
                textSize = 13f
                setTextColor(ContextCompat.getColor(context, R.color.on_surface))
                setBackgroundResource(R.drawable.bg_chip_spec)
                setPadding(28, 14, 28, 14)
            }
            val lp = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 8
            }
            binding.containerHighlights.addView(pill, lp)
        }

        // Description Overview
        binding.tvDescription.text = car.description

        // Seller Profile Card
        binding.tvSellerName.text = car.sellerName
        binding.tvSellerRating.text = "${car.sellerRating}"
        binding.tvSellerSales.text = "${car.sellerReviewCount} sales"
        binding.tvSellerResponse.text = car.sellerResponseTime
        binding.tvSellerLocation.text = car.location

        // ViewPager2 photo slider
        val photos = car.getPhotos()
        val adapter = PhotoSliderAdapter(photos)
        binding.viewPagerPhotos.adapter = adapter

        if (photos.isNotEmpty()) {
            binding.tvPhotoIndicator.text = "1 / ${photos.size}"
            binding.viewPagerPhotos.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    binding.tvPhotoIndicator.text = "${position + 1} / ${photos.size}"
                }
            })
        } else {
            binding.tvPhotoIndicator.text = "1 / 1"
        }

        // Bookmark icon state
        updateBookmarkIcon(car.isFavorite)

        // Delete button for user listings
        binding.btnDeleteListing.visibility = if (car.isUserListing) View.VISIBLE else View.GONE
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnShare.setOnClickListener {
            currentCar?.let { car ->
                val shareText = "Check out this ${car.displayTitle} for ${car.formattedPrice} on DriveMarket!\n" +
                        "Mileage: ${car.formattedMileage} | Location: ${car.location}\n" +
                        "Contact Seller: ${car.sellerName} (${car.sellerPhone})"
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, shareText)
                    type = "text/plain"
                }
                startActivity(Intent.createChooser(sendIntent, "Share Vehicle"))
            }
        }

        binding.btnBookmark.setOnClickListener {
            currentCar?.let { car ->
                val newStatus = !car.isFavorite
                repository.toggleFavorite(car.id)
                currentCar = car.copy(isFavorite = newStatus)
                updateBookmarkIcon(newStatus)
                val msg = if (newStatus) "Saved to favorites" else "Removed from favorites"
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        }

        binding.btn360View.setOnClickListener {
            Toast.makeText(this, "Interactive 360° vehicle view ready", Toast.LENGTH_SHORT).show()
        }

        // Contact Seller & Make Offer both seamlessly launch ChatOffersActivity!
        binding.btnContactSeller.setOnClickListener {
            currentCar?.let { car ->
                ChatOffersActivity.start(this, car)
            }
        }

        binding.btnMakeOffer.setOnClickListener {
            currentCar?.let { car ->
                ChatOffersActivity.start(this, car)
            }
        }

        binding.btnDeleteListing.setOnClickListener {
            showDeleteConfirmation()
        }
    }

    private fun updateBookmarkIcon(isFav: Boolean) {
        if (isFav) {
            binding.btnBookmark.setImageResource(R.drawable.ic_bookmark_filled)
            binding.btnBookmark.setColorFilter(
                ContextCompat.getColor(this, R.color.deal_emerald)
            )
        } else {
            binding.btnBookmark.setImageResource(R.drawable.ic_bookmark)
            binding.btnBookmark.setColorFilter(
                ContextCompat.getColor(this, R.color.on_surface_variant)
            )
        }
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.confirm_delete_title))
            .setMessage(getString(R.string.confirm_delete_msg))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                repository.deleteCar(carId)
                Toast.makeText(this, "Listing deleted successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
}
