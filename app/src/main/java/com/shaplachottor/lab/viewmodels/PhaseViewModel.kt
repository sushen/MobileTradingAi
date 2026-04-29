package com.shaplachottor.lab.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shaplachottor.lab.models.Booking
import com.shaplachottor.lab.models.BookingRequestResult
import com.shaplachottor.lab.models.Phase
import com.shaplachottor.lab.repositories.PhaseRepository
import kotlinx.coroutines.launch

class PhaseViewModel(private val repository: PhaseRepository) : ViewModel() {

    private val _allPhases = MutableLiveData<List<Phase>>()
    private val _filteredPhases = MutableLiveData<List<Phase>>()
    val phases: LiveData<List<Phase>> = _filteredPhases

    private val _bookingStates = MutableLiveData<Map<String, Booking>>(emptyMap())
    val bookingStates: LiveData<Map<String, Booking>> = _bookingStates

    private val _bookingResult = MutableLiveData<BookingRequestResult>()
    val bookingResult: LiveData<BookingRequestResult> = _bookingResult

    val selectedLevel: String get() = _selectedLevel
    private var _selectedLevel: String = "Beginner"

    fun loadPhases() {
        viewModelScope.launch {
            val phases = repository.getPhases()
            _allPhases.value = phases
            _bookingStates.value = repository.getCurrentUserBookings(phases)
            filterByLevel(_selectedLevel)
        }
    }

    fun filterByLevel(level: String) {
        _selectedLevel = level
        val all = _allPhases.value ?: return
        _filteredPhases.value = all.filter { it.level.equals(level, ignoreCase = true) }
            .sortedBy { it.order }
    }

    fun requestSeat(phase: Phase, phoneNumber: String, whatsappNumber: String) {
        viewModelScope.launch {
            _bookingResult.value = repository.requestSeat(phase, phoneNumber, whatsappNumber)
            loadPhases()
        }
    }
}
