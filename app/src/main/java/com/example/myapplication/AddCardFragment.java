package com.example.myapplication;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddCardFragment extends Fragment {

    private static final String TAG = "AddCardFragment";

    private TextInputEditText holderNameEditText, cardNumberEditText, expiryDateEditText, cvvEditText;
    private ImageView ownerOfCard;
    private TextView cardNumber, balanceBox, cardDate, date;
    private LinearLayout bankCard;
    private ExecutorService executorService;
    private Handler mainHandler;
    private LocalStorage localStorage;
    private String currentCardNumber = "";
    private String currentHolderName = "";
    private String currentExpiryDate = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_add_card, container, false);

        LocalStorage.initialize(requireContext());
        localStorage = LocalStorage.getInstance();

        holderNameEditText = view.findViewById(R.id.holderNameEditText);
        cardNumberEditText = view.findViewById(R.id.cardNumberEditText);
        expiryDateEditText = view.findViewById(R.id.expiryDateEditText);
        cvvEditText = view.findViewById(R.id.cvvEditText);
        ownerOfCard = view.findViewById(R.id.ownerOfCard);
        cardNumber = view.findViewById(R.id.numberCard);
        balanceBox = view.findViewById(R.id.cardHolderName);
        cardDate = view.findViewById(R.id.cardDateJustNadpisu);
        date = view.findViewById(R.id.txtDate);
        bankCard = view.findViewById(R.id.bank_card);
        MaterialButton saveCardButton = view.findViewById(R.id.saveCardButton);

        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        cardNumberEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                currentCardNumber = s.toString().trim();
                String formattedNumber = formatCardNumberForDisplay(currentCardNumber, false);
                cardNumber.setText(formattedNumber);

                setCardTypeIcon(currentCardNumber);
            }
        });

        holderNameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                currentHolderName = s.toString().trim();
                balanceBox.setText(currentHolderName.isEmpty() ? "•••• ••••" : currentHolderName);
            }
        });

        expiryDateEditText.addTextChangedListener(new TextWatcher() {
            private String previousText = "";
            private boolean isFormatting;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (!isFormatting) {
                    previousText = s.toString();
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isFormatting) return;

                isFormatting = true;

                String input = s.toString().replaceAll("[^0-9]", "");
                String formatted = previousText;

                if (previousText.length() > s.length()) {
                    if (input.isEmpty()) {
                        expiryDateEditText.setText("");
                        expiryDateEditText.setSelection(0);
                        currentExpiryDate = "";
                        date.setText("••/••••");
                    } else if (input.length() <= 2) {
                        expiryDateEditText.setText(input);
                        expiryDateEditText.setSelection(input.length());
                        currentExpiryDate = input;
                        date.setText(currentExpiryDate);
                    }
                    isFormatting = false;
                    return;
                }

                if (input.length() > 0) {
                    if (input.length() == 1) {
                        int firstDigit = Integer.parseInt(input);
                        if (firstDigit < 0 || firstDigit > 1) {
                            formatted = "1";
                        } else {
                            formatted = input;
                        }
                    } else if (input.length() == 2) {
                        int month = Integer.parseInt(input);
                        if (month < 1 || month > 12) {
                            month = Integer.parseInt(input.substring(0, 1));
                            formatted = "0" + month + "/";
                        } else {
                            formatted = String.format("%02d", month) + "/";
                        }
                    } else if (input.length() >= 4) {
                        String month = input.substring(0, 2);
                        int monthValue = Integer.parseInt(month);
                        if (monthValue < 1 || monthValue > 12) {
                            month = "12";
                        }
                        String year = input.substring(2, Math.min(input.length(), 4));
                        formatted = month + "/" + year;
                    } else {
                        String month = input.substring(0, 2);
                        String year = input.substring(2);
                        formatted = month + "/" + year;
                    }

                    expiryDateEditText.setText(formatted);
                    expiryDateEditText.setSelection(formatted.length());
                    currentExpiryDate = formatted;
                    date.setText(currentExpiryDate);
                }

                isFormatting = false;
            }
        });

        holderNameEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                balanceBox.setText(currentHolderName.isEmpty() ? "•••• ••••" : "•••• ••••");
            }
        });

        cardNumberEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                cardNumber.setText(formatCardNumberForDisplay(currentCardNumber, true));
            }
        });

        expiryDateEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                date.setText(currentExpiryDate.isEmpty() ? "••/••••" : "••/••••");
            }
        });

        bankCard.setOnClickListener(v -> {
            cardNumber.setText(formatCardNumberForDisplay(currentCardNumber, false));
            balanceBox.setText(currentHolderName.isEmpty() ? "•••• ••••" : currentHolderName);
            date.setText(currentExpiryDate.isEmpty() ? "••/••••" : currentExpiryDate);

            mainHandler.postDelayed(() -> {
                cardNumber.setText(formatCardNumberForDisplay(currentCardNumber, true));
                balanceBox.setText(currentHolderName.isEmpty() ? "•••• ••••" : "•••• ••••");
                date.setText(currentExpiryDate.isEmpty() ? "••/••••" : "••/••••");
            }, 2000);
        });

        saveCardButton.setOnClickListener(v -> saveCard());

        loadCardData();

        return view;
    }

    private void setCardTypeIcon(String cardNumber) {
        if (cardNumber.isEmpty() || cardNumber.length() < 1) {
            ownerOfCard.setBackgroundResource(0);
            return;
        }
        char firstDigit = cardNumber.charAt(0);
        if (firstDigit == '4') {
            ownerOfCard.setBackgroundResource(R.drawable.viza);
        } else if (firstDigit == '2') {
            ownerOfCard.setBackgroundResource(R.drawable.mirrrrr__1_);
        } else {
            ownerOfCard.setBackgroundResource(0);
        }
    }

    private String formatCardNumberForDisplay(String cardNumber, boolean mask) {
        if (cardNumber.isEmpty()) {
            return "•••• •••• •••• ••••";
        }

        if (!mask) {
            StringBuilder formatted = new StringBuilder();
            for (int i = 0; i < cardNumber.length(); i++) {
                if (i > 0 && i % 4 == 0) {
                    formatted.append(" ");
                }
                formatted.append(cardNumber.charAt(i));
            }
            return formatted.toString();
        } else {
            String lastFour = cardNumber.length() >= 4 ? cardNumber.substring(cardNumber.length() - 4) : cardNumber;
            return "•••• •••• •••• " + lastFour;
        }
    }

    private void loadCardData() {
        String email = localStorage.getEmail();
        if (email == null) {
            mainHandler.post(() -> Toast.makeText(requireContext(), "Не удалось определить пользователя", Toast.LENGTH_SHORT).show());
            return;
        }

        executorService.execute(() -> {
            try {
                String response = SupabaseApi.getInstance().fetchProfile(email);
                Log.d(TAG, "Ответ от Supabase: " + response);
                if (response != null && !response.isEmpty()) {
                    JSONArray profiles = new JSONArray(response);
                    if (profiles.length() > 0) {
                        JSONObject json = profiles.getJSONObject(0);
                        String cardNumber = json.optString("card_oplats", null);
                        String holderName = json.optString("name", "");
                        String expiryDate = json.optString("expiry_date", "");

                        mainHandler.post(() -> {
                            if (cardNumber != null && !cardNumber.isEmpty()) {
                                currentCardNumber = cardNumber;
                                currentHolderName = holderName;
                                currentExpiryDate = expiryDate;
                                updateCardDisplay(cardNumber, holderName, expiryDate);
                                cardNumberEditText.setText(cardNumber);
                                holderNameEditText.setText(holderName);
                                expiryDateEditText.setText(expiryDate);
                                setCardTypeIcon(cardNumber);
                            }
                        });
                    }
                } else {
                    mainHandler.post(() -> Toast.makeText(requireContext(), "Данные карты не найдены", Toast.LENGTH_SHORT).show());
                }
            } catch (IOException | JSONException e) {
                mainHandler.post(() -> Toast.makeText(requireContext(), "Ошибка загрузки данных: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void saveCard() {
        String holderName = holderNameEditText.getText().toString().trim();
        String cardNumber = cardNumberEditText.getText().toString().trim();
        String expiryDate = expiryDateEditText.getText().toString().trim();
        String cvv = cvvEditText.getText().toString().trim();

        if (holderName.isEmpty() || cardNumber.isEmpty() || expiryDate.isEmpty() || cvv.isEmpty()) {
            Toast.makeText(requireContext(), "Заполните все поля", Toast.LENGTH_SHORT).show();
            return;
        }
        if (cardNumber.length() != 16 || !cardNumber.matches("\\d+")) {
            Toast.makeText(requireContext(), "Неверный номер карты (16 цифр)", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!expiryDate.matches("0[1-9]/\\d{2}") && !expiryDate.matches("1[0-2]/\\d{2}")) {
            Toast.makeText(requireContext(), "Неверный формат даты (например, 05/28)", Toast.LENGTH_SHORT).show();
            return;
        }
        if (cvv.length() != 3 || !cvv.matches("\\d+")) {
            Toast.makeText(requireContext(), "CVV должен содержать 3 цифры", Toast.LENGTH_SHORT).show();
            return;
        }

        executorService.execute(() -> {
            try {
                JSONObject json = new JSONObject();
                json.put("card_oplats", cardNumber);
                json.put("name", holderName);
                json.put("expiry_date", expiryDate);
                Log.d(TAG, "Отправляемый JSON: " + json.toString());
                String response = SupabaseApi.getInstance().updateProfile(localStorage.getEmail(), json.toString());
                mainHandler.post(() -> {
                    Toast.makeText(requireContext(), "Карта сохранена!", Toast.LENGTH_SHORT).show();
                    updateCardDisplay(cardNumber, holderName, expiryDate);
                    requireActivity().getSupportFragmentManager().popBackStack();
                    ((ProfileActivity) requireActivity()).loadProfileData();
                });
            } catch (IOException e) {
                String errorBody = e.getMessage().contains("Тело: ") ? e.getMessage().split("Тело: ")[1] : "Нет тела ошибки";
                Log.e(TAG, "Ошибка сохранения: " + e.getMessage() + ", Тело: " + errorBody);
                mainHandler.post(() -> Toast.makeText(requireContext(), "Ошибка сохранения: " + e.getMessage(), Toast.LENGTH_LONG).show());
            } catch (JSONException e) {
                mainHandler.post(() -> Toast.makeText(requireContext(), "Ошибка создания JSON: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void updateCardDisplay(String cardNumber, String holderName, String expiryDate) {
        currentCardNumber = cardNumber;
        currentHolderName = holderName;
        currentExpiryDate = expiryDate;
        setCardTypeIcon(cardNumber);
        this.cardNumber.setText(formatCardNumberForDisplay(cardNumber, true));
        balanceBox.setText(holderName.isEmpty() ? "•••• ••••" : "•••• ••••");
        cardDate.setText("Expiry date");
        date.setText(expiryDate.isEmpty() ? "••/••••" : "••/••••");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}