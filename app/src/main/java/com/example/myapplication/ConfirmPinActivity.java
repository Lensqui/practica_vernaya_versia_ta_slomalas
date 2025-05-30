package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class ConfirmPinActivity extends AppCompatActivity {

    private EditText pinInput;
    private View[] pinDots;
    private String newPin;
    private String email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_pin);

        pinInput = findViewById(R.id.pinInput);
        pinDots = new View[]{
                findViewById(R.id.dot1),
                findViewById(R.id.dot2),
                findViewById(R.id.dot3),
                findViewById(R.id.dot4)
        };

        email = getIntent().getStringExtra("email");
        newPin = getIntent().getStringExtra("new_pin");

        setupKeyboard();
        setupPinInput();
    }

    private void setupKeyboard() {
        int[] keyIds = new int[]{R.id.key0, R.id.key1, R.id.key2, R.id.key3, R.id.key4, R.id.key5, R.id.key6, R.id.key7, R.id.key8, R.id.key9};
        for (int i = 0; i < keyIds.length; i++) {
            AppCompatButton key = findViewById(keyIds[i]);
            final String digit = String.valueOf(i);
            key.setOnClickListener(v -> {
                if (pinInput.getText().length() < 4) {
                    pinInput.append(digit);
                }
            });
        }

        ImageButton keyDelete = findViewById(R.id.keyDelete);
        keyDelete.setOnClickListener(v -> {
            if (pinInput.getText().length() > 0) {
                pinInput.setText(pinInput.getText().subSequence(0, pinInput.getText().length() - 1));
            }
        });
    }
    private void setupPinInput() {
        pinInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                updatePinDots(s.length());
                if (s.length() == 4) {
                    if (s.toString().equals(newPin)) {
                        savePin(newPin);
                    } else {
                        Toast.makeText(ConfirmPinActivity.this, "PIN-коды не совпадают, попробуйте снова", Toast.LENGTH_SHORT).show();
                        pinInput.setText("");
                    }
                }
            }
        });
    }

    private void updatePinDots(int length) {
        for (int i = 0; i < pinDots.length; i++) {
            pinDots[i].setBackgroundResource(i < length ? R.drawable.pin_dot2 : R.drawable.pin_dot);
        }
    }

    private void savePin(String pin) {
        new Thread(() -> {
            try {
                JSONObject json = new JSONObject();
                json.put("pin_code", pin);
                String response = SupabaseApi.getInstance().updateProfile(email, json.toString());
                runOnUiThread(() -> {
                    Toast.makeText(this, "PIN-код успешно установлен!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, HomeActivity.class);
                    startActivity(intent);
                    finish();
                });
            } catch (IOException | JSONException e) {
                runOnUiThread(() -> Toast.makeText(this, "Ошибка сохранения PIN-кода: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }
}
