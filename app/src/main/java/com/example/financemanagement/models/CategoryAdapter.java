package com.example.financemanagement.models;

import android.content.Context;
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

    static {
        // Aggiungi qui la tua mappa di icone
        categoryIcons.put("Food", R.drawable.ic_food);
        categoryIcons.put("Home", R.drawable.ic_home);
        categoryIcons.put("Sport", R.drawable.ic_sport);
        categoryIcons.put("Wellness", R.drawable.ic_wellness);
        categoryIcons.put("Clothes", R.drawable.ic_clothes);
        categoryIcons.put("Trasportation", R.drawable.ic_transport);
        categoryIcons.put("Subscriptions", R.drawable.ic_subscriptions);
        categoryIcons.put("Cibo fuori casa", R.drawable.ic_outside);
        categoryIcons.put("Svago", R.drawable.ic_entertainment);
        // Aggiungi altre categorie se necessario
    }

    public CategoryAdapter(Context context, List<Category> categories) {
        super(context, 0, categories);
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

        Category category = categories.get(position);

        // Recupera il resId dalla mappa
        Integer iconResId = categoryIcons.get(category.getName());

        if (iconResId != null)
            iconView.setImageResource(iconResId);
        else    // Fallback: se non c'è un'icona per la categoria, usa una default
            iconView.setImageResource(R.drawable.ic_category);

        nameView.setText(category.getName());

        return convertView;
    }
}
