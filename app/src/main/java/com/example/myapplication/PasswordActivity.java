package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PasswordActivity extends AppCompatActivity {

    private TextInputEditText etPassword, etPassword2;
    private AppCompatButton btnRegister;
    private TextView tvLogin2;
    private LocalStorage localStorage;
    private ExecutorService executorService;
    private Handler mainHandler;
    private String email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.password);

        etPassword = findViewById(R.id.etPassword);
        etPassword2 = findViewById(R.id.etPassword2);
        btnRegister = findViewById(R.id.btnRegister);
        tvLogin2 = findViewById(R.id.tvLogin2);
        ImageButton btnBack = findViewById(R.id.btnBack);

        localStorage = LocalStorage.getInstance();
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        email = getIntent().getStringExtra("email");

        btnRegister.setOnClickListener(v -> handlePasswordReset());
        btnBack.setOnClickListener(v -> finish());

        tvLogin2.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("showRegisterFragment", true);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void handlePasswordReset() {
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etPassword2.getText().toString().trim();

        if (!Validator.isValidPassword(password)) {
            etPassword.setError("Пароль должен быть от 6 до 8 символов");
            return;
        }
        if (!Validator.passwordsMatch(password, confirmPassword)) {
            etPassword2.setError("Пароли не совпадают");
            return;
        }

        executorService.execute(() -> {
            try {
                String response = SupabaseApi.getInstance().updatePassword(email, password);
                if (response != null) {
                    mainHandler.post(() -> {
                        Toast.makeText(this, "Пароль успешно изменён!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(this, MainActivity.class);
                        intent.putExtra("showLoginFragment", true);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    });
                } else {
                    mainHandler.post(() -> {
                        etPassword.setError("Ошибка обновления пароля");
                    });
                }
            } catch (IOException e) {
                mainHandler.post(() -> {
                    etPassword.setError("Ошибка: " + e.getMessage());
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}