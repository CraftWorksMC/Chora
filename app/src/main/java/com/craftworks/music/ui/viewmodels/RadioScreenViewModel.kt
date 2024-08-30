package com.craftworks.music.ui.viewmodels

//
//class RadioScreenViewModel : ViewModel() {
//    private val _allRadios = MutableStateFlow<List<MediaData.Radio>>(emptyList())
//    val allRadios: StateFlow<List<MediaData.Radio>> = _allRadios.asStateFlow()
//
//    init {
//        fetchRadios()
//    }
//
//    fun fetchRadios() {
//        viewModelScope.launch {
//            coroutineScope {
//                if (NavidromeManager.getCurrentServer() != null) {
//                    val allRadiosDeferred  = async { getNavidromeRadios() }
//
//                    _allRadios.value = allRadiosDeferred.await()
//                }
//                else {
//                    _allRadios.value = radioList.sortedBy { it.name }
//                }
//            }
//        }
//    }
//}