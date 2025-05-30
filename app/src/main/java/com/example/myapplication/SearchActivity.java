package com.example.myapplication;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
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

public class SearchActivity extends AppCompatActivity {
    private static final String TAG = "SearchActivity";
    private RecyclerView recyclerView;
    private EditText searchEditText;
    private LocalStorage localStorage;
    private ExecutorService executor;
    private Handler mainHandler;
    private List<Product> allProducts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_activity);

        localStorage = LocalStorage.getInstance();
        localStorage.initialize(this);
        ProductAdapter.ContextHolder.setContext(this);

        recyclerView = findViewById(R.id.popularity);
        searchEditText = findViewById(R.id.editexttext2);
        ImageView backButton = findViewById(R.id.btnBack);

        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.setAdapter(new ProductAdapter(
                new ArrayList<>(),
                R.layout.item_product,
                localStorage.getProfileId(),
                (product, isFavorite) -> toggleFavorite(product, isFavorite),
                (product) -> addToCart(product)
        ));
        int spacingInPixels = getResources().getDimensionPixelSize(R.dimen.grid_spacing);
        recyclerView.addItemDecoration(new GridSpacingItemDecoration(2, spacingInPixels, true));

        executor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        backButton.setOnClickListener(v -> finish());

        loadProducts();

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterProducts(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadProducts() {
        executor.execute(() -> {
            try {
                String token = localStorage.getToken();
                if (token == null) {
                    mainHandler.post(() -> showErrorDialog("Ошибка: токен отсутствует", () -> finish()));
                    return;
                }
                Request request = new Request.Builder()
                        .url(BuildConfig.SUPABASE_URL + "/rest/v1/products?select=id,name,price,image_url,category_id(name)")
                        .get()
                        .addHeader("apikey", BuildConfig.SUPABASE_API_KEY)
                        .addHeader("Authorization", "Bearer " + token)
                        .addHeader("Content-Type", "application/json")
                        .build();
                try (Response response = new OkHttpClient().newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        throw new IOException("Ошибка загрузки продуктов: " + response.message());
                    }
                    String responseBody = response.body().string();
                    Log.d(TAG, "Products response: " + responseBody);
                    JSONArray productsArray = new JSONArray(responseBody);
                    allProducts = new ArrayList<>();
                    for (int i = 0; i < productsArray.length(); i++) {
                        JSONObject product = productsArray.getJSONObject(i);
                        JSONObject category = product.optJSONObject("category_id");
                        String categoryName = category != null ? category.optString("name", "") : "";
                        allProducts.add(new Product(
                                product.getString("id"),
                                product.getString("name"),
                                "₽" + String.format("%.2f", product.getDouble("price")),
                                categoryName,
                                product.optString("image_url", ""),
                                "", "", "", "", "", ""
                        ));
                    }
                    mainHandler.post(() -> {
                        ProductAdapter adapter = new ProductAdapter(
                                allProducts,
                                R.layout.item_product,
                                localStorage.getProfileId(),
                                (product, isFavorite) -> toggleFavorite(product, isFavorite),
                                (product) -> addToCart(product)
                        );
                        recyclerView.setAdapter(adapter);
                    });
                }
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Product load error: " + e.getMessage());
                mainHandler.post(() -> showErrorDialog("Ошибка загрузки продуктов: " + e.getMessage(), null));
            }
        });
    }

    private void filterProducts(String query) {
        if (allProducts == null) return;
        List<Product> filtered = new ArrayList<>();
        String lowerQuery = query.toLowerCase();
        for (Product product : allProducts) {
            if (product.getName().toLowerCase().contains(lowerQuery) ||
                    (product.getCategory() != null && product.getCategory().toLowerCase().contains(lowerQuery))) {
                filtered.add(product);
            }
        }
        ProductAdapter adapter = new ProductAdapter(
                filtered,
                R.layout.item_product,
                localStorage.getProfileId(),
                (product, isFavorite) -> toggleFavorite(product, isFavorite),
                (product) -> addToCart(product)
        );
        recyclerView.setAdapter(adapter);
    }

    private void toggleFavorite(Product product, boolean isFavorite) {
        String profileId = localStorage.getProfileId();
        if (profileId == null) {
            showErrorDialog("Ошибка: пользователь не авторизован", null);
            return;
        }
        executor.execute(() -> {
            try {
                String token = localStorage.getToken();
                if (token == null) {
                    mainHandler.post(() -> showErrorDialog("Ошибка: токен отсутствует", null));
                    return;
                }
                if (isFavorite) {
                    JSONObject json = new JSONObject();
                    json.put("profile_id", profileId);
                    json.put("product_id", product.getId());
                    RequestBody body = RequestBody.create(json.toString(), MediaType.get("application/json; charset=utf-8"));
                    Request request = new Request.Builder()
                            .url(BuildConfig.SUPABASE_URL + "/rest/v1/favorites")
                            .post(body)
                            .addHeader("apikey", BuildConfig.SUPABASE_API_KEY)
                            .addHeader("Authorization", "Bearer " + token)
                            .addHeader("Content-Type", "application/json")
                            .build();
                    try (Response response = new OkHttpClient().newCall(request).execute()) {
                        if (!response.isSuccessful()) {
                            throw new IOException("Ошибка добавления: " + response.message());
                        }
                        mainHandler.post(() -> Toast.makeText(this, "Добавлено в избранное", Toast.LENGTH_SHORT).show());
                    }
                } else {
                    String url = BuildConfig.SUPABASE_URL + "/rest/v1/favorites?profile_id=eq." + profileId + "&product_id=eq." + product.getId();
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
                        mainHandler.post(() -> Toast.makeText(this, "Удалено из избранного", Toast.LENGTH_SHORT).show());
                    }
                }
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Favorite error: " + e.getMessage());
                mainHandler.post(() -> showErrorDialog("Ошибка: " + e.getMessage(), null));
            }
        });
    }

    private void addToCart(Product product) {
        String profileId = localStorage.getProfileId();
        if (profileId == null) {
            showErrorDialog("Ошибка: пользователь не авторизован", null);
            return;
        }
        executor.execute(() -> {
            try {
                String token = localStorage.getToken();
                if (token == null) {
                    mainHandler.post(() -> showErrorDialog("Ошибка: токен отсутствует", null));
                    return;
                }
                String checkUrl = BuildConfig.SUPABASE_URL + "/rest/v1/cart?profile_id=eq." + profileId + "&product_id=eq." + product.getId() + "&select=quantity";
                Request checkRequest = new Request.Builder()
                        .url(checkUrl)
                        .get()
                        .addHeader("apikey", BuildConfig.SUPABASE_API_KEY)
                        .addHeader("Authorization", "Bearer " + token)
                        .addHeader("Content-Type", "application/json")
                        .build();
                try (Response checkResponse = new OkHttpClient().newCall(checkRequest).execute()) {
                    if (!checkResponse.isSuccessful()) {
                        throw new IOException("Ошибка проверки корзины: " + checkResponse.message());
                    }
                    String responseBody = checkResponse.body().string();
                    JSONArray cartItems = new JSONArray(responseBody);
                    if (cartItems.length() > 0) {
                        int currentQuantity = cartItems.getJSONObject(0).getInt("quantity");
                        JSONObject json = new JSONObject();
                        json.put("quantity", currentQuantity + 1);
                        RequestBody body = RequestBody.create(json.toString(), MediaType.get("application/json; charset=utf-8"));
                        String updateUrl = BuildConfig.SUPABASE_URL + "/rest/v1/cart?profile_id=eq." + profileId + "&product_id=eq." + product.getId();
                        Request updateRequest = new Request.Builder()
                                .url(updateUrl)
                                .patch(body)
                                .addHeader("apikey", BuildConfig.SUPABASE_API_KEY)
                                .addHeader("Authorization", "Bearer " + token)
                                .addHeader("Content-Type", "application/json")
                                .build();
                        try (Response updateResponse = new OkHttpClient().newCall(updateRequest).execute()) {
                            if (!updateResponse.isSuccessful()) {
                                throw new IOException("Ошибка обновления корзины: " + updateResponse.message());
                            }
                        }
                    } else {
                        JSONObject json = new JSONObject();
                        json.put("profile_id", profileId);
                        json.put("product_id", product.getId());
                        json.put("quantity", 1);
                        RequestBody body = RequestBody.create(json.toString(), MediaType.get("application/json; charset=utf-8"));
                        Request request = new Request.Builder()
                                .url(BuildConfig.SUPABASE_URL + "/rest/v1/cart")
                                .post(body)
                                .addHeader("apikey", BuildConfig.SUPABASE_API_KEY)
                                .addHeader("Authorization", "Bearer " + token)
                                .addHeader("Content-Type", "application/json")
                                .build();
                        try (Response response = new OkHttpClient().newCall(request).execute()) {
                            if (!response.isSuccessful()) {
                                throw new IOException("Ошибка добавления в корзину: " + response.message());
                            }
                        }
                    }
                    mainHandler.post(() -> Toast.makeText(this, "Добавлено в корзину", Toast.LENGTH_SHORT).show());
                }
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Add to cart error: " + e.getMessage());
                mainHandler.post(() -> showErrorDialog("Ошибка добавления в корзину: " + e.getMessage(), null));
            }
        });
    }

    private void showErrorDialog(String message, Runnable onDismiss) {
        new AlertDialog.Builder(this)
                .setTitle("Ошибка")
                .setMessage(message)
                .setPositiveButton("ОК", (dialog, which) -> {
                    dialog.dismiss();
                    if (onDismiss != null) onDismiss.run();
                })
                .setCancelable(false)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}