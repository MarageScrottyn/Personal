package com.android.personal;

import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.WindowCompat;
import androidx.core.widget.NestedScrollView;

import com.android.personal.api.ApiClient;
import com.android.personal.api.model.Comic;
import com.android.personal.api.model.ComicChapter;
import com.android.personal.utils.AuthManager;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.squareup.picasso.Picasso;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ComicReadActivity extends AppCompatActivity {

    private LinearLayout btnBack, imagesContainer;
    private TextView tvComicTitle, tvChapterTitle, tvError;
    private NestedScrollView scrollView;
    private CircularProgressIndicator progressBar;

    private String comicSlug;
    private int chapterId;
    private String comicTitle;
    private String chapterTitle;
    private Comic comic;
    private ComicChapter currentChapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        applyTheme();

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        setContentView(R.layout.activity_comic_read);

        comicSlug = getIntent().getStringExtra("slug");
        chapterId = getIntent().getIntExtra("chapter_id", -1);
        comicTitle = getIntent().getStringExtra("comic_title");
        chapterTitle = getIntent().getStringExtra("chapter_title");

        if (comicSlug == null || chapterId == -1) {
            finish();
            return;
        }

        initViews();
        loadChapterImages();
    }

    private void applyTheme() {
        int themeMode = AuthManager.getThemeMode(this);
        switch (themeMode) {
            case AuthManager.THEME_MODE_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case AuthManager.THEME_MODE_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        imagesContainer = findViewById(R.id.images_container);
        tvComicTitle = findViewById(R.id.tv_comic_title);
        tvChapterTitle = findViewById(R.id.tv_chapter_title);
        tvError = findViewById(R.id.tv_error);
        scrollView = findViewById(R.id.scroll_view);
        progressBar = findViewById(R.id.progress_bar);

        tvComicTitle.setText(comicTitle != null ? comicTitle : "");
        tvChapterTitle.setText(chapterTitle != null ? chapterTitle : "");

        btnBack.setOnClickListener(v -> finish());
    }

    private void loadChapterImages() {
        setLoading(true);
        ApiClient.getApiService(this).getComicDetail(comicSlug).enqueue(new Callback<Comic>() {
            @Override
            public void onResponse(Call<Comic> call, Response<Comic> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    comic = response.body();
                    findAndShowChapter();
                } else {
                    showError("获取章节内容失败");
                }
            }

            @Override
            public void onFailure(Call<Comic> call, Throwable t) {
                setLoading(false);
                showError("网络错误: " + t.getMessage());
            }
        });
    }

    private void findAndShowChapter() {
        if (comic.getChapters() == null) {
            showError("没有章节内容");
            return;
        }

        for (ComicChapter chapter : comic.getChapters()) {
            if (chapter.getId() == chapterId) {
                currentChapter = chapter;
                showImages();
                return;
            }
        }

        showError("章节不存在");
    }

    private void showImages() {
        List<String> images = currentChapter.getImages();
        if (images == null || images.isEmpty()) {
            showError("本章暂无图片");
            return;
        }

        imagesContainer.removeAllViews();

        for (String imagePath : images) {
            ImageView imageView = new ImageView(this);
            imageView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ));
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            imageView.setAdjustViewBounds(true);

            String imageUrl = getImageUrl(imagePath);
            Picasso.get()
                .load(imageUrl)
                .into(imageView);

            imagesContainer.addView(imageView);
        }
    }

    private String getImageUrl(String path) {
        if (path == null) return "";
        if (path.startsWith("http")) return path;
        String baseUrl = ApiClient.BASE_URL;
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        return baseUrl + path;
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? android.view.View.VISIBLE : android.view.View.GONE);
        scrollView.setVisibility(loading ? android.view.View.GONE : android.view.View.VISIBLE);
    }

    private void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(android.view.View.VISIBLE);
        scrollView.setVisibility(android.view.View.GONE);
    }
}