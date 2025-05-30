package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

public class VosstanovlPinKodaActivity extends AppCompatActivity {

    private EditText pinInput;
    private View[] pinDots;
    private String email;
    private String userId;
    private String userName;
    private String avatarUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.vosstanovl_pin_koda);

        pinInput = findViewById(R.id.pinInput);
        pinDots = new View[]{
                findViewById(R.id.dot1),
                findViewById(R.id.dot2),
                findViewById(R.id.dot3),
                findViewById(R.id.dot4)
        };

        email = getIntent().getStringExtra("email");
        userId = getIntent().getStringExtra("userId");
        userName = getIntent().getStringExtra("userName");
        avatarUrl = getIntent().getStringExtra("avatarUrl");

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
                    Intent intent = new Intent(VosstanovlPinKodaActivity.this, ConfirmPinActivity.class);
                    intent.putExtra("email", email);
                    intent.putExtra("userId", userId);
                    intent.putExtra("new_pin", s.toString());
                    intent.putExtra("userName", userName);
                    intent.putExtra("avatarUrl", avatarUrl);
                    startActivity(intent);
                    finish();
                }
            }
        });
    }

    private void updatePinDots(int length) {
        for (int i = 0; i < pinDots.length; i++) {
            pinDots[i].setBackgroundResource(i < length ? R.drawable.pin_dot2 : R.drawable.pin_dot);
        }
    }
}
