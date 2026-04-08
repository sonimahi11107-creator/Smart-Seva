package com.example.smartseva;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.ViewHolder> {

    List<Task> taskList;

    // Constructor
    public TaskAdapter(List<Task> taskList) {
        this.taskList = taskList;
    }

    // ViewHolder class (represents one item/card)
    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvTitle, tvDesc, tvLocation;
        Button btnApply;
        TextView tvStatus;

        public ViewHolder(View itemView) {
            super(itemView);

            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvDesc = itemView.findViewById(R.id.tvDesc);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            btnApply = itemView.findViewById(R.id.btnApply);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        Task task = taskList.get(position);

        holder.tvTitle.setText(task.getTitle());
        holder.tvDesc.setText(task.getDescription());
        holder.tvLocation.setText(task.getLocation());
        holder.tvStatus.setText("Status: " + task.getStatus());

        holder.btnApply.setOnClickListener(v -> {

            // Example volunteer name (later from login)
            String volunteerName = "User1";

            task.getApplicantStatus().put(volunteerName, "Pending");

            Toast.makeText(v.getContext(),
                    "Applied for: " + task.getTitle(),
                    Toast.LENGTH_SHORT).show();
        });

        holder.itemView.setOnClickListener(v -> {

            Intent intent = new Intent(v.getContext(), ApplicantsActivity.class);

            intent.putExtra("task", task);

            v.getContext().startActivity(intent);
        });

    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }
}