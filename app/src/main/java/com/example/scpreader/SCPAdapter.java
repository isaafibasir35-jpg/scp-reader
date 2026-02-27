package com.example.scpreader;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class SCPAdapter extends RecyclerView.Adapter<SCPAdapter.ViewHolder> implements Filterable {
    private List<SCPObject> scpList;
    private List<SCPObject> scpListFull;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(SCPObject scp);
    }

    public SCPAdapter(List<SCPObject> scpList, OnItemClickListener listener) {
        this.scpList = scpList;
        this.scpListFull = new ArrayList<>(scpList);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_scp, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SCPObject scp = scpList.get(position);
        holder.numberText.setText(scp.getNumber());
        holder.titleText.setText(scp.getTitle());
        
        // Доступность: Описание для скринридера
        holder.itemView.setContentDescription("Объект " + scp.getNumber() + ": " + scp.getTitle() + ". Нажмите для чтения статьи.");
        
        holder.itemView.setOnClickListener(v -> listener.onItemClick(scp));
    }

    @Override
    public int getItemCount() { return scpList.size(); }

    // Обновление данных извне
    public void updateList(List<SCPObject> newList) {
        this.scpList = new ArrayList<>(newList);
        this.scpListFull = new ArrayList<>(newList);
        notifyDataSetChanged();
    }

    // Логика поиска
    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                List<SCPObject> filteredList = new ArrayList<>();
                if (constraint == null || constraint.length() == 0) {
                    filteredList.addAll(scpListFull);
                } else {
                    String pattern = constraint.toString().toLowerCase().trim();
                    for (SCPObject item : scpListFull) {
                        if (item.getNumber().toLowerCase().contains(pattern) || 
                            item.getTitle().toLowerCase().contains(pattern)) {
                            filteredList.add(item);
                        }
                    }
                }
                FilterResults results = new FilterResults();
                results.values = filteredList;
                return results;
            }

            @Override
            @SuppressWarnings("unchecked")
            protected void publishResults(CharSequence constraint, FilterResults results) {
                scpList.clear();
                scpList.addAll((List) results.values);
                notifyDataSetChanged();
            }
        };
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView numberText, titleText;
        ViewHolder(View itemView) {
            super(itemView);
            numberText = itemView.findViewById(R.id.scpNumber);
            titleText = itemView.findViewById(R.id.scpTitle);
        }
    }
}
