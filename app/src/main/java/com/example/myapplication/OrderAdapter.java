package com.example.myapplication;

import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.ViewHolder> {
    private static final String TAG = "OrderAdapter";
    private List<Order> orders;

    public OrderAdapter(List<Order> orders) {
        this.orders = orders;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_orders, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Order order = orders.get(position);
        if (holder.orderNumber != null) {
            holder.orderNumber.setText("№ " + order.id);
            Log.d(TAG, "Set order number: № " + order.id);
        }
        if (holder.orderName != null) {
            holder.orderName.setText(order.name != null ? order.name : "Без названия");
        }
        if (holder.orderCurrentPrice != null) {
            holder.orderCurrentPrice.setText(String.format("₽%.2f", order.totalPrice));
        }
        if (holder.orderOriginalPrice != null) {
            holder.orderOriginalPrice.setText(String.format("₽%.2f x %d", order.pricePerItem, order.quantity));
        }
        if (holder.orderDaysAgo != null) {
            holder.orderDaysAgo.setText(order.getTimeCategory());
        }
        if (holder.orderImage != null) {
            String imageUrl = order.imageUrl != null ? order.imageUrl.replaceAll("^\\s+", "") : "";
            Log.d(TAG, "Order imageUrl: " + imageUrl + ", position: " + position);
            String fullImageUrl = !imageUrl.isEmpty()
                    ? BuildConfig.SUPABASE_URL + "/storage/v1/object/public/orders/" + imageUrl
                    : null;
            Log.d(TAG, "Full image URL: " + fullImageUrl);
            if (fullImageUrl != null) {
                Glide.with(holder.itemView.getContext())
                        .load(fullImageUrl)
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.stat_notify_error)
                        .into(holder.orderImage);
            } else {
                holder.orderImage.setImageResource(android.R.drawable.ic_menu_gallery);
                Log.w(TAG, "No image URL for order: " + order.name);
            }
        }

        holder.itemView.setOnClickListener(v -> {
            Log.d(TAG, "Clicked orderId: " + order.id);
            Intent intent = new Intent(holder.itemView.getContext(), OrderConfirmationActivity.class);
            intent.putExtra("ORDER_ID", order.id);
            intent.putExtra("TOTAL_PRICE", order.totalPrice);
            holder.itemView.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView orderImage;
        TextView orderNumber, orderName, orderCurrentPrice, orderOriginalPrice, orderDaysAgo;

        ViewHolder(View itemView) {
            super(itemView);
            orderImage = itemView.findViewById(R.id.order_image);
            orderNumber = itemView.findViewById(R.id.order_number);
            orderName = itemView.findViewById(R.id.order_name);
            orderCurrentPrice = itemView.findViewById(R.id.order_current_price);
            orderOriginalPrice = itemView.findViewById(R.id.order_original_price);
            orderDaysAgo = itemView.findViewById(R.id.order_days_ago);
        }
    }
}