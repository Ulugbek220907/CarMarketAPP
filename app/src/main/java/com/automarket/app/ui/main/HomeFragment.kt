package com.automarket.app.ui.main

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.automarket.app.AutoMarketApplication
import com.automarket.app.R
import com.automarket.app.data.model.CarFilter
import com.automarket.app.data.model.CategoryFilter
import com.automarket.app.data.model.SortOption
import com.automarket.app.databinding.FragmentHomeBinding
import com.automarket.app.ui.adapter.CarAdapter
import com.automarket.app.ui.detail.CarDetailActivity
import com.automarket.app.ui.post.PostCarActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var carAdapter: CarAdapter
    private var currentFilter = CarFilter()
    private var searchJob: Job? = null

    private val updateListener: () -> Unit = {
        loadCars()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupTopBar()
        setupCategoryChips()
        setupSortSelector()
        setupSearch()

        val repo = (requireActivity().application as AutoMarketApplication).repository
        repo.addUpdateListener(updateListener)
        loadCars()
    }

    override fun onResume() {
        super.onResume()
        loadCars()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        val repo = (requireActivity().application as AutoMarketApplication).repository
        repo.removeUpdateListener(updateListener)
        _binding = null
    }

    private fun setupRecyclerView() {
        carAdapter = CarAdapter(
            onCarClick = { car ->
                CarDetailActivity.start(requireContext(), car.id)
            },
            onBookmarkClick = { car, _ ->
                val repo = (requireActivity().application as AutoMarketApplication).repository
                repo.toggleFavorite(car.id)
                loadCars()
            }
        )

        binding.rvCars.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = carAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupTopBar() {
        binding.btnLocationPicker.setOnClickListener {
            showLocationPickerDialog()
        }

        binding.btnPostCarEmpty.setOnClickListener {
            val intent = Intent(requireContext(), PostCarActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupCategoryChips() {
        val chips = listOf(
            binding.chipCategoryAll to CategoryFilter.ALL,
            binding.chipCategorySUV to CategoryFilter.SUV,
            binding.chipCategorySedan to CategoryFilter.SEDAN,
            binding.chipCategoryElectric to CategoryFilter.ELECTRIC,
            binding.chipCategoryTruck to CategoryFilter.TRUCK,
            binding.chipCategoryLuxury to CategoryFilter.LUXURY,
            binding.chipCategoryHybrid to CategoryFilter.HYBRID,
            binding.chipCategoryCoupe to CategoryFilter.COUPE,
            binding.chipCategoryUnder30K to CategoryFilter.UNDER_30K,
            binding.chipCategoryLowMiles to CategoryFilter.LOW_MILES
        )

        for ((chipView, category) in chips) {
            chipView.setOnClickListener {
                currentFilter = currentFilter.copy(category = category)
                updateChipVisuals(chips, category)
                loadCars()
            }
        }
    }

    private fun updateChipVisuals(chips: List<Pair<TextView, CategoryFilter>>, selected: CategoryFilter) {
        val activeBg = ContextCompat.getDrawable(requireContext(), R.drawable.btn_primary_blue)
        val inactiveBg = ContextCompat.getDrawable(requireContext(), R.drawable.bg_chip_category_inactive)
        val activeTextColor = ContextCompat.getColor(requireContext(), R.color.white)
        val inactiveTextColor = ContextCompat.getColor(requireContext(), R.color.on_surface)

        for ((view, category) in chips) {
            if (category == selected) {
                view.background = activeBg
                view.setTextColor(activeTextColor)
            } else {
                view.background = inactiveBg
                view.setTextColor(inactiveTextColor)
            }
        }
    }

    private fun setupSortSelector() {
        binding.btnSort.setOnClickListener {
            showSortDialog()
        }
    }

    private fun showSortDialog() {
        val sortOptions = listOf(
            "Recommended" to SortOption.RECOMMENDED,
            "Price: Low to High" to SortOption.PRICE_ASC,
            "Price: High to Low" to SortOption.PRICE_DESC,
            "Newest Year" to SortOption.NEWEST,
            "Lowest Mileage" to SortOption.MILEAGE_ASC
        )
        val names = sortOptions.map { it.first }.toTypedArray()
        val currentIdx = sortOptions.indexOfFirst { it.second == currentFilter.sortOption }.coerceAtLeast(0)

        AlertDialog.Builder(requireContext())
            .setTitle("Sort Listings")
            .setSingleChoiceItems(names, currentIdx) { dialog, which ->
                val selected = sortOptions[which]
                currentFilter = currentFilter.copy(sortOption = selected.second)
                binding.tvSortLabel.text = selected.first.split(" ").first()
                loadCars()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showLocationPickerDialog() {
        val input = EditText(requireContext()).apply {
            hint = "e.g. Austin, Miami, Dallas, Seattle..."
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Filter by Location")
            .setMessage("Enter city or region to filter, or clear to view all vehicles:")
            .setView(input)
            .setPositiveButton("Filter") { _, _ ->
                val loc = input.text.toString().trim()
                if (loc.isNotEmpty()) {
                    binding.tvCurrentLocation.text = loc
                    currentFilter = currentFilter.copy(location = loc)
                } else {
                    binding.tvCurrentLocation.text = "All Locations"
                    currentFilter = currentFilter.copy(location = "")
                }
                loadCars()
            }
            .setNeutralButton("All Locations") { _, _ ->
                binding.tvCurrentLocation.text = "All Locations"
                currentFilter = currentFilter.copy(location = "")
                loadCars()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString().orEmpty()
                binding.btnClearSearch.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
                searchJob?.cancel()
                searchJob = viewLifecycleOwner.lifecycleScope.launch {
                    delay(250)
                    currentFilter = currentFilter.copy(searchQuery = query)
                    loadCars()
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnClearSearch.setOnClickListener {
            searchJob?.cancel()
            binding.etSearch.text?.clear()
            currentFilter = currentFilter.copy(searchQuery = "")
            loadCars()
        }
    }

    fun loadCars() {
        if (!isAdded) return
        val repo = (requireActivity().application as AutoMarketApplication).repository
        val cars = repo.getCars(currentFilter)

        carAdapter.submitList(cars)
        binding.tvCarCount.text = "(${cars.size} cars)"

        if (cars.isEmpty()) {
            binding.rvCars.visibility = View.GONE
            binding.emptyState.visibility = View.VISIBLE
        } else {
            binding.rvCars.visibility = View.VISIBLE
            binding.emptyState.visibility = View.GONE
        }
    }
}
