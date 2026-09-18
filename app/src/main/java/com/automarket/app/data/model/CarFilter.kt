package com.automarket.app.data.model

enum class CategoryFilter {
    ALL,
    SUV,
    SEDAN,
    ELECTRIC,
    TRUCK,
    LUXURY,
    HYBRID,
    COUPE,
    UNDER_15K,
    UNDER_25K,
    UNDER_30K,
    LOW_MILES,
    CERTIFIED
}

enum class SortOption {
    RECOMMENDED,
    NEWEST,
    PRICE_ASC,
    PRICE_DESC,
    MILEAGE_ASC
}

data class CarFilter(
    val category: CategoryFilter = CategoryFilter.ALL,
    val sortOption: SortOption = SortOption.RECOMMENDED,
    val searchQuery: String = "",
    val location: String = ""
)

