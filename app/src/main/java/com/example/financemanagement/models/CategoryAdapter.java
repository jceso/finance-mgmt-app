package com.example.financemanagement.models;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.financemanagement.R;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CategoryAdapter extends ArrayAdapter<Category> {
    private Context context;
    private List<Category> categories;

    private static final Map<String, Integer> categoryIcons = new HashMap<>();

    public CategoryAdapter(Context context, List<Category> categories) {
        super(context, 0, categories);
        Log.d("CategoryAdapter", "CategoryAdapter created with " + categories.size() + " categories: " + categories);
        this.context = context;
        this.categories = categories;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return createView(position, convertView, parent);
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return createView(position, convertView, parent);
    }

    private View createView(int position, View convertView, ViewGroup parent) {
        if (convertView == null)
            convertView = LayoutInflater.from(context).inflate(R.layout.spinner_item, parent, false);

        ImageView iconView = convertView.findViewById(R.id.icon);
        TextView nameView = convertView.findViewById(R.id.name);

        // Get the category and set the icon
        Category category = categories.get(position);
        int iconResId = getIconResourceId(category.getIcon());
        Log.d("CategoryAdapter", "Category: " + category.getName() + ", Icon resource ID: " + iconResId);

        if (iconResId != 0)
            iconView.setImageResource(iconResId);
        else    // Fallback icon
            iconView.setImageResource(R.drawable.icc_category);

        nameView.setText(category.getName());

        return convertView;
    }

    private int getIconResourceId(String iconName) {
        // Use reflection or a conditional block to return the correct icon resource ID
        switch (iconName) {
            case "icc_food": return R.drawable.icc_food;
            case "icc_home": return R.drawable.icc_home;
            case "icc_sport": return R.drawable.icc_sport;
            case "icc_wellness": return R.drawable.icc_wellness;
            case "icc_clothes": return R.drawable.icc_clothes;
            case "icc_transport": return R.drawable.icc_transport;
            case "icc_subscriptions": return R.drawable.icc_subscriptions;
            case "icc_out_of_home": return R.drawable.icc_outside;
            case "icc_entertainment": return R.drawable.icc_entertainment;
            case "icc_job": return R.drawable.icc_job;
            case "icc_giftcard": return R.drawable.icc_giftcard;
            case "icc_handshake": return R.drawable.icc_handshake;
            default: return 0;  // No icon found, return default icon
        }
    }
}
