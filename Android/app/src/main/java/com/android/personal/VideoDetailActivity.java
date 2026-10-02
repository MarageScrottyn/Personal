package com.android.personal;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.WindowCompat;

import com.android.personal.api.ApiClient;
import com.android.personal.api.model.Video;
import com.android.personal.utils.AuthManager;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.StyledPlayerView;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VideoDetailActivity extends AppCompatActivity {

    private LinearLayout btnBack, categoryContainer, videoContainer, videoInfo;
    private ScrollView contentScroll;
    private StyledPlayerView playerView, playerViewFullscreen;
    private ExoPlayer player;
    private TextView tvTitle, tvDescription, tvError;
    private CircularProgressIndicator progressBar;

    private String videoSlug;
    private Video video;
    private boolean isFullscreen = false;

    private float[] playbackSpeeds = {0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f};
    private String[] speedLabels = {"0.5x", "0.75x", "1.0x", "1.25x", "1.5x", "2.0x"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        applyTheme();

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        setContentView(R.layout.activity_video_detail);

        videoSlug = getIntent().getStringExtra("slug");
        if (videoSlug == null) {
            finish();
            return;
        }

        initViews();
        loadVideoDetail();
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
        contentScroll = findViewById(R.id.content_scroll);
        playerView = findViewById(R.id.video_view);
        playerViewFullscreen = findViewById(R.id.video_view_fullscreen);
        videoContainer = findViewById(R.id.video_container);
        videoInfo = findViewById(R.id.video_info);
        tvTitle = findViewById(R.id.tv_title);
        tvDescription = findViewById(R.id.tv_description);
        tvError = findViewById(R.id.tv_error);
        categoryContainer = findViewById(R.id.category_container);
        progressBar = findViewById(R.id.progress_bar);

        btnBack.setOnClickListener(v -> finish());
    }

    private void initializePlayer() {
        if (player == null) {
            player = new ExoPlayer.Builder(this).build();
            playerView.setPlayer(player);

            player.addListener(new Player.Listener() {
                @Override
                public void onPlaybackStateChanged(int state) {
                    if (state == Player.STATE_READY) {
                        setLoading(false);
                    } else if (state == Player.STATE_BUFFERING) {
                        setLoading(true);
                    }
                }

                @Override
                public void onPlayerError(com.google.android.exoplayer2.PlaybackException error) {
                    showError("视频播放错误: " + error.getMessage());
                }
            });

            playerView.post(new Runnable() {
                @Override
                public void run() {
                    setupControllerButtons();
                }
            });
        }
    }

    private void setupControllerButtons() {
        View speedBtn = playerView.findViewById(R.id.exo_speed);
        if (speedBtn != null) {
            speedBtn.setOnClickListener(v -> showSpeedDialog());
        }
        View fullscreenBtn = playerView.findViewById(R.id.exo_fullscreen);
        if (fullscreenBtn != null) {
            fullscreenBtn.setOnClickListener(v -> toggleFullscreen());
        }

        View speedBtnFs = playerViewFullscreen.findViewById(R.id.exo_speed);
        if (speedBtnFs != null) {
            speedBtnFs.setOnClickListener(v -> showSpeedDialog());
        }
        View fullscreenBtnFs = playerViewFullscreen.findViewById(R.id.exo_fullscreen);
        if (fullscreenBtnFs != null) {
            fullscreenBtnFs.setOnClickListener(v -> toggleFullscreen());
        }
    }

    private void showSpeedDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_speed);
        dialog.setTitle("播放速度");

        LinearLayout speedContainer = dialog.findViewById(R.id.speed_container);
        for (int i = 0; i < playbackSpeeds.length; i++) {
            final float speed = playbackSpeeds[i];
            Button btn = new Button(this);
            btn.setText(speedLabels[i]);
            btn.setTextSize(16);
            btn.setPadding(20, 12, 20, 12);
            btn.setOnClickListener(v -> {
                if (player != null) {
                    player.setPlaybackSpeed(speed);
                }
                dialog.dismiss();
            });

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(8, 8, 8, 8);
            btn.setLayoutParams(params);
            speedContainer.addView(btn);
        }

        dialog.show();
    }

    private void toggleFullscreen() {
        if (isFullscreen) {
            exitFullscreen();
        } else {
            enterFullscreen();
        }
    }

    private void enterFullscreen() {
        isFullscreen = true;

        contentScroll.setVisibility(View.GONE);
        playerViewFullscreen.setVisibility(View.VISIBLE);
        playerViewFullscreen.setPlayer(player);

        Window window = getWindow();
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        WindowInsetsController insetsController = window.getInsetsController();
        if (insetsController != null) {
            insetsController.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
            insetsController.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        }

        ImageButton fullscreenBtn = playerViewFullscreen.findViewById(R.id.exo_fullscreen);
        if (fullscreenBtn != null) {
            fullscreenBtn.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        }
    }

    private void exitFullscreen() {
        isFullscreen = false;

        contentScroll.setVisibility(View.VISIBLE);
        playerViewFullscreen.setVisibility(View.GONE);
        playerView.setPlayer(player);

        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        WindowInsetsController insetsController = window.getInsetsController();
        if (insetsController != null) {
            insetsController.show(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
        }

        ImageButton fullscreenBtn = playerView.findViewById(R.id.exo_fullscreen);
        if (fullscreenBtn != null) {
            fullscreenBtn.setImageResource(android.R.drawable.ic_menu_crop);
        }
    }

    private void loadVideoDetail() {
        setLoading(true);
        ApiClient.getApiService(this).getVideoDetail(videoSlug).enqueue(new Callback<Video>() {
            @Override
            public void onResponse(Call<Video> call, Response<Video> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    video = response.body();
                    showVideoDetail();
                } else {
                    showError("获取视频详情失败");
                }
            }

            @Override
            public void onFailure(Call<Video> call, Throwable t) {
                setLoading(false);
                showError("网络错误: " + t.getMessage());
            }
        });
    }

    private void showVideoDetail() {
        tvTitle.setText(video.getTitle());
        tvDescription.setText(video.getDescription() != null ? video.getDescription() : "暂无简介");

        categoryContainer.removeAllViews();
        if (video.getCategory_names() != null) {
            for (String category : video.getCategory_names()) {
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

        if (video.getVideo_file() != null && !video.getVideo_file().isEmpty()) {
            String videoUrl = getVideoUrl(video.getVideo_file());
            playVideo(videoUrl);
        }
    }

    private String getVideoUrl(String path) {
        if (path == null) return "";
        if (path.startsWith("http")) return path;
        String baseUrl = ApiClient.BASE_URL;
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        return baseUrl + path;
    }

    private void playVideo(String url) {
        initializePlayer();
        MediaItem mediaItem = MediaItem.fromUri(url);
        player.setMediaItem(mediaItem);
        player.prepare();
        player.play();
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        playerView.setVisibility(loading ? View.GONE : View.VISIBLE);
    }

    private void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
        playerView.setVisibility(View.GONE);
    }

    @Override
    public void onBackPressed() {
        if (isFullscreen) {
            exitFullscreen();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (player != null) {
            player.play();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (player != null) {
            player.play();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (player != null) {
            player.pause();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (player != null) {
            player.stop();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
            player = null;
        }
    }
}
