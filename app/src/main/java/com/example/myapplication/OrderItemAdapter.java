package com.example.myapplication;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class OrderItemAdapter extends RecyclerView.Adapter<OrderItemAdapter.OrderViewHolder> {
    private static final String TAG = "OrderItemAdapter";
    private List<OrderItem> items;

    public OrderItemAdapter(List<OrderItem> items) {
        this.items = items;
    }

    @Override
    public OrderViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_orders_orders, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(OrderViewHolder holder, int position) {
        OrderItem item = items.get(position);
        if (holder.name != null) {
            holder.name.setText(item.name != null ? item.name : "Без названия");
        }
        if (holder.currentPrice != null) {
            holder.currentPrice.setText(String.format("₽%.2f", item.price * item.quantity));
        }
        if (holder.originalPrice != null) {
            holder.originalPrice.setText(String.format("₽%.2f x %d", item.price, item.quantity));
        }
        if (holder.orderNumber != null) {
            String displayOrderId = item.orderId != null && item.orderId.length() > 4
                    ? "№ " + item.orderId.substring(item.orderId.length() - 4)
                    : item.orderId != null ? "№ " + item.orderId : "Не указан";
            holder.orderNumber.setText(displayOrderId);
            Log.d(TAG, "Set order number: " + displayOrderId + ", position: " + position);
        }
        if (holder.orderDaysAgo != null) {
            holder.orderDaysAgo.setText("");
        }
        if (holder.image != null) {
            String imageUrl = item.imageUrl != null ? item.imageUrl.replaceAll("^\\s+", "") : "";
            Log.d(TAG, "Item imageUrl: " + imageUrl + ", position: " + position);
            String fullImageUrl = !imageUrl.isEmpty()
                    ? BuildConfig.SUPABASE_URL + "/storage/v1/object/public/orders/" + imageUrl
                    : null;
            Log.d(TAG, "Full image URL: " + fullImageUrl);
            if (fullImageUrl != null) {
                Glide.with(holder.itemView.getContext())
                        .load(fullImageUrl)
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.stat_notify_error)
                        .into(holder.image);
            } else {
                holder.image.setImageResource(android.R.drawable.ic_menu_gallery);
                Log.w(TAG, "No image URL for item: " + item.name);
            }
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class OrderViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView name, currentPrice, originalPrice, orderNumber, orderDaysAgo;

        OrderViewHolder(View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.order_image);
            name = itemView.findViewById(R.id.order_name);
            currentPrice = itemView.findViewById(R.id.order_current_price);
            originalPrice = itemView.findViewById(R.id.order_original_price);
            orderNumber = itemView.findViewById(R.id.order_number);
            orderDaysAgo = itemView.findViewById(R.id.order_days_ago);
        }
    }
}