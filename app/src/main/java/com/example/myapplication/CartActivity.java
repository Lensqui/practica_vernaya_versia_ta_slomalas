package com.example.myapplication;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CartActivity extends AppCompatActivity {
    private static final String TAG = "CartActivity";
    private RecyclerView cartRecyclerView;
    private TextView subtotalText, deliveryText, totalText;
    private Button checkoutButton;
    private LocalStorage localStorage;
    private ExecutorService executor;
    private Handler mainHandler;
    private String profileId;
    private LocalBroadcastManager broadcastManager;
    private CartAdapter cartAdapter;
    private List<CartItem> cartItems;
    private static final double DELIVERY_FEE = 60.20;
    public static final String ACTION_CART_UPDATED = "com.example.myapplication.CART_UPDATED";
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        localStorage = LocalStorage.getInstance();
        localStorage.initialize(this);
        ProductAdapter.ContextHolder.setContext(this);

        cartRecyclerView = findViewById(R.id.cart_recycler_view);
        subtotalText = findViewById(R.id.subtotal_text);
        deliveryText = findViewById(R.id.delivery_text);
        totalText = findViewById(R.id.total_text);
        checkoutButton = findViewById(R.id.checkout_button);
        ImageView btnback = findViewById(R.id.btnBack);
        btnback.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(CartActivity.this, HomeActivity.class);
                startActivity(intent);
                finish();
            }
        });
        executor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
        broadcastManager = LocalBroadcastManager.getInstance(this);
        cartItems = new ArrayList<>();

        cartRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        cartAdapter = new CartAdapter(cartItems, this::updateQuantity);
        cartRecyclerView.setAdapter(cartAdapter);

        profileId = localStorage.getProfileId();
        Log.d(TAG, "ProfileId: " + profileId);
        Log.d(TAG, "Token: " + localStorage.getToken());
        Log.d(TAG, "Email: " + localStorage.getEmail());
        if (profileId == null) {
            showErrorDialog("Ошибка: пользователь не авторизован", () -> {
                finish();
                startActivity(new Intent(this, MainActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));
            });
            return;
        }

        setupSwipeToDelete();
        loadCartItems();

        checkoutButton.setOnClickListener(v -> {
            if (cartItems.isEmpty()) {
                Toast.makeText(this, "Корзина пуста", Toast.LENGTH_SHORT).show();
                return;
            }
            double subtotal = 0.0;
            for (CartItem item : cartItems) {
                try {
                    double price = Double.parseDouble(item.getPrice());
                    subtotal += price * item.getQuantity();
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Invalid price format for item: " + item.getName() + ", price: " + item.getPrice());
                }
            }
            Intent intent = new Intent(this, OrdersWithMapActivity.class);
            intent.putExtra("SUBTOTAL", subtotal);
            startActivity(intent);
            finish();
        });
    }

    private void loadCartItems() {
        executor.execute(() -> {
            try {
                String token = localStorage.getToken();
                if (token == null) {
                    mainHandler.post(() -> showErrorDialog("Ошибка: токен отсутствует", null));
                    return;
                }
                String url = BuildConfig.SUPABASE_URL + "/rest/v1/cart?select=id,quantity,products(id,name,price,image_url)&profile_id=eq." + profileId;
                Request request = new Request.Builder()
                        .url(url)
                        .get()
                        .addHeader("apikey", BuildConfig.SUPABASE_API_KEY)
                        .addHeader("Authorization", "Bearer " + token)
                        .addHeader("Content-Type", "application/json")
                        .build();
                try (Response response = new OkHttpClient().newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        throw new IOException("Ошибка загрузки корзины: " + response.message());
                    }
                    String responseBody = response.body().string();
                    Log.d(TAG, "Cart response: " + responseBody);
                    JSONArray cartArray = new JSONArray(responseBody);
                    List<CartItem> items = new ArrayList<>();
                    for (int i = 0; i < cartArray.length(); i++) {
                        JSONObject cart = cartArray.getJSONObject(i);
                        JSONObject product = cart.getJSONObject("products");
                        items.add(new CartItem(
                                cart.getString("id"),
                                product.getString("id"),
                                product.getString("name"),
                                product.getString("price"),
                                product.optString("image_url", ""),
                                cart.getInt("quantity")
                        ));
                    }
                    mainHandler.post(() -> {
                        cartItems.clear();
                        cartItems.addAll(items);
                        cartAdapter.notifyDataSetChanged();
                        updateTotal();
                        if (items.isEmpty()) {
                            Toast.makeText(this, "Корзина пуста", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Cart load error: " + e.getMessage());
                mainHandler.post(() -> showErrorDialog("Ошибка загрузки корзины: " + e.getMessage(), null));
            }
        });
    }

    private void updateQuantity(CartItem cartItem, int newQuantity) {
        executor.execute(() -> {
            try {
                String token = localStorage.getToken();
                if (token == null) {
                    mainHandler.post(() -> showErrorDialog("Ошибка: токен отсутствует", null));
                    return;
                }
                JSONObject json = new JSONObject();
                json.put("quantity", newQuantity);
                RequestBody body = RequestBody.create(json.toString(), JSON);
                String url = BuildConfig.SUPABASE_URL + "/rest/v1/cart?id=eq." + cartItem.getId();
                Request request = new Request.Builder()
                        .url(url)
                        .patch(body)
                        .addHeader("apikey", BuildConfig.SUPABASE_API_KEY)
                        .addHeader("Authorization", "Bearer " + token)
                        .addHeader("Content-Type", "application/json")
                        .build();
                try (Response response = new OkHttpClient().newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        throw new IOException("Ошибка обновления количества: " + response.message());
                    }
                    mainHandler.post(() -> {
                        updateTotal();
                        broadcastManager.sendBroadcast(new Intent(ACTION_CART_UPDATED));
                    });
                }
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Quantity update error: " + e.getMessage());
                mainHandler.post(() -> showErrorDialog("Ошибка обновления корзины: " + e.getMessage(), null));
            }
        });
    }

    private void deleteCartItem(CartItem cartItem, int position) {
        executor.execute(() -> {
            try {
                String token = localStorage.getToken();
                if (token == null) {
                    mainHandler.post(() -> showErrorDialog("Ошибка: токен отсутствует", null));
                    return;
                }
                String url = BuildConfig.SUPABASE_URL + "/rest/v1/cart?id=eq." + cartItem.getId();
                Request request = new Request.Builder()
                        .url(url)
                        .delete()
                        .addHeader("apikey", BuildConfig.SUPABASE_API_KEY)
                        .addHeader("Authorization", "Bearer " + token)
                        .addHeader("Content-Type", "application/json")
                        .build();
                try (Response response = new OkHttpClient().newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        throw new IOException("Ошибка удаления: " + response.message());
                    }
                    mainHandler.post(() -> {
                        cartAdapter.removeItem(position);
                        updateTotal();
                        broadcastManager.sendBroadcast(new Intent(ACTION_CART_UPDATED));
                    });
                }
            } catch (IOException e) {
                Log.e(TAG, "Delete error: " + e.getMessage());
                mainHandler.post(() -> showErrorDialog("Ошибка удаления: " + e.getMessage(), null));
            }
        });
    }

    private void setupSwipeToDelete() {
        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                CartItem cartItem = cartItems.get(position);
                deleteCartItem(cartItem, position);
            }
        };
        new ItemTouchHelper(callback).attachToRecyclerView(cartRecyclerView);
    }

    private void updateTotal() {
        double subtotal = 0;
        for (CartItem item : cartItems) {
            try {
                double price = Double.parseDouble(item.getPrice());
                subtotal += price * item.getQuantity();
            } catch (NumberFormatException e) {
                Log.e(TAG, "Invalid price format for item: " + item.getName() + ", price: " + item.getPrice());
            }
        }
        double total = subtotal + (cartItems.isEmpty() ? 0 : DELIVERY_FEE);
        subtotalText.setText(String.format("₽%.2f", subtotal));
        deliveryText.setText(cartItems.isEmpty() ? "₽0.00" : String.format("₽%.2f", DELIVERY_FEE));
        totalText.setText(String.format("₽%.2f", total));
    }

    private void showErrorDialog(String message, Runnable onDismiss) {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Ошибка")
                .setMessage(message)
                .setPositiveButton("ОК", (d, which) -> {
                    d.dismiss();
                    if (onDismiss != null) {
                        onDismiss.run();
                    }
                })
                .setCancelable(false)
                .create();
        dialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}