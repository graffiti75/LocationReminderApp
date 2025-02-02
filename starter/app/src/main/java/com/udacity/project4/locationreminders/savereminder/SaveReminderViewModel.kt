package com.udacity.project4.locationreminders.savereminder

import android.app.Application
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.PointOfInterest
import com.udacity.project4.R
import com.udacity.project4.base.BaseViewModel
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.TAG
import kotlinx.coroutines.launch

class SaveReminderViewModel(
    val app: Application, val dataSource: ReminderDataSource
): BaseViewModel(app) {
    val reminderTitle = MutableLiveData<String?>()
    val reminderDescription = MutableLiveData<String?>()
    val selectedPOI = MutableLiveData<PointOfInterest?>()

    val reminderSelectedLocationStr = MutableLiveData<String?>()

    init {
        reminderDescription.postValue("")
    }

    /**
     * Clear the live data objects to start fresh next time the view model gets called.
     */
    fun onClear() {
        reminderTitle.value = null
        reminderDescription.value = null
        reminderSelectedLocationStr.value = null
        selectedPOI.value = null
    }

    /**
     * Validate the entered data then saves the reminder data to the DataSource.
     */
    fun validateAndSaveReminder(reminderData: ReminderDataItem) : Boolean {
        Log.d(TAG, "SaveReminderViewModel.validateAndSaveReminder().")
        if (validateEnteredData(reminderData)) {
            saveReminder(reminderData)
            return true
        }
        return false
    }

    /**
     * Save the reminder to the data source.
     */
    fun saveReminder(reminderData: ReminderDataItem) {
        Log.d(TAG, "SaveReminderViewModel.saveReminder().")
        showLoading.value = true
        viewModelScope.launch {
            dataSource.saveReminder(
                ReminderDTO(
                    reminderData.title,
                    reminderData.description,
                    reminderData.location,
                    reminderData.latitude,
                    reminderData.longitude,
                    reminderData.id
                )
            )
            showLoading.value = false
            showToast.value = app.getString(R.string.reminder_saved)
            navigationCommand.value = NavigationCommand.Back
        }
    }

    /**
     * Validate the entered data and show error to the user if there's any invalid data.
     */
    fun validateEnteredData(reminderData: ReminderDataItem): Boolean {
        Log.d(TAG, "SaveReminderViewModel.validateEnteredData().")
        if (reminderData.title.isNullOrEmpty()) {
            showSnackBarInt.value = R.string.err_enter_title
            return false
        }

        if (reminderData.location.isNullOrEmpty()) {
            showSnackBarInt.value = R.string.err_select_location
            return false
        }

        if (
            (reminderData.latitude.toString().isEmpty()) ||
            (reminderData.longitude.toString().isEmpty())
        ) {
            showSnackBarInt.value = R.string.err_select_latitude_longitude
            return false
        }

        return true
    }

    fun onSaveLocation(poi: PointOfInterest?) {
        Log.d(TAG, "SaveReminderViewModel.onSaveLocation().")
        selectedPOI.value = poi
        navigationCommand.value = NavigationCommand.Back
    }
}