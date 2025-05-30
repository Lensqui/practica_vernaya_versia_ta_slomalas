package com.example.myapplication;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CategoryActivity extends AppCompatActivity {
    private static final String TAG = "Category";
    private DrawerLayout drawerLayout;
    private ImageView backButton;
    private TextView titleTextView, drawerUserFullName;
    private ImageView drawerUserAvatar;
    private RecyclerView buttonsRecyclerView, productsRecyclerView;
    private ProgressBar progressBar;
    private LocalStorage localStorage;
    private ExecutorService executor;
    private Handler mainHandler;
    private String currentEmail;
    private String profileId;
    private LocalBroadcastManager broadcastManager;
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    public static final String ACTION_CART_UPDATED = "com.example.myapplication.CART_UPDATED";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.acitivity_categories);

        localStorage = LocalStorage.getInstance();
        localStorage.initialize(this);
        ProductAdapter.ContextHolder.setContext(this);

        drawerLayout = findViewById(R.id.drawer_layout);
        backButton = findViewById(R.id.btnBack);
        titleTextView = findViewById(R.id.txtNameTitle);
        buttonsRecyclerView = findViewById(R.id.buttons_recycler_view);
        productsRecyclerView = findViewById(R.id.popularity);
        progressBar = findViewById(R.id.progressBar);

        View drawerView = findViewById(R.id.drawer_menu);
        drawerUserFullName = drawerView.findViewById(R.id.user_fullname);
        drawerUserAvatar = drawerView.findViewById(R.id.user_avatar);

        executor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
        broadcastManager = LocalBroadcastManager.getInstance(this);

        buttonsRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        buttonsRecyclerView.setAdapter(new CategoryAdapter(new ArrayList<>(), this::onCategoryClick));
        productsRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        productsRecyclerView.setAdapter(new ProductAdapter(new ArrayList<>(), R.layout.item_product, null, this::toggleFavorite, this::addToCart));

        int spacingInPixels = getResources().getDimensionPixelSize(R.dimen.grid_spacing);
        productsRecyclerView.addItemDecoration(new GridSpacingItemDecoration(2, spacingInPixels, true));

        initializeDrawerMenu(drawerView);

        currentEmail = localStorage.getEmail();
        Log.d(TAG, "Email: " + currentEmail);
        Log.d(TAG, "Token: " + localStorage.getToken());
        if (currentEmail == null) {
            showErrorDialog("Ошибка: пользователь не авторизован", () -> {
                startActivity(new Intent(this, MainActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));
                finish();
            });
            return;
        }

        loadProfileData();

        String category = getIntent().getStringExtra("category");
        Log.d(TAG, "Выбрана категория: " + category);
        titleTextView.setText(category != null ? category : "Категория");

        setupCategoryRecyclerView();
        if (category != null && !category.isEmpty()) {
            setupProductsRecyclerView(category);
        } else {
            showErrorDialog("Категория не выбрана", null);
            progressBar.setVisibility(View.GONE);
        }

        backButton.setOnClickListener(v -> onBackPressed());
    }

    private void onCategoryClick(String categoryName) {
        Intent intent = new Intent(this, CategoryActivity.class);
        intent.putExtra("category", categoryName);
        startActivity(intent);
    }

    private void initializeDrawerMenu(View drawerView) {
        LinearLayout navProfile = drawerView.findViewById(R.id.nav_profile);
        LinearLayout navCart = drawerView.findViewById(R.id.nav_cart);
        LinearLayout navFavorites = drawerView.findViewById(R.id.nav_favorites);
        LinearLayout navOrders = drawerView.findViewById(R.id.nav_orders);
        LinearLayout navNotifications = drawerView.findViewById(R.id.nav_notifications);
        LinearLayout navSettings = drawerView.findViewById(R.id.nav_settings);
        LinearLayout navSupport = drawerView.findViewById(R.id.nav_support);
        LinearLayout navLogout = drawerView.findViewById(R.id.nav_logout);

        navProfile.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            startActivity(new Intent(this, ProfileActivity.class));
        });
        navCart.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            startActivity(new Intent(this, CartActivity.class));
        });
        navFavorites.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            startActivity(new Intent(this, FavoritesActivity.class));
        });
        navOrders.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            Toast.makeText(this, "Заказы (в разработке)", Toast.LENGTH_SHORT).show();
        });
        navNotifications.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            Toast.makeText(this, "Уведомления (в разработке)", Toast.LENGTH_SHORT).show();
        });
        navSettings.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            Toast.makeText(this, "Настройки (в разработке)", Toast.LENGTH_SHORT).show();
        });
        navSupport.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            Toast.makeText(this, "Чат поддержки (в разработке)", Toast.LENGTH_SHORT).show();
        });
        navLogout.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            localStorage.clearAll();
            startActivity(new Intent(this, MainActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));
            finish();
        });
    }

    private void loadProfileData() {
        executor.execute(() -> {
            try {
                String token = localStorage.getToken();
                if (token == null) {
                    mainHandler.post(() -> showErrorDialog("Ошибка: токен отсутствует", () -> {
                        startActivity(new Intent(this, MainActivity.class)
                                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));
                        finish();
                    }));
                    return;
                }
                String response = SupabaseApi.getInstance().fetchProfile(currentEmail);
                Log.d(TAG, "Ответ профиля: " + response);
                if (response != null && !response.equals("[]")) {
                    JSONArray profiles = new JSONArray(response);
                    if (profiles.length() > 0) {
                        JSONObject profile = profiles.getJSONObject(0);
                        String firstName = profile.optString("first_name", "");
                        String lastName = profile.optString("last_name", "");
                        String avatarPath = profile.optString("avatar_url", null);
                        profileId = profile.getString("id");

                        Log.d(TAG, "Saving profileId: " + profileId);
                        localStorage.saveProfileId(profileId);

                        mainHandler.post(() -> {
                            drawerUserFullName.setText(firstName + " " + lastName);
                            if (avatarPath != null && !avatarPath.isEmpty()) {
                                String avatarUrl = BuildConfig.SUPABASE_URL + "/storage/v1/object/public/avatars/" + avatarPath;
                                Glide.with(this)
                                        .load(avatarUrl)
                                        .placeholder(R.drawable.default_avatar)
                                        .error(R.drawable.default_avatar)
                                        .into(drawerUserAvatar);
                            } else {
                                drawerUserAvatar.setImageResource(R.drawable.default_avatar);
                            }
                            ProductAdapter adapter = (ProductAdapter) productsRecyclerView.getAdapter();
                            if (adapter != null) {
                                adapter.setUserId(profileId);
                                adapter.loadFavoriteStatus();
                            }
                        });
                    } else {
                        mainHandler.post(() -> showErrorDialog("Данные профиля не найдены", null));
                    }
                } else {
                    mainHandler.post(() -> showErrorDialog("Данные профиля не найдены", null));
                }
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Profile load error: " + e.getMessage());
                mainHandler.post(() -> showErrorDialog("Ошибка загрузки профиля: " + e.getMessage(), null));
            }
        });
    }

    private void setupCategoryRecyclerView() {
        executor.execute(() -> {
            try {
                String token = localStorage.getToken();
                if (token == null) {
                    mainHandler.post(() -> showErrorDialog("Ошибка: токен отсутствует", null));
                    return;
                }
                Request request = new Request.Builder()
                        .url(BuildConfig.SUPABASE_URL + "/rest/v1/categories?select=id,name")
                        .get()
                        .addHeader("apikey", BuildConfig.SUPABASE_API_KEY)
                        .addHeader("Authorization", "Bearer " + token)
                        .addHeader("Content-Type", "application/json")
                        .build();
                try (Response response = new OkHttpClient().newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        throw new IOException("Ошибка загрузки категорий: " + response.message());
                    }
                    String responseBody = response.body().string();
                    Log.d(TAG, "Ответ категорий: " + responseBody);
                    JSONArray categoriesArray = new JSONArray(responseBody);
                    List<Category> categories = new ArrayList<>();
                    for (int i = 0; i < categoriesArray.length(); i++) {
                        JSONObject category = categoriesArray.getJSONObject(i);
                        categories.add(new Category(
                                category.getString("id"),
                                category.getString("name")
                        ));
                    }
                    mainHandler.post(() -> {
                        CategoryAdapter adapter = new CategoryAdapter(
                                categories.stream().map(Category::getName).collect(Collectors.toList()),
                                this::onCategoryClick
                        );
                        buttonsRecyclerView.setAdapter(adapter);
                    });
                }
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Category load error: " + e.getMessage());
                mainHandler.post(() -> showErrorDialog("Ошибка загрузки категорий: " + e.getMessage(), null));
            }
        });
    }

    private void setupProductsRecyclerView(String categoryName) {
        progressBar.setVisibility(View.VISIBLE);
        executor.execute(() -> {
            try {
                String token = localStorage.getToken();
                if (token == null) {
                    mainHandler.post(() -> {
                        showErrorDialog("Ошибка: токен отсутствует", null);
                        progressBar.setVisibility(View.GONE);
                    });
                    return;
                }
                String encodedCategoryName = URLEncoder.encode(categoryName, "UTF-8");
                Request categoryRequest = new Request.Builder()
                        .url(BuildConfig.SUPABASE_URL + "/rest/v1/categories?select=id&name=eq." + encodedCategoryName)
                        .get()
                        .addHeader("apikey", BuildConfig.SUPABASE_API_KEY)
                        .addHeader("Authorization", "Bearer " + token)
                        .addHeader("Content-Type", "application/json")
                        .build();
                String categoryId;
                try (Response response = new OkHttpClient().newCall(categoryRequest).execute()) {
                    if (!response.isSuccessful()) {
                        throw new IOException("Ошибка загрузки категории: " + response.message());
                    }
                    String responseBody = response.body().string();
                    Log.d(TAG, "Ответ ID категории: " + responseBody);
                    JSONArray categoryArray = new JSONArray(responseBody);
                    if (categoryArray.length() == 0) {
                        throw new IOException("Категория не найдена: " + categoryName);
                    }
                    categoryId = categoryArray.getJSONObject(0).getString("id");
                }

                String url = BuildConfig.SUPABASE_URL + "/rest/v1/products?select=*&category_id=eq." + categoryId;
                Log.d(TAG, "URL запроса продуктов: " + url);
                Request productRequest = new Request.Builder()
                        .url(url)
                        .get()
                        .addHeader("apikey", BuildConfig.SUPABASE_API_KEY)
                        .addHeader("Authorization", "Bearer " + token)
                        .addHeader("Content-Type", "application/json")
                        .build();
                try (Response response = new OkHttpClient().newCall(productRequest).execute()) {
                    if (!response.isSuccessful()) {
                        throw new IOException("Ошибка загрузки продуктов: " + response.message());
                    }
                    String responseBody = response.body().string();
                    Log.d(TAG, "Ответ продуктов: " + responseBody);
                    JSONArray productsArray = new JSONArray(responseBody);
                    List<Product> products = new ArrayList<>();
                    for (int i = 0; i < productsArray.length(); i++) {
                        JSONObject product = productsArray.getJSONObject(i);
                        products.add(new Product(
                                product.getString("id"),
                                product.getString("name"),
                                "₽" + String.format("%.2f", product.getDouble("price")),
                                product.optString("category_id", ""),
                                product.optString("image_url", ""),
                                product.optString("image_url_for_details", ""),
                                product.optString("image_url_for_details_2", ""),
                                product.optString("image_url_for_details_3", ""),
                                product.optString("image_url_for_details_4", ""),
                                product.optString("image_url_for_details_5", ""),
                                product.optString("text_for_details", "")
                        ));
                    }
                    mainHandler.post(() -> {
                        ProductAdapter adapter = new ProductAdapter(products, R.layout.item_product, profileId, this::toggleFavorite, this::addToCart);
                        productsRecyclerView.setAdapter(adapter);
                        progressBar.setVisibility(View.GONE);
                        if (products.isEmpty()) {
                            Toast.makeText(this, "Продукты не найдены для категории: " + categoryName, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Product load error: " + e.getMessage());
                mainHandler.post(() -> {
                    showErrorDialog("Ошибка загрузки продуктов: " + e.getMessage(), null);
                    progressBar.setVisibility(View.GONE);
                });
            }
        });
    }

    private void toggleFavorite(Product product, boolean isFavorite) {
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
                    RequestBody body = RequestBody.create(json.toString(), JSON);
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
                        mainHandler.post(() -> {
                            Toast.makeText(this, "Добавлено в избранное", Toast.LENGTH_SHORT).show();
                            broadcastManager.sendBroadcast(new Intent(ProductAdapter.ACTION_FAVORITE_UPDATED));
                        });
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
                        mainHandler.post(() -> {
                            Toast.makeText(this, "Удалено из избранного", Toast.LENGTH_SHORT).show();
                            broadcastManager.sendBroadcast(new Intent(ProductAdapter.ACTION_FAVORITE_UPDATED));
                        });
                    }
                }
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Favorite error: " + e.getMessage());
                mainHandler.post(() -> showErrorDialog("Ошибка: " + e.getMessage(), null));
            }
        });
    }

    private void addToCart(Product product) {
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
                        RequestBody body = RequestBody.create(json.toString(), JSON);
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
                        RequestBody body = RequestBody.create(json.toString(), JSON);
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
                    mainHandler.post(() -> {
                        Toast.makeText(this, "Добавлено в корзину", Toast.LENGTH_SHORT).show();
                        broadcastManager.sendBroadcast(new Intent(ACTION_CART_UPDATED));
                    });
                }
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Add to cart error: " + e.getMessage());
                mainHandler.post(() -> showErrorDialog("Ошибка добавления в корзину: " + e.getMessage(), null));
            }
        });
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

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}