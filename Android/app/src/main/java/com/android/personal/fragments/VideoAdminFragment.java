package com.android.personal.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.personal.R;
import com.android.personal.api.ApiClient;
import com.android.personal.api.model.Video;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VideoAdminFragment extends Fragment {

    private RecyclerView rvVideos;
    private FloatingActionButton fabAdd;
    private List<Video> videoList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_video_admin, container, false);

        rvVideos = view.findViewById(R.id.rv_videos);
        fabAdd = view.findViewById(R.id.fab_add);

        rvVideos.setLayoutManager(new GridLayoutManager(getContext(), 2));

        fabAdd.setOnClickListener(v -> Toast.makeText(getContext(), "添加视频功能开发中", Toast.LENGTH_SHORT).show());

        fetchVideos();

        return view;
    }

    private void fetchVideos() {
        ApiClient.getApiService(getContext()).getVideos().enqueue(new Callback<List<Video>>() {
            @Override
            public void onResponse(Call<List<Video>> call, Response<List<Video>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    videoList.clear();
                    videoList.addAll(response.body());
                    rvVideos.setAdapter(new VideoAdapter(videoList));
                }
            }

            @Override
            public void onFailure(Call<List<Video>> call, Throwable t) {
                Toast.makeText(getContext(), "获取视频列表失败", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteVideo(Video video) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("确认删除")
                .setMessage("确定要删除视频 " + video.getTitle() + " 吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    ApiClient.getApiService(getContext()).deleteVideo(video.getSlug()).enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(getContext(), "视频删除成功", Toast.LENGTH_SHORT).show();
                                fetchVideos();
                            } else {
                                Toast.makeText(getContext(), "视频删除失败", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            Toast.makeText(getContext(), "网络错误", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("取消", null)
                .show();
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
        private List<Video> videos;

        VideoAdapter(List<Video> videos) {
            this.videos = videos;
        }

        @NonNull
        @Override
        public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_video_admin, parent, false);
            return new VideoViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
            Video video = videos.get(position);
            holder.tvTitle.setText(video.getTitle());
            holder.tvDuration.setText(video.getDuration());
            holder.tvCategory.setText(video.getCategory_names() != null && !video.getCategory_names().isEmpty() 
                ? video.getCategory_names().get(0) : "无分类");
            
            if (video.getThumbnail() != null && !video.getThumbnail().isEmpty()) {
                String imageUrl = getImageUrl(video.getThumbnail());
                Picasso.get().load(imageUrl).into(holder.ivThumbnail);
            }
            
            holder.btnDelete.setOnClickListener(v -> deleteVideo(video));
        }

        @Override
        public int getItemCount() {
            return videos.size();
        }

        class VideoViewHolder extends RecyclerView.ViewHolder {
            android.widget.ImageView ivThumbnail;
            TextView tvTitle;
            TextView tvDuration;
            TextView tvCategory;
            android.widget.Button btnDelete;

            VideoViewHolder(View itemView) {
                super(itemView);
                ivThumbnail = itemView.findViewById(R.id.iv_thumbnail);
                tvTitle = itemView.findViewById(R.id.tv_title);
                tvDuration = itemView.findViewById(R.id.tv_duration);
                tvCategory = itemView.findViewById(R.id.tv_category);
                btnDelete = itemView.findViewById(R.id.btn_delete);
            }
        }
    }
}