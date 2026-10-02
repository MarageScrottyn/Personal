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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.personal.R;
import com.android.personal.api.ApiClient;
import com.android.personal.api.model.CloudFile;
import com.android.personal.api.model.FolderRequest;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CloudDriveFragment extends Fragment {

    private RecyclerView recyclerView;
    private CircularProgressIndicator progressBar;
    private MaterialButton btnCreateFolder, btnUploadFile;
    private LinearLayout breadcrumb;
    private CloudFileAdapter adapter;
    private List<CloudFile> fileList = new ArrayList<>();
    private Integer currentParentId = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cloud_drive, container, false);

        recyclerView = view.findViewById(R.id.recycler_files);
        progressBar = view.findViewById(R.id.progress_bar);
        btnCreateFolder = view.findViewById(R.id.btn_create_folder);
        btnUploadFile = view.findViewById(R.id.btn_upload_file);
        breadcrumb = view.findViewById(R.id.breadcrumb);

        adapter = new CloudFileAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        btnCreateFolder.setOnClickListener(v -> showCreateFolderDialog());
        btnUploadFile.setOnClickListener(v -> Toast.makeText(getContext(), "上传功能开发中", Toast.LENGTH_SHORT).show());

        loadFiles();

        return view;
    }

    private void loadFiles() {
        setLoading(true);
        ApiClient.getApiService(getContext()).getCloudFiles(currentParentId).enqueue(new Callback<List<CloudFile>>() {
            @Override
            public void onResponse(Call<List<CloudFile>> call, Response<List<CloudFile>> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    fileList.clear();
                    fileList.addAll(response.body());
                    adapter.notifyDataSetChanged();
                    updateBreadcrumb();
                } else {
                    Toast.makeText(getContext(), "加载文件失败", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<CloudFile>> call, Throwable t) {
                setLoading(false);
                Toast.makeText(getContext(), "网络错误", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(loading ? View.GONE : View.VISIBLE);
    }

    private void updateBreadcrumb() {
        breadcrumb.removeAllViews();

        TextView home = new TextView(getContext());
        home.setText("云盘");
        home.setTextAppearance(R.style.FloraBreadcrumbHome);
        home.setOnClickListener(v -> {
            currentParentId = null;
            loadFiles();
        });
        breadcrumb.addView(home);

        if (currentParentId != null) {
            TextView separator = new TextView(getContext());
            separator.setText("/");
            separator.setTextAppearance(R.style.FloraBreadcrumbItem);
            breadcrumb.addView(separator);

            TextView current = new TextView(getContext());
            current.setText("当前文件夹");
            current.setTextAppearance(R.style.FloraBreadcrumbItem);
            breadcrumb.addView(current);
        }
    }

    private void showCreateFolderDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_create_folder, null);

        TextInputEditText etName = dialogView.findViewById(R.id.et_folder_name);

        new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .setTitle(R.string.create_folder)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    if (!name.isEmpty()) {
                        createFolder(name);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void createFolder(String name) {
        FolderRequest request = new FolderRequest(name, currentParentId);
        ApiClient.getApiService(getContext()).createFolder(request).enqueue(new Callback<CloudFile>() {
            @Override
            public void onResponse(Call<CloudFile> call, Response<CloudFile> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "文件夹创建成功", Toast.LENGTH_SHORT).show();
                    loadFiles();
                } else {
                    Toast.makeText(getContext(), "创建失败", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<CloudFile> call, Throwable t) {
                Toast.makeText(getContext(), "网络错误", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteFile(int id) {
        ApiClient.getApiService(getContext()).deleteFile(id).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "删除成功", Toast.LENGTH_SHORT).show();
                    loadFiles();
                } else {
                    Toast.makeText(getContext(), "删除失败", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(getContext(), "网络错误", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private class CloudFileAdapter extends RecyclerView.Adapter<CloudFileAdapter.FileViewHolder> {

        @NonNull
        @Override
        public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cloud_file, parent, false);
            return new FileViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
            CloudFile file = fileList.get(position);
            holder.tvName.setText(file.getName());
            holder.tvSize.setText(file.getFormatted_size());
            holder.tvDate.setText(file.getCreated_at());

            boolean isFolder = "folder".equals(file.getFile_type());
            holder.ivIcon.setImageResource(isFolder ? R.drawable.ic_folder : R.drawable.ic_file);

            holder.itemView.setOnClickListener(v -> {
                if (isFolder) {
                    currentParentId = file.getId();
                    loadFiles();
                } else {
                    Toast.makeText(getContext(), "文件预览功能开发中", Toast.LENGTH_SHORT).show();
                }
            });

            holder.itemView.setOnLongClickListener(v -> {
                showFileOptions(file);
                return true;
            });
        }

        @Override
        public int getItemCount() {
            return fileList.size();
        }

        class FileViewHolder extends RecyclerView.ViewHolder {
            android.widget.ImageView ivIcon;
            TextView tvName;
            TextView tvSize;
            TextView tvDate;

            FileViewHolder(@NonNull View itemView) {
                super(itemView);
                ivIcon = itemView.findViewById(R.id.iv_icon);
                tvName = itemView.findViewById(R.id.tv_name);
                tvSize = itemView.findViewById(R.id.tv_size);
                tvDate = itemView.findViewById(R.id.tv_date);
            }
        }
    }

    private void showFileOptions(CloudFile file) {
        String[] options;
        if ("folder".equals(file.getFile_type())) {
            options = new String[]{getString(R.string.delete)};
        } else {
            options = new String[]{getString(R.string.download), getString(R.string.delete)};
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setItems(options, (dialog, which) -> {
                    if (which == 0 && !"folder".equals(file.getFile_type())) {
                        Toast.makeText(getContext(), "下载功能开发中", Toast.LENGTH_SHORT).show();
                    } else {
                        showDeleteDialog(file.getId());
                    }
                }).show();
    }

    private void showDeleteDialog(int id) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("确认删除")
                .setMessage("确定要删除吗？")
                .setPositiveButton(R.string.delete, (dialog, which) -> deleteFile(id))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}