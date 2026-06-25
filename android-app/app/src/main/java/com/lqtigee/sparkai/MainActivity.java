package com.lqtigee.sparkai;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.json.JSONObject;
import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoRuntimeSettings;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoView;

public class MainActivity extends Activity {
    private static final String PREFS_NAME = "lqtigee_app";
    private static final String SERVER_URL_KEY = "server_url";
    private static final String DEFAULT_SERVER_URL = "http://118.24.15.133:20261";
    private static final int APP_VERSION_CODE = 10;
    private static final String APP_VERSION_NAME = "0.2.1";

    private GeckoView geckoView;
    private GeckoRuntime geckoRuntime;
    private GeckoSession geckoSession;
    private ProgressBar progressBar;
    private LinearLayout offlinePanel;
    private SharedPreferences preferences;
    private Uri pendingInstallUri;
    private int lastPromptedUpdateVersionCode = APP_VERSION_CODE;
    private boolean updateCheckInFlight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        installCrashRecorder();
        try {
            configureSystemBars();
            buildLayout();
            configureGeckoView();
            loadServer();
        } catch (Throwable throwable) {
            showLaunchErrorPanel(throwable);
        }
    }

    @Override
    protected void onDestroy() {
        if (geckoSession != null) {
            geckoSession.close();
            geckoSession = null;
        }
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (pendingInstallUri != null && canInstallPackages()) {
            Uri installUri = pendingInstallUri;
            pendingInstallUri = null;
            installDownloadedApk(installUri);
        }
    }

    @Override
    public void onBackPressed() {
        if (geckoSession != null) {
            geckoSession.goBack();
            return;
        }
        super.onBackPressed();
    }

    private void buildLayout() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(246, 248, 251));

        geckoView = new GeckoView(this);
        root.addView(geckoView, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ));

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setVisibility(View.GONE);
        FrameLayout.LayoutParams progressParams = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(3)
        );
        progressParams.gravity = Gravity.TOP;
        root.addView(progressBar, progressParams);

        offlinePanel = buildOfflinePanel();
        offlinePanel.setVisibility(View.GONE);
        root.addView(offlinePanel, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ));

        setContentView(root);
    }

    private void configureGeckoView() {
        GeckoRuntimeSettings runtimeSettings = new GeckoRuntimeSettings.Builder()
            .javaScriptEnabled(true)
            .remoteDebuggingEnabled(false)
            .build();
        geckoRuntime = GeckoRuntime.create(this, runtimeSettings);
        geckoSession = new GeckoSession();
        geckoSession.open(geckoRuntime);
        geckoView.setSession(geckoSession);
    }

    private void loadServer() {
        String serverUrl = normalizeServerUrl(preferences.getString(SERVER_URL_KEY, DEFAULT_SERVER_URL));
        preferences.edit().putString(SERVER_URL_KEY, serverUrl).apply();
        if (!hasNetwork()) {
            showOfflinePanel();
            return;
        }
        offlinePanel.setVisibility(View.GONE);
        geckoView.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.VISIBLE);
        geckoSession.loadUri(serverUrl + "/sessions");
        geckoView.postDelayed(new Runnable() {
            @Override
            public void run() {
                progressBar.setVisibility(View.GONE);
            }
        }, 1600L);
        scheduleAppUpdateCheck(serverUrl);
    }

    private LinearLayout buildOfflinePanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setGravity(Gravity.CENTER);
        panel.setPadding(dp(24), dp(24), dp(24), dp(24));
        panel.setBackgroundColor(Color.rgb(246, 248, 251));

        TextView title = new TextView(this);
        title.setText("无法连接 Lqtigee");
        title.setTextColor(Color.rgb(18, 24, 38));
        title.setTextSize(22);
        title.setGravity(Gravity.CENTER);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        panel.addView(title, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        TextView message = new TextView(this);
        message.setText("确认服务器地址可访问，或切换到你映射好的 20261 地址。");
        message.setTextColor(Color.rgb(96, 112, 131));
        message.setTextSize(15);
        message.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams messageParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        messageParams.setMargins(0, dp(10), 0, dp(18));
        panel.addView(message, messageParams);

        Button retryButton = new Button(this);
        retryButton.setText("重试");
        retryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadServer();
            }
        });
        panel.addView(retryButton, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(48)
        ));

        Button serverButton = new Button(this);
        serverButton.setText("服务器地址");
        serverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showServerDialog();
            }
        });
        LinearLayout.LayoutParams serverParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(48)
        );
        serverParams.setMargins(0, dp(10), 0, 0);
        panel.addView(serverButton, serverParams);

        return panel;
    }

    private void configureSystemBars() {
        Window window = getWindow();
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.WHITE);
        if (Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false);
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
        }
    }

    private void scheduleAppUpdateCheck(String serverUrl) {
        geckoView.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    if (hasNetwork()) {
                        checkForAppUpdate(serverUrl);
                    }
                } catch (Exception exception) {
                    showToast("更新检查失败：" + firstPresent(exception.getMessage(), "启动异常"));
                }
            }
        }, 1800L);
    }

    private void checkForAppUpdate(String serverUrl) {
        synchronized (this) {
            if (updateCheckInFlight) {
                return;
            }
            updateCheckInFlight = true;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    AppUpdateInfo updateInfo = fetchAppUpdateInfo(serverUrl);
                    if (updateInfo.versionCode <= APP_VERSION_CODE || updateInfo.versionCode <= lastPromptedUpdateVersionCode) {
                        return;
                    }
                    lastPromptedUpdateVersionCode = updateInfo.versionCode;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showUpdateDialog(updateInfo);
                        }
                    });
                } catch (Exception exception) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showToast("更新检查失败：" + firstPresent(exception.getMessage(), "网络异常"));
                        }
                    });
                } finally {
                    synchronized (MainActivity.this) {
                        updateCheckInFlight = false;
                    }
                }
            }
        }).start();
    }

    private AppUpdateInfo fetchAppUpdateInfo(String serverUrl) throws Exception {
        URL url = new URL(serverUrl + "/downloads/app-version.json");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.setRequestMethod("GET");
        int statusCode = connection.getResponseCode();
        if (statusCode < 200 || statusCode >= 300) {
            throw new IllegalStateException("Update manifest returned " + statusCode);
        }
        String body = readUtf8(connection.getInputStream());
        JSONObject json = new JSONObject(body);
        return new AppUpdateInfo(
            json.getInt("versionCode"),
            json.optString("versionName", ""),
            absoluteUrl(serverUrl, json.getString("apkUrl")),
            json.optString("sha256", ""),
            json.optString("releaseNotes", "")
        );
    }

    private void showUpdateDialog(AppUpdateInfo updateInfo) {
        String message = "当前版本 " + APP_VERSION_NAME + "，发现新版本 " + updateInfo.versionName + "。";
        if (!isBlank(updateInfo.releaseNotes)) {
            message = message + "\n\n" + updateInfo.releaseNotes;
        }
        new AlertDialog.Builder(this)
            .setTitle("发现 Lqtigee 更新")
            .setMessage(message)
            .setNegativeButton("稍后", null)
            .setPositiveButton("下载更新", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int which) {
                    downloadAndInstallUpdate(updateInfo);
                }
            })
            .show();
    }

    private void downloadAndInstallUpdate(AppUpdateInfo updateInfo) {
        DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        if (downloadManager == null) {
            showToast("系统下载服务不可用");
            return;
        }
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(updateInfo.apkUrl));
        request.setTitle("Lqtigee " + updateInfo.versionName);
        request.setDescription("正在下载 Lqtigee 更新");
        request.setMimeType("application/vnd.android.package-archive");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalFilesDir(
            this,
            Environment.DIRECTORY_DOWNLOADS,
            "Lqtigee-" + updateInfo.versionCode + ".apk"
        );
        long downloadId = downloadManager.enqueue(request);
        showToast("已开始下载更新");
        waitForDownload(downloadManager, downloadId, updateInfo);
    }

    private void waitForDownload(DownloadManager downloadManager, long downloadId, AppUpdateInfo updateInfo) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean waiting = true;
                while (waiting) {
                    DownloadManager.Query query = new DownloadManager.Query().setFilterById(downloadId);
                    try (Cursor cursor = downloadManager.query(query)) {
                        if (cursor != null && cursor.moveToFirst()) {
                            int status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                                waiting = false;
                                Uri downloadedUri = downloadManager.getUriForDownloadedFile(downloadId);
                                if (downloadedUri == null) {
                                    notifyUpdateFailed("更新文件读取失败");
                                    return;
                                }
                                if (!verifyDownloadedApk(downloadedUri, updateInfo.sha256)) {
                                    notifyUpdateFailed("更新包校验失败");
                                    return;
                                }
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        installDownloadedApk(downloadedUri);
                                    }
                                });
                            } else if (status == DownloadManager.STATUS_FAILED) {
                                waiting = false;
                                notifyUpdateFailed("更新下载失败");
                            }
                        }
                    } catch (Exception exception) {
                        waiting = false;
                        notifyUpdateFailed("更新下载异常");
                    }
                    if (waiting) {
                        try {
                            Thread.sleep(1000L);
                        } catch (InterruptedException exception) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                }
            }
        }).start();
    }

    private void installDownloadedApk(Uri apkUri) {
        if (!canInstallPackages()) {
            pendingInstallUri = apkUri;
            new AlertDialog.Builder(this)
                .setTitle("允许安装更新")
                .setMessage("Android 需要先允许 Lqtigee 安装未知来源应用，开启后返回即可继续安装。")
                .setPositiveButton("去开启", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        startActivity(intent);
                    }
                })
                .setNegativeButton("取消", null)
                .show();
            return;
        }
        Intent installIntent = new Intent(Intent.ACTION_VIEW);
        installIntent.setDataAndType(apkUri, "application/vnd.android.package-archive");
        installIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        installIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(installIntent);
        } catch (ActivityNotFoundException exception) {
            showToast("没有找到系统安装器");
        } catch (SecurityException exception) {
            showToast("安装权限不足，请允许 Lqtigee 安装更新");
        } catch (Exception exception) {
            showToast("打开安装器失败：" + firstPresent(exception.getMessage(), "系统异常"));
        }
    }

    private void showServerDialog() {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_URI);
        input.setImeOptions(EditorInfo.IME_ACTION_DONE);
        input.setText(preferences.getString(SERVER_URL_KEY, DEFAULT_SERVER_URL));
        input.setSelectAllOnFocus(true);

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("服务器地址")
            .setMessage("填写映射到 Java 服务 20261 的地址。")
            .setView(input)
            .setNegativeButton("取消", null)
            .setPositiveButton("保存", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface d, int which) {
                    String serverUrl = normalizeServerUrl(input.getText().toString());
                    preferences.edit().putString(SERVER_URL_KEY, serverUrl).apply();
                    loadServer();
                }
            })
            .create();
        input.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
                boolean done = actionId == EditorInfo.IME_ACTION_DONE
                    || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER);
                if (!done) {
                    return false;
                }
                String serverUrl = normalizeServerUrl(input.getText().toString());
                preferences.edit().putString(SERVER_URL_KEY, serverUrl).apply();
                dialog.dismiss();
                loadServer();
                return true;
            }
        });
        dialog.show();
    }

    private void showOfflinePanel() {
        progressBar.setVisibility(View.GONE);
        geckoView.setVisibility(View.GONE);
        offlinePanel.setVisibility(View.VISIBLE);
    }

    private boolean hasNetwork() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null || connectivityManager.getActiveNetwork() == null) {
            return false;
        }
        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
        return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    private boolean canInstallPackages() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.O || getPackageManager().canRequestPackageInstalls();
    }

    private boolean verifyDownloadedApk(Uri apkUri, String expectedSha256) {
        if (isBlank(expectedSha256)) {
            return true;
        }
        try (InputStream inputStream = getContentResolver().openInputStream(apkUri)) {
            if (inputStream == null) {
                return false;
            }
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            return expectedSha256.equalsIgnoreCase(toHex(digest.digest()));
        } catch (Exception exception) {
            return false;
        }
    }

    private String readUtf8(InputStream inputStream) throws Exception {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }
        return builder.toString();
    }

    private String absoluteUrl(String serverUrl, String value) {
        if (value.startsWith("http://") || value.startsWith("https://")) {
            return value;
        }
        if (value.startsWith("/")) {
            return serverUrl + value;
        }
        return serverUrl + "/" + value;
    }

    private String normalizeServerUrl(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            return DEFAULT_SERVER_URL;
        }
        String withScheme = trimmed.startsWith("http://") || trimmed.startsWith("https://")
            ? trimmed
            : "http://" + trimmed;
        while (withScheme.endsWith("/")) {
            withScheme = withScheme.substring(0, withScheme.length() - 1);
        }
        return withScheme;
    }

    private String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format("%02x", value));
        }
        return builder.toString();
    }

    private void notifyUpdateFailed(String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                showToast(message);
            }
        });
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void installCrashRecorder() {
        Thread.UncaughtExceptionHandler previousHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                try {
                    getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                        .edit()
                        .putString("last_crash", stackTraceText(throwable))
                        .apply();
                } catch (Exception ignored) {
                }
                if (previousHandler != null) {
                    previousHandler.uncaughtException(thread, throwable);
                }
            }
        });
    }

    private void showLaunchErrorPanel(Throwable throwable) {
        String serverUrl = normalizeServerUrl(preferences == null ? DEFAULT_SERVER_URL : preferences.getString(SERVER_URL_KEY, DEFAULT_SERVER_URL));
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setGravity(Gravity.CENTER);
        panel.setPadding(dp(22), dp(22), dp(22), dp(22));
        panel.setBackgroundColor(Color.rgb(246, 248, 251));

        TextView title = new TextView(this);
        title.setText("Lqtigee 启动失败");
        title.setTextColor(Color.rgb(18, 24, 38));
        title.setTextSize(22);
        title.setGravity(Gravity.CENTER);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        panel.addView(title, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        TextView details = new TextView(this);
        details.setText(stackTraceText(throwable));
        details.setTextColor(Color.rgb(39, 53, 70));
        details.setTextSize(11);
        details.setPadding(dp(12), dp(12), dp(12), dp(12));
        details.setBackgroundColor(Color.WHITE);
        details.setTextIsSelectable(true);
        LinearLayout.LayoutParams detailsParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            0,
            1f
        );
        detailsParams.setMargins(0, dp(12), 0, dp(12));
        panel.addView(details, detailsParams);

        Button browserButton = new Button(this);
        browserButton.setText("用浏览器打开");
        browserButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openExternalBrowser(serverUrl + "/sessions");
            }
        });
        panel.addView(browserButton, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(48)
        ));

        setContentView(panel);
    }

    private void openExternalBrowser(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception exception) {
            showToast("无法打开浏览器：" + firstPresent(exception.getMessage(), "系统异常"));
        }
    }

    private String stackTraceText(Throwable throwable) {
        if (throwable == null) {
            return "Unknown launch error";
        }
        StringWriter writer = new StringWriter();
        throwable.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }

    private String firstPresent(String value, String fallback) {
        return isBlank(value) ? fallback : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static class AppUpdateInfo {
        final int versionCode;
        final String versionName;
        final String apkUrl;
        final String sha256;
        final String releaseNotes;

        AppUpdateInfo(int versionCode, String versionName, String apkUrl, String sha256, String releaseNotes) {
            this.versionCode = versionCode;
            this.versionName = versionName;
            this.apkUrl = apkUrl;
            this.sha256 = sha256;
            this.releaseNotes = releaseNotes;
        }
    }
}
