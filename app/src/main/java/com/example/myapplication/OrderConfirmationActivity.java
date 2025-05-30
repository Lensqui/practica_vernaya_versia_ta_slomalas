package com.example.myapplication;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

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

public class OrderConfirmationActivity extends AppCompatActivity {
    private static final String TAG = "OrderConfirmationActivity";
    private TextView orderNumberText, timeAgoText, emailText, phoneText, addressText, paymentText;
    private RecyclerView productsRecyclerView;
    private ImageView barcodeImage, btnBack, editEmail, editPhone, changeAddress, paymentDropdown;
    private MapView mapView;
    private ExecutorService executorService;
    private Handler mainHandler;
    private String orderId, address;
    private LocalStorage localStorage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE));
        setContentView(R.layout.activity_order_confirmation);

        localStorage = LocalStorage.getInstance();
        localStorage.initialize(this);

        orderNumberText = findViewById(R.id.order_number);
        timeAgoText = findViewById(R.id.time_ago);
        emailText = findViewById(R.id.email_text);
        phoneText = findViewById(R.id.phone_text);
        addressText = findViewById(R.id.address_text);
        paymentText = findViewById(R.id.payment_text);
        productsRecyclerView = findViewById(R.id.products_recycler_view);
        barcodeImage = findViewById(R.id.barcode_image);
        btnBack = findViewById(R.id.btnBack);
        editEmail = findViewById(R.id.edit_email);
        editPhone = findViewById(R.id.edit_phone);
        changeAddress = findViewById(R.id.change_address);
        paymentDropdown = findViewById(R.id.payment_dropdown);
        mapView = findViewById(R.id.map_view);

        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setBuiltInZoomControls(true);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(10);
        mapView.getController().setCenter(new GeoPoint(9.0765, 7.3986));

        orderId = getIntent().getStringExtra("ORDER_ID");
        double totalPrice = getIntent().getDoubleExtra("TOTAL_PRICE", 0.0);
        address = getIntent().getStringExtra("ADDRESS");
        String paymentMethod = getIntent().getStringExtra("PAYMENT_METHOD");
        String email = getIntent().getStringExtra("EMAIL");
        String phone = getIntent().getStringExtra("PHONE");

        Log.d(TAG, "Received orderId: " + orderId);
        Log.d(TAG, "SUPABASE_URL: " + BuildConfig.SUPABASE_URL);

        if (orderId != null && orderId.length() > 4) {
            orderNumberText.setText("№ " + orderId.substring(orderId.length() - 4));
        } else {
            orderNumberText.setText(orderId != null ? "№ " + orderId : "Не указан");
        }
        emailText.setText(email != null ? email : "Не указан");
        phoneText.setText(phone != null ? phone : "Не указан");
        addressText.setText(address != null ? address : "Не указан");
        paymentText.setText(paymentMethod != null ? paymentMethod : "Не указан");

        if (address != null && !address.isEmpty()) {
            geocodeAddress(address);
        }

        productsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        productsRecyclerView.setHasFixedSize(true);
        productsRecyclerView.setAdapter(new OrderItemAdapter(new ArrayList<>()));

        loadOrderDetails();

        generateBarcode(orderId);

        btnBack.setOnClickListener(v -> onBackPressed());
        editEmail.setOnClickListener(v -> showEditEmailDialog());
        editPhone.setOnClickListener(v -> showEditPhoneDialog());
        changeAddress.setOnClickListener(v -> showEditAddressDialog());
        paymentDropdown.setOnClickListener(v -> showChangePaymentDialog());
    }

    private void loadOrderDetails() {
        executorService.execute(() -> {
            try {
                String token = localStorage.getToken();
                if (token == null) {
                    mainHandler.post(() -> showErrorDialog("Ошибка: токен отсутствует", null));
                    return;
                }

                String orderUrl = BuildConfig.SUPABASE_URL + "/rest/v1/orders?id=eq." + orderId + "&select=created_at";
                String createdAtFormatted = null;
                try (Response orderResponse = new OkHttpClient().newCall(new Request.Builder()
                        .url(orderUrl)
                        .get()
                        .addHeader("apikey", BuildConfig.SUPABASE_API_KEY)
                        .addHeader("Authorization", "Bearer " + token)
                        .build()).execute()) {
                    if (!orderResponse.isSuccessful()) {
                        throw new IOException("Ошибка загрузки заказа: " + orderResponse.code() + ", " + orderResponse.body().string());
                    }
                    String responseBody = orderResponse.body().string();
                    Log.d(TAG, "Order response: " + responseBody);
                    JSONArray orderArray = new JSONArray(responseBody);
                    if (orderArray.length() > 0) {
                        String createdAtStr = orderArray.getJSONObject(0).optString("created_at", null);
                        Log.d(TAG, "created_at: " + createdAtStr);
                        if (createdAtStr != null) {
                            SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMMM yyyy, HH:mm", new Locale("ru"));
                            String[] possibleFormats = {
                                    "yyyy-MM-dd'T'HH:mm:ss.SSSSSS",
                                    "yyyy-MM-dd'T'HH:mm:ss.SSSSSSZ",
                                    "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
                                    "yyyy-MM-dd'T'HH:mm:ssZ"
                            };
                            Date createdAt = null;
                            for (String format : possibleFormats) {
                                try {
                                    SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());
                                    if (format.contains("Z") && createdAtStr.endsWith("Z")) {
                                        createdAtStr = createdAtStr.replace("Z", "+0000");
                                    }
                                    createdAt = sdf.parse(createdAtStr);
                                    createdAtFormatted = outputFormat.format(createdAt);
                                    break;
                                } catch (ParseException e) {
                                    Log.w(TAG, "Failed to parse date with format " + format + ": " + e.getMessage());
                                }
                            }
                            if (createdAt == null) {
                                Log.e(TAG, "Could not parse created_at: " + createdAtStr);
                            }
                        }
                    }
                }

                String itemsUrl = BuildConfig.SUPABASE_URL + "/rest/v1/order_items?order_id=eq." + orderId + "&select=id,quantity,products(name,price,image_url)";
                List<OrderItem> items = new ArrayList<>();
                try (Response itemsResponse = new OkHttpClient().newCall(new Request.Builder()
                        .url(itemsUrl)
                        .get()
                        .addHeader("apikey", BuildConfig.SUPABASE_API_KEY)
                        .addHeader("Authorization", "Bearer " + token)
                        .build()).execute()) {
                    if (!itemsResponse.isSuccessful()) {
                        throw new IOException("Ошибка загрузки товаров: " + itemsResponse.code() + ", " + itemsResponse.body().string());
                    }
                    String responseBody = itemsResponse.body().string();
                    Log.d(TAG, "Order items response: " + responseBody);
                    JSONArray itemsArray = new JSONArray(responseBody);
                    for (int i = 0; i < itemsArray.length(); i++) {
                        JSONObject item = itemsArray.getJSONObject(i);
                        JSONObject product = item.optJSONObject("products");
                        String imageUrl = product != null ? product.optString("image_url", "").replaceAll("^\\s+", "") : "";
                        Log.d(TAG, "Item image_url: " + imageUrl);
                        if (product != null) {
                            items.add(new OrderItem(
                                    item.optString("id", ""),
                                    product.optString("name", "Без названия"),
                                    product.optDouble("price", 0.0),
                                    item.optInt("quantity", 0),
                                    imageUrl,
                                    orderId
                            ));
                        } else {
                            Log.w(TAG, "Product is null for order item: " + item.toString());
                        }
                    }
                }

                final String finalCreatedAt = createdAtFormatted;
                mainHandler.post(() -> {
                    timeAgoText.setText(finalCreatedAt != null ? finalCreatedAt : "Неизвестно");
                    OrderItemAdapter adapter = new OrderItemAdapter(items);
                    productsRecyclerView.setAdapter(adapter);
                });

            } catch (IOException | JSONException e) {
                Log.e(TAG, "Order details load error: ", e);
                mainHandler.post(() -> showErrorDialog("Ошибка загрузки данных заказа: " + e.getMessage(), null));
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

    private void generateBarcode(String orderId) {
        try {
            if (orderId == null || orderId.isEmpty()) {
                throw new IllegalArgumentException("Order ID is empty");
            }
            MultiFormatWriter writer = new MultiFormatWriter();
            BitMatrix bitMatrix = writer.encode(orderId, BarcodeFormat.CODE_128, 400, 80);
            BarcodeEncoder encoder = new BarcodeEncoder();
            Bitmap bitmap = encoder.createBitmap(bitMatrix);
            mainHandler.post(() -> barcodeImage.setImageBitmap(bitmap));
        } catch (Exception e) {
            Log.e(TAG, "Barcode generation error: ", e);
            mainHandler.post(() -> Toast.makeText(this, "Ошибка генерации штрихкода", Toast.LENGTH_SHORT).show());
        }
    }

    private void showEditEmailDialog() {
    }

    private void showEditPhoneDialog() {
    }

    private void showEditAddressDialog() {
    }

    private void showChangePaymentDialog() {
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