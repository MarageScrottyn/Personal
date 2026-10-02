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
import com.android.personal.api.model.Comic;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ComicAdminFragment extends Fragment {

    private RecyclerView rvComics;
    private FloatingActionButton fabAdd;
    private List<Comic> comicList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_comic_admin, container, false);

        rvComics = view.findViewById(R.id.rv_comics);
        fabAdd = view.findViewById(R.id.fab_add);

        rvComics.setLayoutManager(new GridLayoutManager(getContext(), 2));

        fabAdd.setOnClickListener(v -> Toast.makeText(getContext(), "添加漫画功能开发中", Toast.LENGTH_SHORT).show());

        fetchComics();

        return view;
    }

    private void fetchComics() {
        ApiClient.getApiService(getContext()).getComics().enqueue(new Callback<List<Comic>>() {
            @Override
            public void onResponse(Call<List<Comic>> call, Response<List<Comic>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    comicList.clear();
                    comicList.addAll(response.body());
                    rvComics.setAdapter(new ComicAdapter(comicList));
                }
            }

            @Override
            public void onFailure(Call<List<Comic>> call, Throwable t) {
                Toast.makeText(getContext(), "获取漫画列表失败", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteComic(Comic comic) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("确认删除")
                .setMessage("确定要删除漫画 " + comic.getTitle() + " 吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    ApiClient.getApiService(getContext()).deleteComic(comic.getSlug()).enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(getContext(), "漫画删除成功", Toast.LENGTH_SHORT).show();
                                fetchComics();
                            } else {
                                Toast.makeText(getContext(), "漫画删除失败", Toast.LENGTH_SHORT).show();
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

    private class ComicAdapter extends RecyclerView.Adapter<ComicAdapter.ComicViewHolder> {
        private List<Comic> comics;

        ComicAdapter(List<Comic> comics) {
            this.comics = comics;
        }

        @NonNull
        @Override
        public ComicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comic_admin, parent, false);
            return new ComicViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ComicViewHolder holder, int position) {
            Comic comic = comics.get(position);
            holder.tvTitle.setText(comic.getTitle());
            holder.tvAuthor.setText(comic.getAuthor());
            holder.tvCategory.setText(comic.getCategory_names() != null && !comic.getCategory_names().isEmpty() 
                ? comic.getCategory_names().get(0) : "无分类");
            
            if (comic.getCover_image() != null && !comic.getCover_image().isEmpty()) {
                String imageUrl = getImageUrl(comic.getCover_image());
                Picasso.get().load(imageUrl).into(holder.ivCover);
            }
            
            holder.btnDelete.setOnClickListener(v -> deleteComic(comic));
        }

        @Override
        public int getItemCount() {
            return comics.size();
        }

        class ComicViewHolder extends RecyclerView.ViewHolder {
            android.widget.ImageView ivCover;
            TextView tvTitle;
            TextView tvAuthor;
            TextView tvCategory;
            android.widget.Button btnDelete;

            ComicViewHolder(View itemView) {
                super(itemView);
                ivCover = itemView.findViewById(R.id.iv_cover);
                tvTitle = itemView.findViewById(R.id.tv_title);
                tvAuthor = itemView.findViewById(R.id.tv_author);
                tvCategory = itemView.findViewById(R.id.tv_category);
                btnDelete = itemView.findViewById(R.id.btn_delete);
            }
        }
    }
}