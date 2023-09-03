/**
 * Copyright (c) 2023 Natalia Molinero Mingorance
 * All rights reserved.
 */

package harmony.valley.wamboocam.vm


import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import harmony.valley.wamboocam.models.CompressData
import harmony.valley.wamboocam.repository.CompressRepository
import javax.inject.Inject

@HiltViewModel
class CompressViewModel @Inject constructor(private val compressRepository: CompressRepository) :
    ViewModel() {


    fun delete() {
        viewModelScope.launch {
            compressRepository.delete()
        }
    }

    var data: MutableLiveData<Pair<Long, Long>> = MutableLiveData()

    fun setMap(startDate: Long, endDate: Long) {
        data.value = Pair(startDate, endDate)
    }

    val userLiveDataByDate: LiveData<List<CompressData>> = data.switchMap { param1 ->
        compressRepository.getAllUserData(param1.first, param1.second)
    }

    fun deleteSpecific(start: Long, end: Long) {
        viewModelScope.launch {
            compressRepository.deleteSpecific(start, end)
        }
    }

    fun getCount() = compressRepository.getAllCompression()

}