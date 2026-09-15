package com.automarket.app.ui.chat

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.automarket.app.AutoMarketApplication
import com.automarket.app.R
import com.automarket.app.data.model.Car
import com.automarket.app.data.model.ChatMessage
import com.automarket.app.databinding.ActivityChatOffersBinding
import com.automarket.app.util.ImageUtils
import kotlinx.coroutines.launch

class ChatOffersActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_CAR = "extra_car"

        fun start(context: Context, car: Car) {
            val intent = Intent(context, ChatOffersActivity::class.java)
            intent.putExtra(EXTRA_CAR, car)
            context.startActivity(intent)
        }
    }

    private lateinit var binding: ActivityChatOffersBinding
    private val repository by lazy { (application as AutoMarketApplication).repository }
    private var car: Car? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatOffersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        @Suppress("DEPRECATION")
        car = intent.getSerializableExtra(EXTRA_CAR) as? Car
        if (car == null) {
            finish()
            return
        }

        setupHeader()
        setupPinnedListing()
        setupQuickChips()
        setupMessageDock()
        loadMessages()
    }

    private fun setupHeader() {
        val c = car ?: return
        binding.tvSellerName.text = c.sellerName
        binding.tvSellerStatus.text = c.sellerResponseTime

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnCallSeller.setOnClickListener {
            val phone = c.sellerPhone
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
            startActivity(intent)
        }
    }

    private fun setupPinnedListing() {
        val c = car ?: return
        binding.tvPinnedTitle.text = c.displayTitle
        binding.tvPinnedPrice.text = c.formattedPrice

        ImageUtils.loadImage(
            imageView = binding.ivListingThumb,
            placeholderView = null,
            pathOrUri = c.getPhotos().firstOrNull()
        )

        binding.btnViewDetails.setOnClickListener {
            finish()
        }
    }

    private fun setupQuickChips() {
        binding.chipSuggest1.setOnClickListener {
            sendUserMessage(binding.chipSuggest1.text.toString())
        }
        binding.chipSuggest2.setOnClickListener {
            sendUserMessage(binding.chipSuggest2.text.toString())
        }
        binding.chipSuggest3.setOnClickListener {
            sendUserMessage(binding.chipSuggest3.text.toString())
        }
        binding.chipSuggest4.setOnClickListener {
            sendUserMessage(binding.chipSuggest4.text.toString())
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
            Toast.makeText(this, "Attachment: Photo / Document selector", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadMessages() {
        val c = car ?: return
        val messages = repository.getMessagesForCar(c.id)
        binding.llDynamicMessages.removeAllViews()

        for (msg in messages) {
            renderMessage(msg)
        }
        scrollToBottom()

        // Sync fresh messages from Render cloud backend
        lifecycleScope.launch {
            val result = repository.refreshMessagesFromBackend(c.id)
            if (result.isSuccess) {
                val freshMessages = repository.getMessagesForCar(c.id)
                binding.llDynamicMessages.removeAllViews()
                for (msg in freshMessages) {
                    renderMessage(msg)
                }
                scrollToBottom()
            }
        }
    }

    private fun sendUserMessage(text: String) {
        val c = car ?: return
        val userMsg = ChatMessage(
            carId = c.id,
            senderName = "You",
            messageText = text,
            timestamp = System.currentTimeMillis(),
            isFromUser = true
        )
        repository.sendMessage(userMsg)
        renderMessage(userMsg)
        scrollToBottom()

        // Auto seller reply simulation after 1.2 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            val replyText = when {
                text.contains("negotiable", ignoreCase = true) ->
                    "I am open to reasonable offers close to the asking price! Feel free to submit an official offer."
                text.contains("test drive", ignoreCase = true) ->
                    "Sure! I'm available this Saturday morning around Austin South Congress. Does 11 AM work?"
                text.contains("CARFAX", ignoreCase = true) ->
                    "Clean CARFAX report is verified and ready. 1 owner with full dealer service records."
                else ->
                    "Thanks for reaching out! The ${c.displayTitle} is in great shape. Let me know if you'd like to inspect it in person."
            }
            val sellerMsg = ChatMessage(
                carId = c.id,
                senderName = c.sellerName,
                messageText = replyText,
                timestamp = System.currentTimeMillis(),
                isFromUser = false
            )
            repository.sendMessage(sellerMsg)
            renderMessage(sellerMsg)
            scrollToBottom()
        }, 1200)
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
                val tvOfferAmount = cardView.findViewById<TextView>(R.id.tvOfferAmount)
                val tvListPrice = cardView.findViewById<TextView>(R.id.tvListPrice)
                val tvDiscount = cardView.findViewById<TextView>(R.id.tvDiscount)
                val btnAccept = cardView.findViewById<Button>(R.id.btnAcceptOffer)
                val btnDecline = cardView.findViewById<Button>(R.id.btnDeclineOffer)

                val offerAmt = if (msg.offerAmount > 0) msg.offerAmount else 29800.0
                val origAmt = if (msg.originalListPrice > 0) msg.originalListPrice else 31450.0
                val diff = origAmt - offerAmt

                tvOfferAmount.text = "$${String.format("%,.0f", offerAmt)}"
                tvListPrice.text = "$${String.format("%,.0f", origAmt)}"
                tvDiscount.text = "-$${String.format("%,.0f", diff)} below asking"

                btnAccept.setOnClickListener {
                    Toast.makeText(this, "Offer of $${String.format("%,.0f", offerAmt)} accepted! Escrow initiated.", Toast.LENGTH_LONG).show()
                    btnAccept.isEnabled = false
                    btnAccept.text = "Accepted"
                    btnDecline.visibility = View.GONE
                }

                btnDecline.setOnClickListener {
                    Toast.makeText(this, "Counter-offer flow opened.", Toast.LENGTH_SHORT).show()
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
