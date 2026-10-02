package com.android.personal.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.personal.R;
import com.android.personal.api.ApiClient;
import com.android.personal.api.model.Note;
import com.android.personal.api.model.NoteRequest;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NoteListFragment extends Fragment {

    private RecyclerView recyclerView;
    private CircularProgressIndicator progressBar;
    private FloatingActionButton fabCreateNote;
    private NoteAdapter adapter;
    private List<Note> noteList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_note_list, container, false);

        recyclerView = view.findViewById(R.id.recycler_notes);
        progressBar = view.findViewById(R.id.progress_bar);
        fabCreateNote = view.findViewById(R.id.btn_create_note);

        adapter = new NoteAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        fabCreateNote.setOnClickListener(v -> showCreateNoteDialog());

        loadNotes();

        return view;
    }

    private void loadNotes() {
        setLoading(true);
        ApiClient.getApiService(getContext()).getNotes().enqueue(new Callback<List<Note>>() {
            @Override
            public void onResponse(Call<List<Note>> call, Response<List<Note>> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    noteList.clear();
                    noteList.addAll(response.body());
                    adapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(getContext(), "加载笔记失败", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Note>> call, Throwable t) {
                setLoading(false);
                Toast.makeText(getContext(), "网络错误", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(loading ? View.GONE : View.VISIBLE);
    }

    private void showCreateNoteDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_create_note, null);

        TextInputEditText etTitle = dialogView.findViewById(R.id.et_title);
        TextInputEditText etContent = dialogView.findViewById(R.id.et_content);

        new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .setTitle(R.string.create_note)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String title = etTitle.getText().toString().trim();
                    String content = etContent.getText().toString().trim();
                    if (!title.isEmpty()) {
                        createNote(title, content);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void createNote(String title, String content) {
        NoteRequest request = new NoteRequest(title, content);
        ApiClient.getApiService(getContext()).createNote(request).enqueue(new Callback<Note>() {
            @Override
            public void onResponse(Call<Note> call, Response<Note> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "笔记创建成功", Toast.LENGTH_SHORT).show();
                    loadNotes();
                } else {
                    Toast.makeText(getContext(), "创建失败", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Note> call, Throwable t) {
                Toast.makeText(getContext(), "网络错误", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteNote(int id) {
        ApiClient.getApiService(getContext()).deleteNote(id).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "删除成功", Toast.LENGTH_SHORT).show();
                    loadNotes();
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

    private class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {

        @NonNull
        @Override
        public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_note, parent, false);
            return new NoteViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
            Note note = noteList.get(position);
            holder.tvTitle.setText(note.getTitle());
            holder.tvContent.setText(note.getContent());
            holder.tvDate.setText(note.getUpdated_at());

            holder.itemView.setOnClickListener(v -> showNoteDetail(note));
            holder.itemView.setOnLongClickListener(v -> {
                showDeleteDialog(note.getId());
                return true;
            });
        }

        @Override
        public int getItemCount() {
            return noteList.size();
        }

        class NoteViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle;
            TextView tvContent;
            TextView tvDate;

            NoteViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tv_title);
                tvContent = itemView.findViewById(R.id.tv_content);
                tvDate = itemView.findViewById(R.id.tv_date);
            }
        }
    }

    private void showNoteDetail(Note note) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(note.getTitle())
                .setMessage(note.getContent())
                .setPositiveButton(R.string.edit, (dialog, which) -> showEditNoteDialog(note))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showEditNoteDialog(Note note) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_create_note, null);

        TextInputEditText etTitle = dialogView.findViewById(R.id.et_title);
        TextInputEditText etContent = dialogView.findViewById(R.id.et_content);

        etTitle.setText(note.getTitle());
        etContent.setText(note.getContent());

        new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .setTitle(R.string.edit)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String title = etTitle.getText().toString().trim();
                    String content = etContent.getText().toString().trim();
                    if (!title.isEmpty()) {
                        updateNote(note.getId(), title, content);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void updateNote(int id, String title, String content) {
        NoteRequest request = new NoteRequest(title, content);
        ApiClient.getApiService(getContext()).updateNote(id, request).enqueue(new Callback<Note>() {
            @Override
            public void onResponse(Call<Note> call, Response<Note> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "更新成功", Toast.LENGTH_SHORT).show();
                    loadNotes();
                } else {
                    Toast.makeText(getContext(), "更新失败", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Note> call, Throwable t) {
                Toast.makeText(getContext(), "网络错误", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showDeleteDialog(int id) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("确认删除")
                .setMessage("确定要删除这条笔记吗？")
                .setPositiveButton(R.string.delete, (dialog, which) -> deleteNote(id))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}