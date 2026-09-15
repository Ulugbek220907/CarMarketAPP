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
                    val cars = repo.getCars(CarFilter())
                    val firstCar = cars.firstOrNull()
                    if (firstCar != null) {
                        ChatOffersActivity.start(this, firstCar)
                    } else {
                        Toast.makeText(this, "No active vehicle chats yet", Toast.LENGTH_SHORT).show()
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
