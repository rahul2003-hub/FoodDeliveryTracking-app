package com.example.fooddeliverytracking.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fooddeliverytracking.R;
import com.example.fooddeliverytracking.models.Order;
import com.example.fooddeliverytracking.models.OrderItem;
import com.example.fooddeliverytracking.utils.Constants;
import com.example.fooddeliverytracking.utils.DeliveryUi;
import com.google.android.material.button.MaterialButton;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {

    public interface OnTrackOrderClick {
        void onTrackOrder(Order order);
    }

    private final List<Order> orders = new ArrayList<>();
    private final OnTrackOrderClick listener;
    private boolean recentSection;

    public void setRecentSection(boolean enabled) { recentSection = enabled; }

    private int actionTextRes = R.string.track_order;

    public OrderAdapter(OnTrackOrderClick listener) {
        this.listener = listener;
    }

    public void setOrders(List<Order> updatedOrders) {
        orders.clear();
        orders.addAll(updatedOrders);
        notifyDataSetChanged();
    }

    public void setActionTextRes(int actionTextRes) {
        this.actionTextRes = actionTextRes;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order order = orders.get(position);
        boolean delivered = Constants.STATUS_DELIVERED.equals(order.getStatus());
        holder.itemView.findViewById(R.id.textRecentHeader).setVisibility(recentSection && delivered
                && (position == 0 || !Constants.STATUS_DELIVERED.equals(orders.get(position - 1).getStatus()))
                ? View.VISIBLE : View.GONE);
        holder.orderId.setText(holder.itemView.getContext().getString(R.string.order_id, order.getOrderId()));
        ((TextView) holder.itemView.findViewById(R.id.textOrderRestaurant)).setText(order.getRestaurantName());
        TextView destination = holder.itemView.findViewById(R.id.textOrderDestination);
        boolean driverOrder = actionTextRes != R.string.track_order;
        holder.itemView.findViewById(R.id.dropoffRow).setVisibility(driverOrder ? View.VISIBLE : View.GONE);
        holder.itemView.findViewById(R.id.imagePickup).setVisibility(driverOrder ? View.VISIBLE : View.GONE);
        holder.itemView.findViewById(R.id.labelPickup).setVisibility(driverOrder ? View.VISIBLE : View.GONE);
        holder.itemView.findViewById(R.id.imageOrderFood).setVisibility(driverOrder ? View.GONE : View.VISIBLE);
        destination.setText(holder.itemView.getContext().getString(R.string.order_destination, order.getCustomerName(), order.getCustomerAddress()));
        holder.date.setText(driverOrder ? order.getRestaurantAddress()
                : delivered ? DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date(order.getCreatedAt())) : "");
        holder.date.setVisibility(driverOrder || delivered ? View.VISIBLE : View.GONE);
        holder.items.setText(buildItemSummary(order.getItems()));
        holder.status.setText(driverOrder && Constants.STATUS_PLACED.equals(order.getStatus())
                ? R.string.new_assignment : DeliveryUi.statusLabel(order.getStatus()));
        DeliveryUi.badge(holder.status, delivered);
        int itemCount = 0;
        if (order.getItems() != null) for (OrderItem item : order.getItems()) if (item != null) itemCount += item.getQuantity();
        holder.total.setText(holder.itemView.getContext().getString(driverOrder ? R.string.driver_item_count
                : R.string.order_item_count, itemCount, order.getTotalAmount()));
        holder.items.setVisibility(driverOrder ? View.GONE : View.VISIBLE);
        holder.track.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(holder.itemView.getContext(), delivered ? android.R.color.transparent : R.color.primary_orange)));
        holder.track.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), delivered ? R.color.primary_orange : R.color.white));
        android.widget.LinearLayout.LayoutParams actionLayout = (android.widget.LinearLayout.LayoutParams) holder.track.getLayoutParams();
        actionLayout.width = delivered ? ViewGroup.LayoutParams.WRAP_CONTENT : ViewGroup.LayoutParams.MATCH_PARENT;
        actionLayout.gravity = android.view.Gravity.END;
        holder.track.setLayoutParams(actionLayout);
        holder.track.setText(Constants.STATUS_DELIVERED.equals(order.getStatus()) ? R.string.view_details : actionTextRes);
        holder.track.setOnClickListener(view -> listener.onTrackOrder(order));
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    private String buildItemSummary(List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            return "";
        }
        StringBuilder summary = new StringBuilder();
        for (OrderItem item : items) {
            if (summary.length() > 0) {
                summary.append(", ");
            }
            summary.append(item.getQuantity()).append("x ").append(item.getName());
        }
        return summary.toString();
    }

    static class OrderViewHolder extends RecyclerView.ViewHolder {
        final TextView orderId;
        final TextView date;
        final TextView items;
        final TextView status;
        final TextView total;
        final MaterialButton track;

        OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            orderId = itemView.findViewById(R.id.textOrderId);
            date = itemView.findViewById(R.id.textOrderDate);
            items = itemView.findViewById(R.id.textOrderItems);
            status = itemView.findViewById(R.id.textOrderStatus);
            total = itemView.findViewById(R.id.textOrderTotal);
            track = itemView.findViewById(R.id.buttonTrackOrder);
        }
    }
}
