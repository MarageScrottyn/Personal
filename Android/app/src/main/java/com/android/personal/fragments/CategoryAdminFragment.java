package com.android.personal.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CategoryAdminFragment extends Fragment {

    private RecyclerView rvCategories;
    private FloatingActionButton fabAdd;
    private List<Category> categoryList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_category_admin, container, false);

        rvCategories = view.findViewById(R.id.rv_categories);
        fabAdd = view.findViewById(R.id.fab_add);

        rvCategories.setLayoutManager(new LinearLayoutManager(getContext()));

        fabAdd.setOnClickListener(v -> showCategoryDialog(null));

        fetchCategories();

        return view;
    }

    private void fetchCategories() {
        ApiClient.getApiService(getContext()).getCategories().enqueue(new Callback<List<Category>>() {
            @Override
            public void onResponse(Call<List<Category>> call, Response<List<Category>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    categoryList.clear();
                    categoryList.addAll(response.body());
                    rvCategories.setAdapter(new CategoryAdapter(categoryList));
                }
            }

            @Override
            public void onFailure(Call<List<Category>> call, Throwable t) {
                Toast.makeText(getContext(), "获取分类列表失败", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showCategoryDialog(Category category) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_category_form, null);
        
        EditText etName = dialogView.findViewById(R.id.et_name);
        EditText etSlug = dialogView.findViewById(R.id.et_slug);
        EditText etParent = dialogView.findViewById(R.id.et_parent);
        
        if (category != null) {
            etName.setText(category.getName());
            etSlug.setText(category.getSlug());
            etSlug.setEnabled(false);
            if (category.getParent() != null) {
                etParent.setText(String.valueOf(category.getParent()));
            }
        }
        
        String title = category == null ? "新增分类" : "编辑分类";
        
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(title)
                .setView(dialogView)
                .setPositiveButton("保存", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String slug = etSlug.getText().toString().trim();
                    String parentStr = etParent.getText().toString().trim();
                    
                    if (name.isEmpty()) {
                        Toast.makeText(getContext(), "请输入分类名称", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (slug.isEmpty()) {
                        Toast.makeText(getContext(), "请输入slug", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    Category newCategory = new Category();
                    newCategory.setName(name);
                    newCategory.setSlug(slug);
                    if (!parentStr.isEmpty()) {
                        try {
                            newCategory.setParent(Integer.parseInt(parentStr));
                        } catch (NumberFormatException e) {
                            // 忽略
                        }
                    }
                    newCategory.setPermission_level("regular");
                    
                    if (category == null) {
                        createCategory(newCategory);
                    } else {
                        updateCategory(category.getSlug(), newCategory);
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }
    
    private void createCategory(Category category) {
        ApiClient.getApiService(getContext()).createCategory(category).enqueue(new Callback<Category>() {
            @Override
            public void onResponse(Call<Category> call, Response<Category> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "分类创建成功", Toast.LENGTH_SHORT).show();
                    fetchCategories();
                } else {
                    Toast.makeText(getContext(), "分类创建失败", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Category> call, Throwable t) {
                Toast.makeText(getContext(), "网络错误", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void updateCategory(String slug, Category category) {
        ApiClient.getApiService(getContext()).updateCategory(slug, category).enqueue(new Callback<Category>() {
            @Override
            public void onResponse(Call<Category> call, Response<Category> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "分类更新成功", Toast.LENGTH_SHORT).show();
                    fetchCategories();
                } else {
                    Toast.makeText(getContext(), "分类更新失败", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Category> call, Throwable t) {
                Toast.makeText(getContext(), "网络错误", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void deleteCategory(Category category) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("确认删除")
                .setMessage("确定要删除分类 " + category.getName() + " 吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    ApiClient.getApiService(getContext()).deleteCategory(category.getSlug()).enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(getContext(), "分类删除成功", Toast.LENGTH_SHORT).show();
                                fetchCategories();
                            } else {
                                Toast.makeText(getContext(), "分类删除失败", Toast.LENGTH_SHORT).show();
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

    private class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {
        private List<Category> categories;

        CategoryAdapter(List<Category> categories) {
            this.categories = categories;
        }

        @NonNull
        @Override
        public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category_admin, parent, false);
            return new CategoryViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
            Category category = categories.get(position);
            holder.tvName.setText(category.getName());
            holder.tvSlug.setText(category.getSlug());
            
            String permission = category.getPermission_level();
            if ("special".equals(permission)) {
                holder.tvPermission.setText("特殊权限");
            } else {
                holder.tvPermission.setText("普通权限");
            }
            
            holder.btnEdit.setOnClickListener(v -> showCategoryDialog(category));
            holder.btnDelete.setOnClickListener(v -> deleteCategory(category));
        }

        @Override
        public int getItemCount() {
            return categories.size();
        }

        class CategoryViewHolder extends RecyclerView.ViewHolder {
            TextView tvName;
            TextView tvSlug;
            TextView tvPermission;
            android.widget.Button btnEdit;
            android.widget.Button btnDelete;

            CategoryViewHolder(View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_name);
                tvSlug = itemView.findViewById(R.id.tv_slug);
                tvPermission = itemView.findViewById(R.id.tv_permission);
                btnEdit = itemView.findViewById(R.id.btn_edit);
                btnDelete = itemView.findViewById(R.id.btn_delete);
            }
        }
    }
}