package com.android.personal.fragments;

import android.os.Bundle;
import android.util.Log;
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

import com.android.personal.ComicDetailActivity;
import com.android.personal.R;
import com.android.personal.api.ApiClient;
import com.android.personal.api.model.Category;
import com.android.personal.api.model.Comic;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ComicListFragment extends Fragment {

    private RecyclerView recyclerView;
    private CircularProgressIndicator progressBar;
    private LinearLayout categoryContainer;
    private ComicAdapter adapter;
    private List<Comic> comicList = new ArrayList<>();
    private List<Category> allCategories = new ArrayList<>();
    private List<Category> comicCategories = new ArrayList<>();
    private int selectedCategoryId = -1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_comic_list, container, false);

        recyclerView = view.findViewById(R.id.recycler_comics);
        progressBar = view.findViewById(R.id.progress_bar);
        categoryContainer = view.findViewById(R.id.category_container);

        adapter = new ComicAdapter();
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        recyclerView.setAdapter(adapter);

        loadCategories();
        loadComics();

        return view;
    }

    private void loadCategories() {
        ApiClient.getApiService(getContext()).getCategories().enqueue(new Callback<List<Category>>() {
            @Override
            public void onResponse(Call<List<Category>> call, Response<List<Category>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allCategories.clear();
                    allCategories.addAll(response.body());
                    updateComicCategories();
                }
            }

            @Override
            public void onFailure(Call<List<Category>> call, Throwable t) {
                Log.e("ComicList", "加载分类失败: " + t.getMessage());
            }
        });
    }

    private void loadComics() {
        setLoading(true);
        Log.d("ComicList", "开始加载漫画数据");
        Log.d("ComicList", "BASE_URL: " + com.android.personal.api.ApiClient.BASE_URL);
        Log.d("ComicList", "Token: " + com.android.personal.utils.AuthManager.getAccessToken(getContext()));
        
        Call<List<Comic>> call = ApiClient.getApiService(getContext()).getComics();
        Log.d("ComicList", "请求URL: " + call.request().url());
        
        call.enqueue(new Callback<List<Comic>>() {
            @Override
            public void onResponse(Call<List<Comic>> call, Response<List<Comic>> response) {
                setLoading(false);
                Log.d("ComicList", "响应状态码: " + response.code());
                Log.d("ComicList", "响应是否成功: " + response.isSuccessful());
                
                if (response.isSuccessful() && response.body() != null) {
                    Log.d("ComicList", "API返回漫画数量: " + response.body().size());
                    
                    comicList.clear();
                    comicList.addAll(response.body());
                    
                    // 根据漫画数据更新分类列表
                    updateComicCategories();
                    
                    filterComics();
                } else {
                    Log.d("ComicList", "加载漫画失败");
                    try {
                        if (response.errorBody() != null) {
                            Log.d("ComicList", "错误信息: " + response.errorBody().string());
                        }
                    } catch (Exception e) {
                        Log.d("ComicList", "读取错误信息失败: " + e.getMessage());
                    }
                    Toast.makeText(getContext(), "加载漫画失败", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Comic>> call, Throwable t) {
                setLoading(false);
                Log.d("ComicList", "网络错误: " + t.getMessage());
                Toast.makeText(getContext(), "网络错误", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateComicCategories() {
        // 从漫画数据中提取所有分类ID
        Set<Integer> comicCategoryIds = new HashSet<>();
        for (Comic comic : comicList) {
            if (comic.getCategories() != null) {
                comicCategoryIds.addAll(comic.getCategories());
            }
        }
        
        // 根据分类ID筛选对应的分类
        comicCategories.clear();
        comicCategories.add(0, createAllCategory()); // 添加"全部"选项
        
        for (Category category : allCategories) {
            if (comicCategoryIds.contains(category.getId())) {
                comicCategories.add(category);
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
        for (Category category : comicCategories) {
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
                filterComics();
            });

            categoryContainer.addView(tvCategory);
        }
    }

    private void filterComics() {
        List<Comic> filteredList = new ArrayList<>();
        
        if (selectedCategoryId == -1) {
            // 全部
            filteredList.addAll(comicList);
        } else {
            // 根据分类ID过滤
            for (Comic comic : comicList) {
                if (comic.getCategories() != null && comic.getCategories().contains(selectedCategoryId)) {
                    filteredList.add(comic);
                }
            }
        }
        
        adapter.setComics(filteredList);
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

    private class ComicAdapter extends RecyclerView.Adapter<ComicAdapter.ComicViewHolder> {
        private List<Comic> displayList = new ArrayList<>();

        void setComics(List<Comic> comics) {
            this.displayList.clear();
            this.displayList.addAll(comics);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ComicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comic, parent, false);
            return new ComicViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ComicViewHolder holder, int position) {
            Comic comic = displayList.get(position);
            holder.tvTitle.setText(comic.getTitle());
            
            if (comic.getCover_image() != null && !comic.getCover_image().isEmpty()) {
                String imageUrl = getImageUrl(comic.getCover_image());
                Picasso.get().load(imageUrl).into(holder.ivCover);
            }
            
            if (comic.getCategory_names() != null && !comic.getCategory_names().isEmpty()) {
                holder.tvCategory.setText(comic.getCategory_names().get(0));
            }
            
            holder.itemView.setOnClickListener(v -> {
                android.content.Intent intent = new android.content.Intent(getContext(), ComicDetailActivity.class);
                intent.putExtra("slug", comic.getSlug());
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return displayList.size();
        }

        class ComicViewHolder extends RecyclerView.ViewHolder {
            android.widget.ImageView ivCover;
            TextView tvTitle;
            TextView tvCategory;

            ComicViewHolder(@NonNull View itemView) {
                super(itemView);
                ivCover = itemView.findViewById(R.id.iv_cover);
                tvTitle = itemView.findViewById(R.id.tv_title);
                tvCategory = itemView.findViewById(R.id.tv_category);
            }
        }
    }
}
