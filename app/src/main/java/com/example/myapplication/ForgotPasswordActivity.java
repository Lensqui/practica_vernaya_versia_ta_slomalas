package com.example.myapplication;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText etEmail;
    private TextView errorText;
    private AppCompatButton btnSubmit;
    private LocalStorage localStorage;
    private ExecutorService executorService;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        etEmail = findViewById(R.id.etEmail);
        errorText = findViewById(R.id.errorText);
        btnSubmit = findViewById(R.id.btnSubmit);
        ImageButton btnBack = findViewById(R.id.btnBack);

        localStorage = LocalStorage.getInstance();
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        String prefilledEmail = getIntent().getStringExtra("email");
        if (prefilledEmail != null) {
            etEmail.setText(prefilledEmail);
        }

        btnSubmit.setOnClickListener(v -> handleEmailSubmit());
        btnBack.setOnClickListener(v -> finish());

        findViewById(R.id.overlay).setOnClickListener(v -> finish());
    }

    private void handleEmailSubmit() {
        String email = etEmail.getText().toString().trim();

        if (!Validator.isValidEmail(email)) {
            errorText.setVisibility(View.VISIBLE);
            errorText.setText("Введите корректный email (например, name@domenname.ru или name@domenname.com)");
            return;
        }

        executorService.execute(() -> {
            try {
                String response = SupabaseApi.getInstance().fetchProfile(email);
                if (response != null && !response.trim().isEmpty() && !response.equals("[]")) {
                    JSONArray profileArray = new JSONArray(response);
                    if (profileArray.length() > 0) {
                        mainHandler.post(() -> {
                            Intent intent = new Intent(ForgotPasswordActivity.this, PasswordActivity.class);
                            intent.putExtra("email", email);
                            startActivity(intent);
                            finish();
                        });
                    } else {
                        mainHandler.post(() -> {
                            errorText.setVisibility(View.VISIBLE);
                            errorText.setText("Пользователь с таким email не существует");
                        });
                    }
                } else {
                    mainHandler.post(() -> {
                        errorText.setVisibility(View.VISIBLE);
                        errorText.setText("Ошибка проверки email");
                    });
                }
            } catch (IOException | JSONException e) {
                mainHandler.post(() -> {
                    errorText.setVisibility(View.VISIBLE);
                    errorText.setText("Ошибка: " + e.getMessage());
                });
            }
        });
    }
}