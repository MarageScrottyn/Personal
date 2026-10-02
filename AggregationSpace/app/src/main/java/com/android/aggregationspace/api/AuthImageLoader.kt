package com.android.aggregationspace.api

import android.content.Context
import coil3.ImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory

/**
 * 带认证功能的图片加载器
 * 使用 ApiService.client（含 AuthInterceptor）自动添加 JWT 认证头
 */
object AuthImageLoader {

    private lateinit var appContext: Context

    /** 全局 ImageLoader 实例 */
    lateinit var imageLoader: ImageLoader
        private set

    /** 初始化图片加载器，在 Application.onCreate 中调用 */
    fun init(context: Context) {
        appContext = context.applicationContext
        // 使用 ApiService.client（已含认证拦截器和 401 自动刷新）
        imageLoader = ImageLoader.Builder(appContext)
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = ApiService.client))
            }
            .build()
    }
}
