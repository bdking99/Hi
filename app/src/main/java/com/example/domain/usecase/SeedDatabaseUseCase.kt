package com.example.domain.usecase

import com.example.data.local.dao.CountryDao
import com.example.data.local.entity.CountryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SeedDatabaseUseCase(
    private val countryDao: CountryDao
) {
    suspend fun execute() = withContext(Dispatchers.IO) {
        if (countryDao.getCount() == 0) {
            val countries = listOf(
                CountryEntity(name = "Bangladesh", code = "BD", phoneCode = "+880", currency = "BDT", currencySymbol = "৳", flag = "🇧🇩"),
                CountryEntity(name = "United States", code = "US", phoneCode = "+1", currency = "USD", currencySymbol = "$", flag = "🇺🇸"),
                CountryEntity(name = "United Kingdom", code = "GB", phoneCode = "+44", currency = "GBP", currencySymbol = "£", flag = "🇬🇧"),
                CountryEntity(name = "India", code = "IN", phoneCode = "+91", currency = "INR", currencySymbol = "₹", flag = "🇮🇳")
            )
            countryDao.insertCountries(countries)
            
            // In a real app we would seed roles here into RoleDao.
        }
    }
}
