package com.android.aggregationspace.ui.global

import com.android.aggregationspace.ui.pages.specialalbum.SpecialItem

object SpecialAlbumDataStore {
    val videoList: MutableList<SpecialItem> = mutableListOf()
    var currentVideo: SpecialItem? = null
}