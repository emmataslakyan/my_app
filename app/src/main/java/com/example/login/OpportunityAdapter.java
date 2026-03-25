package com.example.login;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.chip.Chip;
import java.util.ArrayList;
import java.util.List;

public class OpportunityAdapter extends RecyclerView.Adapter<OpportunityAdapter.ViewHolder> {

    private List<Opportunity> allOpportunities; // The master list
    private List<Opportunity> filteredOpportunities; // The visible list

    public OpportunityAdapter(List<Opportunity> opportunities) {
        this.allOpportunities = new ArrayList<>(opportunities);
        this.filteredOpportunities = new ArrayList<>(opportunities);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_opportunity, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Opportunity opportunity = filteredOpportunities.get(position);

        holder.title.setText(opportunity.getTitle());
        holder.description.setText(opportunity.getDescription());
        holder.formatChip.setText(opportunity.getFormat());
        holder.categoryChip.setText(opportunity.getCategory());
        holder.costChip.setText(opportunity.getCost());

        if (!opportunity.getLocation().isEmpty()) {
            holder.locationLayout.setVisibility(View.VISIBLE);
            holder.location.setText(opportunity.getLocation());
        } else {
            holder.locationLayout.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            String url = opportunity.getUrl();
            if (!url.isEmpty()) {
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    url = "https://" + url;
                }
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                v.getContext().startActivity(browserIntent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return filteredOpportunities.size();
    }

    public void filter(String format, String category, String cost) {
        filteredOpportunities.clear();

        for (Opportunity opp : allOpportunities) {
            // Case-insensitive matching + "All" logic
            boolean matchesFormat = format.equalsIgnoreCase("All Formats") ||
                    opp.getFormat().equalsIgnoreCase(format);

            boolean matchesCategory = category.equalsIgnoreCase("All Categories") ||
                    opp.getCategory().equalsIgnoreCase(category);

            boolean matchesCost = cost.equalsIgnoreCase("All") ||
                    opp.getCost().equalsIgnoreCase(cost);

            if (matchesFormat && matchesCategory && matchesCost) {
                filteredOpportunities.add(opp);
            }
        }
        notifyDataSetChanged();
    }

    public void updateData(List<Opportunity> newOpportunities) {
        this.allOpportunities = new ArrayList<>(newOpportunities);
        this.filteredOpportunities = new ArrayList<>(newOpportunities);
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, description, location;
        Chip formatChip, categoryChip, costChip;
        LinearLayout locationLayout;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.opportunity_title);
            description = itemView.findViewById(R.id.opportunity_description);
            location = itemView.findViewById(R.id.opportunity_location);
            formatChip = itemView.findViewById(R.id.chip_format);
            categoryChip = itemView.findViewById(R.id.chip_category);
            costChip = itemView.findViewById(R.id.chip_cost);
            locationLayout = itemView.findViewById(R.id.location_layout);
        }
    }
}