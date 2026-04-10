package com.example.smartseva;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import java.util.List;

public class ApplicantAdapter extends ArrayAdapter<String> {

    Task task;


    public ApplicantAdapter(Context context, List<String> applicants, Task task) {
        super(context, 0, applicants);
        this.task = task; // 🔥 important
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.applicant_item, parent, false);
        }


        String name = getItem(position);
        String status = task.getApplicantStatus().get(name);


        TextView tvName = convertView.findViewById(R.id.tvName);
        Button btnAccept = convertView.findViewById(R.id.btnAccept);
        Button btnReject = convertView.findViewById(R.id.btnReject);
        TextView tvStatus = convertView.findViewById(R.id.tvStatus);



        tvName.setText(name);

        // ✅ ACCEPT BUTTON
        btnAccept.setOnClickListener(v -> {
            Toast.makeText(getContext(),
                    name + " Accepted",
                    Toast.LENGTH_SHORT).show();
            notifyDataSetChanged();

            // 🔥 Update status
            ((ApplicantsActivity) getContext()).updateTaskStatus("Accepted");
        });

        // ❌ REJECT BUTTON
        btnReject.setOnClickListener(v -> {
            Toast.makeText(getContext(),
                    name + " Rejected",
                    Toast.LENGTH_SHORT).show();

            // 🔥 Update status
            ((ApplicantsActivity) getContext()).updateTaskStatus("Rejected");
            notifyDataSetChanged();
        });

        //status

        if (status == null) {
            status = "Pending";
        }
        tvStatus.setText("Status: " + status);

        if (status.equals("Accepted")) {
            tvStatus.setTextColor(0xFF4CAF50); // Green
        } else if (status.equals("Rejected")) {
            tvStatus.setTextColor(0xFFF44336); // Red
        } else {
            tvStatus.setTextColor(0xFFFF9800); // Orange
        }



        return convertView;
    }
}