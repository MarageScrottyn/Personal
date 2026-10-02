package com.android.personal.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.personal.R;
import com.android.personal.api.ApiClient;
import com.android.personal.api.model.Category;
import com.android.personal.api.model.Comic;
import com.android.personal.api.model.ResetPasswordRequest;
import com.android.personal.api.model.User;
import com.android.personal.api.model.Video;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DashboardFragment extends Fragment {

    private TextView tvComicCount;
    private TextView tvVideoCount;
    private TextView tvCategoryCount;
    private RecyclerView rvRecentItems;
    private RecyclerView rvUsers;
    private LinearLayout llSyncMedia;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        tvComicCount = view.findViewById(R.id.tv_comic_count);
        tvVideoCount = view.findViewById(R.id.tv_video_count);
        tvCategoryCount = view.findViewById(R.id.tv_category_count);
        rvRecentItems = view.findViewById(R.id.rv_recent_items);
        rvUsers = view.findViewById(R.id.rv_users);
        llSyncMedia = view.findViewById(R.id.ll_sync_media);

        rvRecentItems.setLayoutManager(new LinearLayoutManager(getContext()));
        rvUsers.setLayoutManager(new LinearLayoutManager(getContext()));

        llSyncMedia.setOnClickListener(v -> syncMedia());

        fetchStats();
        fetchUsers();

        return view;
    }

    private void fetchStats() {
        ApiClient.getApiService(getContext()).getComics().enqueue(new Callback<List<Comic>>() {
            @Override
            public void onResponse(Call<List<Comic>> call, Response<List<Comic>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    tvComicCount.setText(String.valueOf(response.body().size()));
                    setupRecentItems(response.body(), null);
                } else {
                    tvComicCount.setText("0");
                }
            }

            @Override
            public void onFailure(Call<List<Comic>> call, Throwable t) {
                tvComicCount.setText("0");
            }
        });

        ApiClient.getApiService(getContext()).getVideos().enqueue(new Callback<List<Video>>() {
            @Override
            public void onResponse(Call<List<Video>> call, Response<List<Video>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    tvVideoCount.setText(String.valueOf(response.body().size()));
                    setupRecentItems(null, response.body());
                } else {
                    tvVideoCount.setText("0");
                }
            }

            @Override
            public void onFailure(Call<List<Video>> call, Throwable t) {
                tvVideoCount.setText("0");
            }
        });

        ApiClient.getApiService(getContext()).getCategories().enqueue(new Callback<List<Category>>() {
            @Override
            public void onResponse(Call<List<Category>> call, Response<List<Category>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    tvCategoryCount.setText(String.valueOf(response.body().size()));
                } else {
                    tvCategoryCount.setText("0");
                }
            }

            @Override
            public void onFailure(Call<List<Category>> call, Throwable t) {
                tvCategoryCount.setText("0");
            }
        });
    }

    private void setupRecentItems(List<Comic> comics, List<Video> videos) {
        if (comics != null && !comics.isEmpty()) {
            rvRecentItems.setAdapter(new RecentItemAdapter(comics));
        } else if (videos != null && !videos.isEmpty()) {
            rvRecentItems.setAdapter(new RecentItemAdapter(null, videos));
        }
    }

    private class RecentItemAdapter extends RecyclerView.Adapter<RecentItemAdapter.RecentViewHolder> {
        private List<Comic> comics;
        private List<Video> videos;
        private boolean isComic;

        RecentItemAdapter(List<Comic> comics) {
            this.comics = comics;
            this.isComic = true;
        }

        RecentItemAdapter(List<Comic> comics, List<Video> videos) {
            this.videos = videos;
            this.isComic = false;
        }

        @NonNull
        @Override
        public RecentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recent_admin, parent, false);
            return new RecentViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecentViewHolder holder, int position) {
            if (isComic && comics != null) {
                Comic comic = comics.get(position);
                holder.tvTitle.setText(comic.getTitle());
                holder.tvType.setText("漫画");
                holder.tvType.setTextColor(getContext().getResources().getColor(R.color.flora_leaf));
            } else if (!isComic && videos != null) {
                Video video = videos.get(position);
                holder.tvTitle.setText(video.getTitle());
                holder.tvType.setText("视频");
                holder.tvType.setTextColor(getContext().getResources().getColor(R.color.flora_bloom));
            }
        }

        @Override
        public int getItemCount() {
            return isComic ? (comics != null ? Math.min(comics.size(), 5) : 0) : (videos != null ? Math.min(videos.size(), 5) : 0);
        }

        class RecentViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle;
            TextView tvType;

            RecentViewHolder(View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tv_title);
                tvType = itemView.findViewById(R.id.tv_type);
            }
        }
    }

    private void fetchUsers() {
        ApiClient.getApiService(getContext()).getUsers().enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    rvUsers.setAdapter(new UserAdapter(response.body()));
                }
            }

            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                Toast.makeText(getContext(), "获取用户列表失败", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void syncMedia() {
        ApiClient.getApiService(getContext()).syncMedia().enqueue(new Callback<com.android.personal.api.model.BaseResponse>() {
            @Override
            public void onResponse(Call<com.android.personal.api.model.BaseResponse> call, Response<com.android.personal.api.model.BaseResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(getContext(), response.body().getMessage(), Toast.LENGTH_SHORT).show();
                    fetchStats();
                } else {
                    Toast.makeText(getContext(), "同步失败", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<com.android.personal.api.model.BaseResponse> call, Throwable t) {
                Toast.makeText(getContext(), "网络错误", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {
        private List<User> users;

        UserAdapter(List<User> users) {
            this.users = users;
        }

        @NonNull
        @Override
        public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user_admin, parent, false);
            return new UserViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
            User user = users.get(position);
            holder.tvUsername.setText(user.getUsername());
            holder.tvEmail.setText(user.getEmail());
            holder.tvUserType.setText(user.getUser_type() != null && user.getUser_type().equals("admin") ? "管理员" : "普通用户");
            
            holder.btnResetPassword.setOnClickListener(v -> showResetPasswordDialog(user));
        }

        @Override
        public int getItemCount() {
            return users.size();
        }

        class UserViewHolder extends RecyclerView.ViewHolder {
            TextView tvUsername;
            TextView tvEmail;
            TextView tvUserType;
            android.widget.Button btnResetPassword;

            UserViewHolder(View itemView) {
                super(itemView);
                tvUsername = itemView.findViewById(R.id.tv_username);
                tvEmail = itemView.findViewById(R.id.tv_email);
                tvUserType = itemView.findViewById(R.id.tv_user_type);
                btnResetPassword = itemView.findViewById(R.id.btn_reset_password);
            }
        }
    }
    
    private void showResetPasswordDialog(User user) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_reset_password, null);
        
        EditText etNewPassword = dialogView.findViewById(R.id.et_new_password);
        EditText etConfirmPassword = dialogView.findViewById(R.id.et_confirm_password);
        
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("重置密码")
                .setMessage("为用户 " + user.getUsername() + " 设置新密码")
                .setView(dialogView)
                .setPositiveButton("确认", (dialog, which) -> {
                    String newPassword = etNewPassword.getText().toString().trim();
                    String confirmPassword = etConfirmPassword.getText().toString().trim();
                    
                    if (newPassword.isEmpty()) {
                        Toast.makeText(getContext(), "请输入新密码", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (newPassword.length() < 6) {
                        Toast.makeText(getContext(), "密码长度不能少于6个字符", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!newPassword.equals(confirmPassword)) {
                        Toast.makeText(getContext(), "两次输入的密码不一致", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    resetPassword(user.getId(), newPassword);
                })
                .setNegativeButton("取消", null)
                .show();
    }
    
    private void resetPassword(int userId, String newPassword) {
        ResetPasswordRequest request = new ResetPasswordRequest(newPassword);
        ApiClient.getApiService(getContext()).resetPassword(userId, request).enqueue(new Callback<com.android.personal.api.model.BaseResponse>() {
            @Override
            public void onResponse(Call<com.android.personal.api.model.BaseResponse> call, Response<com.android.personal.api.model.BaseResponse> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "密码重置成功", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "密码重置失败", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<com.android.personal.api.model.BaseResponse> call, Throwable t) {
                Toast.makeText(getContext(), "网络错误", Toast.LENGTH_SHORT).show();
            }
        });
    }
}