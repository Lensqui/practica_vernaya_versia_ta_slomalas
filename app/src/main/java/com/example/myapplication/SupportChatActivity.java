package com.example.myapplication;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SupportChatActivity extends AppCompatActivity {
    private static final String TAG = "SupportChatActivity";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final List<String> BOT_RESPONSES = Arrays.asList(
            "Спасибо за обращение! Чем могу помочь?",
            "Ваш запрос принят, уточните детали.",
            "Мы здесь, чтобы помочь! Что случилось?",
            "Пожалуйста, опишите проблему подробнее."
    );

    private RecyclerView chatRecyclerView;
    private EditText messageInput;
    private AppCompatButton sendButton;
    private ImageButton emojiButton;
    private ImageView btnBack;
    private TextView title;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> messages;
    private LocalStorage localStorage;
    private ExecutorService executor;
    private Handler mainHandler;
    private String profileId;
    private String userAvatarUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_support);

        localStorage = LocalStorage.getInstance();
        localStorage.initialize(this);

        chatRecyclerView = findViewById(R.id.chat_recycler_view);
        messageInput = findViewById(R.id.message_input);
        sendButton = findViewById(R.id.send_button);
        emojiButton = findViewById(R.id.emoji_button);
        btnBack = findViewById(R.id.btnBack);
        title = findViewById(R.id.title);

        executor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
        messages = new ArrayList<>();

        profileId = localStorage.getProfileId();
        if (profileId == null) {
            showErrorDialog("Ошибка: пользователь не авторизован", () -> {
                finish();
                startActivity(new Intent(this, MainActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));
            });
            return;
        }

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        chatRecyclerView.setLayoutManager(layoutManager);
        chatAdapter = new ChatAdapter(messages, userAvatarUrl, this);
        chatRecyclerView.setAdapter(chatAdapter);

        loadProfileData();
        loadMessages();

        btnBack.setOnClickListener(v -> onBackPressed());
        sendButton.setOnClickListener(v -> sendMessage());
        emojiButton.setOnClickListener(v -> Toast.makeText(this, "Эмодзи (в разработке)", Toast.LENGTH_SHORT).show());
    }

    private void loadProfileData() {
        executor.execute(() -> {
            try {
                String token = localStorage.getToken();
                if (token == null) {
                    mainHandler.post(() -> showErrorDialog("Ошибка: токен отсутствует", null));
                    return;
                }
                String response = SupabaseApi.getInstance().fetchProfile(localStorage.getEmail());
                Log.d(TAG, "Profile response: " + response);
                if (response != null && !response.equals("[]")) {
                    JSONArray profiles = new JSONArray(response);
                    if (profiles.length() > 0) {
                        JSONObject profile = profiles.getJSONObject(0);
                        String avatarPath = profile.optString("avatar_url", null);
                        mainHandler.post(() -> {
                            if (avatarPath != null && !avatarPath.isEmpty()) {
                                userAvatarUrl = BuildConfig.SUPABASE_URL + "/storage/v1/object/public/avatars/" + avatarPath;
                                chatAdapter = new ChatAdapter(messages, userAvatarUrl, this);
                                chatRecyclerView.setAdapter(chatAdapter);
                            }
                        });
                    }
                }
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Profile load error: " + e.getMessage());
                mainHandler.post(() -> showErrorDialog("Ошибка загрузки профиля: " + e.getMessage(), null));
            }
        });
    }

    private void loadMessages() {
        executor.execute(() -> {
            try {
                String token = localStorage.getToken();
                if (token == null) {
                    mainHandler.post(() -> showErrorDialog("Ошибка: токен отсутствует", null));
                    return;
                }
                String url = BuildConfig.SUPABASE_URL + "/rest/v1/support_chats?profile_id=eq." + profileId + "&select=*&order=created_at.asc";
                Request request = new Request.Builder()
                        .url(url)
                        .get()
                        .addHeader("apikey", BuildConfig.SUPABASE_API_KEY)
                        .addHeader("Authorization", "Bearer " + token)
                        .addHeader("Content-Type", "application/json")
                        .build();
                try (Response response = new OkHttpClient().newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        throw new IOException("Ошибка загрузки сообщений: " + response.message());
                    }
                    String responseBody = response.body().string();
                    Log.d(TAG, "Messages response: " + responseBody);
                    JSONArray messagesArray = new JSONArray(responseBody);
                    List<ChatMessage> loadedMessages = new ArrayList<>();
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                    for (int i = 0; i < messagesArray.length(); i++) {
                        JSONObject msg = messagesArray.getJSONObject(i);
                        Date createdAt = sdf.parse(msg.getString("created_at"));
                        loadedMessages.add(new ChatMessage(
                                msg.getString("id"),
                                msg.getString("profile_id"),
                                msg.getString("message"),
                                msg.getString("sender"),
                                createdAt,
                                msg.getString("sender").equals("support")
                        ));
                    }
                    mainHandler.post(() -> {
                        messages.clear();
                        messages.addAll(loadedMessages);
                        chatAdapter.notifyDataSetChanged();
                        chatRecyclerView.scrollToPosition(messages.size() - 1);
                    });
                }
            } catch (IOException | JSONException | ParseException e) {
                Log.e(TAG, "Messages load error: " + e.getMessage());
                mainHandler.post(() -> showErrorDialog("Ошибка загрузки сообщений: " + e.getMessage(), null));
            }
        });
    }

    private void sendMessage() {
        String messageText = messageInput.getText().toString().trim();
        if (messageText.isEmpty()) {
            Toast.makeText(this, "Введите сообщение", Toast.LENGTH_SHORT).show();
            return;
        }

        final ChatMessage userMessage = new ChatMessage(
                "",
                profileId,
                messageText,
                "user",
                new Date(),
                false
        );

        executor.execute(() -> {
            try {
                String token = localStorage.getToken();
                if (token == null) {
                    mainHandler.post(() -> showErrorDialog("Ошибка: токен отсутствует", null));
                    return;
                }
                JSONObject json = new JSONObject();
                json.put("profile_id", profileId);
                json.put("message", messageText);
                json.put("sender", "user");
                RequestBody body = RequestBody.create(json.toString(), JSON);
                Request request = new Request.Builder()
                        .url(BuildConfig.SUPABASE_URL + "/rest/v1/support_chats")
                        .post(body)
                        .addHeader("apikey", BuildConfig.SUPABASE_API_KEY)
                        .addHeader("Authorization", "Bearer " + token)
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Prefer", "return=representation")
                        .build();
                try (Response response = new OkHttpClient().newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        throw new IOException("Ошибка отправки сообщения: " + response.message());
                    }
                    String responseBody = response.body().string();
                    JSONArray responseArray = new JSONArray(responseBody);
                    ChatMessage updatedUserMessage = userMessage;
                    if (responseArray.length() > 0) {
                        JSONObject msg = responseArray.getJSONObject(0);
                        updatedUserMessage = new ChatMessage(
                                msg.getString("id"),
                                msg.getString("profile_id"),
                                msg.getString("message"),
                                msg.getString("sender"),
                                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(msg.getString("created_at")),
                                false
                        );
                    }
                    final ChatMessage finalUserMessage = updatedUserMessage;
                    mainHandler.post(() -> {
                        messages.add(finalUserMessage);
                        chatAdapter.notifyItemInserted(messages.size() - 1);
                        chatRecyclerView.scrollToPosition(messages.size() - 1);
                        messageInput.setText("");
                        scheduleBotResponse();
                    });
                }
            } catch (IOException | JSONException | ParseException e) {
                Log.e(TAG, "Send message error: " + e.getMessage());
                mainHandler.post(() -> showErrorDialog("Ошибка отправки сообщения: " + e.getMessage(), null));
            }
        });
    }

    private void scheduleBotResponse() {
        mainHandler.postDelayed(() -> {
            String botMessageText = BOT_RESPONSES.get((int) (Math.random() * BOT_RESPONSES.size()));
            final ChatMessage botMessage = new ChatMessage(
                    "",
                    profileId,
                    botMessageText,
                    "support",
                    new Date(),
                    true
            );

            executor.execute(() -> {
                try {
                    String token = localStorage.getToken();
                    if (token == null) {
                        mainHandler.post(() -> showErrorDialog("Ошибка: токен отсутствует", null));
                        return;
                    }
                    JSONObject json = new JSONObject();
                    json.put("profile_id", profileId);
                    json.put("message", botMessageText);
                    json.put("sender", "support");
                    RequestBody body = RequestBody.create(json.toString(), JSON);
                    Request request = new Request.Builder()
                            .url(BuildConfig.SUPABASE_URL + "/rest/v1/support_chats")
                            .post(body)
                            .addHeader("apikey", BuildConfig.SUPABASE_API_KEY)
                            .addHeader("Authorization", "Bearer " + token)
                            .addHeader("Content-Type", "application/json")
                            .addHeader("Prefer", "return=representation")
                            .build();
                    try (Response response = new OkHttpClient().newCall(request).execute()) {
                        if (!response.isSuccessful()) {
                            throw new IOException("Ошибка отправки ответа бота: " + response.message());
                        }
                        String responseBody = response.body().string();
                        JSONArray responseArray = new JSONArray(responseBody);
                        ChatMessage updatedBotMessage = botMessage;
                        if (responseArray.length() > 0) {
                            JSONObject msg = responseArray.getJSONObject(0);
                            updatedBotMessage = new ChatMessage(
                                    msg.getString("id"),
                                    msg.getString("profile_id"),
                                    msg.getString("message"),
                                    msg.getString("sender"),
                                    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(msg.getString("created_at")),
                                    true
                            );
                        }
                        final ChatMessage finalBotMessage = updatedBotMessage;
                        mainHandler.post(() -> {
                            for (int i = messages.size() - 1; i >= 0; i--) {
                                if (messages.get(i).getSender().equals("user") && !messages.get(i).isRead()) {
                                    chatAdapter.updateMessageReadStatus(i);
                                    break;
                                }
                            }
                            messages.add(finalBotMessage);
                            chatAdapter.notifyItemInserted(messages.size() - 1);
                            chatRecyclerView.scrollToPosition(messages.size() - 1);
                        });
                    }
                } catch (IOException | JSONException | ParseException e) {
                    Log.e(TAG, "Bot response error: " + e.getMessage());
                    mainHandler.post(() -> showErrorDialog("Ошибка ответа бота: " + e.getMessage(), null));
                }
            });
        }, 5000);
    }

    private void showErrorDialog(String message, Runnable onDismiss) {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Ошибка")
                .setMessage(message)
                .setPositiveButton("ОК", (d, which) -> {
                    d.dismiss();
                    if (onDismiss != null) {
                        onDismiss.run();
                    }
                })
                .setCancelable(false)
                .create();
        dialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
        mainHandler.removeCallbacksAndMessages(null);
    }
}