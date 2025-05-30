package com.example.myapplication;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {
    private static final String TAG = "CartAdapter";
    private List<CartItem> cartItems;
    private OnQuantityChangeListener quantityChangeListener;

    public interface OnQuantityChangeListener {
        void onQuantityChange(CartItem cartItem, int newQuantity);
    }

    public CartAdapter(List<CartItem> cartItems, OnQuantityChangeListener listener) {
        this.cartItems = cartItems;
        this.quantityChangeListener = listener;
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cart, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        CartItem cartItem = cartItems.get(position);
        holder.name.setText(cartItem.getName());
        try {
            double price = Double.parseDouble(cartItem.getPrice());
            double totalPrice = price * cartItem.getQuantity();
            holder.price.setText(String.format("₽%.2f", totalPrice));
        } catch (NumberFormatException e) {
            Log.e(TAG, "Invalid price format for item: " + cartItem.getName() + ", price: " + cartItem.getPrice());
            holder.price.setText("Ошибка цены");
        }

        holder.quantity.setText(String.valueOf(cartItem.getQuantity()));
        holder.quantityLayout.setVisibility(View.VISIBLE);

        String imageUrl = cartItem.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            String fullImageUrl = BuildConfig.SUPABASE_URL + "/storage/v1/object/public/orders/" + imageUrl.replaceAll("^\\s+", "");
            Glide.with(holder.itemView)
                    .load(fullImageUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_gallery)
                    .into(holder.image);
        } else {
            holder.image.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        holder.increaseButton.setOnClickListener(v -> {
            int newQuantity = cartItem.getQuantity() + 1;
            cartItem.setQuantity(newQuantity);
            holder.quantity.setText(String.valueOf(newQuantity));
            quantityChangeListener.onQuantityChange(cartItem, newQuantity);
            try {
                double price = Double.parseDouble(cartItem.getPrice());
                double totalPrice = price * newQuantity;
                holder.price.setText(String.format("₽%.2f", totalPrice));
            } catch (NumberFormatException e) {
                Log.e(TAG, "Invalid price format for item: " + cartItem.getName());
                holder.price.setText("Ошибка цены");
            }
        });

        holder.decreaseButton.setOnClickListener(v -> {
            int newQuantity = cartItem.getQuantity() - 1;
            if (newQuantity > 0) {
                cartItem.setQuantity(newQuantity);
                holder.quantity.setText(String.valueOf(newQuantity));
                quantityChangeListener.onQuantityChange(cartItem, newQuantity);
                try {
                    double price = Double.parseDouble(cartItem.getPrice());
                    double totalPrice = price * newQuantity;
                    holder.price.setText(String.format("₽%.2f", totalPrice));
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Invalid price format for item: " + cartItem.getName());
                    holder.price.setText("Ошибка цены");
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return cartItems.size();
    }

    public void removeItem(int position) {
        cartItems.remove(position);
        notifyItemRemoved(position);
    }

    static class CartViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView name, price, quantity;
        ImageButton increaseButton, decreaseButton;
        ViewGroup quantityLayout;

        public CartViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.product_image);
            name = itemView.findViewById(R.id.product_name);
            price = itemView.findViewById(R.id.product_price);
            quantity = itemView.findViewById(R.id.quantity_text);
            increaseButton = itemView.findViewById(R.id.increase_button);
            decreaseButton = itemView.findViewById(R.id.decrease_button);
            quantityLayout = itemView.findViewById(R.id.quantity_layout);
        }
    }
}