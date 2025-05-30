package com.example.myapplication;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

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

public class FavoritesActivity extends AppCompatActivity {
    private static final String TAG = "FavoritesActivity";
    private DrawerLayout drawerLayout;
    private ImageView backButton;
    private ImageButton cartButton;
    private RecyclerView favoriteRecyclerView;
    private TextView drawerUserFullName;
    private ImageView drawerUserAvatar;
    private LocalStorage localStorage;
    private ExecutorService executor;
    private Handler mainHandler;
    private String currentEmail;
    private String userId;
    private LocalBroadcastManager broadcast;
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    public static final String ACTION_FAVORITE = "com.example.myapp.FAVORITE.action";
    public static final String ACTION_CART_UPDATED = "com.example.myapplication.CART_UPDATED";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.favorite_activity);

        localStorage = LocalStorage.getInstance();
        localStorage.initialize(this);
        ProductAdapter.ContextHolder.setContext(this);

        drawerLayout = findViewById(R.id.drawer_layout);
        backButton = findViewById(R.id.btnBack);
        cartButton = findViewById(R.id.btnCart);
        favoriteRecyclerView = findViewById(R.id.favoriteRecyclerView);
        View drawerView = findViewById(R.id.drawer_menu);
        drawerUserFullName = drawerView.findViewById(R.id.user_fullname);
        drawerUserAvatar = drawerView.findViewById(R.id.user_avatar);

        executor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
        broadcast = LocalBroadcastManager.getInstance(this);

        favoriteRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        favoriteRecyclerView.setAdapter(new ProductAdapter(new ArrayList<>(), R.layout.item_product, null, this::toggleFavorite, this::addToCart));

        int spacingInPixels = getResources().getDimensionPixelSize(R.dimen.grid_spacing);
        favoriteRecyclerView.addItemDecoration(new GridSpacingItemDecoration(2, spacingInPixels, true));

        initializeDrawerMenu(drawerView);

        currentEmail = localStorage.getEmail();
        if (currentEmail == null) {
            showErrorDialog("Ошибка: пользователь не авторизован");
            startActivity(new Intent(this, MainActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));
            finish();
            return;
        }

        loadProfileData();

        backButton.setOnClickListener(v -> onBackPressed());
        cartButton.setOnClickListener(v -> startActivity(new Intent(this, CartActivity.class)));

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        FloatingActionButton fabCart = findViewById(R.id.fab_cart);
        ImageView btnBack = findViewById(R.id.btnBack);
        ImageButton btnCart = findViewById(R.id.btnCart);

        if (bottomNavigationView == null) {
            Log.e(TAG, "BottomNavigationView не найден в разметке!");
            return;
        }

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navigation_home) {
                Log.d(TAG, "Переход на Главную");
                startActivity(new Intent(FavoritesActivity.this, HomeActivity.class));
                finish();
                return true;
            } else if (id == R.id.navigation_favorites) {
                Log.d(TAG, "Уже в Избранном");
                return true;
            } else if (id == R.id.navigation_notifications) {
                Log.d(TAG, "Переход в Уведомления");
                startActivity(new Intent(FavoritesActivity.this, NotificationsActivity.class));
                finish();
                return true;
            } else if (id == R.id.navigation_profile) {
                Log.d(TAG, "Переход в Профиль");
                startActivity(new Intent(FavoritesActivity.this, ProfileActivity.class));
                finish();
                return true;
            }
            return false;
        });

        fabCart.setOnClickListener(v -> {
            Log.d(TAG, "Переход в Корзину");
            startActivity(new Intent(FavoritesActivity.this, CartActivity.class));
        });

        btnBack.setOnClickListener(v -> {
            Log.d(TAG, "Нажата кнопка Назад");
            finish();
        });

        btnCart.setOnClickListener(v -> {
            Log.d(TAG, "Переход в Корзину");
            startActivity(new Intent(FavoritesActivity.this, CartActivity.class));
        });

        bottomNavigationView.setSelectedItemId(R.id.navigation_favorites);

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
        navFavorites.setOnClickListener(v -> drawerLayout.closeDrawer(GravityCompat.START));
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
                String response = SupabaseApi.getInstance().fetchProfile(currentEmail);
                Log.d(TAG, "Profile response: " + response);
                if (response != null && !response.equals("[]")) {
                    JSONArray profiles = new JSONArray(response);
                    if (profiles.length() > 0) {
                        JSONObject profile = profiles.getJSONObject(0);
                        String firstName = profile.optString("first_name", "");
                        String lastName = profile.optString("last_name", "");
                        String avatarPath = profile.optString("avatar_url", null);
                        userId = profile.getString("id");

                        localStorage.saveProfileId(userId);

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
                            loadFavoriteProducts();
                        });
                    } else {
                        mainHandler.post(() -> showErrorDialog("Данные профиля не найдены"));
                    }
                } else {
                    mainHandler.post(() -> showErrorDialog("Данные профиля не найдены"));
                }
            } catch (IOException | JSONException e) {
                mainHandler.post(() -> showErrorDialog("Ошибка загрузки профиля: " + e.getMessage()));
            }
        });
    }

    private void loadFavoriteProducts() {
        if (userId == null) {
            Log.e(TAG, "userId is null, cannot load favorites");
            return;
        }
        executor.execute(() -> {
            try {
                String token = localStorage.getToken();
                String url = BuildConfig.SUPABASE_URL + "/rest/v1/favorites?select=products(id,name,price,image_url,category_id,image_url_for_details,image_url_for_details_2,image_url_for_details_3,image_url_for_details_4,image_url_for_details_5,text_for_details)&profile_id=eq." + userId;
                Request request = new Request.Builder()
                        .url(url)
                        .get()
                        .addHeader("apikey", BuildConfig.SUPABASE_API_KEY)
                        .addHeader("Authorization", "Bearer " + token)
                        .addHeader("Content-Type", "application/json")
                        .build();
                try (Response response = new OkHttpClient().newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        throw new IOException("Ошибка загрузки избранного: " + response.message());
                    }
                    String responseBody = response.body().string();
                    JSONArray favorites = new JSONArray(responseBody);
                    List<Product> products = new ArrayList<>();
                    for (int i = 0; i < favorites.length(); i++) {
                        JSONObject favorite = favorites.getJSONObject(i);
                        JSONObject product = favorite.getJSONObject("products");
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
                        ProductAdapter adapter = new ProductAdapter(products, R.layout.item_product, userId, this::toggleFavorite, this::addToCart);
                        favoriteRecyclerView.setAdapter(adapter);
                        if (products.isEmpty()) {
                            Toast.makeText(this, "Избранное пусто", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } catch (IOException | JSONException e) {
                mainHandler.post(() -> showErrorDialog("Ошибка загрузки избранного: " + e.getMessage()));
            }
        });
    }

    private void toggleFavorite(Product product, boolean isFavorite) {
        if (userId == null) {
            showErrorDialog("Ошибка: пользователь не авторизован");
            return;
        }
        executor.execute(() -> {
            try {
                String token = localStorage.getToken();
                if (token == null) {
                    throw new IOException("Токен отсутствует");
                }
                if (isFavorite) {
                    JSONObject json = new JSONObject();
                    json.put("profile_id", userId);
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
                            throw new IOException("Ошибка добавления в избранное: " + response.message());
                        }
                        mainHandler.post(() -> {
                            Toast.makeText(this, "Добавлено в избранное", Toast.LENGTH_SHORT).show();
                            broadcast.sendBroadcast(new Intent(ACTION_FAVORITE));
                        });
                    }
                } else {
                    String url = BuildConfig.SUPABASE_URL + "/rest/v1/favorites?profile_id=eq." + userId + "&product_id=eq." + product.getId();
                    Request request = new Request.Builder()
                            .url(url)
                            .delete()
                            .addHeader("apikey", BuildConfig.SUPABASE_API_KEY)
                            .addHeader("Authorization", "Bearer " + token)
                            .addHeader("Content-Type", "application/json")
                            .build();
                    try (Response response = new OkHttpClient().newCall(request).execute()) {
                        if (!response.isSuccessful()) {
                            throw new IOException("Ошибка удаления из избранного: " + response.message());
                        }
                        mainHandler.post(() -> {
                            Toast.makeText(this, "Удалено из избранного", Toast.LENGTH_SHORT).show();
                            loadFavoriteProducts();
                            broadcast.sendBroadcast(new Intent(ACTION_FAVORITE));
                        });
                    }
                }
            } catch (IOException | JSONException e) {
                mainHandler.post(() -> showErrorDialog("Ошибка: " + e.getMessage()));
            }
        });
    }

    private void addToCart(Product product) {
        if (userId == null) {
            showErrorDialog("Ошибка: пользователь не авторизован");
            return;
        }
        executor.execute(() -> {
            try {
                String token = localStorage.getToken();
                if (token == null) {
                    throw new IOException("Токен отсутствует");
                }
                String checkUrl = BuildConfig.SUPABASE_URL + "/rest/v1/cart?profile_id=eq." + userId + "&product_id=eq." + product.getId() + "&select=quantity";
                Request checkRequest = new Request.Builder()
                        .url(checkUrl)
                        .get()
                        .addHeader("apikey", BuildConfig.SUPABASE_API_KEY)
                        .addHeader("Authorization", "Bearer " + token)
                        .addHeader("Content-Type", "application/json")
                        .build();
                Response checkResponse = new OkHttpClient().newCall(checkRequest).execute();
                try {
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
                        String updateUrl = BuildConfig.SUPABASE_URL + "/rest/v1/cart?profile_id=eq." + userId + "&product_id=eq." + product.getId();
                        Request updateRequest = new Request.Builder()
                                .url(updateUrl)
                                .patch(body)
                                .addHeader("apikey", BuildConfig.SUPABASE_API_KEY)
                                .addHeader("Authorization", "Bearer " + token)
                                .addHeader("Content-Type", "application/json")
                                .build();
                        Response updateResponse = new OkHttpClient().newCall(updateRequest).execute();
                        try {
                            if (!updateResponse.isSuccessful()) {
                                throw new IOException("Ошибка обновления корзины: " + updateResponse.message());
                            }
                        } finally {
                            updateResponse.close();
                        }
                    } else {
                        JSONObject json = new JSONObject();
                        json.put("profile_id", userId);
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
                        Response postResponse = new OkHttpClient().newCall(request).execute();
                        try {
                            if (!postResponse.isSuccessful()) {
                                throw new IOException("Ошибка добавления в корзину: " + postResponse.message());
                            }
                        } finally {
                            postResponse.close();
                        }
                    }
                    mainHandler.post(() -> {
                        Toast.makeText(this, "Добавлено в корзину", Toast.LENGTH_SHORT).show();
                        broadcast.sendBroadcast(new Intent(ACTION_CART_UPDATED));
                    });
                } finally {
                    checkResponse.close();
                }
            } catch (IOException | JSONException e) {
                mainHandler.post(() -> showErrorDialog("Ошибка добавления в корзину: " + e.getMessage()));
            }
        });
    }

    private void showErrorDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Ошибка")
                .setMessage(message)
                .setPositiveButton("ОК", (dialog, which) -> dialog.dismiss())
                .setCancelable(false)
                .show();
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