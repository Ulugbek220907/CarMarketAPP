package com.automarket.app.ui.main

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.automarket.app.AutoMarketApplication
import com.automarket.app.R
import com.automarket.app.data.api.ApiClient
import com.automarket.app.data.model.CarFilter
import com.automarket.app.data.model.CategoryFilter
import com.automarket.app.data.model.SortOption
import com.automarket.app.databinding.FragmentHomeBinding
import com.automarket.app.ui.adapter.CarAdapter
import com.automarket.app.ui.detail.CarDetailActivity
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var carAdapter: CarAdapter
    private var currentFilter = CarFilter()

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
        setupSearch()
        setupCategoryChips()
        setupSecondaryChips()
        setupSort()
        loadCars()
    }

    override fun onResume() {
        super.onResume()
        loadCars()
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
            Toast.makeText(requireContext(), "Showing listings in Austin, TX & within 50 miles", Toast.LENGTH_SHORT).show()
        }

        binding.btnNotifications.setOnClickListener {
            showServerConfigDialog()
        }

        binding.btnFilterTune.setOnClickListener {
            showFilterDialog()
        }
    }

    private fun showServerConfigDialog() {
        val currentUrl = ApiClient.getBaseUrl()
        val input = EditText(requireContext()).apply {
            setText(currentUrl)
            setSelection(currentUrl.length)
            hint = "https://your-app.onrender.com/"
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Render Cloud Backend URL")
            .setMessage("Connected to Render database. Enter your live Render URL or keep the default:")
            .setView(input)
            .setPositiveButton("Save & Connect") { _, _ ->
                val newUrl = input.text.toString().trim()
                if (newUrl.isNotEmpty()) {
                    ApiClient.setBaseUrl(requireContext(), newUrl)
                    Toast.makeText(requireContext(), "Updated backend URL to $newUrl", Toast.LENGTH_SHORT).show()
                    loadCars()
                }
            }
            .setNeutralButton("Reset Default") { _, _ ->
                ApiClient.setBaseUrl(requireContext(), ApiClient.DEFAULT_RENDER_URL)
                Toast.makeText(requireContext(), "Reset to default Render URL", Toast.LENGTH_SHORT).show()
                loadCars()
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString().orEmpty()
                binding.btnClearSearch.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
                currentFilter = currentFilter.copy(searchQuery = query)
                loadCars()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnClearSearch.setOnClickListener {
            binding.etSearch.text?.clear()
        }

        binding.btnResetFilters.setOnClickListener {
            binding.etSearch.text?.clear()
            selectPrimaryChip(CategoryFilter.ALL)
        }
    }

    private fun setupCategoryChips() {
        binding.chipAll.setOnClickListener { selectPrimaryChip(CategoryFilter.ALL) }
        binding.chipSuv.setOnClickListener { selectPrimaryChip(CategoryFilter.SUV) }
        binding.chipSedan.setOnClickListener { selectPrimaryChip(CategoryFilter.SEDAN) }
        binding.chipElectric.setOnClickListener { selectPrimaryChip(CategoryFilter.ELECTRIC) }
        binding.chipTruck.setOnClickListener { selectPrimaryChip(CategoryFilter.TRUCK) }
        binding.chipLuxury.setOnClickListener { selectPrimaryChip(CategoryFilter.LUXURY) }
        binding.chipHybrid.setOnClickListener { selectPrimaryChip(CategoryFilter.HYBRID) }
    }

    private fun selectPrimaryChip(category: CategoryFilter) {
        currentFilter = currentFilter.copy(category = category)

        val chips = listOf(
            binding.chipAll to CategoryFilter.ALL,
            binding.chipSuv to CategoryFilter.SUV,
            binding.chipSedan to CategoryFilter.SEDAN,
            binding.chipElectric to CategoryFilter.ELECTRIC,
            binding.chipTruck to CategoryFilter.TRUCK,
            binding.chipLuxury to CategoryFilter.LUXURY,
            binding.chipHybrid to CategoryFilter.HYBRID
        )

        for ((chipView, cat) in chips) {
            if (cat == category) {
                chipView.setBackgroundResource(R.drawable.bg_chip_category_active)
                chipView.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            } else {
                chipView.setBackgroundResource(R.drawable.bg_chip_category_inactive)
                chipView.setTextColor(ContextCompat.getColor(requireContext(), R.color.on_surface))
            }
        }

        // Reset secondary chips styling
        resetSecondaryChips()
        loadCars()
    }

    private fun setupSecondaryChips() {
        binding.chipUnder15k.setOnClickListener {
            toggleSecondaryFilter(CategoryFilter.UNDER_15K, binding.chipUnder15k)
        }
        binding.chipUnder25k.setOnClickListener {
            toggleSecondaryFilter(CategoryFilter.UNDER_25K, binding.chipUnder25k)
        }
        binding.chipLowMiles.setOnClickListener {
            toggleSecondaryFilter(CategoryFilter.LOW_MILES, binding.chipLowMiles)
        }
        binding.chipCertified.setOnClickListener {
            toggleSecondaryFilter(CategoryFilter.CERTIFIED, binding.chipCertified)
        }
    }

    private fun toggleSecondaryFilter(category: CategoryFilter, chipView: TextView) {
        if (currentFilter.category == category) {
            selectPrimaryChip(CategoryFilter.ALL)
        } else {
            currentFilter = currentFilter.copy(category = category)
            resetSecondaryChips()
            chipView.setBackgroundResource(R.drawable.bg_chip_category_active)
            chipView.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            loadCars()
        }
    }

    private fun resetSecondaryChips() {
        binding.chipUnder15k.setBackgroundResource(R.drawable.bg_chip_budget)
        binding.chipUnder15k.setTextColor(ContextCompat.getColor(requireContext(), R.color.on_surface_variant))

        binding.chipUnder25k.setBackgroundResource(R.drawable.bg_chip_budget)
        binding.chipUnder25k.setTextColor(ContextCompat.getColor(requireContext(), R.color.on_surface_variant))

        binding.chipLowMiles.setBackgroundResource(R.drawable.bg_chip_budget)
        binding.chipLowMiles.setTextColor(ContextCompat.getColor(requireContext(), R.color.on_surface_variant))

        binding.chipCertified.setBackgroundResource(R.drawable.bg_chip_budget)
        binding.chipCertified.setTextColor(ContextCompat.getColor(requireContext(), R.color.deal_emerald))
    }

    private fun setupSort() {
        binding.btnSort.setOnClickListener { view ->
            val popup = PopupMenu(requireContext(), view)
            popup.menu.add(0, 1, 0, getString(R.string.sort_recommended))
            popup.menu.add(0, 2, 1, getString(R.string.sort_newest))
            popup.menu.add(0, 3, 2, getString(R.string.sort_price_asc))
            popup.menu.add(0, 4, 3, getString(R.string.sort_price_desc))
            popup.menu.add(0, 5, 4, getString(R.string.sort_mileage))

            popup.setOnMenuItemClickListener { item ->
                val (sort, label) = when (item.itemId) {
                    1 -> SortOption.RECOMMENDED to getString(R.string.sort_recommended)
                    2 -> SortOption.NEWEST to getString(R.string.sort_newest)
                    3 -> SortOption.PRICE_ASC to getString(R.string.sort_price_asc)
                    4 -> SortOption.PRICE_DESC to getString(R.string.sort_price_desc)
                    5 -> SortOption.MILEAGE_ASC to getString(R.string.sort_mileage)
                    else -> SortOption.RECOMMENDED to getString(R.string.sort_recommended)
                }
                binding.tvSortCurrent.text = label
                currentFilter = currentFilter.copy(sortOption = sort)
                loadCars()
                true
            }
            popup.show()
        }
    }

    private fun showFilterDialog() {
        val options = arrayOf("All Body Styles", "Electric Vehicles Only", "Under $25,000", "Under 30,000 Miles", "CARFAX Clean Title Only")
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Quick Filter")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> selectPrimaryChip(CategoryFilter.ALL)
                    1 -> selectPrimaryChip(CategoryFilter.ELECTRIC)
                    2 -> toggleSecondaryFilter(CategoryFilter.UNDER_25K, binding.chipUnder25k)
                    3 -> toggleSecondaryFilter(CategoryFilter.LOW_MILES, binding.chipLowMiles)
                    4 -> toggleSecondaryFilter(CategoryFilter.CERTIFIED, binding.chipCertified)
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
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

        // Asynchronously synchronize with Render cloud backend
        viewLifecycleOwner.lifecycleScope.launch {
            val result = repo.refreshCarsFromBackend()
            if (result.isSuccess && isAdded) {
                val updatedCars = repo.getCars(currentFilter)
                carAdapter.submitList(updatedCars)
                binding.tvCarCount.text = "(${updatedCars.size} cars)"

                if (updatedCars.isEmpty()) {
                    binding.rvCars.visibility = View.GONE
                    binding.emptyState.visibility = View.VISIBLE
                } else {
                    binding.rvCars.visibility = View.VISIBLE
                    binding.emptyState.visibility = View.GONE
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
