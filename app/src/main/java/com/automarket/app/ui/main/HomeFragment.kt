package com.automarket.app.ui.main

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.automarket.app.AutoMarketApplication
import com.automarket.app.data.api.ApiClient
import com.automarket.app.data.model.CarFilter
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
            showLocationPickerDialog()
        }

        binding.btnLocationPicker.setOnLongClickListener {
            showServerConfigDialog()
            true
        }

        binding.btnPostCarEmpty.setOnClickListener {
            val intent = Intent(requireContext(), PostCarActivity::class.java)
            startActivity(intent)
        }
    }

    private fun showLocationPickerDialog() {
        val input = EditText(requireContext()).apply {
            hint = "e.g. Tashkent, London, New York..."
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
                    val success = ApiClient.setBaseUrl(requireContext(), newUrl)
                    if (success) {
                        Toast.makeText(requireContext(), "Updated backend URL to $newUrl", Toast.LENGTH_SHORT).show()
                        loadCars()
                    } else {
                        Toast.makeText(requireContext(), "Invalid backend URL format. Must start with http:// or https://", Toast.LENGTH_LONG).show()
                    }
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
                searchJob?.cancel()
                searchJob = viewLifecycleOwner.lifecycleScope.launch {
                    delay(300)
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
