package com.example.myapplication;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class LoginFragment extends Fragment {

    private TextInputEditText etEmail, etPassword;
    private AppCompatButton btnRegister;
    private TextView tvLogin2, tvRestore;
    private ImageButton btnBack;
    private LocalStorage localStorage;
    private ExecutorService executorService;
    private Handler mainHandler;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_login, container, false);

        etEmail = view.findViewById(R.id.etEmail);
        etPassword = view.findViewById(R.id.etPassword);
        btnRegister = view.findViewById(R.id.btnRegister);
        tvLogin2 = view.findViewById(R.id.tvLogin2);
        tvRestore = view.findViewById(R.id.restore);
        btnBack = view.findViewById(R.id.btnBack);

        localStorage = LocalStorage.getInstance();

        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        btnRegister.setOnClickListener(v -> handleLogin());
        tvLogin2.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.container, new RegisterFragment())
                    .addToBackStack(null)
                    .commit();
        });
        tvRestore.setOnClickListener(v -> handlePasswordReset());
        btnBack.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        checkSavedCredentials();

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }

    private void checkSavedCredentials() {
        String savedEmail = localStorage.getEmail();
        String savedPassword = localStorage.getPassword();
        if (savedEmail != null && savedPassword != null) {
            etEmail.setText(savedEmail);
            etPassword.setText(savedPassword);
        }
    }

    private void handleLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (!Validator.isValidEmail(email)) {
            etEmail.setError("Введите корректный email (например, name@domenname.ru или name@domenname.com)");
            return;
        }
        if (!Validator.isValidPassword(password)) {
            etPassword.setError("Пароль должен быть не длиннее 8 символов");
            return;
        }

        executorService.execute(() -> {
            try {
                String response = SupabaseApi.getInstance().signIn(email, password);
                localStorage.saveCredentials(email, password);

                String profileResponse = SupabaseApi.getInstance().fetchProfile(email);
                String userId = null;
                String fullName = "Пользователь";
                String avatarUrl = "";
                String pinCode = "";

                if (profileResponse != null && !profileResponse.trim().isEmpty() && !profileResponse.equals("[]")) {
                    JSONArray profileArray = new JSONArray(profileResponse);
                    if (profileArray.length() > 0) {
                        JSONObject profile = profileArray.getJSONObject(0);
                        userId = profile.getString("id");
                        fullName = profile.optString("first_name", "") + " " + profile.optString("last_name", "");
                        avatarUrl = profile.optString("avatar_url", "");
                        pinCode = profile.optString("pin_code", "");
                        if (!avatarUrl.isEmpty()) {
                            avatarUrl = BuildConfig.SUPABASE_URL + "/storage/v1/object/public/avatars/" + avatarUrl;
                        }
                    } else {
                        throw new IOException("Профиль не содержит данных");
                    }
                } else {
                    throw new IOException("Профиль не найден");
                }

                String finalUserId = userId;
                String finalFullName = fullName;
                String finalAvatarUrl = avatarUrl;
                String finalPinCode = pinCode;

                mainHandler.post(() -> {
                    Toast.makeText(requireContext(), "Вход успешен!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(requireContext(), EnterPinActivity.class);
                    intent.putExtra("email", email);
                    intent.putExtra("userId", finalUserId);
                    intent.putExtra("pin_code", finalPinCode);
                    intent.putExtra("userName", finalFullName);
                    intent.putExtra("avatarUrl", finalAvatarUrl);
                    startActivity(intent);
                    requireActivity().finish();
                });
            } catch (IOException | JSONException e) {
                mainHandler.post(() -> showErrorDialog("Ошибка: " + e.getMessage()));
            }
        });
    }

    private void handlePasswordReset() {

        Intent intent = new Intent(requireContext(), ForgotPasswordActivity.class);

        String email = etEmail.getText().toString().trim();
        if (!email.isEmpty()) {
            intent.putExtra("email", email);
        }
        startActivity(intent);
    }

    private void showErrorDialog(String message) {
        new android.app.AlertDialog.Builder(requireContext())
                .setTitle("Ошибка")
                .setMessage(message)
                .setPositiveButton("ОК", (dialog, which) -> dialog.dismiss())
                .setCancelable(false)
                .show();
    }
}