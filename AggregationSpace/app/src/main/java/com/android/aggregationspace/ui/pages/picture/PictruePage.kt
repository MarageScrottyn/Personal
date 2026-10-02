package com.android.aggregationspace.ui.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.android.aggregationspace.ui.components.PictureCard


@Composable
fun PicturePage() {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 55.dp),    // 内边距
        verticalItemSpacing = 4.dp,
        horizontalArrangement = Arrangement.spacedBy(8.dp),      // 列边距
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        items(20) { index ->
            PictureCard(
                imageUrl = "https://api.elaina.cat/random/mobile"
            )
        }
    }
}