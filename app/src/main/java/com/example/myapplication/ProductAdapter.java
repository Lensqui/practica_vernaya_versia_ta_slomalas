package com.example.myapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {
    private static final String TAG = "ProductAdapter";
    private List<Product> products;
    private int layoutResId;
    private OnFavoriteClickListener favoriteClickListener;
    private OnCartClickListener cartClickListener;
    private Map<String, Boolean> favoriteStatus;
    private ExecutorService executorService;
    private String userId;
    private LocalBroadcastManager broadcastManager;
    private BroadcastReceiver favoriteUpdateReceiver;
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    public static final String ACTION_FAVORITE_UPDATED = "com.example.myapplication.FAVORITE_UPDATED";

    public interface OnFavoriteClickListener {
        void onFavoriteClick(Product product, boolean isFavorite);
    }

    public interface OnCartClickListener {
        void onAddToCart(Product product);
    }

    public ProductAdapter(List<Product> products, int layoutResId, String userId, OnFavoriteClickListener favoriteListener, OnCartClickListener cartListener) {
        this.products = products;
        this.layoutResId = layoutResId;
        this.favoriteClickListener = favoriteListener;
        this.cartClickListener = cartListener;
        this.favoriteStatus = new HashMap<>();
        this.executorService = Executors.newSingleThreadExecutor();
        this.userId = userId;
        this.broadcastManager = LocalBroadcastManager.getInstance(ContextHolder.getContext());
        setupFavoriteUpdateReceiver();
        if (userId != null) {
            loadFavoriteStatus();
        }
    }

    public static class ContextHolder {
        private static Context context;
        public static void setContext(Context ctx) {
            context = ctx.getApplicationContext();
        }
        public static Context getContext() {
            if (context == null) {
                throw new IllegalStateException("Контекст не инициализирован");
            }
            return context;
        }
    }

    private void setupFavoriteUpdateReceiver() {
        favoriteUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "Received favorite update broadcast");
                loadFavoriteStatus();
            }
        };
        broadcastManager.registerReceiver(favoriteUpdateReceiver, new IntentFilter(ACTION_FAVORITE_UPDATED));
    }

    public void setUserId(String userId) {
        this.userId = userId;
        if (userId != null) {
            loadFavoriteStatus();
        }
    }

    public void loadFavoriteStatus() {
        if (userId == null) {
            Log.e(TAG, "userId is null, cannot load favorite status");
            return;
        }
        LocalStorage localStorage = LocalStorage.getInstance();
        String token = localStorage.getToken();
        executorService.execute(() -> {
            try {
                String url = BuildConfig.SUPABASE_URL + "/rest/v1/favorites?select=product_id&profile_id=eq." + userId;
                Log.d(TAG, "Loading favorite status: " + url);
                Request request = new Request.Builder()
                        .url(url)
                        .get()
                        .addHeader("apikey", BuildConfig.SUPABASE_API_KEY)
                        .addHeader("Authorization", "Bearer " + token)
                        .addHeader("Content-Type", "application/json")
                        .build();
                try (Response response = new OkHttpClient().newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        String responseBody = response.body().string();
                        Log.d(TAG, "Favorite status response: " + responseBody);
                        JSONArray favorites = new JSONArray(responseBody);
                        favoriteStatus.clear();
                        for (int i = 0; i < favorites.length(); i++) {
                            String productId = favorites.getJSONObject(i).getString("product_id");
                            favoriteStatus.put(productId, true);
                        }
                        new Handler(Looper.getMainLooper()).post(this::notifyDataSetChanged);
                    } else {
                        Log.e(TAG, "Failed to load favorite status: " + response.code());
                    }
                }
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Error loading favorite status: " + e.getMessage());
            }
        });
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutResId, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = products.get(position);
        holder.productName.setText(product.getName());
        holder.productPrice.setText(product.getPrice());

        String imageUrl = product.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            String fullImageUrl = BuildConfig.SUPABASE_URL + "/storage/v1/object/public/orders/" + imageUrl.replaceAll("^/+", "");
            Log.d(TAG, "Loading image URL: " + fullImageUrl);
            Glide.with(holder.itemView)
                    .load(fullImageUrl)
                    .placeholder(R.drawable.default_avatar)
                    .error(R.drawable.default_avatar)
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            Log.e(TAG, "Failed to load image: " + fullImageUrl + ", error: " + (e != null ? e.getMessage() : "unknown"));
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            Log.d(TAG, "Image loaded successfully: " + fullImageUrl);
                            return false;
                        }
                    })
                    .into(holder.productImage);
        } else {
            Log.w(TAG, "Image URL is empty for product: " + product.getName());
            holder.productImage.setImageResource(R.drawable.default_avatar);
        }

        boolean isFavorite = favoriteStatus.getOrDefault(product.getId(), false);
        holder.favoriteButton.setImageResource(isFavorite ? R.drawable.for_cartionok_add : R.drawable.for_cartinok);
        holder.favoriteButton.setOnClickListener(v -> {
            if (userId == null) {
                Log.e(TAG, "userId is null, cannot toggle favorite");
                Toast.makeText(holder.itemView.getContext(), "Ошибка: пользователь не авторизован", Toast.LENGTH_SHORT).show();
                return;
            }
            boolean newFavoriteStatus = !isFavorite;
            favoriteStatus.put(product.getId(), newFavoriteStatus);
            holder.favoriteButton.setImageResource(newFavoriteStatus ? R.drawable.for_cartionok_add : R.drawable.for_cartinok);
            favoriteClickListener.onFavoriteClick(product, newFavoriteStatus);
        });

        holder.addToCart.setOnClickListener(v -> {
            if (cartClickListener != null) {
                cartClickListener.onAddToCart(product);
            }
        });
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        broadcastManager.unregisterReceiver(favoriteUpdateReceiver);
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {
        ImageView productImage;
        TextView productName, productPrice;
        ImageButton favoriteButton, addToCart;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.product_image);
            productName = itemView.findViewById(R.id.product_name);
            productPrice = itemView.findViewById(R.id.product_price);
            favoriteButton = itemView.findViewById(R.id.favorite_button);
            addToCart = itemView.findViewById(R.id.add_to_cart_button);
        }
    }
}