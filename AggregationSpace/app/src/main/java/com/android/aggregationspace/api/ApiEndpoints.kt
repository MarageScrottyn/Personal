package com.android.aggregationspace.api

/** API端点配置 - 与后端urls.py保持同步 */
object ApiEndpoints {

    /** 认证相关 */
    object Auth {
        const val LOGIN = "/api/auth/login/"
        const val REGISTER = "/api/auth/register/"
        const val REFRESH = "/api/auth/refresh/"
    }

    /** 用户相关 */
    object User {
        const val LIST = "/api/users/"
        const val DETAIL = "/api/users/"
        const val PROFILE = "/api/users/me/"
        const val STATS = "/api/users/me/stats/"
        const val ELEVATE = "/api/users/me/elevate/"
        const val CHANGE_PASSWORD = "/api/change-password/"
        const val RESET_PASSWORD = "/api/admin/users/"
    }

    /** 分类相关 */
    object Category {
        const val LIST = "/api/categories/"
        const val DETAIL = "/api/categories/"
        const val CREATE = "/api/admin/categories/create/"
        const val UPDATE = "/api/admin/categories/"
        const val DELETE = "/api/admin/categories/"
    }

    /** 图片相关 */
    object Image {
        const val LIST = "/api/images/"
        const val DETAIL = "/api/images/"
        const val CREATE = "/api/images/create/"
        const val UPDATE = "/api/images/"
        const val DELETE = "/api/images/"
        const val UPLOAD = "/api/upload/image/"
        const val BATCH_UPLOAD = "/api/admin/images/batch-upload/"
    }

    /** 图集相关 */
    object Album {
        const val LIST = "/api/albums/"
        const val DETAIL = "/api/albums/"
        const val CREATE = "/api/albums/create/"
        const val UPDATE = "/api/albums/"
        const val DELETE = "/api/albums/"
        const val UPLOAD_IMAGES = "/api/admin/albums/"
    }

    /** 视频相关 */
    object Video {
        const val LIST = "/api/videos/"
        const val DETAIL = "/api/videos/"
        const val CREATE = "/api/admin/videos/create/"
        const val UPDATE = "/api/admin/videos/"
        const val DELETE = "/api/admin/videos/"
        const val UPLOAD = "/api/upload/video/"
        const val UPLOAD_FOLDER = "/api/upload/video-folder/"
        const val UPLOAD_THUMBNAIL = "/api/upload/video-thumbnail/"
        const val STREAM = "/api/media/"
    }

    /** 特辑相关（通过视频的video_type=special过滤） */
    object SpecialAlbum {
        const val LIST = "/api/videos/"
        const val DETAIL = "/api/videos/"
        const val CREATE = "/api/admin/videos/create/"
        const val UPDATE = "/api/admin/videos/"
        const val DELETE = "/api/admin/videos/"
        const val STREAM = "/api/media/"
        const val RECOMMEND = "/api/videos/"
    }

    /** 漫画相关 */
    object Comic {
        const val LIST = "/api/comics/"
        const val DETAIL = "/api/comics/"
        const val CREATE = "/api/admin/comics/create/"
        const val UPDATE = "/api/admin/comics/"
        const val DELETE = "/api/admin/comics/"
        const val UPLOAD = "/api/upload/image/"
    }

    /** 漫画章节相关 */
    object ComicChapter {
        const val CREATE = "/api/admin/comic-chapters/create/"
        const val UPDATE = "/api/admin/comic-chapters/"
        const val DELETE = "/api/admin/comic-chapters/"
    }

    /** 笔记相关 */
    object Note {
        const val LIST = "/api/notes/"
        const val DETAIL = "/api/notes/"
        const val CREATE = "/api/notes/"
        const val UPDATE = "/api/notes/"
        const val DELETE = "/api/notes/"
    }

    /** 云盘相关 */
    object CloudDrive {
        const val LIST = "/api/cloud/"
        const val DETAIL = "/api/cloud/"
        const val UPLOAD = "/api/upload/image/"
        const val DOWNLOAD = "/api/media/"
        const val DELETE = "/api/cloud/"
        const val UPDATE = "/api/cloud/"
        const val CREATE_FOLDER = "/api/cloud/create-folder/"
    }

    /** AI 助手相关 */
    object Assistant {
        /** SSE 流式对话（带人格与多轮记忆，App 主用） */
        const val CHAT_STREAM = "/api/chat/stream/"
        /** 同步对话（Agent 工具调用模式，备用） */
        const val CHAT = "/api/chat/"
    }

    /** 系统维护相关 */
    object System {
        const val SYNC_MEDIA = "/api/sync-media/"
        const val CLEANUP_TEMP = "/api/cleanup-temp/"
        const val CONFIG = "/api/users/me/stats/"
        const val STATUS = "/api/users/me/stats/"
        const val STORAGE = "/api/users/me/stats/"
    }

    /** 上传相关快捷入口 */
    object Upload {
        const val IMAGE = "/api/upload/image/"
        const val VIDEO = "/api/upload/video/"
        const val VIDEO_FOLDER = "/api/upload/video-folder/"
        const val VIDEO_THUMBNAIL = "/api/upload/video-thumbnail/"
    }
}
