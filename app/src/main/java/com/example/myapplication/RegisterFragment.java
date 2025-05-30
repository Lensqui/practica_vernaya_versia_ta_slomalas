package com.example.myapplication;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RegisterFragment extends Fragment {

    private TextInputEditText etName, etEmail, etPassword, etConfirmPassword;
    private CheckBox cbAgreement;
    private AppCompatButton btnRegister;
    private TextView tvLogin2;
    private LocalStorage localStorage;
    private ExecutorService executorService;
    private Handler mainHandler;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_register, container, false);

        etName = view.findViewById(R.id.etName);
        etEmail = view.findViewById(R.id.etEmail);
        etPassword = view.findViewById(R.id.etPassword);
        etConfirmPassword = view.findViewById(R.id.etConfirmPassword);
        cbAgreement = view.findViewById(R.id.cbAgreement);
        btnRegister = view.findViewById(R.id.btnRegister);
        tvLogin2 = view.findViewById(R.id.tvLogin2);
        ImageButton btnBack = view.findViewById(R.id.btnBack);

        localStorage = LocalStorage.getInstance();
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        btnRegister.setOnClickListener(v -> handleRegister());
        tvLogin2.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.container, new LoginFragment())
                    .addToBackStack(null)
                    .commit();
        });
        btnBack.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        return view;
    }

    private void handleRegister() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (!Validator.isValidName(name)) {
            etName.setError("Имя должно содержать только буквы (русские или латинские), до 20 символов");
            return;
        }
        if (!Validator.isValidEmail(email)) {
            etEmail.setError("Введите корректный email (например, name@domenname.ru или name@domenname.com)");
            return;
        }
        if (!Validator.isValidPassword(password)) {
            etPassword.setError("Пароль должен быть от 6 до 8 символов");
            return;
        }
        if (!Validator.passwordsMatch(password, confirmPassword)) {
            etConfirmPassword.setError("Пароли не совпадают");
            return;
        }
        if (!cbAgreement.isChecked()) {
            Toast.makeText(requireContext(), "Необходимо дать согласие на обработку данных", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_notification, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);

        TextView title = dialogView.findViewById(R.id.notification_title);
        TextView message = dialogView.findViewById(R.id.notification_message);

        if (title != null) title.setText("Код отправлен");
        if (message != null) message.setText("Проверьте вашу почту и введите код, чтобы подтвердить регистрацию");

        dialogView.setOnClickListener(v -> {
            dialog.dismiss();
            executorService.execute(() -> {
                try {
                    SupabaseApi.getInstance().signInWithOtp(email);
                    mainHandler.post(() -> {
                        Toast.makeText(requireContext(), "Код отправлен на email", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(requireContext(), OtpVerificationActivity.class);
                        intent.putExtra("email", email);
                        intent.putExtra("name", name);
                        intent.putExtra("password", password);
                        startActivity(intent);
                    });
                } catch (IOException e) {
                    mainHandler.post(() -> {
                        Toast.makeText(requireContext(), "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }
            });
        });

        mainHandler.post(() -> dialog.show());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}