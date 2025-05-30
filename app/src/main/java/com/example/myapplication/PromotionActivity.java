package com.example.myapplication;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
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

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class PromotionActivity extends AppCompatActivity {
    private static final String TAG = "PromotionActivity";
    private RecyclerView recyclerView;
    private LocalStorage localStorage;
    private ExecutorService executor;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.promotion_activity);

        localStorage = LocalStorage.getInstance();
        localStorage.initialize(this);

        recyclerView = findViewById(R.id.popularity);
        ImageView backButton = findViewById(R.id.btnBack);

        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        recyclerView.setAdapter(new PromotionAdapter(new ArrayList<>()));

        executor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        backButton.setOnClickListener(v -> finish());

        loadPromotions();
    }

    private void loadPromotions() {
        executor.execute(() -> {
            try {
                String token = localStorage.getToken();
                if (token == null) {
                    mainHandler.post(() -> showErrorDialog("Ошибка: токен отсутствует", () -> finish()));
                    return;
                }
                Request request = new Request.Builder()
                        .url(BuildConfig.SUPABASE_URL + "/rest/v1/promotions?select=id,image_url")
                        .get()
                        .addHeader("apikey", BuildConfig.SUPABASE_API_KEY)
                        .addHeader("Authorization", "Bearer " + token)
                        .addHeader("Content-Type", "application/json")
                        .build();
                try (Response response = new OkHttpClient().newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        throw new IOException("Ошибка загрузки акций: " + response.message());
                    }
                    String responseBody = response.body().string();
                    Log.d(TAG, "Promotions response: " + responseBody);
                    JSONArray promotionsArray = new JSONArray(responseBody);
                    List<Promotion> promotions = new ArrayList<>();
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
                    mainHandler.post(() -> {
                        PromotionAdapter adapter = new PromotionAdapter(promotions);
                        recyclerView.setAdapter(adapter);
                    });
                }
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Promotion load error: " + e.getMessage());
                mainHandler.post(() -> showErrorDialog("Ошибка загрузки акций: " + e.getMessage(), null));
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
