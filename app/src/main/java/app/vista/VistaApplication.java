/* =========================================================
   VistaApplication.java  —  کلاس Application اپ
   مسیر: app/src/main/java/app/vista/VistaApplication.java
   =========================================================
   📌 کلاس Application سراسری اپ
   📌 مدیریت خطاهای سراسری
   📌 پاکسازی حافظه در بسته شدن اپ
   📌 فعال‌سازی WebView debug در حالت Debug
   ========================================================= */

package app.vista;

import android.app.Application;
import android.os.Build;
import android.util.Log;
import android.webkit.WebView;

import androidx.annotation.NonNull;

public class VistaApplication extends Application {

    private static final String TAG = "VistaApp";

    // ===== نسخه‌ی اپ =====
    public static final String APP_VERSION = "1.0.0";

    // ===== Reference استاتیک =====
    private static VistaApplication sInstance;

    @Override
    public void onCreate() {
        super.onCreate();

        sInstance = this;

        // ===== فعال‌سازی WebView debug در حالت Debug =====
        if (BuildConfig.DEBUG) {
            try {
                WebView.setWebContentsDebuggingEnabled(true);
                Log.d(TAG, "WebView debugging enabled");
            } catch (Exception e) {
                Log.e(TAG, "Error enabling WebView debugging: " + e.getMessage());
            }
        } else {
            try {
                WebView.setWebContentsDebuggingEnabled(false);
            } catch (Exception e) {
                // سکوت
            }
        }

        // ===== نصب هندلر خطای سراسری =====
        installGlobalExceptionHandler();

        Log.i(TAG, "Vista Application started — v" + APP_VERSION);
    }

    /**
     * نصب هندلر خطای سراسری
     * اگه اپ کرش کرد → لاگ بشه (در Release هم)
     */
    private void installGlobalExceptionHandler() {
        final Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(@NonNull Thread thread, @NonNull Throwable throwable) {
                try {
                    Log.e(TAG, "Uncaught exception on thread: " + thread.getName(), throwable);
                } catch (Exception e) {
                    // سکوت
                }

                // ===== پاکسازی WebView cache =====
                try {
                    PreloadManager.clear();
                } catch (Exception e) {
                    // سکوت
                }

                // ===== فراخوانی هندلر پیش‌فرض =====
                if (defaultHandler != null) {
                    defaultHandler.uncaughtException(thread, throwable);
                } else {
                    System.exit(1);
                }
            }
        });
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Log.w(TAG, "onLowMemory — clearing caches");

        // ===== پاکسازی WebView cache =====
        try {
            PreloadManager.clear();
        } catch (Exception e) {
            // سکوت
        }
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);

        // ===== اگه حافظه خیلی کم بود =====
        if (level >= TRIM_MEMORY_RUNNING_LOW) {
            Log.w(TAG, "onTrimMemory: " + level);
        }
    }

    /**
     * گرفتن instance سراسری
     */
    public static VistaApplication getInstance() {
        return sInstance;
    }
}
