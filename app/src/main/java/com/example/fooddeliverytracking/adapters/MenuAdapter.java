package com.example.fooddeliverytracking.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fooddeliverytracking.R;
import com.example.fooddeliverytracking.models.MenuItem;
import com.example.fooddeliverytracking.models.OrderItem;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MenuAdapter extends RecyclerView.Adapter<MenuAdapter.MenuViewHolder> {

    public interface OnCartChanged {
        void onCartChanged();
    }

    private List<MenuItem> menuItems;
    private boolean cartMode;
    private final Map<String, Integer> quantities = new HashMap<>();
    private final Map<String, MenuItem> itemsById = new HashMap<>();
    private final OnCartChanged cartChangedListener;

    public MenuAdapter(List<MenuItem> menuItems, OnCartChanged cartChangedListener) {
        this.menuItems = menuItems;
        this.cartChangedListener = cartChangedListener;
        rememberItems(menuItems);
    }

    public void setMenuItems(List<MenuItem> menuItems) {
        this.menuItems = menuItems;
        rememberItems(menuItems);
        notifyDataSetChanged();
    }

    public List<OrderItem> getOrderItems() {
        List<OrderItem> selectedItems = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : quantities.entrySet()) {
            MenuItem item = itemsById.get(entry.getKey());
            int quantity = entry.getValue();
            if (item != null && quantity > 0) {
                selectedItems.add(new OrderItem(item.getName(), quantity, item.getPrice()));
            }
        }
        return selectedItems;
    }

    public int getCartItemCount() {
        int count = 0;
        for (int quantity : quantities.values()) {
            count += quantity;
        }
        return count;
    }

    public double getCartTotal() {
        double total = 0;
        for (Map.Entry<String, Integer> entry : quantities.entrySet()) {
            MenuItem item = itemsById.get(entry.getKey());
            if (item != null) {
                total += entry.getValue() * item.getPrice();
            }
        }
        return total;
    }

    public List<MenuItem> getSelectedMenuItems() {
        List<MenuItem> selected = new ArrayList<>();
        for (MenuItem item : itemsById.values()) if (getQuantity(item) > 0) selected.add(item);
        return selected;
    }

    public void setCartMode(boolean cartMode) {
        this.cartMode = cartMode;
    }

    public void clearCart() {
        quantities.clear();
        itemsById.clear();
        rememberItems(menuItems);
        notifyDataSetChanged();
        cartChangedListener.onCartChanged();
    }

    @NonNull
    @Override
    public MenuViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_menu, parent, false);
        return new MenuViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MenuViewHolder holder, int position) {
        MenuItem item = menuItems.get(position);
        ViewGroup row = (ViewGroup) holder.image.getParent();
        int imagePosition = cartMode ? 0 : row.getChildCount() - 1;
        if (row.indexOfChild(holder.image) != imagePosition) {
            row.removeView(holder.image);
            row.addView(holder.image, imagePosition);
        }
        holder.image.setImageResource(item.getImageResId());
        holder.name.setText(item.getName());
        holder.description.setText(item.getDescription());
        holder.price.setText(holder.itemView.getContext().getString(R.string.price, item.getPrice()));
        int quantity = getQuantity(item);
        holder.quantity.setText(String.valueOf(quantity));
        holder.quantity.setVisibility(quantity == 0 ? View.GONE : View.VISIBLE);
        holder.decrease.setVisibility(quantity == 0 ? View.GONE : View.VISIBLE);
        holder.increase.setText(quantity == 0 ? holder.itemView.getContext().getString(R.string.add) : holder.itemView.getContext().getString(R.string.quantity_plus));
        View remove = holder.itemView.findViewById(R.id.buttonRemove);
        remove.setVisibility(cartMode ? View.VISIBLE : View.GONE);
        remove.setOnClickListener(view -> changeQuantity(item, -getQuantity(item), holder.getAdapterPosition()));
        holder.description.setVisibility(cartMode ? View.GONE : View.VISIBLE);
        holder.increase.setOnClickListener(view -> changeQuantity(item, 1, holder.getAdapterPosition()));
        holder.decrease.setOnClickListener(view -> changeQuantity(item, -1, holder.getAdapterPosition()));
    }

    @Override
    public int getItemCount() {
        return menuItems.size();
    }

    private int getQuantity(MenuItem item) {
        Integer quantity = quantities.get(item.getId());
        return quantity == null ? 0 : quantity;
    }

    private void rememberItems(List<MenuItem> items) {
        for (MenuItem item : items) {
            itemsById.put(item.getId(), item);
        }
    }

    private void changeQuantity(MenuItem item, int change, int position) {
        int updatedQuantity = Math.max(0, getQuantity(item) + change);
        quantities.put(item.getId(), updatedQuantity);
        if (position != RecyclerView.NO_POSITION) {
            notifyItemChanged(position);
        }
        cartChangedListener.onCartChanged();
    }

    static class MenuViewHolder extends RecyclerView.ViewHolder {
        final ImageView image;
        final TextView name;
        final TextView description;
        final TextView price;
        final TextView quantity;
        final MaterialButton increase;
        final MaterialButton decrease;

        MenuViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.imageMenuItem);
            name = itemView.findViewById(R.id.textMenuName);
            description = itemView.findViewById(R.id.textMenuDescription);
            price = itemView.findViewById(R.id.textMenuPrice);
            quantity = itemView.findViewById(R.id.textQuantity);
            increase = itemView.findViewById(R.id.buttonIncrease);
            decrease = itemView.findViewById(R.id.buttonDecrease);
        }
    }
}
