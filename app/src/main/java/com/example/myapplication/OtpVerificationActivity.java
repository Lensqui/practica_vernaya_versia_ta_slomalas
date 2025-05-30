package com.example.myapplication;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OtpVerificationActivity extends AppCompatActivity {

    private EditText[] otpCells;
    private TextView timerText, resendCodeText;
    private LocalStorage localStorage;
    private ExecutorService executorService;
    private Handler mainHandler;
    private String email, name, password;
    private CountDownTimer timer;
    private boolean canResend = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp_verification);

        otpCells = new EditText[]{
                findViewById(R.id.otp_cell_1),
                findViewById(R.id.otp_cell_2),
                findViewById(R.id.otp_cell_3),
                findViewById(R.id.otp_cell_4),
                findViewById(R.id.otp_cell_5),
                findViewById(R.id.otp_cell_6)
        };
        timerText = findViewById(R.id.timerText);
        resendCodeText = findViewById(R.id.resendCodeText);
        ImageButton btnBack = findViewById(R.id.btnBack);

        localStorage = LocalStorage.getInstance();
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        email = getIntent().getStringExtra("email");
        name = getIntent().getStringExtra("name");
        password = getIntent().getStringExtra("password");

        setupOtpInput();
        startTimer();

        resendCodeText.setOnClickListener(v -> {
            if (canResend) {
                resendOtp();
            }
        });
        btnBack.setOnClickListener(v -> finish());
    }

    private void setupOtpInput() {
        for (int i = 0; i < otpCells.length; i++) {
            final int index = i;
            EditText cell = otpCells[i];

            cell.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    cell.setBackgroundResource(R.drawable.neutral_border);
                    if (s.length() == 1 && index < otpCells.length - 1) {
                        otpCells[index + 1].requestFocus();
                    } else if (s.length() == 0 && index > 0) {
                        otpCells[index - 1].requestFocus();
                    }
                    if (index == otpCells.length - 1 && s.length() == 1) {
                        verifyOtp();
                    }
                }
            });

            cell.setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (cell.getText().length() == 0 && index > 0) {
                        otpCells[index - 1].requestFocus();
                        otpCells[index - 1].setText("");
                    }
                }
                return false;
            });
        }
    }

    private void startTimer() {
        canResend = false;
        resendCodeText.setVisibility(View.GONE);
        timer = new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long seconds = millisUntilFinished / 1000;
                timerText.setText(String.format("00:%02d", seconds));
            }

            @Override
            public void onFinish() {
                timerText.setText("00:00");
                resendCodeText.setVisibility(View.VISIBLE);
                canResend = true;
            }
        }.start();
    }

    private void resendOtp() {
        if (!canResend) {
            Toast.makeText(this, "Подождите перед повторной отправкой", Toast.LENGTH_SHORT).show();
            return;
        }

        executorService.execute(() -> {
            try {
                SupabaseApi.getInstance().signInWithOtp(email);
                mainHandler.post(() -> {
                    showResendOtpDialog();
                    startTimer();
                    clearOtpCells();
                });
            } catch (IOException e) {
                mainHandler.post(() -> {
                    if (e.getMessage().contains("429")) {
                        Toast.makeText(this, "Превышен лимит отправки. Попробуйте позже.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private void showResendOtpDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_resend_otp, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);

        TextView title = dialogView.findViewById(R.id.notification_title);
        TextView message = dialogView.findViewById(R.id.notification_message);

        if (title != null) title.setText("Код отправлен повторно");
        if (message != null) message.setText("Проверьте вашу почту для получения нового кода");

        dialogView.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void clearOtpCells() {
        for (EditText cell : otpCells) {
            cell.setText("");
            cell.setBackgroundResource(R.drawable.neutral_border);
        }
        otpCells[0].requestFocus();
    }

    private void verifyOtp() {
        StringBuilder otp = new StringBuilder();
        for (EditText cell : otpCells) {
            otp.append(cell.getText().toString());
        }
        String otpCode = otp.toString();

        if (otpCode.length() != 6) {
            Toast.makeText(this, "Введите полный код", Toast.LENGTH_SHORT).show();
            return;
        }

        executorService.execute(() -> {
            try {
                String response = SupabaseApi.getInstance().verifyOtp(email, otpCode, password);
                JSONObject jsonResponse = new JSONObject(response);
                String userId = jsonResponse.getJSONObject("user").getString("id");

                SupabaseApi.getInstance().updateProfileByUserId(name, email, userId);

                mainHandler.post(() -> {
                    Toast.makeText(this, "Регистрация успешна!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                });
            } catch (IOException | JSONException e) {
                mainHandler.post(() -> {
                    Toast.makeText(this, "Ошибка верификации: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    for (EditText cell : otpCells) {
                        cell.setBackgroundResource(R.drawable.error_border);
                    }
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
        if (timer != null) {
            timer.cancel();
        }
    }
}