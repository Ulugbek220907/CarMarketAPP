package com.automarket.app

import com.automarket.app.data.model.Car
import com.automarket.app.data.model.ChatMessage
import com.automarket.app.data.model.Offer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CarModelTest {

    @Test
    fun testCarDisplayTitle() {
        val car = Car(make = "Tesla", model = "Model 3", year = 2022, price = 31450.0, mileage = 28450)
        assertEquals("2022 Tesla Model 3", car.displayTitle)

        val carNoYear = Car(make = "Ford", model = "Mustang", year = 0, price = 25000.0, mileage = 10000)
        assertEquals("Ford Mustang", carNoYear.displayTitle)
    }

    @Test
    fun testPriceAndMileageFormatting() {
        val car = Car(make = "Porsche", model = "911", year = 2023, price = 128900.0, mileage = 8200)
        assertEquals("$128,900", car.formattedPrice)
        assertEquals("8,200 mi", car.formattedMileage)
        assertEquals("8,200 mi • Automatic • Sedan", car.specsSummary)
    }

    @Test
    fun testMonthlyEstimateCalculation() {
        val car = Car(make = "Tesla", model = "Model 3", year = 2022, price = 31450.0, mileage = 28450)
        assertTrue(car.monthlyEstimate.contains("/mo est."))
        assertTrue(car.monthlyEstimate.startsWith("$"))

        val zeroCar = Car(make = "Test", model = "Zero", price = 0.0, mileage = 0)
        assertEquals("$0/mo est.", zeroCar.monthlyEstimate)
    }

    @Test
    fun testPhotoUrlsHandling() {
        val photos = listOf("https://example.com/p1.jpg", "https://example.com/p2.jpg")
        val car = Car(make = "BMW", model = "M4", price = 74200.0, mileage = 19500, photoUrls = photos)

        assertEquals(2, car.photoCount)
        assertEquals("https://example.com/p1.jpg", car.primaryPhotoUrl)
        assertEquals("https://example.com/p1.jpg", car.photo1)
        assertEquals("https://example.com/p2.jpg", car.photo2)
        assertEquals(null, car.photo3)
        assertEquals(photos, car.getPhotos())
    }

    @Test
    fun testOfferDifferenceCalculation() {
        val offer = Offer(
            carId = "car_123",
            offeredPrice = 28000.0,
            originalPrice = 30000.0
        )
        assertEquals("$28,000", offer.formattedOfferedPrice)
        assertEquals("$30,000", offer.formattedOriginalPrice)
        assertEquals("-$2,000 below asking", offer.differenceText)
    }

    @Test
    fun testChatMessageProperties() {
        val msg = ChatMessage(
            carId = "car_123",
            senderName = "Marcus",
            messageText = "Vehicle is still available",
            isFromUser = false
        )
        assertFalse(msg.isFromUser)
        assertFalse(msg.isOfficialOffer)
        assertEquals("Marcus", msg.senderName)
        assertTrue(msg.formattedTime.isNotEmpty())
    }

    @Test
    fun testChatMessageIncomingVsOutgoing() {
        val outgoingMsg = ChatMessage(
            carId = "car_123",
            senderUid = "user_abc",
            senderName = "Buyer",
            messageText = "Hi, is this available?",
            isFromUser = true
        )
        assertTrue(outgoingMsg.isFromUser)
        assertEquals("user_abc", outgoingMsg.senderUid)

        val incomingMsg = ChatMessage(
            carId = "car_123",
            senderUid = "seller_xyz",
            senderName = "Seller",
            messageText = "Yes, available today!",
            isFromUser = false
        )
        assertFalse(incomingMsg.isFromUser)
        assertEquals("seller_xyz", incomingMsg.senderUid)
    }

    @Test
    fun testOfficialOfferDifferencePositiveAndNegative() {
        // Below asking price
        val belowOffer = Offer(carId = "c1", offeredPrice = 27000.0, originalPrice = 30000.0)
        assertEquals("-$3,000 below asking", belowOffer.differenceText)

        // Above asking price (competitive bidding)
        val aboveOffer = Offer(carId = "c1", offeredPrice = 32000.0, originalPrice = 30000.0)
        assertEquals("+$2,000 above asking", aboveOffer.differenceText)

        // Exact asking price
        val exactOffer = Offer(carId = "c1", offeredPrice = 30000.0, originalPrice = 30000.0)
        assertEquals("-$0 below asking", exactOffer.differenceText)
    }

    @Test
    fun testCarSellerUidAndOwnershipFlags() {
        val car = Car(
            id = "car_99",
            make = "Tesla",
            model = "Model Y",
            year = 2023,
            price = 42000.0,
            sellerUid = "uid_owner_123",
            isUserListing = true
        )
        assertEquals("uid_owner_123", car.sellerUid)
        assertTrue(car.isUserListing)
        assertEquals(2023, car.year)
        assertEquals("$42,000", car.formattedPrice)
    }

    @Test
    fun testCarFilterDefaultsAndSortOptions() {
        val filter = com.automarket.app.data.model.CarFilter()
        assertEquals(com.automarket.app.data.model.CategoryFilter.ALL, filter.category)
        assertEquals(com.automarket.app.data.model.SortOption.RECOMMENDED, filter.sortOption)
        assertEquals("", filter.searchQuery)
        assertEquals("", filter.location)

        val customFilter = filter.copy(
            category = com.automarket.app.data.model.CategoryFilter.COUPE,
            sortOption = com.automarket.app.data.model.SortOption.PRICE_ASC,
            searchQuery = "Porsche",
            location = "Miami"
        )
        assertEquals(com.automarket.app.data.model.CategoryFilter.COUPE, customFilter.category)
        assertEquals(com.automarket.app.data.model.SortOption.PRICE_ASC, customFilter.sortOption)
        assertEquals("Porsche", customFilter.searchQuery)
        assertEquals("Miami", customFilter.location)
    }
}
