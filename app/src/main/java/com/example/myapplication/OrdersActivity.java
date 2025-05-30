package com.example.myapplication;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
public class OrdersActivity extends AppCompatActivity {
    private static final String TAG = "OrdersActivity";
    private RecyclerView ordersRecyclerView;
    private ExecutorService executorService;
    private Handler mainHandler;
    private LocalStorage localStorage;
    private String profileId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.acitivity_orders);

        localStorage = LocalStorage.getInstance();
        localStorage.initialize(this);
        profileId = localStorage.getProfileId();
        Log.d(TAG, "Profile ID: " + profileId);
        Log.d(TAG, "SUPABASE_URL: " + BuildConfig.SUPABASE_URL);

        ordersRecyclerView = findViewById(R.id.orders_recycler_view);
        ordersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        ordersRecyclerView.setHasFixedSize(true);
        ordersRecyclerView.setAdapter(new OrderAdapter(new ArrayList<>()));

        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        loadOrders();
    }

    private void loadOrders() {
        executorService.execute(() -> {
            try {
                String token = localStorage.getToken();
                if (token == null) {
                    mainHandler.post(() -> showErrorDialog("Ошибка: токен отсутствует", null));
                    return;
                }
                String url = BuildConfig.SUPABASE_URL + "/rest/v1/orders?profile_id=eq." + profileId + "&select=*,order_items(*,products(name,price,image_url))";
                Request request = new Request.Builder()
                        .url(url)
                        .get()
                        .addHeader("apikey", BuildConfig.SUPABASE_API_KEY)
                        .addHeader("Authorization", "Bearer " + token)
                        .build();
                try (Response response = new OkHttpClient().newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        throw new IOException("Ошибка загрузки заказов: " + response.code() + ", " + response.body().string());
                    }
                    String responseBody = response.body().string();
                    Log.d(TAG, "Orders response: " + responseBody);
                    JSONArray ordersArray = new JSONArray(responseBody);
                    List<Order> orders = new ArrayList<>();
                    String[] possibleFormats = {
                            "yyyy-MM-dd'T'HH:mm:ss.SSSSSS",
                            "yyyy-MM-dd'T'HH:mm:ss.SSSSSSZ",
                            "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
                            "yyyy-MM-dd'T'HH:mm:ssZ"
                    };
                    for (int i = 0; i < ordersArray.length(); i++) {
                        JSONObject order = ordersArray.getJSONObject(i);
                        JSONArray itemsArray = order.getJSONArray("order_items");
                        JSONObject firstItem = itemsArray.length() > 0 ? itemsArray.getJSONObject(0).optJSONObject("products") : null;

                        Date createdAt = null;
                        String createdAtStr = order.optString("created_at", null);
                        Log.d(TAG, "Order ID: " + order.optString("id") + ", created_at: " + createdAtStr);
                        if (createdAtStr != null) {
                            for (String format : possibleFormats) {
                                try {
                                    SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());
                                    if (format.contains("Z") && createdAtStr.endsWith("Z")) {
                                        createdAtStr = createdAtStr.replace("Z", "+0000");
                                    }
                                    createdAt = sdf.parse(createdAtStr);
                                    break;
                                } catch (ParseException e) {
                                    Log.w(TAG, "Failed to parse date with format " + format + ": " + e.getMessage());
                                }
                            }
                        }

                        String imageUrl = firstItem != null ? firstItem.optString("image_url", "").replaceAll("^\\s+", "") : "";
                        Log.d(TAG, "Order ID: " + order.optString("id") + ", image_url: " + imageUrl);

                        orders.add(new Order(
                                order.optString("id", "unknown"),
                                firstItem != null ? firstItem.optString("name", "Заказ #" + order.optString("id")) : "Заказ #" + order.optString("id"),
                                order.optDouble("total_price", 0.0),
                                firstItem != null ? firstItem.optDouble("price", 0.0) : 0.0,
                                firstItem != null ? itemsArray.getJSONObject(0).optInt("quantity", 0) : 0,
                                imageUrl,
                                createdAt
                        ));
                    }
                    mainHandler.post(() -> {
                        OrderAdapter adapter = new OrderAdapter(orders);
                        ordersRecyclerView.setAdapter(adapter);
                    });
                }
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Orders load error: ", e);
                mainHandler.post(() -> showErrorDialog("Ошибка загрузки заказов: " + e.getMessage(), null));
            }
        });
    }

    private void showErrorDialog(String message, Runnable onDismiss) {
        new AlertDialog.Builder(this)
                .setTitle("Ошибка")
                .setMessage(message)
                .setPositiveButton("ОК", (d, which) -> {
                    d.dismiss();
                    if (onDismiss != null) onDismiss.run();
                })
                .setCancelable(false)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}