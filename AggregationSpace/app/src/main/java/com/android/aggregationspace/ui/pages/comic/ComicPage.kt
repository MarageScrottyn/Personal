package com.android.aggregationspace.ui.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.android.aggregationspace.ui.components.ComicCard
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color

@Composable
fun ComicPage() {
    val scrollState = rememberLazyListState()
    // 监听滚动偏移量
    val scrollOffset by remember {
        derivedStateOf { scrollState.firstVisibleItemScrollOffset }
    }

    // 计算透明度：滚动越多，越透明
    val alpha = (1f - (scrollOffset / 500f)).coerceIn(0f, 1f)
    // 计算 Y 轴位移：向上移动，制造“被吸入”的感觉
    val offsetY = scrollOffset * 0.5f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
//            .background(Color.Black)
//            .padding(start = 20.dp, end = 20.dp)
    ) {
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalItemSpacing = 12.dp
        ) {
            val data = listOf(
                mapOf(
                    "id" to "1",
                    "imageUrl" to "https://picsum.photos/seed/picsum/200/300",
                    "title" to "风景",
                    "labels" to listOf("风景", "森林", "平原")
                ),
                mapOf(
                    "id" to "2",
                    "imageUrl" to "https://picsum.photos/seed/picsum/200/300",
                    "title" to "城市",
                    "labels" to listOf("城市", "夜景", "夜空", "银河", "车流")
                ),
                mapOf(
                    "id" to "3",
                    "imageUrl" to "https://picsum.photos/seed/picsum/200/300",
                    "title" to "风景",
                    "labels" to listOf("风景", "森林", "平原")
                ),
                mapOf(
                    "id" to "4",
                    "imageUrl" to "https://picsum.photos/seed/picsum/200/300",
                    "title" to "城市",
                    "labels" to listOf("城市", "夜景", "夜空", "银河", "车流")
                ),
                mapOf(
                    "id" to "5",
                    "imageUrl" to "https://picsum.photos/seed/picsum/200/300",
                    "title" to "风景",
                    "labels" to listOf("风景", "森林", "平原")
                ),
                mapOf(
                    "id" to "6",
                    "imageUrl" to "https://picsum.photos/seed/picsum/200/300",
                    "title" to "城市",
                    "labels" to listOf("城市", "夜景", "夜空", "银河", "车流")
                ),
                mapOf(
                    "id" to "7",
                    "imageUrl" to "https://picsum.photos/seed/picsum/200/300",
                    "title" to "风景",
                    "labels" to listOf("风景", "森林", "平原")
                ),
                mapOf(
                    "id" to "8",
                    "imageUrl" to "https://picsum.photos/seed/picsum/200/300",
                    "title" to "城市",
                    "labels" to listOf("城市", "夜景", "夜空", "银河", "车流")
                ),
                mapOf(
                    "id" to "9",
                    "imageUrl" to "https://picsum.photos/seed/picsum/200/300",
                    "title" to "风景",
                    "labels" to listOf("风景", "森林", "平原")
                ),
                mapOf(
                    "id" to "10",
                    "imageUrl" to "https://picsum.photos/seed/picsum/200/300",
                    "title" to "城市",
                    "labels" to listOf("城市", "夜景", "夜空", "银河", "车流")
                )
            )
            items(
                items = data,
                key = { it["id"] as String }
            ) { item ->
                ComicCard(
                    imageURL = item["imageUrl"] as String,
                    imageName = item["title"] as String,
                    labelList = item["labels"] as List<String>
                )
            }
        }
    }
}