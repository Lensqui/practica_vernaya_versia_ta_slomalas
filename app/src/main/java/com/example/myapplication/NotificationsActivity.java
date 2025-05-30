package com.example.myapplication;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

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

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class NotificationsActivity extends AppCompatActivity {
    private static final String TAG = "NotificationsActivity";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private RecyclerView recyclerView;
    private NotificationsAdapter adapter;
    private List<Notification> notifications;
    private LocalStorage localStorage;
    private ExecutorService executor;
    private Handler mainHandler;
    private String profileId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.notification_activity);

        localStorage = LocalStorage.getInstance();
        localStorage.initialize(this);

        recyclerView = findViewById(R.id.notifications_recycler_view);
        notifications = new ArrayList<>();
        adapter = new NotificationsAdapter(notifications, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        executor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        profileId = localStorage.getProfileId();
        if (profileId == null) {
            showErrorDialog("Ошибка: пользователь не авторизован", () -> finish());
            return;
        }

        loadNotifications();
        markNotificationsAsRead();

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
                startActivity(new Intent(NotificationsActivity.this, HomeActivity.class));
                finish();
                return true;
            } else if (id == R.id.navigation_favorites) {
                Log.d(TAG, "Переход в Избранное");
                startActivity(new Intent(NotificationsActivity.this, FavoritesActivity.class));
                finish();
                return true;
            } else if (id == R.id.navigation_notifications) {
                Log.d(TAG, "Уже в Уведомлениях");
                return true;
            } else if (id == R.id.navigation_profile) {
                Log.d(TAG, "Переход в Профиль");
                startActivity(new Intent(NotificationsActivity.this, ProfileActivity.class));
                finish();
                return true;
            }
            return false;
        });

        fabCart.setOnClickListener(v -> {
            Log.d(TAG, "Переход в Корзину");
            startActivity(new Intent(NotificationsActivity.this, CartActivity.class));
        });

        bottomNavigationView.setSelectedItemId(R.id.navigation_notifications);

    }

    private void loadNotifications() {
        executor.execute(() -> {
            try {
                String token = localStorage.getToken();
                if (token == null) {
                    mainHandler.post(() -> showErrorDialog("Ошибка: токен отсутствует", null));
                    return;
                }
                String url = BuildConfig.SUPABASE_URL + "/rest/v1/notifications?profile_id=eq." + profileId + "&select=*&order=created_at.desc";
                Request request = new Request.Builder()
                        .url(url)
                        .get()
                        .addHeader("apikey", BuildConfig.SUPABASE_API_KEY)
                        .addHeader("Authorization", "Bearer " + token)
                        .build();
                try (Response response = new OkHttpClient().newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        throw new IOException("Ошибка загрузки уведомлений: " + response.message());
                    }
                    String responseBody = response.body().string();
                    Log.d(TAG, "Notifications response: " + responseBody);
                    JSONArray notificationsArray = new JSONArray(responseBody);
                    List<Notification> loadedNotifications = new ArrayList<>();
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                    for (int i = 0; i < notificationsArray.length(); i++) {
                        JSONObject obj = notificationsArray.getJSONObject(i);
                        Date createdAt = sdf.parse(obj.getString("created_at"));
                        loadedNotifications.add(new Notification(
                                obj.getString("id"),
                                obj.getString("profile_id"),
                                obj.getString("message"),
                                obj.getBoolean("is_read"),
                                createdAt
                        ));
                    }
                    mainHandler.post(() -> {
                        notifications.clear();
                        notifications.addAll(loadedNotifications);
                        adapter.notifyDataSetChanged();
                    });
                }
            } catch (IOException | JSONException | ParseException e) {
                Log.e(TAG, "Ошибка загрузки уведомлений: " + e.getMessage());
                mainHandler.post(() -> showErrorDialog("Ошибка: " + e.getMessage(), null));
            }
        });
    }

    private void markNotificationsAsRead() {
        executor.execute(() -> {
            try {
                String token = localStorage.getToken();
                if (token == null) {
                    mainHandler.post(() -> showErrorDialog("Ошибка: токен отсутствует", null));
                    return;
                }
                JSONObject json = new JSONObject();
                json.put("is_read", true);
                RequestBody body = RequestBody.create(json.toString(), JSON);
                String url = BuildConfig.SUPABASE_URL + "/rest/v1/notifications?profile_id=eq." + profileId + "&is_read=eq.false";
                Request request = new Request.Builder()
                        .url(url)
                        .patch(body)
                        .addHeader("apikey", BuildConfig.SUPABASE_API_KEY)
                        .addHeader("Authorization", "Bearer " + token)
                        .addHeader("Content-Type", "application/json")
                        .build();
                try (Response response = new OkHttpClient().newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        throw new IOException("Ошибка обновления уведомлений: " + response.message());
                    }
                    mainHandler.post(() -> {
                        for (Notification notification : notifications) {
                            notification.setRead(true);
                        }
                        adapter.notifyDataSetChanged();
                        LocalBroadcastManager.getInstance(this)
                                .sendBroadcast(new Intent(HomeActivity.ACTION_NOTIFICATIONS_UPDATED));
                    });
                }
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Ошибка пометки уведомлений: " + e.getMessage());
                mainHandler.post(() -> showErrorDialog("Ошибка: " + e.getMessage(), null));
            }
        });
    }

    private void showErrorDialog(String message, Runnable onDismiss) {
        new AlertDialog.Builder(this)
                .setTitle("Ошибка")
                .setMessage(message)
                .setPositiveButton("ОК", (d, which) -> {
                    d.dismiss();
                    if (onDismiss != null) {
                        onDismiss.run();
                    }
                })
                .setCancelable(false)
                .create()
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
        mainHandler.removeCallbacksAndMessages(null);
    }
}
