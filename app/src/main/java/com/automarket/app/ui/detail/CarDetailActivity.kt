package com.automarket.app.ui.detail

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
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
import java.util.Locale

class CarDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CAR_ID = "extra_car_id"

        fun start(context: Context, carId: String) {
            val intent = Intent(context, CarDetailActivity::class.java).apply {
                putExtra(EXTRA_CAR_ID, carId)
            }
            context.startActivity(intent)
        }
    }

    private lateinit var binding: ActivityCarDetailBinding
    private val repository by lazy { (application as AutoMarketApplication).repository }
    private var carId: String = ""
    private var currentCar: Car? = null

    private val updateListener: () -> Unit = {
        loadCarDetails()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCarDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        carId = intent.getStringExtra(EXTRA_CAR_ID).orEmpty()
        if (carId.isEmpty()) {
            Toast.makeText(this, "Vehicle not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        repository.addUpdateListener(updateListener)
        loadCarDetails()
        setupListeners()
    }

    override fun onDestroy() {
        super.onDestroy()
        repository.removeUpdateListener(updateListener)
    }

    private fun loadCarDetails() {
        val car = repository.getCarById(carId)
        if (car == null) {
            return
        }
        currentCar = car

        // Title, Price & Location
        binding.tvCarTitle.text = car.displayTitle
        binding.tvPrice.text = car.formattedPrice
        binding.tvMonthlyEstimate.text = car.monthlyEstimate
        binding.tvLocation.text = car.location.ifBlank { "Location not specified" }

        // Genuine Vehicle Specifications Grid
        binding.tvSpecYear.text = car.year.toString()
        binding.tvSpecMileage.text = car.formattedMileage
        binding.tvSpecTransmission.text = car.transmission.ifBlank { "Automatic" }
        binding.tvSpecBodyStyle.text = car.bodyStyle.ifBlank { "Sedan" }

        // Description Overview
        binding.tvDescription.text = car.description.ifBlank { "No additional details provided." }

        // Seller Information & Bottom Action Context
        val currentUid = repository.getCurrentUserUid()
        val isUser = car.isUserListing || (currentUid.isNotEmpty() && car.sellerUid == currentUid)

        if (isUser) {
            binding.tvSellerName.text = "${car.sellerName.ifBlank { "You" }} (Your Listing)"
            binding.tvSellerPhone.text = car.sellerPhone.ifBlank { "Owner listing" }
            binding.btnCallSeller.visibility = View.GONE
            binding.btnContactSeller.text = "View Inquiries & Offers"
        } else {
            binding.tvSellerName.text = car.sellerName.ifBlank { "Verified Private Seller" }
            binding.tvSellerPhone.text = car.sellerPhone.ifBlank { "Direct in-app messaging" }
            binding.btnCallSeller.visibility = View.VISIBLE
            binding.btnContactSeller.text = getString(R.string.action_message)
        }
        binding.tvSellerLocation.text = "Location: ${car.location.ifBlank { "Austin, TX" }}"

        // Photos Slider
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
        binding.btnDeleteListing.visibility = if (isUser) View.VISIBLE else View.GONE
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnShare.setOnClickListener {
            currentCar?.let { car ->
                val shareText = "Check out this ${car.displayTitle} for ${car.formattedPrice} on AutoMarket!\n" +
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
                val newStatus = repository.toggleFavorite(car.id)
                currentCar = car.copy(isFavorite = newStatus)
                updateBookmarkIcon(newStatus)
                val msg = if (newStatus) "Saved to favorites" else "Removed from favorites"
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        }

        // Call Seller button
        binding.btnCallSeller.setOnClickListener {
            currentCar?.let { car ->
                val phone = car.sellerPhone.trim()
                if (phone.isNotBlank()) {
                    try {
                        val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:$phone")
                        }
                        startActivity(dialIntent)
                    } catch (e: Exception) {
                        Toast.makeText(this, "Unable to place call on this device", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Seller phone number not available", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Chat & Offer button
        binding.btnContactSeller.setOnClickListener {
            currentCar?.let { car ->
                ChatOffersActivity.start(this, car.id)
            }
        }

        binding.llMonthlyEstimate.setOnClickListener {
            currentCar?.let { car -> showFinancingCalculatorDialog(car) }
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

    private fun showFinancingCalculatorDialog(car: Car) {
        val price = car.price
        val downPayment = price * 0.10
        val loanAmount = price - downPayment
        val termMonths = 72
        val apr = 5.49
        val monthly = if (termMonths > 0) (price * 1.15) / termMonths else 0.0

        val downPaymentStr = String.format(Locale.US, "%,.0f", downPayment)
        val loanAmountStr = String.format(Locale.US, "%,.0f", loanAmount)
        val monthlyStr = String.format(Locale.US, "%,.0f", monthly)

        val message = "Vehicle: " + car.displayTitle + "\n" +
            "Listing Price: " + car.formattedPrice + "\n\n" +
            "• Down Payment (10%): $" + downPaymentStr + "\n" +
            "• Estimated Loan Amount: $" + loanAmountStr + "\n" +
            "• Term Length: " + termMonths + " months\n" +
            "• Estimated APR: " + apr + "%\n\n" +
            "Estimated Monthly Payment: $" + monthlyStr + "/mo*\n\n" +
            "*Estimates based on tier-1 credit. Taxes, titles, and registration fees may vary by state."

        AlertDialog.Builder(this)
            .setTitle("Financing & Payment Calculator")
            .setMessage(message)
            .setPositiveButton("Get Pre-Qualified") { _, _ ->
                Toast.makeText(this, "Pre-qualification request sent to DriveMarket Financing Partners", Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("Close", null)
            .show()
    }
}
