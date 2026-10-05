/* =========================================================
   PreloadManager.java  —  مدیریت لود پس‌زمینه‌ی سایت
   مسیر: app/src/main/java/app/vista/PreloadManager.java
   =========================================================
   📌 یه WebView مخفی می‌سازه
   📌 سایت رو توی پس‌زمینه لود می‌کنه
   📌 وقتی onPageFinished شد → callback صدا زده می‌شه
   📌 ذخیره‌ی WebView در حافظه برای استفاده‌ی بعدی
   📌 فقط یک بار اجرا می‌شه
   ========================================================= */

package app.vista;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicBoolean;

public class PreloadManager {

    private static final String TAG = "PreloadManager";

    // ===== URL سایت =====
    private static final String BASE_URL = "https://rosha-24.ir/app/app1/";

    // ===== حداکثر انتظار (میلی‌ثانیه) =====
    private static final long TIMEOUT_MS = 8000L;

    // ===== WebView سراسری (برای استفاده‌ی دوباره) =====
    @SuppressLint("StaticFieldLeak")
    private static volatile WebView sCachedWebView = null;
    private static volatile boolean sIsLoaded = false;
    private static volatile String sLoadedUrl = null;

    // ===== Callback =====
    public interface PreloadCallback {
        void onPageLoaded();
        void onPageFailed();
    }

    /**
     * شروع preload سایت
     */
    public static void preload(@NonNull Context context, @NonNull PreloadCallback callback) {

        // ===== اگه قبلاً لود شده، فوری callback =====
        if (sIsLoaded && sCachedWebView != null) {
            Log.d(TAG, "Already loaded — callback immediately");
            callback.onPageLoaded();
            return;
        }

        Context appContext = context.getApplicationContext();
        Handler mainHandler = new Handler(Looper.getMainLooper());
        AtomicBoolean completed = new AtomicBoolean(false);

        // ===== ساخت WebView توی main thread =====
        mainHandler.post(() -> {

            try {
                // ===== ساخت WebView =====
                WebView webView = new WebView(appContext);
                setupWebView(webView);

                // ===== Reference برای client =====
                final WeakReference<Context> ctxRef = new WeakReference<>(appContext);

                // ===== Timeout =====
                final Runnable timeoutRunnable = () -> {
                    if (completed.compareAndSet(false, true)) {
                        Log.w(TAG, "Preload timeout");
                        callback.onPageFailed();
                    }
                };
                mainHandler.postDelayed(timeoutRunnable, TIMEOUT_MS);

                // ===== WebViewClient =====
                webView.setWebViewClient(new WebViewClient() {

                    @Override
                    public void onPageStarted(WebView view, String url, Bitmap favicon) {
                        super.onPageStarted(view, url, favicon);
                        Log.d(TAG, "onPageStarted: " + url);
                    }

                    @Override
                    public void onPageFinished(WebView view, String url) {
                        super.onPageFinished(view, url);
                        Log.d(TAG, "onPageFinished: " + url);

                        // ===== ذخیره‌ی WebView =====
                        sCachedWebView = view;
                        sIsLoaded = true;
                        sLoadedUrl = url;

                        // ===== حذف timeout =====
                        mainHandler.removeCallbacks(timeoutRunnable);

                        // ===== callback =====
                        if (completed.compareAndSet(false, true)) {
                            callback.onPageLoaded();
                        }
                    }

                    @Override
                    public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                        super.onReceivedError(view, request, error);

                        if (request.isForMainFrame()) {
                            Log.e(TAG, "onReceivedError (main frame): " + error.getDescription());
                            mainHandler.removeCallbacks(timeoutRunnable);
                            if (completed.compareAndSet(false, true)) {
                                callback.onPageFailed();
                            }
                        }
                    }

                    @Override
                    public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
                        super.onReceivedHttpError(view, request, errorResponse);

                        if (request.isForMainFrame()) {
                            int status = errorResponse != null ? errorResponse.getStatusCode() : -1;
                            Log.e(TAG, "onReceivedHttpError (main frame): " + status);

                            if (status >= 400) {
                                mainHandler.removeCallbacks(timeoutRunnable);
                                if (completed.compareAndSet(false, true)) {
                                    callback.onPageFailed();
                                }
                            }
                        }
                    }
                });

                // ===== لود سایت =====
                webView.loadUrl(BASE_URL);

            } catch (Exception e) {
                Log.e(TAG, "Error during preload: " + e.getMessage(), e);
                if (completed.compareAndSet(false, true)) {
                    callback.onPageFailed();
                }
            }
        });
    }

    /**
     * تنظیمات امن و بهینه‌ی WebView
     */
    @SuppressLint("SetJavaScriptEnabled")
    private static void setupWebView(WebView webView) {
        WebSettings settings = webView.getSettings();

        // ===== JavaScript =====
        settings.setJavaScriptEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);

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

        // ===== User Agent =====
        settings.setUserAgentString(
            settings.getUserAgentString() + " VistaApp/1.0"
        );

        // ===== امنیت =====
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setAllowFileAccessFromFileURLs(false);
        settings.setAllowUniversalAccessFromFileURLs(false);

        // ===== Mixed Content (غیرفعال) =====
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);

        // ===== Media =====
        settings.setMediaPlaybackRequiresUserGesture(false);

        // ===== Text Zoom (ثابت) =====
        settings.setTextZoom(100);

        // ===== پس‌زمینه =====
        webView.setBackgroundColor(0xFFF5F0EB);
    }

    /**
     * گرفتن WebView لود‌شده برای استفاده در MainActivity
     */
    public static WebView takeWebView() {
        WebView wv = sCachedWebView;
        sCachedWebView = null; // MainActivity مالک می‌شه
        return wv;
    }

    /**
     * چک: آیا سایت قبلاً لود شده؟
     */
    public static boolean isLoaded() {
        return sIsLoaded && sCachedWebView != null;
    }

    /**
     * URL لود‌شده
     */
    public static String getLoadedUrl() {
        return sLoadedUrl;
    }

    /**
     * پاک کردن WebView کش شده (برای پاکسازی حافظه)
     */
    public static void clear() {
        if (sCachedWebView != null) {
            try {
                sCachedWebView.stopLoading();
                sCachedWebView.loadUrl("about:blank");
                sCachedWebView.removeAllViews();
                sCachedWebView.destroy();
            } catch (Exception e) {
                Log.e(TAG, "Error clearing WebView: " + e.getMessage());
            }
            sCachedWebView = null;
        }
        sIsLoaded = false;
        sLoadedUrl = null;
    }

    /**
     * ریست کامل (برای logout یا خطا)
     */
    public static void reset() {
        clear();
    }
                              }
