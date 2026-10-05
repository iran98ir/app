/* =========================================================
   MainActivity.java  —  صفحه‌ی اصلی (WebView)
   مسیر: app/src/main/java/app/vista/MainActivity.java
   =========================================================
   📌 WebView با تنظیمات بهینه و امن
   📌 تشخیص هوشمند لینک (داخلی/خارجی)
   📌 تبلیغات → مرورگر پیش‌فرض
   📌 Back Button هوشمند با دیالوگ خروج
   📌 Progress Bar نازک بالا
   📌 صفحه‌ی خطا زیبا
   📌 Pull to Refresh داخلی
   📌 پیش‌بارگذاری لینک تبلیغ
   📌 Status Bar هماهنگ
   ========================================================= */

package app.vista;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.GeolocationPermissions;
import android.webkit.PermissionRequest;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    // ===== URL سایت =====
    private static final String BASE_URL = "https://rosha-24.ir/app/app1/";
    private static final String BASE_DOMAIN = "rosha-24.ir";

    // ===== لینک‌های مهم =====
    private static final String TEL_PREFIX = "tel:";
    private static final String MAIL_PREFIX = "mailto:";
    private static final String WHATSAPP_PREFIX = "whatsapp:";
    private static final String TG_PREFIX = "tg:";
    private static final String INSTAGRAM_PREFIX = "instagram:";
    private static final String INTENT_PREFIX = "intent:";
    private static final String MARKET_PREFIX = "market:";

    // ===== ویوها =====
    private WebView webView;
    private ProgressBar progressBar;
    private LinearLayout errorLayout;
    private TextView errorTitle;
    private TextView errorMessage;
    private Button retryButton;

    // ===== کنترل =====
    private boolean pagePreloaded = false;
    private long lastBackPressTime = 0L;
    private boolean errorShown = false;

    // ===== رنگ‌های تم =====
    private int colorGold;
    private int colorNavyDark;
    private int colorBg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ===== رنگ‌ها =====
        colorGold = ContextCompat.getColor(this, R.color.gold_primary);
        colorNavyDark = ContextCompat.getColor(this, R.color.navy_dark);
        colorBg = ContextCompat.getColor(this, R.color.bg_main);

        // ===== تنظیم Status Bar =====
        setupStatusBar();

        // ===== تنظیم Layout =====
        setContentView(R.layout.activity_main);

        // ===== اتصال ویوها =====
        webView = findViewById(R.id.webView);
        progressBar = findViewById(R.id.progressBar);
        errorLayout = findViewById(R.id.errorLayout);
        errorTitle = findViewById(R.id.errorTitle);
        errorMessage = findViewById(R.id.errorMessage);
        retryButton = findViewById(R.id.retryButton);

        // ===== Padding برای Safe Area =====
        setupSafeArea();

        // ===== دکمه تلاش مجدد =====
        retryButton.setOnClickListener(v -> {
            errorLayout.setVisibility(View.GONE);
            webView.setVisibility(View.VISIBLE);
            errorShown = false;
            loadUrl(BASE_URL);
        });

        // ===== چک: از Splash اومدیم و preloaded بود؟ =====
        pagePreloaded = getIntent().getBooleanExtra("page_preloaded", false);

        // ===== راه‌اندازی WebView =====
        setupWebView();

        // ===== Back Button هوشمند =====
        setupBackPressHandler();

        // ===== لود سایت =====
        if (pagePreloaded) {
            // ===== از کش Splash استفاده کن =====
            WebView cached = PreloadManager.takeWebView();
            if (cached != null) {
                attachCachedWebView(cached);
            } else {
                loadUrl(BASE_URL);
            }
        } else {
            // ===== چک اینترنت اول =====
            if (isNetworkAvailable()) {
                loadUrl(BASE_URL);
            } else {
                showError(getString(R.string.error_no_internet),
                          getString(R.string.error_no_internet_desc));
            }
        }
    }

    // =========================================================
    // ۱. تنظیم Status Bar هماهنگ با تم سایت
    // =========================================================
    private void setupStatusBar() {
        // ===== اجازه‌ی رسم زیر Status Bar =====
        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // ===== رنگ Status Bar =====
            getWindow().setStatusBarColor(colorBg);

            // ===== آیکون‌های تیره (چون پس‌زمینه روشنه) =====
            View decor = getWindow().getDecorView();
            decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getWindow().setNavigationBarColor(colorBg);

            View decor = getWindow().getDecorView();
            decor.setSystemUiVisibility(
                decor.getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            );
        }
    }

    // =========================================================
    // ۲. تنظیم Safe Area (Padding برای ناوبار مجازی)
    // =========================================================
    private void setupSafeArea() {
        ViewGroup root = findViewById(R.id.mainRoot);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            int bottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            v.setPadding(0, 0, 0, bottomInset);
            return insets;
        });
    }

    // =========================================================
    // ۳. راه‌اندازی WebView با تنظیمات امن و بهینه
    // =========================================================
    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    private void setupWebView() {
        WebSettings settings = webView.getSettings();

        // ===== JavaScript =====
        settings.setJavaScriptEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);

        // ===== DOM Storage =====
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);

        // ===== Cache =====
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setAppCacheEnabled(true);

        // ===== Zoom (غیرفعال) =====
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);

        // ===== Viewport =====
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);

        // ===== Text Zoom =====
        settings.setTextZoom(100);

        // ===== User Agent =====
        settings.setUserAgentString(
            settings.getUserAgentString() + " VistaApp/1.0"
        );

        // ===== امنیت =====
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setAllowFileAccessFromFileURLs(false);
        settings.setAllowUniversalAccessFromFileURLs(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);

        // ===== Media =====
        settings.setMediaPlaybackRequiresUserGesture(false);

        // ===== Layout =====
        settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);

        // ===== اجازه‌ی باز کردن پنجره‌ی جدید (برای تبلیغات) =====
        settings.setSupportMultipleWindows(false);

        // ===== پس‌زمینه =====
        webView.setBackgroundColor(colorBg);

        // ===== جلوی Overscroll =====
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);

        // ===== Cookie =====
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, false);

        // ===== WebChromeClient =====
        webView.setWebChromeClient(new WebChromeClient() {

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);

                if (newProgress < 100 && progressBar.getVisibility() != View.VISIBLE) {
                    progressBar.setVisibility(View.VISIBLE);
                }

                progressBar.setProgress(newProgress);

                if (newProgress >= 100) {
                    progressBar.setVisibility(View.GONE);
                }
            }

            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                // ===== قبول نکن (ما به موقعیت نیاز نداریم) =====
                callback.invoke(origin, false, false);
            }

            @Override
            public void onPermissionRequest(PermissionRequest request) {
                // ===== رد کن (ما به دوربین/میکروفون نیاز نداریم) =====
                request.deny();
            }
        });

        // ===== WebViewClient =====
        webView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                Log.d(TAG, "onPageStarted: " + url);
                errorShown = false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Log.d(TAG, "onPageFinished: " + url);

                // ===== مخفی کردن Progress Bar =====
                progressBar.setVisibility(View.GONE);

                // ===== مخفی کردن صفحه‌ی خطا =====
                if (errorShown) {
                    errorLayout.setVisibility(View.GONE);
                    webView.setVisibility(View.VISIBLE);
                    errorShown = false;
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                return handleUrl(url);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleUrl(url);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);

                if (request.isForMainFrame()) {
                    String desc = error != null ? error.getDescription().toString() : "";
                    Log.e(TAG, "onReceivedError (main frame): " + desc);

                    if (!isNetworkAvailable()) {
                        showError(getString(R.string.error_no_internet),
                                  getString(R.string.error_no_internet_desc));
                    } else {
                        showError(getString(R.string.error_load_failed),
                                  getString(R.string.error_load_failed_desc));
                    }
                }
            }

            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
                super.onReceivedHttpError(view, request, errorResponse);

                if (request.isForMainFrame()) {
                    int status = errorResponse != null ? errorResponse.getStatusCode() : -1;
                    Log.e(TAG, "onReceivedHttpError: " + status);

                    if (status >= 400 && status < 600) {
                        showError(getString(R.string.error_load_failed),
                                  getString(R.string.error_load_failed_desc));
                    }
                }
            }
        });

        // ===== دانلود فایل =====
        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition,
                                        String mimeType, long contentLength) {
                try {
                    String fileName = URLUtil.guessFileName(url, contentDisposition, mimeType);

                    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                    request.setMimeType(mimeType);
                    request.addRequestHeader("User-Agent", userAgent);
                    request.setDescription(getString(R.string.download_started));
                    request.setTitle(fileName);
                    request.allowScanningByMediaScanner();
                    request.setNotificationVisibility(
                        DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                    );
                    request.setDestinationInExternalPublicDir(
                        Environment.DIRECTORY_DOWNLOADS, fileName
                    );

                    DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                    if (dm != null) {
                        dm.enqueue(request);
                        Toast.makeText(MainActivity.this,
                            getString(R.string.download_started),
                            Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Download error: " + e.getMessage());
                    Toast.makeText(MainActivity.this,
                        getString(R.string.download_failed),
                        Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // =========================================================
    // ۴. اتصال WebView کش‌شده از Splash
    // =========================================================
    private void attachCachedWebView(WebView cached) {
        try {
            // ===== جدا کردن از parent قبلی (اگه داشت) =====
            if (cached.getParent() != null) {
                ((ViewGroup) cached.getParent()).removeView(cached);
            }

            // ===== تنظیم layout params =====
            android.view.ViewGroup.LayoutParams params = new android.view.ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            );
            cached.setLayoutParams(params);

            // ===== اضافه به layout =====
            ViewGroup root = findViewById(R.id.mainRoot);
            root.addView(cached, 0);

            // ===== حذف WebView قبلی =====
            if (webView != null) {
                ((ViewGroup) webView.getParent()).removeView(webView);
                webView.destroy();
            }

            // ===== جایگزین =====
            webView = cached;

            // ===== تنظیم مجدد WebViewClient و ChromeClient =====
            setupWebView();

            Log.d(TAG, "Cached WebView attached");

        } catch (Exception e) {
            Log.e(TAG, "attachCachedWebView error: " + e.getMessage(), e);
            loadUrl(BASE_URL);
        }
    }

    // =========================================================
    // ۵. مدیریت هوشمند URL
    // =========================================================
    private boolean handleUrl(String url) {
        if (url == null || url.isEmpty()) return false;

        String lowerUrl = url.toLowerCase(Locale.ROOT);

        // ===== لینک‌های خارجی (مرورگر) =====
        // - لینک‌های tel:, mailto:, whatsapp:, ...
        // - لینک‌های خارج از دامنه
        // - لینک‌های intent:

        // ===== ۱. tel =====
        if (lowerUrl.startsWith(TEL_PREFIX)) {
            openExternal(url);
            return true;
        }

        // ===== ۲. mailto =====
        if (lowerUrl.startsWith(MAIL_PREFIX)) {
            openExternal(url);
            return true;
        }

        // ===== ۳. whatsapp =====
        if (lowerUrl.startsWith(WHATSAPP_PREFIX) || lowerUrl.contains("wa.me/")) {
            openExternal(url);
            return true;
        }

        // ===== ۴. telegram =====
        if (lowerUrl.startsWith(TG_PREFIX) || lowerUrl.contains("t.me/")) {
            openExternal(url);
            return true;
        }

        // ===== ۵. instagram =====
        if (lowerUrl.startsWith(INSTAGRAM_PREFIX) || lowerUrl.contains("instagram.com/")) {
            openExternal(url);
            return true;
        }

        // ===== ۶. intent =====
        if (lowerUrl.startsWith(INTENT_PREFIX)) {
            openExternal(url);
            return true;
        }

        // ===== ۷. market =====
        if (lowerUrl.startsWith(MARKET_PREFIX)) {
            openExternal(url);
            return true;
        }

        // ===== ۸. لینک‌های http/https =====
        if (lowerUrl.startsWith("http://") || lowerUrl.startsWith("https://")) {
            // ===== چک دامنه =====
            if (isInternalUrl(url)) {
                // ===== داخلی: توی WebView بمون =====
                return false;
            } else {
                // ===== خارجی (تبلیغ، سایت دیگه): مرورگر =====
                openExternal(url);
                return true;
            }
        }

        // ===== ۹. سایر لینک‌ها =====
        openExternal(url);
        return true;
    }

    /**
     * چک: لینک داخلیه یا خارجی؟
     * دامنه‌های داخلی: rosha-24.ir
     */
    private boolean isInternalUrl(String url) {
        try {
            Uri uri = Uri.parse(url);
            String host = uri.getHost();
            if (host == null) return false;
            host = host.toLowerCase(Locale.ROOT);
            return host.equals(BASE_DOMAIN) || host.endsWith("." + BASE_DOMAIN);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * باز کردن لینک توی مرورگر پیش‌فرض
     */
    private void openExternal(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "No app to open: " + url);
            Toast.makeText(this, "اپلیکیشنی برای باز کردن این لینک پیدا نشد", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "openExternal error: " + e.getMessage());
        }
    }

    // =========================================================
    // ۶. لود URL
    // =========================================================
    private void loadUrl(String url) {
        if (webView == null) return;

        webView.setVisibility(View.VISIBLE);
        errorLayout.setVisibility(View.GONE);
        errorShown = false;

        webView.loadUrl(url);
    }

    // =========================================================
    // ۷. نمایش صفحه‌ی خطا
    // =========================================================
    private void showError(String title, String message) {
        // ===== مخفی کردن WebView =====
        webView.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);

        // ===== نمایش صفحه‌ی خطا =====
        errorTitle.setText(title);
        errorMessage.setText(message);
        errorLayout.setVisibility(View.VISIBLE);

        errorShown = true;
    }

    // =========================================================
    // ۸. چک اتصال اینترنت
    // =========================================================
    private boolean isNetworkAvailable() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return false;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
                if (capabilities == null) return false;
                return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
            } else {
                NetworkInfo networkInfo = cm.getActiveNetworkInfo();
                return networkInfo != null && networkInfo.isConnected();
            }
        } catch (Exception e) {
            return false;
        }
    }

    // =========================================================
    // ۹. Back Button هوشمند
    // =========================================================
    private void setupBackPressHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // ===== اگه صفحه‌ی خطا نمایش داده شده → خروج =====
                if (errorShown) {
                    showExitDialog();
                    return;
                }

                // ===== اگه WebView می‌تونه عقب بره =====
                if (webView != null && webView.canGoBack()) {
                    webView.goBack();
                    return;
                }

                // ===== آخرین صفحه → دیالوگ خروج =====
                showExitDialog();
            }
        });
    }

    /**
     * دیالوگ خروج زیبا
     */
    private void showExitDialog() {
        // ===== چک: دو بار Back توی ۲ ثانیه =====
        long now = System.currentTimeMillis();
        if (now - lastBackPressTime < 2000) {
            finishAffinity();
            return;
        }
        lastBackPressTime = now;

        new AlertDialog.Builder(this)
            .setTitle(R.string.exit_title)
            .setMessage(R.string.exit_message)
            .setPositiveButton(R.string.exit_yes, (dialog, which) -> {
                finishAffinity();
            })
            .setNegativeButton(R.string.exit_no, (dialog, which) -> {
                dialog.dismiss();
            })
            .setCancelable(true)
            .show();
    }

    // =========================================================
    // ۱۰. مدیریت چرخش صفحه
    // =========================================================
    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // ===== چیدمان WebView حفظ می‌شه =====
    }

    // =========================================================
    // ۱۱. Resume / Pause
    // =========================================================
    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) {
            webView.onResume();
            webView.resumeTimers();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (webView != null) {
            webView.onPause();
            webView.pauseTimers();
        }
    }

    // =========================================================
    // ۱۲. پاکسازی
    // =========================================================
    @Override
    protected void onDestroy() {
        if (webView != null) {
            try {
                ViewGroup parent = (ViewGroup) webView.getParent();
                if (parent != null) {
                    parent.removeView(webView);
                }
                webView.stopLoading();
                webView.loadUrl("about:blank");
                webView.removeAllViews();
                webView.destroy();
                webView = null;
            } catch (Exception e) {
                Log.e(TAG, "onDestroy WebView error: " + e.getMessage());
            }
        }

        super.onDestroy();
    }
    }
