package com.automarket.app.ui.chat

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.automarket.app.AutoMarketApplication
import com.automarket.app.R
import com.automarket.app.data.model.Car
import com.automarket.app.data.model.ChatMessage
import com.automarket.app.databinding.ActivityChatOffersBinding
import com.automarket.app.util.ImageUtils
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch

class ChatOffersActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CAR_ID = "extra_car_id"

        fun start(context: Context, carId: String) {
            val intent = Intent(context, ChatOffersActivity::class.java).apply {
                putExtra(EXTRA_CAR_ID, carId)
            }
            context.startActivity(intent)
        }

        fun start(context: Context, car: Car) {
            start(context, car.id)
        }
    }

    private lateinit var binding: ActivityChatOffersBinding
    private val repository by lazy { (application as AutoMarketApplication).repository }
    private var car: Car? = null
    private var messagesListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatOffersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val carId = intent.getStringExtra(EXTRA_CAR_ID).orEmpty()
        if (carId.isNotEmpty()) {
            car = repository.getCarById(carId)
        }

        if (car == null) {
            lifecycleScope.launch {
                val resolved = repository.getCarById(carId)
                if (resolved != null) {
                    car = resolved
                    initViews(resolved)
                } else {
                    Toast.makeText(this@ChatOffersActivity, "Vehicle not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            return
        }

        initViews(car!!)
    }

    private fun initViews(c: Car) {
        setupHeader()
        setupPinnedListing()
        setupQuickChips()
        setupMessageDock()
        startRealtimeChatListener(c.id)
    }

    override fun onDestroy() {
        super.onDestroy()
        messagesListener?.remove()
    }

    private fun setupHeader() {
        val c = car ?: return
        val currentUid = repository.getCurrentUserUid()
        val isSeller = c.isUserListing || (currentUid.isNotEmpty() && c.sellerUid == currentUid)

        if (isSeller) {
            binding.tvSellerName.text = "Inquiries & Offers"
            binding.tvSellerStatus.text = "Buyer communication • ${c.displayTitle}"
            binding.btnCallSeller.visibility = View.GONE
        } else {
            binding.tvSellerName.text = c.sellerName.ifBlank { "Verified Seller" }
            binding.tvSellerStatus.text = if (c.sellerPhone.isNotBlank()) c.sellerPhone else "Response time ~10 min"
            binding.btnCallSeller.visibility = View.VISIBLE
            binding.btnCallSeller.setOnClickListener {
                val phone = c.sellerPhone.trim()
                if (phone.isNotBlank()) {
                    try {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                        startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(this, "Unable to place call on this device", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Seller phone number not available", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun setupPinnedListing() {
        val c = car ?: return
        binding.tvPinnedTitle.text = c.displayTitle
        binding.tvPinnedPrice.text = c.formattedPrice

        ImageUtils.loadImage(
            imageView = binding.ivListingThumb,
            placeholderView = null,
            pathOrUri = c.primaryPhotoUrl
        )

        binding.btnViewDetails.setOnClickListener {
            finish()
        }
    }

    private fun setupQuickChips() {
        val c = car ?: return
        val currentUid = repository.getCurrentUserUid()
        val isSeller = c.isUserListing || (currentUid.isNotEmpty() && c.sellerUid == currentUid)

        if (isSeller) {
            binding.chipSuggest1.text = "Vehicle available for inspection"
            binding.chipSuggest2.text = "Clean title, ready for transfer"
            binding.chipSuggest3.text = "Price is firm at listed amount"
            binding.chipSuggest4.visibility = View.GONE
            binding.btnAttach.visibility = View.GONE

            binding.chipSuggest1.setOnClickListener { sendUserMessage(binding.chipSuggest1.text.toString()) }
            binding.chipSuggest2.setOnClickListener { sendUserMessage(binding.chipSuggest2.text.toString()) }
            binding.chipSuggest3.setOnClickListener { sendUserMessage(binding.chipSuggest3.text.toString()) }
        } else {
            binding.chipSuggest1.text = getString(R.string.chip_negotiable)
            binding.chipSuggest2.text = getString(R.string.chip_test_drive)
            binding.chipSuggest3.text = getString(R.string.chip_carfax)
            binding.chipSuggest4.visibility = View.VISIBLE
            binding.btnAttach.visibility = View.VISIBLE

            binding.chipSuggest1.setOnClickListener { sendUserMessage(binding.chipSuggest1.text.toString()) }
            binding.chipSuggest2.setOnClickListener { sendUserMessage(binding.chipSuggest2.text.toString()) }
            binding.chipSuggest3.setOnClickListener { sendUserMessage(binding.chipSuggest3.text.toString()) }
            binding.chipSuggest4.setOnClickListener { showMakeOfferDialog() }
        }
    }

    private fun setupMessageDock() {
        binding.btnSend.setOnClickListener {
            val text = binding.etMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                sendUserMessage(text)
                binding.etMessage.setText("")
            }
        }

        binding.btnAttach.setOnClickListener {
            showMakeOfferDialog()
        }
    }

    private fun startRealtimeChatListener(carId: String) {
        messagesListener?.remove()
        messagesListener = repository.observeMessages(carId) { messages ->
            displayMessages(messages)
        }
    }

    private fun displayMessages(messages: List<ChatMessage>) {
        binding.llDynamicMessages.removeAllViews()

        if (messages.isEmpty()) {
            val emptyTv = TextView(this).apply {
                text = "No messages yet.\nSend a message or make an offer to start the conversation!"
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                setTextColor(ContextCompat.getColor(context, R.color.on_surface_variant))
                textSize = 13f
                setPadding(0, 32, 0, 32)
            }
            binding.llDynamicMessages.addView(emptyTv)
        } else {
            for (msg in messages) {
                renderMessage(msg)
            }
            scrollToBottom()
        }
    }

    private fun sendUserMessage(text: String) {
        val c = car ?: return
        val sender = repository.getCurrentUserName()

        lifecycleScope.launch {
            repository.sendMessage(c.id, text, sender)
        }
    }

    private fun showMakeOfferDialog() {
        val c = car ?: return
        val input = EditText(this).apply {
            hint = "e.g. 29500"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }

        AlertDialog.Builder(this)
            .setTitle("Make an Official Offer")
            .setMessage("Vehicle asking price is ${c.formattedPrice}. Enter your proposed offer in USD:")
            .setView(input)
            .setPositiveButton("Submit Offer") { _, _ ->
                val amtStr = input.text.toString().trim()
                val amt = amtStr.toDoubleOrNull()
                if (amt != null && amt > 0) {
                    val buyerName = repository.getCurrentUserName()

                    lifecycleScope.launch {
                        repository.submitOffer(c.id, amt, c.price, buyerName)
                        Toast.makeText(this@ChatOffersActivity, "Offer submitted to seller via Firestore!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Please enter a valid offer amount", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun renderMessage(msg: ChatMessage) {
        val inflater = LayoutInflater.from(this)

        when {
            msg.isSystemNotification -> {
                // Centered System Notification Bubble
                val tv = TextView(this).apply {
                    text = msg.messageText
                    textSize = 12f
                    setTextColor(ContextCompat.getColor(context, R.color.on_surface_variant))
                    setBackgroundResource(R.drawable.bg_chip_spec)
                    setPadding(32, 12, 32, 12)
                    gravity = Gravity.CENTER
                }
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.CENTER_HORIZONTAL
                    topMargin = 16
                    bottomMargin = 16
                }
                binding.llDynamicMessages.addView(tv, lp)
            }

            msg.isOfficialOffer -> {
                // Official Offer Card
                val cardView = inflater.inflate(R.layout.item_offer_card, binding.llDynamicMessages, false)
                val tvOfferBadge = cardView.findViewById<TextView>(R.id.tvOfferBadge)
                val tvOfferAmount = cardView.findViewById<TextView>(R.id.tvOfferAmount)
                val tvListPrice = cardView.findViewById<TextView>(R.id.tvListPrice)
                val tvDiscount = cardView.findViewById<TextView>(R.id.tvDiscount)
                val llOfferActions = cardView.findViewById<View>(R.id.llOfferActions)
                val btnAccept = cardView.findViewById<Button>(R.id.btnAcceptOffer)
                val btnDecline = cardView.findViewById<Button>(R.id.btnDeclineOffer)
                val llOfferStatusBuyer = cardView.findViewById<View>(R.id.llOfferStatusBuyer)
                val tvOfferStatusBuyer = cardView.findViewById<TextView>(R.id.tvOfferStatusBuyer)
                val ivOfferStatusIcon = cardView.findViewById<android.widget.ImageView>(R.id.ivOfferStatusIcon)

                val offerAmt = if (msg.offerAmount > 0) msg.offerAmount else 29800.0
                val origAmt = if (msg.originalListPrice > 0) msg.originalListPrice else (car?.price ?: 31450.0)
                val diff = origAmt - offerAmt

                tvOfferAmount.text = "$${String.format("%,.0f", offerAmt)}"
                tvListPrice.text = "$${String.format("%,.0f", origAmt)}"
                tvDiscount.text = if (diff >= 0) "-$${String.format("%,.0f", diff)} below asking" else "+$${String.format("%,.0f", -diff)} above asking"

                val currentUid = repository.getCurrentUserUid()
                val isSeller = car?.let { it.isUserListing || (currentUid.isNotEmpty() && it.sellerUid == currentUid) } ?: false

                if (isSeller) {
                    tvOfferBadge?.text = getString(R.string.official_offer_received)
                    llOfferStatusBuyer?.visibility = View.GONE
                    llOfferActions?.visibility = View.VISIBLE

                    when (msg.offerStatus) {
                        "ACCEPTED" -> {
                            btnAccept.isEnabled = false
                            btnAccept.text = "Accepted ✓"
                            btnDecline.visibility = View.GONE
                        }
                        "DECLINED" -> {
                            btnDecline.isEnabled = false
                            btnDecline.text = "Declined ✕"
                            btnAccept.visibility = View.GONE
                        }
                        else -> {
                            btnAccept.isEnabled = true
                            btnAccept.text = getString(R.string.btn_accept_offer)
                            btnDecline.visibility = View.VISIBLE
                            btnDecline.isEnabled = true

                            btnAccept.setOnClickListener {
                                lifecycleScope.launch {
                                    car?.let { c ->
                                        repository.updateOfferStatus(c.id, msg.id, "ACCEPTED")
                                    }
                                    Toast.makeText(this@ChatOffersActivity, "Offer accepted! Escrow initiated.", Toast.LENGTH_LONG).show()
                                }
                            }

                            btnDecline.setOnClickListener {
                                lifecycleScope.launch {
                                    car?.let { c ->
                                        repository.updateOfferStatus(c.id, msg.id, "DECLINED")
                                    }
                                    Toast.makeText(this@ChatOffersActivity, "Offer declined.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                } else {
                    tvOfferBadge?.text = "Official Offer Submitted"
                    llOfferActions?.visibility = View.GONE
                    llOfferStatusBuyer?.visibility = View.VISIBLE

                    when (msg.offerStatus) {
                        "ACCEPTED" -> {
                            tvOfferStatusBuyer?.text = "Offer Accepted by Seller! Escrow Initiated ✓"
                            tvOfferStatusBuyer?.setTextColor(ContextCompat.getColor(this, R.color.deal_emerald))
                            ivOfferStatusIcon?.setImageResource(R.drawable.ic_verified)
                            ivOfferStatusIcon?.setColorFilter(ContextCompat.getColor(this, R.color.deal_emerald))
                        }
                        "DECLINED" -> {
                            tvOfferStatusBuyer?.text = "Offer Declined by Seller"
                            tvOfferStatusBuyer?.setTextColor(ContextCompat.getColor(this, R.color.error))
                            ivOfferStatusIcon?.setImageResource(R.drawable.ic_close)
                            ivOfferStatusIcon?.setColorFilter(ContextCompat.getColor(this, R.color.error))
                        }
                        else -> {
                            tvOfferStatusBuyer?.text = "Pending Seller Review • Escrow Protected"
                            tvOfferStatusBuyer?.setTextColor(ContextCompat.getColor(this, R.color.on_surface))
                            ivOfferStatusIcon?.setImageResource(R.drawable.ic_policy)
                            ivOfferStatusIcon?.setColorFilter(ContextCompat.getColor(this, R.color.primary_container))
                        }
                    }
                }

                binding.llDynamicMessages.addView(cardView)
            }

            msg.isFromUser -> {
                // Buyer Message (Right-aligned, primary blue)
                val bubble = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.END
                }

                val msgContainer = LinearLayout(this).apply {
                    setBackgroundResource(R.drawable.bg_chat_bubble_buyer)
                    setPadding(36, 24, 36, 24)
                    orientation = LinearLayout.VERTICAL
                }

                val tvText = TextView(this).apply {
                    text = msg.messageText
                    setTextColor(ContextCompat.getColor(context, R.color.white))
                    textSize = 14f
                }
                msgContainer.addView(tvText)

                val tvTime = TextView(this).apply {
                    text = "${msg.formattedTime}  ✓✓"
                    setTextColor(ContextCompat.getColor(context, R.color.primary_container))
                    textSize = 11f
                    gravity = Gravity.END
                    setPadding(0, 6, 8, 16)
                }

                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.END
                    marginStart = 80
                    bottomMargin = 12
                }

                bubble.addView(msgContainer)
                bubble.addView(tvTime)
                binding.llDynamicMessages.addView(bubble, lp)
            }

            else -> {
                // Seller Message (Left-aligned, white card)
                val bubble = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.START
                }

                val msgContainer = LinearLayout(this).apply {
                    setBackgroundResource(R.drawable.bg_chat_bubble_seller)
                    setPadding(36, 24, 36, 24)
                    orientation = LinearLayout.VERTICAL
                }

                val tvText = TextView(this).apply {
                    text = msg.messageText
                    setTextColor(ContextCompat.getColor(context, R.color.on_surface))
                    textSize = 14f
                }
                msgContainer.addView(tvText)

                val tvTime = TextView(this).apply {
                    text = msg.formattedTime
                    setTextColor(ContextCompat.getColor(context, R.color.outline))
                    textSize = 11f
                    setPadding(8, 6, 0, 16)
                }

                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.START
                    marginEnd = 80
                    bottomMargin = 12
                }

                bubble.addView(msgContainer)
                bubble.addView(tvTime)
                binding.llDynamicMessages.addView(bubble, lp)
            }
        }
    }

    private fun scrollToBottom() {
        binding.scrollMessages.post {
            binding.scrollMessages.fullScroll(View.FOCUS_DOWN)
        }
    }
}
