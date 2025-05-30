package com.example.myapplication;

import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SupabaseApi {
    private static SupabaseApi instance;
    private final OkHttpClient client;
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final String baseUrl = BuildConfig.SUPABASE_URL;
    private final String apiKey = BuildConfig.SUPABASE_API_KEY;

    private SupabaseApi() {
        client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public static synchronized SupabaseApi getInstance() {
        if (instance == null) {
            instance = new SupabaseApi();
        }
        return instance;
    }

    public String signIn(String email, String password) throws IOException {
        JSONObject json = new JSONObject();
        try {
            json.put("email", email);
            json.put("password", password);
        } catch (JSONException e) {
            throw new IOException("Ошибка создания JSON: " + e.getMessage());
        }

        RequestBody body = RequestBody.create(json.toString(), JSON);
        Request request = new Request.Builder()
                .url(baseUrl + "/auth/v1/token?grant_type=password")
                .post(body)
                .addHeader("apikey", apiKey)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Ошибка входа: " + response.code() + " - " + response.message() + ", Тело: " + (response.body() != null ? response.body().string() : "null"));
            }
            String responseBody = response.body().string();
            try {
                JSONObject jsonResponse = new JSONObject(responseBody);
                String accessToken = jsonResponse.getString("access_token");
                LocalStorage.getInstance().saveToken(accessToken);
                LocalStorage.getInstance().saveCredentials(email, password);
                return responseBody;
            } catch (JSONException e) {
                throw new IOException("Ошибка парсинга ответа: " + e.getMessage() + ", Тело: " + responseBody);
            }
        }
    }

    public String[] signUp(String email, String password) throws IOException {
        JSONObject json = new JSONObject();
        try {
            json.put("email", email);
            json.put("password", password);
            JSONObject userMetadata = new JSONObject();
            userMetadata.put("full_name", "");
            userMetadata.put("avatar_url", "");
            json.put("user_metadata", userMetadata);
        } catch (JSONException e) {
            throw new IOException("Ошибка создания JSON: " + e.getMessage());
        }

        RequestBody body = RequestBody.create(json.toString(), JSON);
        Request request = new Request.Builder()
                .url(baseUrl + "/auth/v1/signup")
                .post(body)
                .addHeader("apikey", apiKey)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Ошибка регистрации: " + response.code() + " - " + response.message() + ", Тело: " + (response.body() != null ? response.body().string() : "null"));
            }
            String responseBody = response.body().string();
            try {
                JSONObject jsonResponse = new JSONObject(responseBody);
                String accessToken = jsonResponse.getString("access_token");
                LocalStorage.getInstance().saveToken(accessToken);
                LocalStorage.getInstance().saveCredentials(email, password);
                String userId = jsonResponse.getJSONObject("user").getString("id");
                Log.d("SupabaseApi", "Полный ответ signUp: " + responseBody);
                return new String[]{responseBody, userId};
            } catch (JSONException e) {
                throw new IOException("Ошибка парсинга ответа: " + e.getMessage() + ", Тело: " + responseBody);
            }
        }
    }

    public String fetchProfileById(String id) throws IOException {
        String token = LocalStorage.getInstance().getToken();
        if (token == null) {
            throw new IOException("Токен авторизации отсутствует");
        }

        Request request = new Request.Builder()
                .url(baseUrl + "/rest/v1/profiles?select=*&id=eq." + id)
                .get()
                .addHeader("apikey", apiKey)
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Ошибка загрузки профиля: " + response.code() + " - " + response.message() + ", Тело: " + (response.body() != null ? response.body().string() : "null"));
            }
            String responseBody = response.body().string();
            Log.d("SupabaseApi", "Fetch profile by id response: " + responseBody);
            return responseBody;
        }
    }

    public String resetPassword(String email) throws IOException {
        JSONObject json = new JSONObject();
        try {
            json.put("email", email);
        } catch (JSONException e) {
            throw new IOException("Ошибка создания JSON: " + e.getMessage());
        }

        RequestBody body = RequestBody.create(json.toString(), JSON);
        Request request = new Request.Builder()
                .url(baseUrl + "/auth/v1/recover")
                .post(body)
                .addHeader("apikey", apiKey)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Ошибка сброса пароля: " + response.code() + " - " + response.message() + ", Тело: " + (response.body() != null ? response.body().string() : "null"));
            }
            return response.body().string();
        }
    }

    public String updateProfileByUserId(String firstName, String email, String userId) throws IOException {
        String token = LocalStorage.getInstance().getToken();
        if (token == null) {
            throw new IOException("Токен авторизации отсутствует");
        }

        try {
            String[] tokenParts = token.split("\\.");
            if (tokenParts.length > 1) {
                String payload = new String(android.util.Base64.decode(tokenParts[1], android.util.Base64.URL_SAFE), StandardCharsets.UTF_8);
                JSONObject payloadJson = new JSONObject(payload);
                String sub = payloadJson.getString("sub");
                Log.d("SupabaseApi", "auth.uid() из токена (sub): " + sub);
                Log.d("SupabaseApi", "Передаваемый userId: " + userId);
                if (!sub.equals(userId)) {
                    Log.w("SupabaseApi", "Предупреждение: sub и userId не совпадают!");
                }
            }
        } catch (Exception e) {
            Log.e("SupabaseApi", "Ошибка декодирования токена: " + e.getMessage());
        }

        JSONObject json = new JSONObject();
        try {
            json.put("first_name", firstName);
            json.put("email", email);
            json.put("username", userId);
            json.put("last_name", "");
            json.put("telephone_number", "");
            json.put("address_home", "");
            json.put("card_oplats", "");
            json.put("pin_code", "");
            json.put("name", "");
            json.put("expiry_date", "");
            Log.d("SupabaseApi", "Обновляемые данные профиля: " + json.toString());
        } catch (JSONException e) {
            throw new IOException("Ошибка создания JSON: " + e.getMessage());
        }

        RequestBody body = RequestBody.create(json.toString(), JSON);
        Request request = new Request.Builder()
                .url(baseUrl + "/rest/v1/profiles?id=eq." + userId)
                .patch(body)
                .addHeader("apikey", apiKey)
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Ошибка обновления профиля: " + response.code() + " - " + response.message() + ", Тело: " + (response.body() != null ? response.body().string() : "null"));
            }
            return response.body().string();
        }
    }

    public String fetchProfile(String email) throws IOException {
        String token = LocalStorage.getInstance().getToken();
        if (token == null) {
            throw new IOException("Токен авторизации отсутствует");
        }

        Request request = new Request.Builder()
                .url(baseUrl + "/rest/v1/profiles?select=*&email=eq." + email)
                .get()
                .addHeader("apikey", apiKey)
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Ошибка загрузки профиля: " + response.code() + " - " + response.message() + ", Тело: " + (response.body() != null ? response.body().string() : "null"));
            }
            String responseBody = response.body().string();
            if (responseBody.equals("[]")) {
                return null;
            }
            return responseBody;
        }
    }

    public String updateProfile(String email, String data) throws IOException {
        String token = LocalStorage.getInstance().getToken();
        if (token == null) {
            throw new IOException("Токен авторизации отсутствует");
        }

        RequestBody body = RequestBody.create(data, JSON);
        Request request = new Request.Builder()
                .url(baseUrl + "/rest/v1/profiles?email=eq." + email)
                .patch(body)
                .addHeader("apikey", apiKey)
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "Нет тела ошибки";
                throw new IOException("Ошибка обновления профиля: " + response.code() + " - " + response.message() + ", Тело: " + errorBody);
            }
            return response.body().string();
        }
    }

    public String uploadAvatar(String email, File avatarFile) throws IOException {
        String token = LocalStorage.getInstance().getToken();
        if (token == null) {
            throw new IOException("Токен авторизации отсутствует");
        }

        String fileName = email.replace("@", "_") + "_" + System.currentTimeMillis() + ".png";
        RequestBody fileBody = RequestBody.create(avatarFile, MediaType.parse("image/png"));
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", fileName, fileBody)
                .build();

        Request request = new Request.Builder()
                .url(baseUrl + "/storage/v1/object/avatars/" + fileName)
                .post(requestBody)
                .addHeader("apikey", apiKey)
                .addHeader("Authorization", "Bearer " + token)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Ошибка загрузки аватарки: " + response.code() + " - " + response.message());
            }

            JSONObject json = new JSONObject();
            try {
                json.put("avatar_url", fileName);
            } catch (JSONException e) {
                throw new IOException("Ошибка создания JSON: " + e.getMessage());
            }
            String updateResponse = updateProfile(email, json.toString());
            return fileName;
        }
    }

    public String resetPasswordForEmail(String email, String redirectTo) throws IOException {
        RequestBody body = new FormBody.Builder()
                .add("email", email)
                .add("redirect_to", redirectTo)
                .build();
        Request request = new Request.Builder()
                .url(baseUrl + "/auth/v1/recover")
                .post(body)
                .addHeader("apikey", apiKey)
                .addHeader("Content-Type", "application/json")
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
            return response.body().string();
        }
    }

    private void updatePasswordAfterOtp(String userId, String password) throws IOException {
        String token = LocalStorage.getInstance().getToken();
        if (token == null) {
            throw new IOException("Токен авторизации отсутствует");
        }

        JSONObject json = new JSONObject();
        try {
            json.put("password", password);
        } catch (JSONException e) {
            throw new IOException("Ошибка создания JSON: " + e.getMessage());
        }

        RequestBody body = RequestBody.create(json.toString(), JSON);
        Request request = new Request.Builder()
                .url(baseUrl + "/auth/v1/user")
                .put(body)
                .addHeader("apikey", apiKey)
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Ошибка обновления пароля: " + response.code() + " - " + response.body().string());
            }
        }
    }

    public String updatePassword(String email, String password) throws IOException {
        String token = LocalStorage.getInstance().getToken();
        if (token == null) {
            throw new IOException("Токен авторизации отсутствует");
        }

        JSONObject json = new JSONObject();
        try {
            json.put("password", password);
        } catch (JSONException e) {
            throw new IOException("Ошибка создания JSON: " + e.getMessage());
        }

        RequestBody body = RequestBody.create(json.toString(), JSON);
        Request request = new Request.Builder()
                .url(baseUrl + "/auth/v1/user")
                .put(body)
                .addHeader("apikey", apiKey)
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Content-Type", "application/json")
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful())
                throw new IOException("Failed to update password: " + response.code());
            return response.body().string();
        }
    }

    public String fetchCart(String profileId) throws IOException {
        String token = LocalStorage.getInstance().getToken();
        if (token == null) {
            throw new IOException("Токен авторизации отсутствует");
        }

        Request request = new Request.Builder()
                .url(baseUrl + "/rest/v1/cart?profile_id=eq." + profileId + "&select=product_id,quantity,products(price,name,image_url)")
                .get()
                .addHeader("apikey", apiKey)
                .addHeader("Authorization", "Bearer " + token)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Ошибка загрузки корзины: " + response.code() + ", " + response.body().string());
            }
            return response.body().string();
        }
    }

    public void createOrderItem(String orderId, String productId, int quantity, double price) throws IOException {
        String token = LocalStorage.getInstance().getToken();
        if (token == null) {
            throw new IOException("Токен авторизации отсутствует");
        }

        JSONObject json = new JSONObject();
        try {
            json.put("order_id", orderId);
            json.put("product_id", productId);
            json.put("quantity", quantity);
            json.put("price", price);
        } catch (JSONException e) {
            throw new IOException("Ошибка создания JSON: " + e.getMessage());
        }

        RequestBody body = RequestBody.create(json.toString(), JSON);
        Request request = new Request.Builder()
                .url(baseUrl + "/rest/v1/order_items")
                .post(body)
                .addHeader("apikey", apiKey)
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Ошибка создания order_item: " + response.code() + ", " + response.body().string());
            }
        }
    }

    public void clearCart(String profileId) throws IOException {
        String token = LocalStorage.getInstance().getToken();
        if (token == null) {
            throw new IOException("Токен авторизации отсутствует");
        }

        Request request = new Request.Builder()
                .url(baseUrl + "/rest/v1/cart?profile_id=eq." + profileId)
                .delete()
                .addHeader("apikey", apiKey)
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Ошибка очистки корзины: " + response.code() + ", " + response.body().string());
            }
        }
    }

    public String signInWithOtp(String email) throws IOException {
        JSONObject json = new JSONObject();
        try {
            json.put("email", email);
        } catch (JSONException e) {
            throw new IOException("Ошибка создания JSON: " + e.getMessage());
        }

        RequestBody body = RequestBody.create(json.toString(), JSON);
        Request request = new Request.Builder()
                .url(baseUrl + "/auth/v1/otp")
                .post(body)
                .addHeader("apikey", apiKey)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Ошибка отправки OTP: " + response.code() + " - " + response.body().string());
            }
            return response.body().string();
        }
    }

    public String verifyOtp(String email, String token, String password) throws IOException {
        JSONObject json = new JSONObject();
        try {
            json.put("email", email);
            json.put("token", token);
            json.put("type", "signup");
        } catch (JSONException e) {
            throw new IOException("Ошибка создания JSON: " + e.getMessage());
        }

        RequestBody body = RequestBody.create(json.toString(), JSON);
        Request request = new Request.Builder()
                .url(baseUrl + "/auth/v1/verify")
                .post(body)
                .addHeader("apikey", apiKey)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Ошибка верификации OTP: " + response.code() + " - " + response.body().string());
            }
            String responseBody = response.body().string();
            try {
                JSONObject jsonResponse = new JSONObject(responseBody);
                String accessToken = jsonResponse.getString("access_token");
                LocalStorage.getInstance().saveToken(accessToken);

                if (password != null) {
                    updatePasswordAfterOtp(jsonResponse.getJSONObject("user").getString("id"), password);
                }

                return responseBody;
            } catch (JSONException e) {
                throw new IOException("Ошибка парсинга ответа: " + e.getMessage());
            }
        }
    }
}