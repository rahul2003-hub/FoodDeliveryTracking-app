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
        holder.orderId.setText(holder.itemView.getContext().getString(R.string.order_id, order.getOrderId()));
        ((TextView) holder.itemView.findViewById(R.id.textOrderRestaurant)).setText(order.getRestaurantName());
        TextView destination = holder.itemView.findViewById(R.id.textOrderDestination);
        boolean driverOrder = actionTextRes != R.string.track_order;
        destination.setVisibility(driverOrder ? View.VISIBLE : View.GONE);
        destination.setText(holder.itemView.getContext().getString(R.string.order_destination, order.getCustomerName(), order.getCustomerAddress()));
        holder.date.setText(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                .format(new Date(order.getCreatedAt())));
        holder.items.setText(buildItemSummary(order.getItems()));
        String status = formatStatus(order.getStatus());
        holder.status.setText(holder.itemView.getContext().getString(R.string.status, status));
        holder.status.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(),
                Constants.STATUS_DELIVERED.equals(order.getStatus()) ? R.color.success_surface : R.color.primary_orange_light));
        holder.status.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), getStatusColor(order.getStatus())));
        holder.total.setText(holder.itemView.getContext().getString(R.string.order_total, order.getTotalAmount()));
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

    private String formatStatus(String status) {
        return status == null ? "" : status.replace('_', ' ');
    }

    private int getStatusColor(String status) {
        if (Constants.STATUS_DELIVERED.equals(status)) {
            return R.color.accent_green;
        }
        if (Constants.STATUS_ACCEPTED.equals(status) || Constants.STATUS_PICKED_UP.equals(status)) {
            return R.color.status_info;
        }
        return R.color.status_pending;
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
