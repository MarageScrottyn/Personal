package com.android.personal;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.WindowCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.personal.api.ApiClient;
import com.android.personal.api.model.Comic;
import com.android.personal.api.model.ComicChapter;
import com.android.personal.utils.AuthManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ComicDetailActivity extends AppCompatActivity {

    private ImageView ivCover;
    private TextView tvTitle, tvAuthor, tvDescription, tvError;
    private LinearLayout categoryContainer, btnBack;
    private MaterialButton btnStartRead;
    private RecyclerView recyclerChapters;
    private CircularProgressIndicator progressBar;
    
    private Comic comic;
    private String comicSlug;
    private ChapterAdapter chapterAdapter;
    private List<ComicChapter> chapterList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        applyTheme();
        
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        
        setContentView(R.layout.activity_comic_detail);

        comicSlug = getIntent().getStringExtra("slug");
        if (comicSlug == null) {
            finish();
            return;
        }

        initViews();
        loadComicDetail();
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
        ivCover = findViewById(R.id.iv_cover);
        tvTitle = findViewById(R.id.tv_title);
        tvAuthor = findViewById(R.id.tv_author);
        tvDescription = findViewById(R.id.tv_description);
        tvError = findViewById(R.id.tv_error);
        categoryContainer = findViewById(R.id.category_container);
        btnBack = findViewById(R.id.btn_back);
        btnStartRead = findViewById(R.id.btn_start_read);
        recyclerChapters = findViewById(R.id.recycler_chapters);
        progressBar = findViewById(R.id.progress_bar);

        chapterAdapter = new ChapterAdapter();
        recyclerChapters.setLayoutManager(new LinearLayoutManager(this));
        recyclerChapters.setAdapter(chapterAdapter);

        btnBack.setOnClickListener(v -> finish());
        btnStartRead.setOnClickListener(v -> startReading());
    }

    private void loadComicDetail() {
        setLoading(true);
        ApiClient.getApiService(this).getComicDetail(comicSlug).enqueue(new Callback<Comic>() {
            @Override
            public void onResponse(Call<Comic> call, Response<Comic> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    comic = response.body();
                    showComicDetail();
                } else {
                    showError("获取漫画详情失败");
                }
            }

            @Override
            public void onFailure(Call<Comic> call, Throwable t) {
                setLoading(false);
                showError("网络错误: " + t.getMessage());
            }
        });
    }

    private void showComicDetail() {
        tvTitle.setText(comic.getTitle());
        tvAuthor.setText("作者: " + (comic.getAuthor() != null ? comic.getAuthor() : "未知"));
        tvDescription.setText(comic.getDescription() != null ? comic.getDescription() : "暂无简介");

        if (comic.getCover_image() != null && !comic.getCover_image().isEmpty()) {
            String imageUrl = getImageUrl(comic.getCover_image());
            Picasso.get().load(imageUrl).into(ivCover);
        }

        // 显示分类标签
        categoryContainer.removeAllViews();
        if (comic.getCategory_names() != null) {
            for (String category : comic.getCategory_names()) {
                TextView tvCategory = new TextView(this);
                tvCategory.setText(category);
                tvCategory.setTextSize(12);
                tvCategory.setTextColor(getResources().getColor(R.color.flora_stem, getTheme()));
                tvCategory.setPadding(20, 8, 20, 8);
                tvCategory.setBackgroundResource(R.drawable.flora_edittext_bg);
                
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                );
                params.setMarginEnd(8);
                tvCategory.setLayoutParams(params);
                
                categoryContainer.addView(tvCategory);
            }
        }

        // 显示章节列表
        chapterList.clear();
        if (comic.getChapters() != null) {
            chapterList.addAll(comic.getChapters());
        }
        chapterAdapter.notifyDataSetChanged();

        btnStartRead.setVisibility(chapterList.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void startReading() {
        if (chapterList.isEmpty()) return;
        
        ComicChapter firstChapter = chapterList.get(0);
        Intent intent = new Intent(this, ComicReadActivity.class);
        intent.putExtra("slug", comicSlug);
        intent.putExtra("chapter_id", firstChapter.getId());
        intent.putExtra("comic_title", comic.getTitle());
        intent.putExtra("chapter_title", firstChapter.getTitle());
        startActivity(intent);
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
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        ivCover.setVisibility(loading ? View.GONE : View.VISIBLE);
        btnStartRead.setVisibility(loading ? View.GONE : View.VISIBLE);
    }

    private void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
    }

    private class ChapterAdapter extends RecyclerView.Adapter<ChapterAdapter.ChapterViewHolder> {

        @NonNull
        @Override
        public ChapterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chapter, parent, false);
            return new ChapterViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ChapterViewHolder holder, int position) {
            ComicChapter chapter = chapterList.get(position);
            holder.tvTitle.setText(chapter.getTitle());
            int pages = chapter.getImages() != null ? chapter.getImages().size() : 0;
            holder.tvPages.setText(pages + " 页");
            
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(ComicDetailActivity.this, ComicReadActivity.class);
                intent.putExtra("slug", comicSlug);
                intent.putExtra("chapter_id", chapter.getId());
                intent.putExtra("comic_title", comic.getTitle());
                intent.putExtra("chapter_title", chapter.getTitle());
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return chapterList.size();
        }

        class ChapterViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvPages;

            ChapterViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tv_chapter_title);
                tvPages = itemView.findViewById(R.id.tv_pages);
            }
        }
    }
}