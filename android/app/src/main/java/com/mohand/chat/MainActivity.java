package com.mohand.chat;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends Activity {
    private static final String PREFS = "chat_preferences";
    private static final String URL_KEY = "chat_url";

    private final JSONArray history = new JSONArray();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private LinearLayout messagesView;
    private ScrollView scrollView;
    private TextView statusView;
    private EditText messageInput;
    private Button sendButton;
    private String chatUrl = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        chatUrl = getSharedPreferences(PREFS, MODE_PRIVATE).getString(URL_KEY, "");
        buildInterface();
        if (chatUrl.isEmpty()) {
            statusView.setText("أضف رابط خادم الدردشة من زر الإعدادات للبدء");
            addBubble("أهلاً، هذه دردشة نصية فقط. يلزم رابط خادم ينتهي بـ /chat لتعمل الردود", false);
        } else {
            statusView.setText("رابط الخادم محفوظ، جرّب إرسال رسالة");
        }
    }

    private void buildInterface() {
        Window window = getWindow();
        window.setStatusBarColor(Color.rgb(34, 48, 58));
        window.setNavigationBarColor(Color.rgb(34, 48, 58));
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setPadding(dp(14), dp(10), dp(14), dp(10));
        root.setBackgroundColor(Color.rgb(246, 246, 243));
        setContentView(root);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        TextView title = new TextView(this);
        title.setText("دردشة مهند");
        title.setTextSize(22);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(Color.rgb(27, 39, 48));
        header.addView(title, new LinearLayout.LayoutParams(0, dp(48), 1f));

        Button settingsButton = new Button(this);
        settingsButton.setText("الإعدادات");
        settingsButton.setOnClickListener(v -> showServerSettings());
        header.addView(settingsButton, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(48)));
        root.addView(header, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        statusView = new TextView(this);
        statusView.setTextSize(13);
        statusView.setTextColor(Color.rgb(88, 98, 104));
        statusView.setPadding(dp(2), dp(4), dp(2), dp(8));
        root.addView(statusView);

        scrollView = new ScrollView(this);
        scrollView.setFillViewport(false);
        messagesView = new LinearLayout(this);
        messagesView.setOrientation(LinearLayout.VERTICAL);
        messagesView.setPadding(dp(2), dp(4), dp(2), dp(8));
        scrollView.addView(messagesView, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));
        root.addView(scrollView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout composer = new LinearLayout(this);
        composer.setOrientation(LinearLayout.HORIZONTAL);
        composer.setGravity(Gravity.CENTER_VERTICAL);
        composer.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        messageInput = new EditText(this);
        messageInput.setHint("اكتب رسالتك...");
        messageInput.setTextSize(16);
        messageInput.setMinLines(1);
        messageInput.setMaxLines(4);
        messageInput.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        messageInput.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        messageInput.setPadding(dp(10), dp(6), dp(10), dp(6));
        messageInput.setBackground(roundBackground(Color.WHITE, dp(12)));
        composer.addView(messageInput, new LinearLayout.LayoutParams(0, dp(52), 1f));

        sendButton = new Button(this);
        sendButton.setText("إرسال");
        sendButton.setOnClickListener(v -> sendMessage());
        composer.addView(sendButton, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(52)));
        root.addView(composer, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
    }

    private void showServerSettings() {
        EditText addressInput = new EditText(this);
        addressInput.setSingleLine(true);
        addressInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        addressInput.setTextDirection(View.TEXT_DIRECTION_LTR);
        addressInput.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        addressInput.setHint("https://example.com/chat");
        addressInput.setText(chatUrl);
        addressInput.setPadding(dp(12), dp(8), dp(12), dp(8));

        new AlertDialog.Builder(this)
                .setTitle("رابط خادم الردود")
                .setMessage("أدخل رابط HTTPS لخادم الدردشة، وينتهي مساره بـ /chat. لا تضع مفتاح API هنا")
                .setView(addressInput)
                .setPositiveButton("حفظ", (dialog, which) -> {
                    String candidate = addressInput.getText().toString().trim();
                    if (!isValidChatUrl(candidate)) {
                        Toast.makeText(this,
                                "استخدم رابط HTTPS كامل ينتهي بـ /chat",
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    chatUrl = candidate;
                    getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                            .putString(URL_KEY, chatUrl).apply();
                    statusView.setText("رابط الخادم محفوظ، جرّب إرسال رسالة");
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private boolean isValidChatUrl(String value) {
        try {
            URI uri = new URI(value);
            return "https".equalsIgnoreCase(uri.getScheme())
                    && uri.getHost() != null
                    && uri.getPath() != null
                    && uri.getPath().endsWith("/chat");
        } catch (Exception ignored) {
            return false;
        }
    }

    private void sendMessage() {
        String text = messageInput.getText().toString().trim();
        if (text.isEmpty()) return;
        if (!isValidChatUrl(chatUrl)) {
            statusView.setText("أضف رابط خادم الدردشة من الإعدادات أولاً");
            return;
        }

        try {
            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            userMessage.put("content", text);
            history.put(userMessage);
            JSONObject body = new JSONObject();
            body.put("messages", history);
            String requestBody = body.toString();

            addBubble(text, true);
            messageInput.setText("");
            sendButton.setEnabled(false);
            statusView.setText("جارٍ إرسال الرسالة...");
            executor.execute(() -> {
                try {
                    String reply = postMessage(requestBody);
                    runOnUiThread(() -> {
                        JSONObject assistantMessage = new JSONObject();
                        try {
                            assistantMessage.put("role", "assistant");
                            assistantMessage.put("content", reply);
                            history.put(assistantMessage);
                        } catch (JSONException ignored) {
                        }
                        addBubble(reply, false);
                        statusView.setText("وصل الرد");
                        sendButton.setEnabled(true);
                    });
                } catch (Exception error) {
                    runOnUiThread(() -> {
                        statusView.setText("تعذّر الاتصال بخادم الردود، تحقق من الرابط وعمل الخادم");
                        sendButton.setEnabled(true);
                    });
                }
            });
        } catch (JSONException error) {
            statusView.setText("تعذّر تجهيز الرسالة");
        }
    }

    private String postMessage(String requestBody) throws Exception {
        HttpsURLConnection connection = (HttpsURLConnection) new URL(chatUrl).openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(20000);
        connection.setReadTimeout(90000);
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        connection.setRequestProperty("Accept", "application/json");

        byte[] payload = requestBody.getBytes(StandardCharsets.UTF_8);
        connection.setFixedLengthStreamingMode(payload.length);
        try (OutputStream output = connection.getOutputStream()) {
            output.write(payload);
        }

        int status = connection.getResponseCode();
        InputStream stream = status >= 200 && status < 300
                ? connection.getInputStream() : connection.getErrorStream();
        if (stream == null) {
            connection.disconnect();
            throw new IOException("No response body");
        }
        String responseText = readAll(stream);
        connection.disconnect();
        if (status < 200 || status >= 300) throw new IOException("HTTP " + status);

        String reply = new JSONObject(responseText).optString("reply", "").trim();
        if (reply.isEmpty()) throw new IOException("Empty reply");
        return reply;
    }

    private String readAll(InputStream stream) throws IOException {
        try (InputStream input = stream;
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private void addBubble(String text, boolean fromUser) {
        TextView bubble = new TextView(this);
        bubble.setText((fromUser ? "أنت: " : "مساعد: ") + text);
        bubble.setTextSize(16);
        bubble.setTextColor(Color.rgb(32, 42, 48));
        bubble.setPadding(dp(12), dp(9), dp(12), dp(9));
        bubble.setMaxWidth((int) (getResources().getDisplayMetrics().widthPixels * 0.82f));
        bubble.setBackground(roundBackground(
                fromUser ? Color.rgb(218, 238, 231) : Color.WHITE, dp(14)));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = fromUser ? Gravity.END : Gravity.START;
        params.setMargins(0, 0, 0, dp(8));
        messagesView.addView(bubble, params);
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }

    private GradientDrawable roundBackground(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        return drawable;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}
