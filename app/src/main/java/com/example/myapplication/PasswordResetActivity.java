package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

import java.io.IOException;

public class PasswordResetActivity extends AppCompatActivity {

    private EditText etPassword, etPassword2;
    private String email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.password);

        email = getIntent().getStringExtra("email");
        etPassword = findViewById(R.id.etPassword);
        etPassword2 = findViewById(R.id.etPassword2);
        findViewById(R.id.btnBack).setOnClickListener(v -> onBackPressed());
        findViewById(R.id.btnRegister).setOnClickListener(this::updatePassword);
    }

    private void updatePassword(View v) {
        String password = etPassword.getText().toString().trim();
        String password2 = etPassword2.getText().toString().trim();

        if (!password.equals(password2)) {
            Toast.makeText(this, "Пароли не совпадают", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!Validator.isValidPassword(password)) {
            Toast.makeText(this, "Пароль должен быть от 6 до 8 символов", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {
                SupabaseApi.getInstance().updatePassword(email, password);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Пароль успешно изменён", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, LoginFragment.class);
                    startActivity(intent);
                    finish();
                });
            } catch (IOException e) {
                runOnUiThread(() -> Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}