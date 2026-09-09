package com.fde.gallery3d.ui.picture

import android.app.Application
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.fde.gallery3d.ui.bean.Multimedia
import com.fde.gallery3d.ui.utils.FileUtils

class PictureViewModel : ViewModel() {



    private val _pictureList = MutableLiveData<List<Multimedia>>(emptyList())
    val pictureList: LiveData<List<Multimedia>> = _pictureList

    private val _selectedCount = MutableLiveData(0)
    val selectedCount: LiveData<Int> = _selectedCount

    private val _showBottomBar = MutableLiveData(false)
    val showBottomBar: LiveData<Boolean> = _showBottomBar

    private val _allSelected = MutableLiveData(false)
    val allSelected: LiveData<Boolean> = _allSelected

    private var deleteList: List<Multimedia> = emptyList()

    fun getAllImages(context: Context) {
        val images = FileUtils.getAllImages(context)
        _pictureList.postValue(images)
        _selectedCount.postValue(0)
        _allSelected.postValue(false)
    }

    fun setBottomBarVisible(show: Boolean) {
        _showBottomBar.value = show

        if (!show) {
            clearSelection()
        }
    }

    fun toggleBottomBar() {
        setBottomBarVisible(!(_showBottomBar.value ?: false))
    }

    fun selectItem(position: Int, selected: Boolean) {
        val currentList = _pictureList.value?.toMutableList() ?: return

        if (position < 0 || position >= currentList.size) {
            return
        }

        currentList[position].isSelected = selected

        _pictureList.value = currentList
        updateSelectedCount(currentList)
    }

    fun toggleSelectAll() {
        val currentList = _pictureList.value?.toMutableList() ?: return

        val selectAll = !(_allSelected.value ?: false)

        currentList.forEach {
            it.isSelected = selectAll
        }

        _allSelected.value = selectAll
        _pictureList.value = currentList
        _selectedCount.value = if (selectAll) currentList.size else 0
    }

    private fun updateSelectedCount(list: List<Multimedia>) {
        val count = list.count { it.isSelected }

        _selectedCount.value = count
        _allSelected.value = count > 0 && count == list.size
    }

    fun prepareDelete(): Boolean {
        val currentList = _pictureList.value ?: emptyList()

        deleteList = currentList.filter {
            it.isSelected
        }

        return deleteList.isNotEmpty()
    }

    fun deleteImage(context: Context): Boolean {
        if (deleteList.isEmpty()) {
            return false
        }

        deleteList.forEach {
            FileUtils.deleteImage(context, it.path)
        }

        deleteList = emptyList()

        getAllImages(context)

        return true
    }

    fun clearSelection() {
        val currentList = _pictureList.value?.toMutableList() ?: return

        currentList.forEach {
            it.isSelected = false
        }

        _pictureList.value = currentList
        _selectedCount.value = 0
        _allSelected.value = false
    }
}