package com.android.personal.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.personal.R;
import com.android.personal.VideoDetailActivity;
import com.android.personal.api.ApiClient;
import com.android.personal.api.model.Category;
import com.android.personal.api.model.Video;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VideoListFragment extends Fragment {

    private RecyclerView recyclerView;
    private CircularProgressIndicator progressBar;
    private LinearLayout categoryContainer;
    private VideoAdapter adapter;
    private List<Video> videoList = new ArrayList<>();
    private List<Category> allCategories = new ArrayList<>();
    private List<Category> videoCategories = new ArrayList<>();
    private int selectedCategoryId = -1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_video_list, container, false);

        recyclerView = view.findViewById(R.id.recycler_videos);
        progressBar = view.findViewById(R.id.progress_bar);
        categoryContainer = view.findViewById(R.id.category_container);

        adapter = new VideoAdapter();
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        recyclerView.setAdapter(adapter);

        loadCategories();
        loadVideos();

        return view;
    }

    private void loadCategories() {
        ApiClient.getApiService(getContext()).getCategories().enqueue(new Callback<List<Category>>() {
            @Override
            public void onResponse(Call<List<Category>> call, Response<List<Category>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allCategories.clear();
                    allCategories.addAll(response.body());
                    updateVideoCategories();
                }
            }

            @Override
            public void onFailure(Call<List<Category>> call, Throwable t) {
                // 即使加载失败，也显示空列表，等待视频数据加载
            }
        });
    }

    private void loadVideos() {
        setLoading(true);
        ApiClient.getApiService(getContext()).getVideos().enqueue(new Callback<List<Video>>() {
            @Override
        public void onResponse(Call<List<Video>> call, Response<List<Video>> response) {
            setLoading(false);
            if (response.isSuccessful() && response.body() != null) {
                videoList.clear();
                videoList.addAll(response.body());
                
                // 调试：打印前3个视频的标题
                android.util.Log.d("VideoList", "=== API返回的视频数据 ===");
                for (int i = 0; i < Math.min(3, videoList.size()); i++) {
                    Video v = videoList.get(i);
                    android.util.Log.d("VideoList", "视频" + i + ": title=" + v.getTitle() 
                        + ", categories=" + v.getCategories()
                        + ", category_names=" + v.getCategory_names());
                }
                android.util.Log.d("VideoList", "=== 共 " + videoList.size() + " 个视频 ===");
                
                // 根据视频数据更新分类列表
                updateVideoCategories();
                
                filterVideos();
            } else {
                Toast.makeText(getContext(), "加载视频失败", Toast.LENGTH_SHORT).show();
            }
        }

            @Override
            public void onFailure(Call<List<Video>> call, Throwable t) {
                setLoading(false);
                Toast.makeText(getContext(), "网络错误", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateVideoCategories() {
        // 从视频数据中提取所有分类ID
        Set<Integer> videoCategoryIds = new HashSet<>();
        for (Video video : videoList) {
            if (video.getCategories() != null) {
                videoCategoryIds.addAll(video.getCategories());
            }
        }
        
        // 根据分类ID筛选对应的分类
        videoCategories.clear();
        videoCategories.add(0, createAllCategory()); // 添加"全部"选项
        
        for (Category category : allCategories) {
            if (videoCategoryIds.contains(category.getId())) {
                videoCategories.add(category);
            }
        }
        
        showCategories();
    }

    private Category createAllCategory() {
        Category allCategory = new Category();
        allCategory.setId(-1);
        allCategory.setName(getString(R.string.all));
        return allCategory;
    }

    private void showCategories() {
        categoryContainer.removeAllViews();
        for (Category category : videoCategories) {
            TextView tvCategory = new TextView(getContext());
            tvCategory.setText(category.getName());
            tvCategory.setId(category.getId());
            
            boolean isSelected = category.getId() == selectedCategoryId;
            if (isSelected) {
                tvCategory.setBackgroundResource(R.drawable.flora_button_bg);
                tvCategory.setTextColor(getResources().getColor(R.color.white, getContext().getTheme()));
            } else {
                tvCategory.setBackgroundResource(R.drawable.flora_edittext_bg);
                tvCategory.setTextColor(getResources().getColor(R.color.flora_stem, getContext().getTheme()));
            }
            
            tvCategory.setTextSize(16);
            tvCategory.setPadding(
                (int) getResources().getDimension(R.dimen.category_padding_horizontal),
                (int) getResources().getDimension(R.dimen.category_padding_vertical),
                (int) getResources().getDimension(R.dimen.category_padding_horizontal),
                (int) getResources().getDimension(R.dimen.category_padding_vertical)
            );
            
            tvCategory.setClickable(true);
            tvCategory.setFocusable(true);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            int marginEnd = (int) android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());
            params.setMarginEnd(marginEnd);
            tvCategory.setLayoutParams(params);

            tvCategory.setOnClickListener(v -> {
                selectedCategoryId = category.getId();
                showCategories();
                filterVideos();
            });

            categoryContainer.addView(tvCategory);
        }
    }

    private void filterVideos() {
        List<Video> filteredList = new ArrayList<>();
        
        if (selectedCategoryId == -1) {
            // 全部 - 只显示有分类的视频（过滤掉自动同步的未分类视频）
            android.util.Log.d("VideoList", "filterVideos: 全部分类, videoList.size=" + videoList.size());
            for (Video video : videoList) {
                if (video.getCategories() != null && !video.getCategories().isEmpty()) {
                    filteredList.add(video);
                }
            }
        } else {
            android.util.Log.d("VideoList", "filterVideos: 分类ID=" + selectedCategoryId);
            // 根据分类ID过滤
            for (Video video : videoList) {
                if (video.getCategories() != null && video.getCategories().contains(selectedCategoryId)) {
                    filteredList.add(video);
                }
            }
        }
        
        android.util.Log.d("VideoList", "filterVideos: filteredList.size=" + filteredList.size());
        adapter.setVideos(filteredList);
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(loading ? View.GONE : View.VISIBLE);
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

    private class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VideoViewHolder> {
        private List<Video> displayList = new ArrayList<>();

        void setVideos(List<Video> videos) {
            this.displayList.clear();
            this.displayList.addAll(videos);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_video, parent, false);
            return new VideoViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
            Video video = displayList.get(position);
            // 直接使用后端返回的标题
            String title = video.getTitle();
            android.util.Log.d("VideoList", "bindViewHolder position=" + position + ", title=" + title);
            holder.tvTitle.setText(title);

            if (video.getThumbnail() != null && !video.getThumbnail().isEmpty()) {
                String imageUrl = getImageUrl(video.getThumbnail());
                Picasso.get().load(imageUrl).into(holder.ivThumbnail);
            }

            if (video.getCategory_names() != null && !video.getCategory_names().isEmpty()) {
                holder.tvCategory.setText(video.getCategory_names().get(0));
            }
            
            holder.itemView.setOnClickListener(v -> {
                android.content.Intent intent = new android.content.Intent(getContext(), VideoDetailActivity.class);
                intent.putExtra("slug", video.getSlug());
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return displayList.size();
        }

        class VideoViewHolder extends RecyclerView.ViewHolder {
            android.widget.ImageView ivThumbnail;
            TextView tvTitle;
            TextView tvCategory;

            VideoViewHolder(@NonNull View itemView) {
                super(itemView);
                ivThumbnail = itemView.findViewById(R.id.iv_thumbnail);
                tvTitle = itemView.findViewById(R.id.tv_title);
                tvCategory = itemView.findViewById(R.id.tv_category);
            }
        }
    }
}
