package com.automarket.app.ui.main

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.automarket.app.AutoMarketApplication
import com.automarket.app.R
import com.automarket.app.data.model.CarFilter
import com.automarket.app.databinding.ActivityMainBinding
import com.automarket.app.ui.chat.ChatOffersActivity
import com.automarket.app.ui.post.PostCarActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val homeFragment = HomeFragment()
    private val favoritesFragment = FavoritesFragment()
    private val myListingsFragment = MyListingsFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()

        if (savedInstanceState == null) {
            switchFragment(homeFragment)
        }
    }

    private fun setupNavigation() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_market -> {
                    switchFragment(homeFragment)
                    true
                }
                R.id.navigation_favorites -> {
                    switchFragment(favoritesFragment)
                    true
                }
                R.id.navigation_post -> {
                    val intent = Intent(this, PostCarActivity::class.java)
                    startActivity(intent)
                    false // Don't highlight tab as active permanently
                }
                R.id.navigation_messages -> {
                    val repo = (application as AutoMarketApplication).repository
                    val userCars = repo.getUserListings()
                    val allCars = repo.getCars(CarFilter())

                    when {
                        userCars.isNotEmpty() -> {
                            if (userCars.size == 1) {
                                ChatOffersActivity.start(this, userCars[0].id)
                            } else {
                                val titles = userCars.map { "${it.displayTitle} (${it.formattedPrice})" }.toTypedArray()
                                androidx.appcompat.app.AlertDialog.Builder(this)
                                    .setTitle("Select Vehicle Inquiries")
                                    .setItems(titles) { _, which ->
                                        ChatOffersActivity.start(this, userCars[which].id)
                                    }
                                    .show()
                            }
                        }
                        allCars.isNotEmpty() -> {
                            ChatOffersActivity.start(this, allCars[0].id)
                        }
                        else -> {
                            Toast.makeText(this, "No vehicle listings available for chat", Toast.LENGTH_SHORT).show()
                        }
                    }
                    false
                }
                R.id.navigation_profile -> {
                    switchFragment(myListingsFragment)
                    true
                }
                else -> false
            }
        }
    }

    private fun switchFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
