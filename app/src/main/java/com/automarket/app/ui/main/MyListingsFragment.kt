package com.automarket.app.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.automarket.app.AutoMarketApplication
import com.automarket.app.databinding.FragmentMyListingsBinding
import com.automarket.app.ui.adapter.CarAdapter
import com.automarket.app.ui.detail.CarDetailActivity
import com.automarket.app.ui.post.PostCarActivity

class MyListingsFragment : Fragment() {

    private var _binding: FragmentMyListingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var carAdapter: CarAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyListingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupListeners()
        loadListings()
    }

    override fun onResume() {
        super.onResume()
        loadListings()
    }

    private fun setupRecyclerView() {
        carAdapter = CarAdapter(
            onCarClick = { car ->
                val intent = Intent(requireContext(), CarDetailActivity::class.java).apply {
                    putExtra(CarDetailActivity.EXTRA_CAR_ID, car.id)
                }
                startActivity(intent)
            },
            onBookmarkClick = { car, _ ->
                val repo = (requireActivity().application as AutoMarketApplication).repository
                repo.toggleFavorite(car.id)
                loadListings()
            }
        )

        binding.recyclerViewMyListings.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = carAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupListeners() {
        binding.btnPostFirstCar.setOnClickListener {
            val intent = Intent(requireContext(), PostCarActivity::class.java)
            startActivity(intent)
        }
    }

    fun loadListings() {
        if (!isAdded) return
        val repo = (requireActivity().application as AutoMarketApplication).carRepository
        val listings = repo.getUserListings()

        carAdapter.submitList(listings)
        binding.tvMyCount.text = "${listings.size} POSTED"

        if (listings.isEmpty()) {
            binding.recyclerViewMyListings.visibility = View.GONE
            binding.emptyMyListings.visibility = View.VISIBLE
        } else {
            binding.recyclerViewMyListings.visibility = View.VISIBLE
            binding.emptyMyListings.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
