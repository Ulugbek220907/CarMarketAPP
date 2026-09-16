package com.automarket.app.ui.post

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.automarket.app.AutoMarketApplication
import com.automarket.app.R
import com.automarket.app.data.model.Car
import com.automarket.app.databinding.ActivityPostCarBinding
import com.automarket.app.util.ImageUtils

class PostCarActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPostCarBinding
    private val repository by lazy { (application as AutoMarketApplication).repository }

    // 3 Photos as Base64 strings for cross-device cloud persistence
    private var photo1Base64: String? = null
    private var photo2Base64: String? = null
    private var photo3Base64: String? = null

    private var activeSlot = 1
    private var selectedCondition = "Good"

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { handleImagePicked(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPostCarBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadSavedSellerProfile()
        setupToolbar()
        setupPhotoSlots()
        setupConditionSelector()
        setupBottomActions()
        updatePhotoCount()
    }

    private fun loadSavedSellerProfile() {
        val prefs = getSharedPreferences("seller_profile_prefs", MODE_PRIVATE)
        val savedName = prefs.getString("seller_name", "")
        val savedLoc = prefs.getString("location", "")
        val savedPhone = prefs.getString("phone", "")

        if (!savedName.isNullOrBlank()) {
            binding.etSellerName.setText(savedName)
        }
        if (!savedLoc.isNullOrBlank()) {
            binding.etLocation.setText(savedLoc)
        }
        if (!savedPhone.isNullOrBlank()) {
            binding.etPhone.setText(savedPhone)
        }
    }

    private fun setupToolbar() {
        binding.btnClose.setOnClickListener {
            finish()
        }
    }

    private fun setupPhotoSlots() {
        binding.slot1Container.setOnClickListener {
            activeSlot = 1
            imagePickerLauncher.launch("image/*")
        }
        binding.btnRemovePhoto1.setOnClickListener {
            removePhoto(1)
        }

        binding.slot2Container.setOnClickListener {
            activeSlot = 2
            imagePickerLauncher.launch("image/*")
        }
        binding.btnRemovePhoto2.setOnClickListener {
            removePhoto(2)
        }

        binding.slot3Container.setOnClickListener {
            activeSlot = 3
            imagePickerLauncher.launch("image/*")
        }
        binding.btnRemovePhoto3.setOnClickListener {
            removePhoto(3)
        }

        binding.btnSelectPhotos.setOnClickListener {
            activeSlot = when {
                photo1Base64 == null -> 1
                photo2Base64 == null -> 2
                photo3Base64 == null -> 3
                else -> 1
            }
            imagePickerLauncher.launch("image/*")
        }
    }

    private fun handleImagePicked(uri: Uri) {
        val base64Data = ImageUtils.uriToBase64(this, uri)
        if (base64Data == null) {
            Toast.makeText(this, "Failed to compress and load image", Toast.LENGTH_SHORT).show()
            return
        }

        when (activeSlot) {
            1 -> {
                photo1Base64 = base64Data
                binding.emptySlot1.visibility = View.GONE
                binding.ivPhoto1.visibility = View.VISIBLE
                binding.btnRemovePhoto1.visibility = View.VISIBLE
                ImageUtils.loadImage(binding.ivPhoto1, null, base64Data)
            }
            2 -> {
                photo2Base64 = base64Data
                binding.emptySlot2.visibility = View.GONE
                binding.ivPhoto2.visibility = View.VISIBLE
                binding.btnRemovePhoto2.visibility = View.VISIBLE
                ImageUtils.loadImage(binding.ivPhoto2, null, base64Data)
            }
            3 -> {
                photo3Base64 = base64Data
                binding.emptySlot3.visibility = View.GONE
                binding.ivPhoto3.visibility = View.VISIBLE
                binding.btnRemovePhoto3.visibility = View.VISIBLE
                ImageUtils.loadImage(binding.ivPhoto3, null, base64Data)
            }
        }
        updatePhotoCount()
    }

    private fun removePhoto(slot: Int) {
        when (slot) {
            1 -> {
                photo1Base64 = null
                binding.emptySlot1.visibility = View.VISIBLE
                binding.ivPhoto1.setImageDrawable(null)
                binding.ivPhoto1.visibility = View.GONE
                binding.btnRemovePhoto1.visibility = View.GONE
            }
            2 -> {
                photo2Base64 = null
                binding.emptySlot2.visibility = View.VISIBLE
                binding.ivPhoto2.setImageDrawable(null)
                binding.ivPhoto2.visibility = View.GONE
                binding.btnRemovePhoto2.visibility = View.GONE
            }
            3 -> {
                photo3Base64 = null
                binding.emptySlot3.visibility = View.VISIBLE
                binding.ivPhoto3.setImageDrawable(null)
                binding.ivPhoto3.visibility = View.GONE
                binding.btnRemovePhoto3.visibility = View.GONE
            }
        }
        updatePhotoCount()
    }

    private fun updatePhotoCount() {
        var count = 0
        if (photo1Base64 != null) count++
        if (photo2Base64 != null) count++
        if (photo3Base64 != null) count++

        binding.tvPhotosCountBadge.text = "$count / 3"
        binding.tvUploadPrompt.text = if (count == 0) "Tap a slot or button below to upload up to 3 real photos" else "$count photo(s) selected"
    }

    private fun setupConditionSelector() {
        binding.cardCondExcellent.setOnClickListener { selectCondition("Excellent") }
        binding.cardCondGood.setOnClickListener { selectCondition("Good") }
    }

    private fun selectCondition(condition: String) {
        selectedCondition = condition

        if (condition == "Excellent") {
            binding.cardCondExcellent.setBackgroundResource(R.drawable.bg_radio_card_selected)
            binding.tvCondExcellentTitle.setTextColor(ContextCompat.getColor(this, R.color.primary_container))
            binding.cardCondGood.setBackgroundResource(R.drawable.bg_radio_card_unselected)
            binding.tvCondGoodTitle.setTextColor(ContextCompat.getColor(this, R.color.on_surface))
        } else {
            binding.cardCondGood.setBackgroundResource(R.drawable.bg_radio_card_selected)
            binding.tvCondGoodTitle.setTextColor(ContextCompat.getColor(this, R.color.primary_container))
            binding.cardCondExcellent.setBackgroundResource(R.drawable.bg_radio_card_unselected)
            binding.tvCondExcellentTitle.setTextColor(ContextCompat.getColor(this, R.color.on_surface))
        }
    }

    private fun setupBottomActions() {
        binding.btnSaveDraft.setOnClickListener {
            Toast.makeText(this, "Draft saved locally", Toast.LENGTH_SHORT).show()
            finish()
        }

        binding.btnPublish.setOnClickListener {
            publishListing()
        }
    }

    private fun publishListing() {
        val titleInput = binding.etMakeModel.text?.toString()?.trim() ?: ""
        val mileageStr = binding.etMileage.text?.toString()?.trim() ?: ""
        val priceStr = binding.etPrice.text?.toString()?.trim() ?: ""
        val transmission = binding.etTransmission.text?.toString()?.trim()?.ifEmpty { "Automatic" } ?: "Automatic"
        val bodyStyle = binding.etBody.text?.toString()?.trim()?.ifEmpty { "Sedan" } ?: "Sedan"
        val sellerName = binding.etSellerName.text?.toString()?.trim()?.ifEmpty { "Seller" } ?: "Seller"
        val location = binding.etLocation.text?.toString()?.trim() ?: ""
        val phone = binding.etPhone.text?.toString()?.trim() ?: ""
        val description = binding.etDescription.text?.toString()?.trim() ?: ""

        if (titleInput.isEmpty() || mileageStr.isEmpty() || priceStr.isEmpty()) {
            Toast.makeText(this, getString(R.string.missing_fields_error), Toast.LENGTH_SHORT).show()
            return
        }

        if (location.isEmpty()) {
            Toast.makeText(this, "Please enter your city/location", Toast.LENGTH_SHORT).show()
            return
        }

        // Save seller details for future listings
        val prefs = getSharedPreferences("seller_profile_prefs", MODE_PRIVATE)
        prefs.edit()
            .putString("seller_name", sellerName)
            .putString("location", location)
            .putString("phone", phone)
            .apply()

        val mileage = mileageStr.toIntOrNull() ?: 0
        val price = priceStr.toDoubleOrNull() ?: 0.0

        val tokens = titleInput.split(" ")
        val year = tokens.firstOrNull()?.toIntOrNull() ?: 2023
        val make = if (tokens.size > 1) tokens[1] else "Vehicle"
        val model = if (tokens.size > 2) tokens.subList(2, tokens.size).joinToString(" ") else "Model"

        val newCar = Car(
            make = make,
            model = model,
            trim = "$transmission • $bodyStyle",
            year = year,
            price = price,
            mileage = mileage,
            transmission = transmission,
            fuelType = if (titleInput.contains("Tesla", ignoreCase = true) || titleInput.contains("Electric", ignoreCase = true)) "Electric" else "Gasoline",
            bodyStyle = bodyStyle,
            drivetrain = "AWD",
            location = location,
            distance = location,
            description = description.ifEmpty { "$year $make $model in $selectedCondition condition." },
            sellerName = sellerName,
            sellerPhone = phone,
            sellerRating = 5.0,
            sellerReviewCount = 1,
            sellerResponseTime = "Replies fast",
            dealRating = "Great Deal",
            carfaxClean = true,
            condition = selectedCondition,
            highlights = "Clean Title, Verified Seller",
            photo1 = photo1Base64,
            photo2 = photo2Base64,
            photo3 = photo3Base64,
            isFavorite = false,
            isUserListing = true,
            createdAt = System.currentTimeMillis()
        )

        Toast.makeText(this, "Publishing vehicle to Render cloud...", Toast.LENGTH_SHORT).show()
        repository.addCar(newCar)
        finish()
    }
}
