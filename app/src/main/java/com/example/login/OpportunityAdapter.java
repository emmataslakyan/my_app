package com.example.login;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.chip.Chip;
import java.util.ArrayList;
import java.util.List;

public class OpportunityAdapter extends RecyclerView.Adapter<OpportunityAdapter.ViewHolder> {

    private List<Opportunity> allOpportunities;
    private List<Opportunity> filteredOpportunities;

    public OpportunityAdapter(List<Opportunity> opportunities) {
        this.allOpportunities = new ArrayList<>(opportunities);
        this.filteredOpportunities = new ArrayList<>(opportunities);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Ensure this matches your card's XML file name exactly
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_opportunity, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Opportunity opp = filteredOpportunities.get(position);

        // Setting Text Fields
        holder.title.setText(opp.getTitle());

        // If scraper doesn't find a description, show a friendly default
        String desc = opp.getDescription();
        holder.description.setText((desc == null || desc.isEmpty())
                ? "Visit " + opp.getSource() + " to learn more about this opportunity."
                : desc);

        // Setting Chip Data
        holder.chipFormat.setText(opp.getFormat());
        holder.chipCategory.setText(opp.getCategory());
        holder.chipCost.setText(opp.getCost());

        // Handle Location Visibility
        if (opp.getLocation() != null && !opp.getLocation().isEmpty()) {
            holder.locationLayout.setVisibility(View.VISIBLE);
            holder.locationText.setText(opp.getLocation());
        } else {
            holder.locationLayout.setVisibility(View.GONE);
        }

        // Click Logic to open the website
        holder.itemView.setOnClickListener(v -> {
            String url = opp.getUrl();
            if (url != null && !url.isEmpty()) {
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    url = "https://" + url;
                }
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                v.getContext().startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return filteredOpportunities.size();
    }

    public void updateData(List<Opportunity> newList) {
        this.allOpportunities = new ArrayList<>(newList);
        this.filteredOpportunities = new ArrayList<>(newList);
        notifyDataSetChanged();
    }

    public void filter(String format, String category, String cost) {
        filteredOpportunities.clear();
        for (Opportunity opp : allOpportunities) {
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

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, description, locationText;
        Chip chipFormat, chipCategory, chipCost;
        LinearLayout locationLayout;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.opportunity_title);
            description = itemView.findViewById(R.id.opportunity_description);
            locationText = itemView.findViewById(R.id.opportunity_location);
            chipFormat = itemView.findViewById(R.id.chip_format);
            chipCategory = itemView.findViewById(R.id.chip_category);
            chipCost = itemView.findViewById(R.id.chip_cost);
            locationLayout = itemView.findViewById(R.id.location_layout);
        }
    }
}