package com.automarket.app

import android.app.Application
import com.automarket.app.data.repository.CarRepository

class AutoMarketApplication : Application() {

    lateinit var carRepository: CarRepository
        private set

    val repository: CarRepository
        get() = carRepository

    override fun onCreate() {
        super.onCreate()
        carRepository = CarRepository(this)
    }
}
