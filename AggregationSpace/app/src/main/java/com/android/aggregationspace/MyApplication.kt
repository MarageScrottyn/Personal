package com.android.aggregationspace

import android.app.Application
import com.android.aggregationspace.api.ApiService
import com.android.aggregationspace.api.AuthImageLoader
import com.android.aggregationspace.api.UserManager
import com.android.aggregationspace.ui.global.SettingsStore

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ApiService.init(this)
        UserManager.init(this)
        AuthImageLoader.init(this)
        // 初始化应用设置存储（主题、字体、背景、隐私模式等）
        SettingsStore.init(this)
    }
}
