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

    // 3 Photos paths
    private var photo1Path: String? = null
    private var photo2Path: String? = null
    private var photo3Path: String? = null

    // Active slot
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

        setupToolbar()
        setupPhotoSlots()
        setupAutofill()
        setupConditionSelector()
        setupBottomActions()
        updatePhotoCount()
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
            // Find next empty slot
            activeSlot = when {
                photo1Path == null -> 1
                photo2Path == null -> 2
                photo3Path == null -> 3
                else -> 1
            }
            imagePickerLauncher.launch("image/*")
        }
    }

    private fun handleImagePicked(uri: Uri) {
        val savedPath = ImageUtils.saveImageToInternalStorage(this, uri)
        if (savedPath == null) {
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
            return
        }

        when (activeSlot) {
            1 -> {
                photo1Path = savedPath
                binding.emptySlot1.visibility = View.GONE
                binding.ivPhoto1.visibility = View.VISIBLE
                binding.btnRemovePhoto1.visibility = View.VISIBLE
                ImageUtils.loadImage(binding.ivPhoto1, null, savedPath)
            }
            2 -> {
                photo2Path = savedPath
                binding.emptySlot2.visibility = View.GONE
                binding.ivPhoto2.visibility = View.VISIBLE
                binding.btnRemovePhoto2.visibility = View.VISIBLE
                ImageUtils.loadImage(binding.ivPhoto2, null, savedPath)
            }
            3 -> {
                photo3Path = savedPath
                binding.emptySlot3.visibility = View.GONE
                binding.ivPhoto3.visibility = View.VISIBLE
                binding.btnRemovePhoto3.visibility = View.VISIBLE
                ImageUtils.loadImage(binding.ivPhoto3, null, savedPath)
            }
        }
        updatePhotoCount()
    }

    private fun removePhoto(slot: Int) {
        when (slot) {
            1 -> {
                photo1Path = null
                binding.emptySlot1.visibility = View.VISIBLE
                binding.ivPhoto1.setImageDrawable(null)
                binding.ivPhoto1.visibility = View.GONE
                binding.btnRemovePhoto1.visibility = View.GONE
            }
            2 -> {
                photo2Path = null
                binding.emptySlot2.visibility = View.VISIBLE
                binding.ivPhoto2.setImageDrawable(null)
                binding.ivPhoto2.visibility = View.GONE
                binding.btnRemovePhoto2.visibility = View.GONE
            }
            3 -> {
                photo3Path = null
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
        if (photo1Path != null) count++
        if (photo2Path != null) count++
        if (photo3Path != null) count++
        binding.tvPhotosCountBadge.text = "$count/3 Added"
    }

    private fun setupAutofill() {
        binding.btnAutofillDemo.setOnClickListener {
            binding.etMakeModel.setText("2020 Honda Accord Sport 2.0T")
            binding.etMileage.setText("42100")
            binding.etTransmission.setText("10-Speed Automatic")
            binding.etBody.setText("Sedan")
            binding.etLocation.setText("Austin, TX")
            binding.etPrice.setText("23900")
            binding.etDescription.setText("Single owner Honda Accord Sport 2.0T with clean CARFAX and complete maintenance records.")
            Toast.makeText(this, "Autofill applied from registry scan", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupConditionSelector() {
        binding.cardCondExcellent.setOnClickListener {
            selectedCondition = "Excellent"
            binding.cardCondExcellent.setBackgroundResource(R.drawable.bg_radio_card_selected)
            binding.cardCondGood.setBackgroundResource(R.drawable.bg_radio_card_unselected)
            binding.tvCondExcellentTitle.setTextColor(ContextCompat.getColor(this, R.color.primary_container))
            binding.tvCondGoodTitle.setTextColor(ContextCompat.getColor(this, R.color.on_surface))
        }

        binding.cardCondGood.setOnClickListener {
            selectedCondition = "Good"
            binding.cardCondGood.setBackgroundResource(R.drawable.bg_radio_card_selected)
            binding.cardCondExcellent.setBackgroundResource(R.drawable.bg_radio_card_unselected)
            binding.tvCondGoodTitle.setTextColor(ContextCompat.getColor(this, R.color.primary_container))
            binding.tvCondExcellentTitle.setTextColor(ContextCompat.getColor(this, R.color.on_surface))
        }
    }

    private fun setupBottomActions() {
        binding.btnSaveDraft.setOnClickListener {
            Toast.makeText(this, "Listing saved as draft", Toast.LENGTH_SHORT).show()
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
        val location = binding.etLocation.text?.toString()?.trim()?.ifEmpty { "Austin, TX" } ?: "Austin, TX"
        val phone = binding.etPhone.text?.toString()?.trim()?.ifEmpty { "+1 (512) 555-0192" } ?: "+1 (512) 555-0192"
        val description = binding.etDescription.text?.toString()?.trim() ?: ""

        if (titleInput.isEmpty() || mileageStr.isEmpty() || priceStr.isEmpty()) {
            Toast.makeText(this, getString(R.string.missing_fields_error), Toast.LENGTH_SHORT).show()
            return
        }

        val mileage = mileageStr.toIntOrNull() ?: 0
        val price = priceStr.toDoubleOrNull() ?: 0.0

        // Parse year, make, model from input
        val tokens = titleInput.split(" ")
        val year = tokens.firstOrNull()?.toIntOrNull() ?: 2022
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
            distance = "0 miles away",
            description = description.ifEmpty { "Pristine $titleInput with verified clean title and excellent condition." },
            sellerName = "Alex (You)",
            sellerPhone = phone,
            sellerRating = 5.0,
            sellerReviewCount = 1,
            sellerResponseTime = "Replies < 5 mins",
            dealRating = "Great Deal",
            carfaxClean = true,
            condition = selectedCondition,
            highlights = "Clean Title, Verified Seller, Fresh Inspection",
            photo1 = photo1Path ?: "sample_accord_1",
            photo2 = photo2Path ?: "sample_accord_2",
            photo3 = photo3Path ?: "sample_accord_3",
            isFavorite = false,
            isUserListing = true,
            createdAt = System.currentTimeMillis()
        )

        repository.addCar(newCar)
        Toast.makeText(this, getString(R.string.listing_created_success), Toast.LENGTH_SHORT).show()
        finish()
    }
}
