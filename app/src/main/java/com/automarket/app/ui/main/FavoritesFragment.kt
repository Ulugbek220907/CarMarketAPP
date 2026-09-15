package com.automarket.app.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.automarket.app.AutoMarketApplication
import com.automarket.app.databinding.FragmentFavoritesBinding
import com.automarket.app.ui.adapter.CarAdapter
import com.automarket.app.ui.detail.CarDetailActivity

class FavoritesFragment : Fragment() {

    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!

    private lateinit var carAdapter: CarAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        loadFavorites()
    }

    override fun onResume() {
        super.onResume()
        loadFavorites()
    }

    private fun setupRecyclerView() {
        carAdapter = CarAdapter(
            onCarClick = { car ->
                CarDetailActivity.start(requireContext(), car.id)
            },
            onBookmarkClick = { car, _ ->
                val repo = (requireActivity().application as AutoMarketApplication).repository
                repo.toggleFavorite(car.id)
                loadFavorites()
            }
        )

        binding.recyclerViewFavorites.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = carAdapter
            setHasFixedSize(true)
        }
    }

    fun loadFavorites() {
        if (!isAdded) return
        val repo = (requireActivity().application as AutoMarketApplication).repository
        val favorites = repo.getFavorites()

        carAdapter.submitList(favorites)
        binding.tvFavCount.text = "${favorites.size} Saved"

        if (favorites.isEmpty()) {
            binding.recyclerViewFavorites.visibility = View.GONE
            binding.emptyFavorites.visibility = View.VISIBLE
        } else {
            binding.recyclerViewFavorites.visibility = View.VISIBLE
            binding.emptyFavorites.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
