package com.timeforge.simple;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    public static final String CHANNEL_ID = "timeforge_reminders";
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(android.graphics.Color.rgb(9, 13, 24));
        getWindow().setNavigationBarColor(android.graphics.Color.rgb(9, 13, 24));
        createNotificationChannel();
        requestNotificationPermissionIfNeeded();

        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setMediaPlaybackRequiresUserGesture(false);

        webView.setWebViewClient(new WebViewClient());
        webView.addJavascriptInterface(new AndroidBridge(this), "TimeForgeAndroid");
        webView.loadUrl("file:///android_asset/index.html");
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1001);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "TimeForge 提醒",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("TimeForge 的任务、日程与重要日子提醒");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    public static class AndroidBridge {
        private final Context context;
        private final SharedPreferences prefs;

        AndroidBridge(Context context) {
            this.context = context;
            this.prefs = context.getSharedPreferences("timeforge_data", Context.MODE_PRIVATE);
        }

        @JavascriptInterface
        public String loadData() {
            return prefs.getString("app_data", "{}");
        }

        @JavascriptInterface
        public void saveData(String json) {
            prefs.edit().putString("app_data", json).apply();
        }

        @JavascriptInterface
        public void toast(String message) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }

        @JavascriptInterface
        public void scheduleReminder(String title, String message, long triggerAtMillis) {
            if (triggerAtMillis <= System.currentTimeMillis()) return;
            Intent intent = new Intent(context, AlarmReceiver.class);
            intent.putExtra("title", title == null || title.trim().isEmpty() ? "TimeForge 提醒" : title);
            intent.putExtra("message", message == null || message.trim().isEmpty() ? "你的任务马上开始～" : message);
            int requestCode = (int) (triggerAtMillis % Integer.MAX_VALUE);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager == null) return;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            }
        }

        @JavascriptInterface
        public void shareImage(String dataUrl) {
            try {
                String base64 = dataUrl.substring(dataUrl.indexOf(',') + 1);
                byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
                File file = writeSharedFile("timeforge_heatmap.png", bytes);
                Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
                Intent share = new Intent(Intent.ACTION_SEND);
                share.setType("image/png");
                share.putExtra(Intent.EXTRA_STREAM, uri);
                share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                Intent chooser = Intent.createChooser(share, "分享热力图");
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(chooser);
            } catch (Exception e) {
                Toast.makeText(context, "导出失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }

        @JavascriptInterface
        public void shareTextFile(String filename, String content) {
            try {
                if (filename == null || filename.trim().isEmpty()) filename = "timeforge-backup.json";
                byte[] bytes = content == null ? new byte[0] : content.getBytes(StandardCharsets.UTF_8);
                File file = writeSharedFile(filename, bytes);
                Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
                Intent share = new Intent(Intent.ACTION_SEND);
                share.setType("application/json");
                share.putExtra(Intent.EXTRA_STREAM, uri);
                share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                Intent chooser = Intent.createChooser(share, "导出 TimeForge 备份");
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(chooser);
            } catch (Exception e) {
                Toast.makeText(context, "导出失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }

        private File writeSharedFile(String filename, byte[] bytes) throws Exception {
            File dir = new File(context.getCacheDir(), "shared");
            if (!dir.exists()) dir.mkdirs();
            File file = new File(dir, filename);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(bytes);
            fos.close();
            return file;
        }
    }
}
