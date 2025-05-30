package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ProductDetailActivity extends AppCompatActivity {
    private static final String TAG = "ProductDetailActivity";
    private ExecutorService executor;
    private Handler mainHandler;
    private String profileId;
    private LocalStorage localStorage;
    private ImageView mainProductImage;
    private ImageView thumbnail1, thumbnail2, thumbnail3;
    private TextView productTitle, productPrice, productDescription, readMore;
    private ImageButton backButton, cartButton, btnAddFavorites;
    private androidx.appcompat.widget.AppCompatButton addToCartButton;
    private String currentMainImageUrl = "";
    private String[] detailImageUrls = new String[3];
    private Product currentProduct;

    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        localStorage = LocalStorage.getInstance();
        localStorage.initialize(this);
        executor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
        profileId = localStorage.getProfileId();

        mainProductImage = findViewById(R.id.main_product_image);
        thumbnail1 = findViewById(R.id.thumbnail_1);
        thumbnail2 = findViewById(R.id.thumbnail_2);
        thumbnail3 = findViewById(R.id.thumbnail_3);
        productTitle = findViewById(R.id.product_title);
        productPrice = findViewById(R.id.product_price);
        productDescription = findViewById(R.id.product_description);
        readMore = findViewById(R.id.read_more);
        backButton = findViewById(R.id.back_button);
        cartButton = findViewById(R.id.cart);
        btnAddFavorites = findViewById(R.id.btnAddFavorites);
        addToCartButton = findViewById(R.id.add_to_cart_button);

        String productId = getIntent().getStringExtra("product_id");
        if (productId != null) {
            loadProductDetails(productId);
        } else {
            Toast.makeText(this, "Ошибка: ID товара не найден", Toast.LENGTH_SHORT).show();
            finish();
        }

        backButton.setOnClickListener(v -> finish());
        cartButton.setOnClickListener(v -> startActivity(new Intent(this, CartActivity.class)));
        thumbnail1.setOnClickListener(v -> swapMainImage(0));
        thumbnail2.setOnClickListener(v -> swapMainImage(1));
        thumbnail3.setOnClickListener(v -> swapMainImage(2));
        readMore.setOnClickListener(v -> toggleDescription());
        btnAddFavorites.setOnClickListener(v -> toggleFavorite());
        addToCartButton.setOnClickListener(v -> addToCart());
    }

    private void loadProductDetails(String productId) {
        executor.execute(() -> {
            try {
                String token = localStorage.getToken();
                if (token == null) {
                    mainHandler.post(() -> showErrorDialog("Ошибка: токен отсутствует", this::finish));
                    return;
                }
                String url = BuildConfig.SUPABASE_URL + "/rest/v1/products?id=eq." + productId + "&select=*";
                Request request = new Request.Builder()
                        .url(url)
                        .get()
                        .addHeader("apikey", BuildConfig.SUPABASE_API_KEY)
                        .addHeader("Authorization", "Bearer " + token)
                        .addHeader("Content-Type", "application/json")
                        .build();
                try (Response response = new OkHttpClient().newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        throw new IOException("Ошибка загрузки данных товара: " + response.message());
                    }
                    String responseBody = response.body().string();
                    Log.d(TAG, "Product details response: " + responseBody);
                    JSONArray productsArray = new JSONArray(responseBody);
                    if (productsArray.length() > 0) {
                        JSONObject product = productsArray.getJSONObject(0);
                        final String name = product.getString("name");
                        final String price = "₽" + String.format("%.2f", product.getDouble("price"));
                        final String category = product.optString("category_id", "");
                        final String imageUrl = product.optString("image_url", "");
                        final String imageUrlForDetails = product.optString("image_url_for_details", "");
                        final String imageUrlForDetails2 = product.optString("image_url_for_details_2", "");
                        final String imageUrlForDetails3 = product.optString("image_url_for_details_3", "");
                        final String imageUrlForDetails4 = product.optString("image_url_for_details_4", "");
                        final String imageUrlForDetails5 = product.optString("image_url_for_details_5", "");
                        final String textForDetails = product.optString("text_for_details", "");

                        currentProduct = new Product(
                                productId, name, price, category, imageUrl,
                                imageUrlForDetails, imageUrlForDetails2, imageUrlForDetails3,
                                imageUrlForDetails4, imageUrlForDetails5, textForDetails
                        );

                        detailImageUrls[0] = imageUrlForDetails;
                        detailImageUrls[1] = imageUrlForDetails2;
                        detailImageUrls[2] = imageUrlForDetails3;

                        mainHandler.post(() -> {
                            productTitle.setText(name);
                            productPrice.setText(price);
                            productDescription.setText(textForDetails);
                            if (!imageUrl.isEmpty()) {
                                currentMainImageUrl = imageUrl;
                                Glide.with(this)
                                        .load(BuildConfig.SUPABASE_URL + "/storage/v1/object/public/orders/" + imageUrl)
                                        .placeholder(R.drawable.img123)
                                        .error(R.drawable.img123)
                                        .into(mainProductImage);
                            }

                            if (!imageUrlForDetails.isEmpty()) {
                                thumbnail1.setVisibility(View.VISIBLE);
                                Glide.with(this)
                                        .load(BuildConfig.SUPABASE_URL + "/storage/v1/object/public/orders/" + imageUrlForDetails)
                                        .placeholder(R.drawable.img123)
                                        .error(R.drawable.img123)
                                        .into(thumbnail1);
                            }
                            if (!imageUrlForDetails2.isEmpty()) {
                                thumbnail2.setVisibility(View.VISIBLE);
                                Glide.with(this)
                                        .load(BuildConfig.SUPABASE_URL + "/storage/v1/object/public/orders/" + imageUrlForDetails2)
                                        .placeholder(R.drawable.img123)
                                        .error(R.drawable.img123)
                                        .into(thumbnail2);
                            }
                            if (!imageUrlForDetails3.isEmpty()) {
                                thumbnail3.setVisibility(View.VISIBLE);
                                Glide.with(this)
                                        .load(BuildConfig.SUPABASE_URL + "/storage/v1/object/public/orders/" + imageUrlForDetails3)
                                        .placeholder(R.drawable.img123)
                                        .error(R.drawable.img123)
                                        .into(thumbnail3);
                            }

                            updateDescriptionVisibility(textForDetails);
                        });
                    }
                }
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Error loading product details: " + e.getMessage());
                mainHandler.post(() -> showErrorDialog("Ошибка загрузки данных: " + e.getMessage(), this::finish));
            }
        });
    }

    private void updateDescriptionVisibility(String description) {
        if (description == null || description.isEmpty()) {
            productDescription.setVisibility(View.GONE);
            readMore.setVisibility(View.GONE);
        } else if (description.length() > 200) {
            productDescription.setMaxLines(3);
            productDescription.setEllipsize(android.text.TextUtils.TruncateAt.END);
            readMore.setVisibility(View.VISIBLE);
        } else {
            productDescription.setMaxLines(Integer.MAX_VALUE);
            readMore.setVisibility(View.GONE);
        }
    }

    private void toggleDescription() {
        if (productDescription.getMaxLines() == 3) {
            productDescription.setMaxLines(Integer.MAX_VALUE);
            readMore.setText("Свернуть");
        } else {
            productDescription.setMaxLines(3);
            productDescription.setEllipsize(android.text.TextUtils.TruncateAt.END);
            readMore.setText("Подробно");
        }
    }

    private void swapMainImage(int thumbnailIndex) {
        String selectedImageUrl = detailImageUrls[thumbnailIndex];
        if (selectedImageUrl == null || selectedImageUrl.isEmpty()) return;

        String temp = currentMainImageUrl;
        currentMainImageUrl = selectedImageUrl;
        detailImageUrls[thumbnailIndex] = temp;

        Glide.with(this)
                .load(BuildConfig.SUPABASE_URL + "/storage/v1/object/public/orders/" + currentMainImageUrl)
                .placeholder(R.drawable.img123)
                .error(R.drawable.img123)
                .into(mainProductImage);

        ImageView thumbnail;
        switch (thumbnailIndex) {
            case 0:
                thumbnail = thumbnail1;
                break;
            case 1:
                thumbnail = thumbnail2;
                break;
            case 2:
                thumbnail = thumbnail3;
                break;
            default:
                return;
        }
        Glide.with(this)
                .load(BuildConfig.SUPABASE_URL + "/storage/v1/object/public/orders/" + temp)
                .placeholder(R.drawable.img123)
                .error(R.drawable.img123)
                .into(thumbnail);
    }

    private void toggleFavorite() {
        if (profileId == null) {
            showErrorDialog("Ошибка: пользователь не авторизован", null);
            return;
        }
        boolean isFavorite = btnAddFavorites.getBackground().getConstantState() != getResources().getDrawable(R.drawable.for_cartinok).getConstantState();
        executor.execute(() -> {
            try {
                String token = localStorage.getToken();
                if (token == null) {
                    mainHandler.post(() -> showErrorDialog("Ошибка: токен отсутствует", null));
                    return;
                }
                if (isFavorite) {
                    String url = BuildConfig.SUPABASE_URL + "/rest/v1/favorites?profile_id=eq." + profileId + "&product_id=eq." + currentProduct.getId();
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
                            btnAddFavorites.setBackgroundResource(R.drawable.for_cartinok);
                            Toast.makeText(this, "Удалено из избранного", Toast.LENGTH_SHORT).show();
                        });
                    }
                } else {
                    JSONObject json = new JSONObject();
                    json.put("profile_id", profileId);
                    json.put("product_id", currentProduct.getId());
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
                            btnAddFavorites.setBackgroundResource(R.drawable.for_cartionok_add);
                            Toast.makeText(this, "Добавлено в избранное", Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Favorite error: " + e.getMessage());
                mainHandler.post(() -> showErrorDialog("Ошибка: " + e.getMessage(), null));
            }
        });
    }

    private void addToCart() {
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
                String checkUrl = BuildConfig.SUPABASE_URL + "/rest/v1/cart?profile_id=eq." + profileId + "&product_id=eq." + currentProduct.getId() + "&select=quantity";
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
                        String updateUrl = BuildConfig.SUPABASE_URL + "/rest/v1/cart?profile_id=eq." + profileId + "&product_id=eq." + currentProduct.getId();
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
                        json.put("product_id", currentProduct.getId());
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
                        cartButton.setImageResource(R.drawable.for_cart_dot);
                        Toast.makeText(this, "Добавлено в корзину", Toast.LENGTH_SHORT).show();
                    });
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
                .setPositiveButton("ОК", (d, which) -> {
                    d.dismiss();
                    if (onDismiss != null) onDismiss.run();
                })
                .setCancelable(false)
                .create()
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
