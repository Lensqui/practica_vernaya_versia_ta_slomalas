package com.example.myapplication;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";

    private TextView profileFullname, firstNameText, lastNameText, telephoneText, addressText, cardOplatsText;
    private ImageView profileImage, firstNameCheck, lastNameCheck, telephoneCheck, addressCheck, cardCheck;
    private DrawerLayout drawerLayout;
    private LinearLayout navProfile, navCart, navFavorites, navOrders, navNotifications, navSettings, navSupport, navLogout;
    private TextView drawerUserFullname;
    private ImageView drawerUserAvatar;
    private ExecutorService executorService;
    private Handler mainHandler;
    private LocalStorage localStorage;
    private String currentEmail;
    private View profileContent;
    private View fragmentContainer;
    private ActivityResultLauncher<String> pickImageLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

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
                startActivity(new Intent(ProfileActivity.this, HomeActivity.class));
                finish();
                return true;
            } else if (id == R.id.navigation_favorites) {
                Log.d(TAG, "Переход в Избранное");
                startActivity(new Intent(ProfileActivity.this, FavoritesActivity.class));
                finish();
                return true;
            } else if (id == R.id.navigation_notifications) {
                Log.d(TAG, "Переход в Уведомления");
                startActivity(new Intent(ProfileActivity.this, NotificationsActivity.class));
                finish();
                return true;
            } else if (id == R.id.navigation_profile) {
                Log.d(TAG, "Уже в Профиле");
                return true;
            }
            return false;
        });

        fabCart.setOnClickListener(v -> {
            Log.d(TAG, "Переход в Корзину");
            startActivity(new Intent(ProfileActivity.this, CartActivity.class));
        });

        bottomNavigationView.setSelectedItemId(R.id.navigation_profile);


        LocalStorage.initialize(this);
        localStorage = LocalStorage.getInstance();

        profileFullname = findViewById(R.id.profile_fullname);
        firstNameText = findViewById(R.id.name_text);
        lastNameText = findViewById(R.id.surname_text);
        telephoneText = findViewById(R.id.phone_text);
        addressText = findViewById(R.id.address_text);
        cardOplatsText = findViewById(R.id.payment_text);
        profileImage = findViewById(R.id.profile_image);
        firstNameCheck = findViewById(R.id.name_check);
        lastNameCheck = findViewById(R.id.familia_check);
        telephoneCheck = findViewById(R.id.telephoneNumber_check);
        addressCheck = findViewById(R.id.adress_check);
        cardCheck = findViewById(R.id.card_check);
        drawerLayout = findViewById(R.id.drawer_layout);
        profileContent = findViewById(R.id.profile_content);
        fragmentContainer = findViewById(R.id.fragment_container);

        View drawerView = findViewById(R.id.drawer_menu);
        navProfile = drawerView.findViewById(R.id.nav_profile);
        navCart = drawerView.findViewById(R.id.nav_cart);
        navFavorites = drawerView.findViewById(R.id.nav_favorites);
        navOrders = drawerView.findViewById(R.id.nav_orders);
        navNotifications = drawerView.findViewById(R.id.nav_notifications);
        navSettings = drawerView.findViewById(R.id.nav_settings);
        navSupport = drawerView.findViewById(R.id.nav_support);
        navLogout = drawerView.findViewById(R.id.nav_logout);
        drawerUserFullname = drawerView.findViewById(R.id.user_fullname);
        drawerUserAvatar = drawerView.findViewById(R.id.user_avatar);

        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                uploadNewAvatar(uri);
            } else {
                Toast.makeText(this, "Изображение не выбрано", Toast.LENGTH_SHORT).show();
            }
        });

        currentEmail = localStorage.getEmail();
        if (currentEmail != null) {
            loadProfileData();
        } else {
            showErrorDialog("Ошибка: пользователь не авторизован");
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        }

        navProfile.setOnClickListener(v -> drawerLayout.closeDrawer(GravityCompat.START));
        navCart.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            Toast.makeText(this, "Корзина (в разработке)", Toast.LENGTH_SHORT).show();
        });
        navFavorites.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            Toast.makeText(this, "Избранное (в разработке)", Toast.LENGTH_SHORT).show();
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
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
    }

    public void onEditProfileImage(View view) {

        pickImageLauncher.launch("image/*");
    }

    public void onEditName(View view) {
        showEditDialog("Введите имя", firstNameText.getText().toString(), newValue -> {
            if (!newValue.isEmpty()) {
                firstNameText.setText(newValue);
                firstNameCheck.setVisibility(View.VISIBLE);
                updateProfileData();
            }
        });
    }

    public void onEditSurname(View view) {
        showEditDialog("Введите фамилию", lastNameText.getText().toString(), newValue -> {
            if (!newValue.isEmpty()) {
                lastNameText.setText(newValue);
                lastNameCheck.setVisibility(View.VISIBLE);
                updateProfileData();
            }
        });
    }

    public void onEditPhone(View view) {
        showEditDialog("Введите телефон", telephoneText.getText().toString().replaceAll("[^0-9]", ""), newValue -> {
            if (!newValue.isEmpty()) {
                String formattedPhone = formatPhoneNumber(newValue);
                telephoneText.setText(formattedPhone);
                telephoneCheck.setVisibility(View.VISIBLE);
                updateProfileData();
            }
        });
    }

    public void onEditAddress(View view) {
        showEditDialog("Введите адрес", addressText.getText().toString(), newValue -> {
            if (!newValue.isEmpty()) {
                addressText.setText(newValue);
                addressCheck.setVisibility(View.VISIBLE);
                updateProfileData();
            }
        });
    }

    public void onEditCard(View view) {
        AddCardDialogFragment addCardDialog = new AddCardDialogFragment();
        addCardDialog.show(getSupportFragmentManager(), "AddCardDialog");
    }

    private void showEditDialog(String title, String currentValue, OnValueChangedListener listener) {
        TextInputEditText input = new TextInputEditText(this);
        input.setText(currentValue);
        new android.app.AlertDialog.Builder(this)
                .setTitle(title)
                .setView(input)
                .setPositiveButton("Сохранить", (dialog, which) -> {
                    String newValue = input.getText().toString().trim();
                    listener.onValueChanged(newValue);
                })
                .setNegativeButton("Отмена", (dialog, which) -> dialog.dismiss())
                .setCancelable(false)
                .show();
    }

    interface OnValueChangedListener {
        void onValueChanged(String newValue);
    }

    public void loadProfileData() {
        executorService.execute(() -> {
            try {
                String response = SupabaseApi.getInstance().fetchProfile(currentEmail);
                if (response != null) {
                    JSONArray profiles = new JSONArray(response);
                    if (profiles.length() > 0) {
                        JSONObject profile = profiles.getJSONObject(0);
                        String firstName = profile.optString("first_name", "");
                        String lastName = profile.optString("last_name", "");
                        String telephone = profile.optString("telephone_number", "");
                        String address = profile.optString("address_home", "");
                        String cardOplats = profile.optString("card_oplats", "");
                        String avatarPath = profile.optString("avatar_url", null);

                        mainHandler.post(() -> {
                            profileFullname.setText(firstName + " " + lastName);
                            firstNameText.setText(firstName);
                            lastNameText.setText(lastName);
                            telephoneText.setText(telephone.isEmpty() ? "" : formatPhoneNumber(telephone.replaceAll("[^0-9]", "")));
                            addressText.setText(address);
                            cardOplatsText.setText(cardOplats.isEmpty() ? "Не добавлена" : "•••• " + cardOplats.substring(cardOplats.length() - 4));
                            drawerUserFullname.setText(firstName + " " + lastName);

                            if (avatarPath != null && !avatarPath.isEmpty()) {
                                String avatarUrl = BuildConfig.SUPABASE_URL + "/storage/v1/object/public/avatars/" + avatarPath;
                                Glide.with(this)
                                        .load(avatarUrl)
                                        .placeholder(R.drawable.default_avatar)
                                        .error(R.drawable.default_avatar)
                                        .into(profileImage);
                                Glide.with(this)
                                        .load(avatarUrl)
                                        .placeholder(R.drawable.default_avatar)
                                        .error(R.drawable.default_avatar)
                                        .into(drawerUserAvatar);
                            } else {
                                profileImage.setImageResource(R.drawable.default_avatar);
                                drawerUserAvatar.setImageResource(R.drawable.default_avatar);
                            }

                            firstNameCheck.setVisibility(firstName.isEmpty() ? View.GONE : View.VISIBLE);
                            lastNameCheck.setVisibility(lastName.isEmpty() ? View.GONE : View.VISIBLE);
                            telephoneCheck.setVisibility(telephone.isEmpty() ? View.GONE : View.VISIBLE);
                            addressCheck.setVisibility(address.isEmpty() ? View.GONE : View.VISIBLE);
                            cardCheck.setVisibility(cardOplats.isEmpty() ? View.GONE : View.VISIBLE);
                        });
                    } else {
                        mainHandler.post(() -> showErrorDialog("Данные профиля не найдены"));
                    }
                } else {
                    mainHandler.post(() -> showErrorDialog("Данные профиля не найдены"));
                }
            } catch (IOException e) {
                mainHandler.post(() -> showErrorDialog("Ошибка загрузки профиля: " + e.getMessage()));
            } catch (JSONException e) {
                mainHandler.post(() -> showErrorDialog("Ошибка парсинга данных профиля: " + e.getMessage()));
            }
        });
    }

    private void updateProfileData() {
        String firstName = firstNameText.getText().toString().trim();
        String lastName = lastNameText.getText().toString().trim();
        String telephone = telephoneText.getText().toString().replaceAll("[^0-9]", "");
        String address = addressText.getText().toString().trim();

        executorService.execute(() -> {
            try {
                JSONObject json = new JSONObject();
                json.put("email", currentEmail);
                json.put("first_name", firstName);
                json.put("last_name", lastName);
                json.put("telephone_number", telephone);
                json.put("address_home", address);

                Log.d(TAG, "Отправляемый JSON: " + json.toString());
                String response = SupabaseApi.getInstance().updateProfile(currentEmail, json.toString());
                mainHandler.post(() -> {
                    Toast.makeText(this, "Профиль обновлён!", Toast.LENGTH_SHORT).show();
                    loadProfileData();
                });
            } catch (IOException e) {
                mainHandler.post(() -> showErrorDialog("Ошибка обновления профиля: " + e.getMessage()));
            } catch (JSONException e) {
                mainHandler.post(() -> showErrorDialog("Ошибка создания JSON: " + e.getMessage()));
            }
        });
    }

    private void uploadNewAvatar(Uri imageUri) {
        try {

            File avatarFile = new File(getCacheDir(), "avatar_" + System.currentTimeMillis() + ".png");
            try (InputStream inputStream = getContentResolver().openInputStream(imageUri);
                 OutputStream outputStream = new FileOutputStream(avatarFile)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }

            executorService.execute(() -> {
                try {
                    String fileName = SupabaseApi.getInstance().uploadAvatar(currentEmail, avatarFile);
                    mainHandler.post(() -> {
                        String avatarUrl = BuildConfig.SUPABASE_URL + "/storage/v1/object/public/avatars/" + fileName;
                        Glide.with(this)
                                .load(avatarUrl)
                                .placeholder(R.drawable.default_avatar)
                                .error(R.drawable.default_avatar)
                                .into(profileImage);
                        Glide.with(this)
                                .load(avatarUrl)
                                .placeholder(R.drawable.default_avatar)
                                .error(R.drawable.default_avatar)
                                .into(drawerUserAvatar);
                        Toast.makeText(this, "Аватарка обновлена", Toast.LENGTH_SHORT).show();
                    });
                } catch (IOException e) {
                    mainHandler.post(() -> Toast.makeText(this, "Ошибка загрузки аватарки: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                } finally {

                    if (avatarFile.exists()) {
                        avatarFile.delete();
                    }
                }
            });
        } catch (IOException e) {
            Toast.makeText(this, "Ошибка обработки изображения: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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

    private void showErrorDialog(String message) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Ошибка")
                .setMessage(message)
                .setPositiveButton("ОК", (dialog, which) -> dialog.dismiss())
                .setCancelable(false)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
            loadProfileData();
        } else if (fragmentContainer.getVisibility() == View.VISIBLE) {
            profileContent.setVisibility(View.VISIBLE);
            fragmentContainer.setVisibility(View.GONE);
            getSupportFragmentManager().popBackStack();
            loadProfileData();
        } else {
            super.onBackPressed();
        }
    }
}