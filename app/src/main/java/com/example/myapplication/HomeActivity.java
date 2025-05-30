package com.example.myapplication;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class HomeActivity extends AppCompatActivity {
    private static final String TAG = "HomeActivity";
    private DrawerLayout drawerLayout;
    private ImageView menuButton;
    private ImageButton cartButton, filtersButton;
    private TextView drawerUserFullName;
    private ImageView drawerUserAvatar;
    private RecyclerView buttonsRecyclerView, popularRecyclerView, specialRecyclerView;
    private LocalStorage localStorage;
    private ExecutorService executor;
    private Handler mainHandler;
    private String currentEmail;
    private String profileId;
    private LocalBroadcastManager broadcast;
    private BroadcastReceiver cartUpdateReceiver;
    private BroadcastReceiver notificationsUpdateReceiver;
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    public static final String ACTION_CART_UPDATED = "com.example.myapplication.CART_UPDATED";
    public static final String ACTION_NOTIFICATIONS_UPDATED = "com.example.myapplication.NOTIFICATIONS_UPDATED";
    private TextView popularSeeAll, specialSeeAll;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        localStorage = LocalStorage.getInstance();
        localStorage.initialize(this);
        ProductAdapter.ContextHolder.setContext(this);
        HomeProductAdapter.ContextHolder.setContext(this);

        drawerLayout = findViewById(R.id.drawer_layout);
        menuButton = findViewById(R.id.menu_on_home);
        cartButton = findViewById(R.id.handba);
        filtersButton = findViewById(R.id.filters);
        buttonsRecyclerView = findViewById(R.id.buttons_recycler_view);
        popularRecyclerView = findViewById(R.id.popularView);
        specialRecyclerView = findViewById(R.id.SpecialView);
        popularSeeAll = findViewById(R.id.txtAll);
        specialSeeAll = findViewById(R.id.txtAll2);

        View drawerView = findViewById(R.id.drawer_menu);
        drawerUserFullName = drawerView.findViewById(R.id.user_fullname);
        drawerUserAvatar = drawerView.findViewById(R.id.user_avatar);

        executor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
        broadcast = LocalBroadcastManager.getInstance(this);

        buttonsRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        buttonsRecyclerView.setAdapter(new CategoryAdapter(new ArrayList<>(), this::onCategoryClick));

        popularRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        HomeProductAdapter popularAdapter = new HomeProductAdapter(
                new ArrayList<>(),
                R.layout.item_product2,
                localStorage.getProfileId(),
                (product, isFavorite) -> toggleFavorite(product, isFavorite),
                product -> addToCart(product)
        );
        popularRecyclerView.setAdapter(popularAdapter);

        specialRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        specialRecyclerView.setAdapter(new PromotionAdapter(new ArrayList<>()));

        initializeDrawerMenu(drawerView);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        FloatingActionButton fabCart = findViewById(R.id.fab_cart);

        if (bottomNavigationView == null) {
            Log.e(TAG, "BottomNavigationView не найден в разметке!");
            return;
        }

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navigation_home) {
                Log.d(TAG, "Переход на Главную");
                return true;
            } else if (id == R.id.navigation_favorites) {
                Log.d(TAG, "Переход в Избранное");
                startActivity(new Intent(HomeActivity.this, FavoritesActivity.class));
                return true;
            } else if (id == R.id.navigation_notifications) {
                Log.d(TAG, "Переход в Уведомления");
                startActivity(new Intent(HomeActivity.this, NotificationsActivity.class));
                return true;
            } else if (id == R.id.navigation_profile) {
                Log.d(TAG, "Переход в Профиль");
                startActivity(new Intent(HomeActivity.this, ProfileActivity.class));
                return true;
            }
            return false;
        });

        fabCart.setOnClickListener(v -> {
            Log.d(TAG, "Переход в Корзину");
            startActivity(new Intent(HomeActivity.this, CartActivity.class));
        });

        bottomNavigationView.setSelectedItemId(R.id.navigation_home);

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
        setupCategoryRecyclerView();
        setupProductRecyclerViews();

        menuButton.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        cartButton.setOnClickListener(v -> startActivity(new Intent(this, CartActivity.class)));
        EditText searchEditText = findViewById(R.id.editexttext2);
        searchEditText.setOnClickListener(v -> startActivity(new Intent(this, SearchActivity.class)));

        popularSeeAll.setOnClickListener(v -> startActivity(new Intent(this, PopularActivity.class)));
        specialSeeAll.setOnClickListener(v -> startActivity(new Intent(this, PromotionActivity.class)));

        cartUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "Получено событие обновления корзины");
                updateCartIcon();
            }
        };
        broadcast.registerReceiver(cartUpdateReceiver, new IntentFilter(ACTION_CART_UPDATED));

        notificationsUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "Получено событие обновления уведомлений");
                updateNotificationIcon();
            }
        };
        broadcast.registerReceiver(notificationsUpdateReceiver, new IntentFilter(ACTION_NOTIFICATIONS_UPDATED));

        updateCartIcon();
        updateNotificationIcon();
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
            startActivity(new Intent(this, OrdersActivity.class));
        });
        navNotifications.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            startActivity(new Intent(this, NotificationsActivity.class));
        });
        navSettings.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            Toast.makeText(this, "Настройки (в разработке)", Toast.LENGTH_SHORT).show();
        });
        navSupport.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            startActivity(new Intent(this, SupportChatActivity.class));
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
                Log.d(TAG, "Profile response: " + response);
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
                            HomeProductAdapter popularAdapter = (HomeProductAdapter) popularRecyclerView.getAdapter();
                            if (popularAdapter != null) {
                                popularAdapter.setUserId(profileId);
                                popularAdapter.loadFavoriteStatus();
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
                    Log.d(TAG, "Categories response: " + responseBody);
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

    private void onCategoryClick(String categoryName) {
        Intent intent = new Intent(this, CategoryActivity.class);
        intent.putExtra("category", categoryName);
        startActivity(intent);
    }

    private void setupProductRecyclerViews() {
        executor.execute(() -> {
            try {
                String token = localStorage.getToken();
                if (token == null) {
                    mainHandler.post(() -> showErrorDialog("Ошибка: токен отсутствует", null));
                    return;
                }
                Request productRequest = new Request.Builder()
                        .url(BuildConfig.SUPABASE_URL + "/rest/v1/products?select=id,name,price,image_url,category_id,image_url_for_details,image_url_for_details_2,image_url_for_details_3,image_url_for_details_4,image_url_for_details_5,text_for_details")
                        .get()
                        .addHeader("apikey", BuildConfig.SUPABASE_API_KEY)
                        .addHeader("Authorization", "Bearer " + token)
                        .addHeader("Content-Type", "application/json")
                        .build();
                List<Product> products = new ArrayList<>();
                try (Response response = new OkHttpClient().newCall(productRequest).execute()) {
                    if (!response.isSuccessful()) {
                        throw new IOException("Ошибка загрузки продуктов: " + response.message());
                    }
                    String responseBody = response.body().string();
                    Log.d(TAG, "Products response: " + responseBody);
                    JSONArray productsArray = new JSONArray(responseBody);
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
                }

                Request promotionRequest = new Request.Builder()
                        .url(BuildConfig.SUPABASE_URL + "/rest/v1/promotions?select=id,image_url")
                        .get()
                        .addHeader("apikey", BuildConfig.SUPABASE_API_KEY)
                        .addHeader("Authorization", "Bearer " + token)
                        .addHeader("Content-Type", "application/json")
                        .build();
                List<Promotion> promotions = new ArrayList<>();
                try (Response response = new OkHttpClient().newCall(promotionRequest).execute()) {
                    if (!response.isSuccessful()) {
                        throw new IOException("Ошибка загрузки акций: " + response.message());
                    }
                    String responseBody = response.body().string();
                    Log.d(TAG, "Promotions response: " + responseBody);
                    JSONArray promotionsArray = new JSONArray(responseBody);
                    for (int i = 0; i < promotionsArray.length(); i++) {
                        JSONObject promotion = promotionsArray.getJSONObject(i);
                        String imageUrl = promotion.optString("image_url", "");
                        if (!imageUrl.isEmpty()) {
                            imageUrl = BuildConfig.SUPABASE_URL + "/storage/v1/object/public/orders/" + imageUrl;
                        }
                        promotions.add(new Promotion(
                                promotion.getString("id"),
                                imageUrl
                        ));
                    }
                }

                List<Product> popularProducts = new ArrayList<>(products);
                List<Promotion> randomPromotion = getRandomPromotions(promotions, 1);

                mainHandler.post(() -> {
                    HomeProductAdapter popularAdapter = (HomeProductAdapter) popularRecyclerView.getAdapter();
                    if (popularAdapter != null) {
                        popularAdapter.updateProducts(popularProducts);
                        popularAdapter.setUserId(profileId);
                        popularAdapter.loadFavoriteStatus();
                    }
                    PromotionAdapter specialAdapter = new PromotionAdapter(randomPromotion);
                    specialRecyclerView.setAdapter(specialAdapter);
                });
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Product/Promotion load error: " + e.getMessage());
                mainHandler.post(() -> showErrorDialog("Ошибка загрузки данных: " + e.getMessage(), null));
            }
        });
    }

    private List<Promotion> getRandomPromotions(List<Promotion> promotions, int count) {
        List<Promotion> shuffled = new ArrayList<>(promotions);
        Collections.shuffle(shuffled);
        return shuffled.subList(0, Math.min(count, shuffled.size()));
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
                            broadcast.sendBroadcast(new Intent(HomeProductAdapter.ACTION_FAVORITE_UPDATED));
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
                            broadcast.sendBroadcast(new Intent(HomeProductAdapter.ACTION_FAVORITE_UPDATED));
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
                        broadcast.sendBroadcast(new Intent(ACTION_CART_UPDATED));
                    });
                }
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Add to cart error: " + e.getMessage());
                mainHandler.post(() -> showErrorDialog("Ошибка добавления в корзину: " + e.getMessage(), null));
            }
        });
    }

    public void createOrderNotification(String orderId) {
        executor.execute(() -> {
            try {
                String token = localStorage.getToken();
                if (token == null) {
                    mainHandler.post(() -> showErrorDialog("Ошибка: токен отсутствует", null));
                    return;
                }
                JSONObject json = new JSONObject();
                json.put("profile_id", profileId);
                json.put("message", "Ваш заказ №" + orderId + " в обработке");
                json.put("is_read", false);
                RequestBody body = RequestBody.create(json.toString(), JSON);
                Request request = new Request.Builder()
                        .url(BuildConfig.SUPABASE_URL + "/rest/v1/notifications")
                        .post(body)
                        .addHeader("apikey", BuildConfig.SUPABASE_API_KEY)
                        .addHeader("Authorization", "Bearer " + token)
                        .addHeader("Content-Type", "application/json")
                        .build();
                try (Response response = new OkHttpClient().newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        throw new IOException("Ошибка создания уведомления: " + response.message());
                    }
                    Log.d(TAG, "Уведомление успешно создано для заказа " + orderId);
                    mainHandler.post(() -> {
                        broadcast.sendBroadcast(new Intent(ACTION_NOTIFICATIONS_UPDATED));
                        Log.d(TAG, "Отправлен broadcast ACTION_NOTIFICATIONS_UPDATED");
                        mainHandler.postDelayed(this::updateNotificationIcon, 500);
                    });
                }
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Ошибка создания уведомления: " + e.getMessage());
                mainHandler.post(() -> Log.e(TAG, "Не удалось создать уведомление для заказа " + orderId));
            }
        });
    }

    private void updateNotificationIcon() {
        if (profileId == null) {
            Log.w(TAG, "profileId is null, skipping notification icon update");
            View drawerView = findViewById(R.id.drawer_menu);
            if (drawerView != null) {
                ImageView notificationIcon = drawerView.findViewById(R.id.nav_notifications)
                        .findViewById(R.id.notification_icon);
                if (notificationIcon != null) {
                    notificationIcon.setImageResource(R.drawable.dot_not_no);
                    Log.d(TAG, "Установлена иконка dot_not_no (profileId null)");
                } else {
                    Log.e(TAG, "notificationIcon is null (profileId null)");
                }
            } else {
                Log.e(TAG, "drawerView is null (profileId null)");
            }
            return;
        }
        executor.execute(() -> {
            try {
                String token = localStorage.getToken();
                if (token == null) {
                    mainHandler.post(() -> {
                        Log.w(TAG, "Token is null, setting default notification icon");
                        View drawerView = findViewById(R.id.drawer_menu);
                        if (drawerView != null) {
                            ImageView notificationIcon = drawerView.findViewById(R.id.nav_notifications)
                                    .findViewById(R.id.notification_icon);
                            if (notificationIcon != null) {
                                notificationIcon.setImageResource(R.drawable.dot_not_no);
                                Log.d(TAG, "Установлена иконка dot_not_no (token null)");
                            } else {
                                Log.e(TAG, "notificationIcon is null (token null)");
                            }
                        } else {
                            Log.e(TAG, "drawerView is null (token null)");
                        }
                    });
                    return;
                }
                String url = BuildConfig.SUPABASE_URL + "/rest/v1/notifications?profile_id=eq." + profileId + "&is_read=eq.false&select=id";
                Log.d(TAG, "Запрос уведомлений: " + url);
                Request request = new Request.Builder()
                        .url(url)
                        .get()
                        .addHeader("apikey", BuildConfig.SUPABASE_API_KEY)
                        .addHeader("Authorization", "Bearer " + token)
                        .build();
                try (Response response = new OkHttpClient().newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        throw new IOException("Ошибка проверки уведомлений: " + response.message());
                    }
                    String responseBody = response.body().string();
                    Log.d(TAG, "Ответ Supabase (уведомления): " + responseBody);
                    JSONArray notificationsArray = new JSONArray(responseBody);
                    mainHandler.post(() -> {
                        View drawerView = findViewById(R.id.drawer_menu);
                        if (drawerView == null) {
                            Log.e(TAG, "drawerView is null");
                            return;
                        }
                        ImageView notificationIcon = drawerView.findViewById(R.id.nav_notifications)
                                .findViewById(R.id.notification_icon);
                        if (notificationIcon == null) {
                            Log.e(TAG, "notificationIcon not found");
                            return;
                        }
                        boolean hasUnread = notificationsArray.length() > 0;
                        notificationIcon.setImageResource(hasUnread ? R.drawable.dot_notif : R.drawable.dot_not_no);
                        Log.d(TAG, "Обновлена иконка: " + (hasUnread ? "dot_notif" : "dot_not_no") + ", непрочитанных: " + notificationsArray.length());
                    });
                }
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Ошибка обновления иконки уведомлений: " + e.getMessage());
                mainHandler.post(() -> {
                    View drawerView = findViewById(R.id.drawer_menu);
                    if (drawerView != null) {
                        ImageView notificationIcon = drawerView.findViewById(R.id.nav_notifications)
                                .findViewById(R.id.notification_icon);
                        if (notificationIcon != null) {
                            notificationIcon.setImageResource(R.drawable.dot_not_no);
                            Log.d(TAG, "Установлена иконка dot_not_no (ошибка)");
                        } else {
                            Log.e(TAG, "notificationIcon is null (ошибка)");
                        }
                    } else {
                        Log.e(TAG, "drawerView is null (ошибка)");
                    }
                });
            }
        });
    }

    private void updateCartIcon() {
        if (profileId == null) {
            cartButton.setImageResource(R.drawable.for_cart_not_dot);
            return;
        }
        executor.execute(() -> {
            try {
                String token = localStorage.getToken();
                if (token == null) {
                    mainHandler.post(() -> cartButton.setImageResource(R.drawable.for_cart_not_dot));
                    return;
                }
                String url = BuildConfig.SUPABASE_URL + "/rest/v1/cart?profile_id=eq." + profileId + "&select=id";
                Request request = new Request.Builder()
                        .url(url)
                        .get()
                        .addHeader("apikey", BuildConfig.SUPABASE_API_KEY)
                        .addHeader("Authorization", "Bearer " + token)
                        .addHeader("Content-Type", "application/json")
                        .build();
                try (Response response = new OkHttpClient().newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        throw new IOException("Ошибка проверки корзины: " + response.message());
                    }
                    String responseBody = response.body().string();
                    JSONArray cartItems = new JSONArray(responseBody);
                    mainHandler.post(() -> {
                        cartButton.setImageResource(cartItems.length() > 0 ? R.drawable.for_cart_dot : R.drawable.for_cart_not_dot);
                    });
                }
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Cart icon update error: " + e.getMessage());
                mainHandler.post(() -> cartButton.setImageResource(R.drawable.for_cart_not_dot));
            }
        });
    }

    private void showErrorDialog(String message, Runnable onDismiss) {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Ошибка")
                .setMessage(message)
                .setPositiveButton("ОК", (d, which) -> {
                    d.dismiss();
                    if (onDismiss != null) onDismiss.run();
                })
                .setCancelable(false)
                .create();
        dialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
        broadcast.unregisterReceiver(cartUpdateReceiver);
        broadcast.unregisterReceiver(notificationsUpdateReceiver);
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