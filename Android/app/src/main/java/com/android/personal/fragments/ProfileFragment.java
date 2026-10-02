package com.android.personal.fragments;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.android.personal.AdminActivity;
import com.android.personal.LoginActivity;
import com.android.personal.MainActivity;
import com.android.personal.R;
import com.android.personal.api.ApiClient;
import com.android.personal.api.model.BaseResponse;
import com.android.personal.api.model.ChangePasswordRequest;
import com.android.personal.api.model.User;
import com.android.personal.utils.AuthManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {

    private TextView tvUsername;
    private TextView tvEmail;
    private TextView tvThemeValue;
    private TextView tvUserType;
    private TextView tvPermissions;
    private TextView tvAccessComicCategories;
    private TextView tvAccessVideoCategories;
    private MaterialButton btnLogout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        if (getActivity() != null) {
            getActivity().getWindow().setStatusBarColor(getResources().getColor(R.color.flora_leaf, getContext().getTheme()));
        }

        tvUsername = view.findViewById(R.id.tv_username);
        tvEmail = view.findViewById(R.id.tv_email);
        tvThemeValue = view.findViewById(R.id.tv_theme_value);
        tvUserType = view.findViewById(R.id.tv_user_type);
        tvPermissions = view.findViewById(R.id.tv_permissions);
        tvAccessComicCategories = view.findViewById(R.id.tv_access_comic_categories);
        tvAccessVideoCategories = view.findViewById(R.id.tv_access_video_categories);
        btnLogout = view.findViewById(R.id.btn_logout);

        LinearLayout llSyncMedia = view.findViewById(R.id.ll_sync_media);
        LinearLayout llChangePassword = view.findViewById(R.id.ll_change_password);
        LinearLayout llThemeMode = view.findViewById(R.id.ll_theme_mode);
        LinearLayout llSettings = view.findViewById(R.id.ll_settings);
        LinearLayout llAdmin = view.findViewById(R.id.ll_admin);

        User user = AuthManager.getUser(getContext());
        if (user != null) {
            tvUsername.setText(user.getUsername());
            tvEmail.setText(user.getEmail());
            
            String userType = user.getUser_type();
            if (userType != null) {
                if ("admin".equals(userType)) {
                    tvUserType.setText("管理员");
                } else if ("premium".equals(userType)) {
                    tvUserType.setText("高级用户");
                } else {
                    tvUserType.setText("普通用户");
                }
            }
            
            if (user.getPermissions() != null && !user.getPermissions().isEmpty()) {
                tvPermissions.setText("权限: " + String.join(", ", user.getPermissions()));
            } else {
                tvPermissions.setText("权限: 无");
            }
            
            if (user.getCan_access_comic_categories() != null && !user.getCan_access_comic_categories().isEmpty()) {
                tvAccessComicCategories.setText("可访问漫画分类: " + String.join(", ", user.getCan_access_comic_categories()));
            } else {
                tvAccessComicCategories.setText("可访问漫画分类: 全部");
            }
            
            if (user.getCan_access_video_categories() != null && !user.getCan_access_video_categories().isEmpty()) {
                tvAccessVideoCategories.setText("可访问视频分类: " + String.join(", ", user.getCan_access_video_categories()));
            } else {
                tvAccessVideoCategories.setText("可访问视频分类: 全部");
            }
        }

        updateThemeValue();

        llSyncMedia.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                syncMedia();
            }
        });

        llChangePassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showChangePasswordDialog();
            }
        });

        llThemeMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showThemeDialog();
            }
        });

        llSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getContext(), "设置功能开发中", Toast.LENGTH_SHORT).show();
            }
        });

        llAdmin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getContext(), AdminActivity.class));
            }
        });

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLogoutDialog();
            }
        });

        return view;
    }

    private void syncMedia() {
        ApiClient.getApiService(getContext()).syncMedia().enqueue(new Callback<BaseResponse>() {
            @Override
            public void onResponse(Call<BaseResponse> call, Response<BaseResponse> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "媒体同步成功", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "同步失败", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<BaseResponse> call, Throwable t) {
                Toast.makeText(getContext(), "网络错误", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showChangePasswordDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_change_password, null);
        
        com.google.android.material.dialog.MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        builder.setView(dialogView);
        
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        
        dialogView.findViewById(R.id.btn_cancel).setOnClickListener(v -> dialog.dismiss());
        
        dialogView.findViewById(R.id.btn_confirm).setOnClickListener(v -> {
            android.widget.EditText etCurrent = dialogView.findViewById(R.id.et_current_password);
            android.widget.EditText etNew = dialogView.findViewById(R.id.et_new_password);
            android.widget.EditText etConfirm = dialogView.findViewById(R.id.et_confirm_password);
            
            String currentPassword = etCurrent.getText().toString().trim();
            String newPassword = etNew.getText().toString().trim();
            String confirmPassword = etConfirm.getText().toString().trim();
            
            if (currentPassword.isEmpty()) {
                etCurrent.setError("请输入当前密码");
                return;
            }
            if (newPassword.isEmpty()) {
                etNew.setError("请输入新密码");
                return;
            }
            if (newPassword.length() < 6) {
                etNew.setError("新密码长度不能少于6个字符");
                return;
            }
            if (!newPassword.equals(confirmPassword)) {
                etConfirm.setError("两次输入的密码不一致");
                return;
            }
            
            changePassword(currentPassword, newPassword, dialog);
        });
        
        dialog.show();
    }
    
    private void changePassword(String currentPassword, String newPassword, androidx.appcompat.app.AlertDialog dialog) {
        ChangePasswordRequest request = new ChangePasswordRequest(currentPassword, newPassword);
        ApiClient.getApiService(getContext()).changePassword(request).enqueue(new Callback<BaseResponse>() {
            @Override
            public void onResponse(Call<BaseResponse> call, Response<BaseResponse> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "密码修改成功", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                } else {
                    try {
                        if (response.errorBody() != null) {
                            String errorMsg = response.errorBody().string();
                            if (errorMsg.contains("detail")) {
                                Toast.makeText(getContext(), "当前密码错误", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getContext(), "修改密码失败", Toast.LENGTH_SHORT).show();
                            }
                        }
                    } catch (Exception e) {
                        Toast.makeText(getContext(), "修改密码失败", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<BaseResponse> call, Throwable t) {
                Toast.makeText(getContext(), "网络错误", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLogoutDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("退出登录")
                .setMessage("确定要退出登录吗？")
                .setPositiveButton("确定", (dialog, which) -> {
                    AuthManager.clearAuth(getContext());
                    Intent intent = new Intent(getContext(), LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void updateThemeValue() {
        int themeMode = AuthManager.getThemeMode(getContext());
        String themeValue;
        switch (themeMode) {
            case AuthManager.THEME_MODE_LIGHT:
                themeValue = getString(R.string.theme_light);
                break;
            case AuthManager.THEME_MODE_DARK:
                themeValue = getString(R.string.theme_dark);
                break;
            default:
                themeValue = getString(R.string.theme_follow_system);
                break;
        }
        tvThemeValue.setText(themeValue);
    }

    private void showThemeDialog() {
        String[] themes = {getString(R.string.theme_follow_system), getString(R.string.theme_light), getString(R.string.theme_dark)};
        int currentMode = AuthManager.getThemeMode(getContext());

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.theme_mode))
                .setSingleChoiceItems(themes, currentMode, (dialog, which) -> {
                    int newMode;
                    switch (which) {
                        case 1:
                            newMode = AuthManager.THEME_MODE_LIGHT;
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                            break;
                        case 2:
                            newMode = AuthManager.THEME_MODE_DARK;
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                            break;
                        default:
                            newMode = AuthManager.THEME_MODE_FOLLOW_SYSTEM;
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                            break;
                    }
                    AuthManager.setThemeMode(getContext(), newMode);
                    updateThemeValue();
                    dialog.dismiss();
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }
}