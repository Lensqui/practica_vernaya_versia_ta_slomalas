package com.example.myapplication;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

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

public class OrdersWithMapActivity extends AppCompatActivity {
    private static final String TAG = "OrdersWithMapActivity";
    private static final double DELIVERY_FEE = 60.20;
    private static final MediaType JSON = MediaType.parse("application/json; charset=UTF-8");

    private TextView emailText, phoneText, addressText, paymentText, subtotalText, deliveryText, totalText;
    private ImageView btnBack, editEmail, editPhone, changeAddress, paymentDropdown;
    private MaterialButton checkoutButton;
    private MapView mapView;
    private LocalStorage localStorage;
    private ExecutorService executorService;
    private Handler mainHandler;
    private String currentEmail;
    private String profileId;
    private double subtotal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE));
        setContentView(R.layout.activity_orders_with_map);

        LocalStorage.initialize(this);
        localStorage = LocalStorage.getInstance();

        emailText = findViewById(R.id.email_text);
        phoneText = findViewById(R.id.phone_text);
        addressText = findViewById(R.id.address_text);
        paymentText = findViewById(R.id.payment_text);
        subtotalText = findViewById(R.id.subtotal_text);
        deliveryText = findViewById(R.id.delivery_text);
        totalText = findViewById(R.id.total_text);
        btnBack = findViewById(R.id.btnBack);
        editEmail = findViewById(R.id.edit_email);
        editPhone = findViewById(R.id.edit_phone);
        changeAddress = findViewById(R.id.change_address);
        paymentDropdown = findViewById(R.id.payment_dropdown);
        checkoutButton = findViewById(R.id.checkout_button);
        mapView = findViewById(R.id.map_view);

        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setBuiltInZoomControls(true);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(10);
        mapView.getController().setCenter(new GeoPoint(9.0765, 7.3986));

        subtotal = getIntent().getDoubleExtra("SUBTOTAL", 0.0);
        double total = subtotal + DELIVERY_FEE;

        subtotalText.setText(String.format("₽%.2f", subtotal));
        deliveryText.setText(String.format("₽%.2f", DELIVERY_FEE));
        totalText.setText(String.format("₽%.2f", total));

        currentEmail = localStorage.getEmail();
        profileId = localStorage.getProfileId();
        Log.d(TAG, "Email: " + currentEmail + ", ProfileId: " + profileId + ", Token: " + localStorage.getToken());
        if (currentEmail == null || profileId == null) {
            showErrorDialog("Ошибка: пользователь не авторизован", () -> {
                startActivity(new Intent(this, MainActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));
                finish();
            });
            return;
        }

        loadProfileData();

        btnBack.setOnClickListener(v -> onBackPressed());
        editEmail.setOnClickListener(v -> showEditEmailDialog());
        editPhone.setOnClickListener(v -> showEditPhoneDialog());
        changeAddress.setOnClickListener(v -> showEditAddressDialog());
        paymentDropdown.setOnClickListener(v -> {
            if (paymentText.getText().toString().equals("Не добавлена")) {
                AddCardDialogFragment dialog = new AddCardDialogFragment();
                dialog.setOnCardAddedListener(cardNumber -> {
                    paymentText.setText("**** **** **** " + cardNumber.substring(cardNumber.length() - 4));
                });
                dialog.show(getSupportFragmentManager(), "AddCardDialog");
            } else {
                Toast.makeText(this, "Изменение карты (в разработке)", Toast.LENGTH_SHORT).show();
            }
        });
        checkoutButton.setOnClickListener(v -> confirmOrder());
    }

    private void loadProfileData() {
        executorService.execute(() -> {
            try {
                String token = localStorage.getToken();
                if (token == null) {
                    mainHandler.post(() -> showErrorDialog("Ошибка: токен отсутствует", null));
                    return;
                }
                String response = SupabaseApi.getInstance().fetchProfile(currentEmail);
                Log.d(TAG, "Profile response: " + response);
                if (response != null && !response.equals("[]")) {
                    JSONArray profiles = new JSONArray(response);
                    if (profiles.length() > 0) {
                        JSONObject profile = profiles.getJSONObject(0);
                        String email = profile.optString("email", "");
                        String telephone = profile.optString("telephone_number", "");
                        String address = profile.optString("address_home", "");
                        String cardOplats = profile.optString("card_oplats", "");

                        mainHandler.post(() -> {
                            emailText.setText(email.isEmpty() ? "Не указан" : email);
                            phoneText.setText(telephone.isEmpty() ? "Не указан" : formatPhoneNumber(telephone));
                            addressText.setText(address.isEmpty() ? "Не указан" : address);
                            paymentText.setText(cardOplats.isEmpty() ? "Не добавлена" : "**** **** **** " + cardOplats.substring(cardOplats.length() - 4));
                            if (!address.isEmpty()) {
                                geocodeAddress(address);
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

    private void geocodeAddress(String address) {
        executorService.execute(() -> {
            try {
                String url = "https://nominatim.openstreetmap.org/search?q=" + address.replace(" ", "+") + "&format=json&limit=1";
                Request request = new Request.Builder()
                        .url(url)
                        .addHeader("User-Agent", "MyApp/1.0")
                        .build();
                try (Response response = new OkHttpClient().newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        throw new IOException("Ошибка геокодирования: " + response.message());
                    }
                    String responseBody = response.body().string();
                    JSONArray jsonArray = new JSONArray(responseBody);
                    if (jsonArray.length() > 0) {
                        JSONObject json = jsonArray.getJSONObject(0);
                        double lat = json.getDouble("lat");
                        double lng = json.getDouble("lon");
                        mainHandler.post(() -> {
                            GeoPoint point = new GeoPoint(lat, lng);
                            mapView.getOverlays().clear();
                            Marker marker = new Marker(mapView);
                            marker.setPosition(point);
                            marker.setTitle(address);
                            mapView.getOverlays().add(marker);
                            mapView.getController().setCenter(point);
                            mapView.getController().setZoom(15);
                            mapView.invalidate();
                        });
                    }
                }
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Geocode error: " + e.getMessage());
                mainHandler.post(() -> Toast.makeText(this, "Ошибка геокодирования адреса", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void showEditEmailDialog() {
        TextInputEditText input = new TextInputEditText(this);
        input.setText(emailText.getText().toString());
        input.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        new AlertDialog.Builder(this)
                .setTitle("Изменить email")
                .setView(input)
                .setPositiveButton("Сохранить", (dialog, which) -> {
                    String newEmail = input.getText().toString().trim();
                    if (Validator.isValidEmail(newEmail)) {
                        emailText.setText(newEmail);
                        updateEmailInProfile(newEmail);
                    } else {
                        Toast.makeText(this, "Неверный формат email (только .com или .ru)", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Отмена", (dialog, which) -> dialog.dismiss())
                .setCancelable(false)
                .show();
    }

    private void updateEmailInProfile(String email) {
        executorService.execute(() -> {
            try {
                String token = localStorage.getToken();
                if (token == null) {
                    mainHandler.post(() -> showErrorDialog("Ошибка: токен отсутствует", null));
                    return;
                }
                JSONObject json = new JSONObject();
                json.put("email", email);
                String response = SupabaseApi.getInstance().updateProfile(currentEmail, json.toString());
                Log.d(TAG, "Update email response: " + response);
                currentEmail = email;
                localStorage.setEmail(email);
                mainHandler.post(() -> Toast.makeText(this, "Email обновлён", Toast.LENGTH_SHORT).show());
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Update email error: " + e.getMessage());
                mainHandler.post(() -> showErrorDialog("Ошибка обновления email: " + e.getMessage(), null));
            }
        });
    }

    private void showEditPhoneDialog() {
        TextInputEditText input = new TextInputEditText(this);
        input.setText(phoneText.getText().toString().replaceAll("[^0-9]", ""));
        input.setInputType(InputType.TYPE_CLASS_PHONE);
        new AlertDialog.Builder(this)
                .setTitle("Изменить телефон")
                .setView(input)
                .setPositiveButton("Сохранить", (dialog, which) -> {
                    String newPhone = input.getText().toString().trim().replaceAll("[^0-9]", "");
                    if (newPhone.length() >= 10) {
                        String formattedPhone = formatPhoneNumber(newPhone);
                        phoneText.setText(formattedPhone);
                        updatePhoneInProfile(newPhone);
                    } else {
                        Toast.makeText(this, "Телефон должен содержать минимум 10 цифр", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Отмена", (dialog, which) -> dialog.dismiss())
                .setCancelable(false)
                .show();
    }

    private void updatePhoneInProfile(String telephone) {
        executorService.execute(() -> {
            try {
                String token = localStorage.getToken();
                if (token == null) {
                    mainHandler.post(() -> showErrorDialog("Ошибка: токен отсутствует", null));
                    return;
                }
                JSONObject json = new JSONObject();
                json.put("telephone_number", telephone);
                String response = SupabaseApi.getInstance().updateProfile(currentEmail, json.toString());
                Log.d(TAG, "Update phone response: " + response);
                mainHandler.post(() -> Toast.makeText(this, "Телефон обновлён", Toast.LENGTH_SHORT).show());
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Update phone error: " + e.getMessage());
                mainHandler.post(() -> showErrorDialog("Ошибка обновления телефона: " + e.getMessage(), null));
            }
        });
    }

    private void showEditAddressDialog() {
        TextInputEditText input = new TextInputEditText(this);
        input.setText(addressText.getText().toString());
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS);
        new AlertDialog.Builder(this)
                .setTitle("Изменить адрес")
                .setView(input)
                .setPositiveButton("Сохранить", (dialog, which) -> {
                    String newAddress = input.getText().toString().trim();
                    if (!newAddress.isEmpty()) {
                        addressText.setText(newAddress);
                        updateAddressInProfile(newAddress);
                        geocodeAddress(newAddress);
                    } else {
                        Toast.makeText(this, "Адрес не может быть пустым", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Отмена", (dialog, which) -> dialog.dismiss())
                .setCancelable(false)
                .show();
    }

    private void updateAddressInProfile(String address) {
        executorService.execute(() -> {
            try {
                String token = localStorage.getToken();
                if (token == null) {
                    mainHandler.post(() -> showErrorDialog("Ошибка: токен отсутствует", null));
                    return;
                }
                JSONObject json = new JSONObject();
                json.put("address_home", address);
                String response = SupabaseApi.getInstance().updateProfile(currentEmail, json.toString());
                Log.d(TAG, "Update address response: " + response);
                mainHandler.post(() -> Toast.makeText(this, "Адрес обновлён", Toast.LENGTH_SHORT).show());
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Update address error: " + e.getMessage());
                mainHandler.post(() -> showErrorDialog("Ошибка обновления адреса: " + e.getMessage(), null));
            }
        });
    }

    private void confirmOrder() {
        if (emailText.getText().toString().equals("Не указан")) {
            Toast.makeText(this, "Укажите email", Toast.LENGTH_SHORT).show();
            return;
        }
        if (phoneText.getText().toString().equals("Не указан")) {
            Toast.makeText(this, "Укажите телефон", Toast.LENGTH_SHORT).show();
            return;
        }
        if (addressText.getText().toString().equals("Не указан")) {
            Toast.makeText(this, "Укажите адрес", Toast.LENGTH_SHORT).show();
            return;
        }
        if (paymentText.getText().toString().equals("Не добавлена")) {
            Toast.makeText(this, "Добавьте способ оплаты", Toast.LENGTH_SHORT).show();
            return;
        }

        executorService.execute(() -> {
            try {
                String token = localStorage.getToken();
                if (token == null) {
                    mainHandler.post(() -> showErrorDialog("Ошибка: токен отсутствует", null));
                    return;
                }

                String cartResponse = SupabaseApi.getInstance().fetchCart(profileId);
                List<JSONObject> cartItems = new ArrayList<>();
                JSONArray cartArray = new JSONArray(cartResponse);
                for (int i = 0; i < cartArray.length(); i++) {
                    cartItems.add(cartArray.getJSONObject(i));
                }

                JSONObject orderJson = new JSONObject();
                orderJson.put("profile_id", profileId);
                orderJson.put("total_price", subtotal + DELIVERY_FEE);
                orderJson.put("status", "pending");
                orderJson.put("address", addressText.getText().toString());
                orderJson.put("payment_method", paymentText.getText().toString());
                RequestBody orderBody = RequestBody.create(JSON, orderJson.toString());
                String orderUrl = BuildConfig.SUPABASE_URL + "/rest/v1/orders";
                Request orderRequest = new Request.Builder()
                        .url(orderUrl)
                        .post(orderBody)
                        .addHeader("apikey", BuildConfig.SUPABASE_API_KEY)
                        .addHeader("Authorization", "Bearer " + token)
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Prefer", "return=representation")
                        .build();
                String orderId;
                try (Response orderResponse = new OkHttpClient().newCall(orderRequest).execute()) {
                    if (!orderResponse.isSuccessful()) {
                        String errorBody = orderResponse.body().string();
                        Log.e(TAG, "Order creation failed: " + orderResponse.code() + ", " + errorBody);
                        throw new IOException("Ошибка создания заказа: " + errorBody);
                    }
                    JSONArray orderArray = new JSONArray(orderResponse.body().string());
                    if (orderArray.length() == 0) {
                        throw new IOException("Заказ создан, но ID не возвращен");
                    }
                    orderId = orderArray.getJSONObject(0).getString("id");
                    Log.d(TAG, "Order created with ID: " + orderId);
                }

                SupabaseApi supabaseApi = SupabaseApi.getInstance();
                for (JSONObject item : cartItems) {
                    JSONObject product = item.getJSONObject("products");
                    supabaseApi.createOrderItem(
                            orderId,
                            item.getString("product_id"),
                            item.getInt("quantity"),
                            product.getDouble("price")
                    );
                }

                supabaseApi.clearCart(profileId);

                JSONObject notificationJson = new JSONObject();
                notificationJson.put("profile_id", profileId);
                notificationJson.put("message", "Ваш заказ №" + orderId + " в обработке");
                notificationJson.put("is_read", false);
                RequestBody notificationBody = RequestBody.create(notificationJson.toString(), JSON);
                String notificationUrl = BuildConfig.SUPABASE_URL + "/rest/v1/notifications";
                Request notificationRequest = new Request.Builder()
                        .url(notificationUrl)
                        .post(notificationBody)
                        .addHeader("apikey", BuildConfig.SUPABASE_API_KEY)
                        .addHeader("Authorization", "Bearer " + token)
                        .addHeader("Content-Type", "application/json")
                        .build();
                try (Response notificationResponse = new OkHttpClient().newCall(notificationRequest).execute()) {
                    if (!notificationResponse.isSuccessful()) {
                        Log.e(TAG, "Ошибка создания уведомления: " + notificationResponse.message());
                    } else {
                        Log.d(TAG, "Уведомление создано для заказа " + orderId);
                    }
                }

                mainHandler.post(() -> {
                    LocalBroadcastManager.getInstance(this)
                            .sendBroadcast(new Intent(HomeActivity.ACTION_NOTIFICATIONS_UPDATED));
                    Log.d(TAG, "Отправлен broadcast ACTION_NOTIFICATIONS_UPDATED");
                    showSuccessDialog(orderId);
                });

            } catch (IOException | JSONException e) {
                Log.e(TAG, "Order creation error: ", e);
                mainHandler.post(() -> showErrorDialog("Ошибка создания заказа: " + e.getMessage(), null));
            }
        });
    }

    private void showSuccessDialog(String orderId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_success, null);
        builder.setView(dialogView);
        builder.setCancelable(false);

        TextView title = dialogView.findViewById(R.id.title);
        TextView message = dialogView.findViewById(R.id.message);
        MaterialButton btnContinue = dialogView.findViewById(R.id.btn_continue);

        title.setText("Вы успешно");
        message.setText("оформили заказ");

        btnContinue.setOnClickListener(v -> {
            Intent intent = new Intent(this, OrderConfirmationActivity.class);
            intent.putExtra("ORDER_ID", orderId);
            intent.putExtra("TOTAL_PRICE", subtotal + DELIVERY_FEE);
            intent.putExtra("ADDRESS", addressText.getText().toString());
            intent.putExtra("PAYMENT_METHOD", paymentText.getText().toString());
            intent.putExtra("EMAIL", emailText.getText().toString());
            intent.putExtra("PHONE", phoneText.getText().toString());
            intent.putExtra("IS_NEW_ORDER", true);
            startActivity(intent);
            finish();
        });

        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();
    }

    private void clearCart(String token) throws IOException {
        String cartUrl = BuildConfig.SUPABASE_URL + "/rest/v1/cart?profile_id=eq." + profileId;
        Request cartRequest = new Request.Builder()
                .url(cartUrl)
                .delete()
                .addHeader("apikey", BuildConfig.SUPABASE_API_KEY)
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Content-Type", "application/json")
                .build();
        try (Response cartResponse = new OkHttpClient().newCall(cartRequest).execute()) {
            if (!cartResponse.isSuccessful()) {
                Log.e(TAG, "Ошибка очистки корзины: " + cartResponse.message());
            }
        }
    }

    private String formatPhoneNumber(String phone) {
        if (phone.length() < 10) return phone;
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.length() < 10) return phone;

        String countryCode = "+7";
        String areaCode = digits.substring(1, 4);
        String firstPart = digits.substring(4, 7);
        String secondPart = digits.substring(7, 9);
        String lastPart = digits.substring(9, 11);

        return countryCode + "-" + areaCode + "-" + firstPart + "-" + secondPart + "-" + lastPart;
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
    protected void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapView != null) {
            mapView.onPause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mapView != null) {
            mapView.onDetach();
        }
        executorService.shutdown();
    }
}