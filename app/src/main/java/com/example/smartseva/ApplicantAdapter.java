package com.example.smartseva;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import java.util.List;

public class ApplicantAdapter extends ArrayAdapter<String> {

    public ApplicantAdapter(@NonNull android.content.Context context, List<String> applicants) {
        super(context, 0, applicants);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.applicant_item, parent, false);
        }

        String name = getItem(position);

        TextView tvName = convertView.findViewById(R.id.tvName);
        Button btnAccept = convertView.findViewById(R.id.btnAccept);
        Button btnReject = convertView.findViewById(R.id.btnReject);



        tvName.setText(name);

        // ✅ ACCEPT BUTTON
        btnAccept.setOnClickListener(v -> {
            Toast.makeText(getContext(),
                    name + " Accepted",
                    Toast.LENGTH_SHORT).show();

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
        });


        return convertView;
    }
}