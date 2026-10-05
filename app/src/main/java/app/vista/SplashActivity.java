/* =========================================================
   SplashActivity.java  —  صفحه‌ی Splash
   مسیر: app/src/main/java/app/vista/SplashActivity.java
   =========================================================
   📌 صفحه‌ی خوش‌آمد با انیمیشن نرم
   📌 لود پس‌زمینه‌ی سایت در همین حین
   📌 وقتی سایت لود شد → می‌ره MainActivity
   📌 حداقل نمایش: 1.5 ثانیه
   📌 حداکثر انتظار: 8 ثانیه (بعدش می‌ره MainActivity)
   ========================================================= */

package app.vista;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import java.util.concurrent.atomic.AtomicBoolean;

public class SplashActivity extends AppCompatActivity {

    // ===== زمان‌ها (میلی‌ثانیه) =====
    private static final long MIN_DISPLAY_TIME = 1500L; // حداقل نمایش Splash
    private static final long MAX_WAIT_TIME    = 8000L; // حداکثر انتظار برای لود سایت

    // ===== ویوها =====
    private LinearLayout splashContent;
    private ImageView splashLogo;
    private TextView splashAppName;
    private View splashDivider;
    private TextView splashWelcome;
    private LinearLayout splashLoading;
    private ProgressBar splashProgress;
    private TextView splashLoadingText;

    // ===== کنترل =====
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final AtomicBoolean hasNavigated = new AtomicBoolean(false);
    private long splashStartTime = 0L;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // ===== نصب Splash Screen API برای اندروید 12+ =====
        SplashScreen.installSplashScreen(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // ===== اتصال ویوها =====
        splashContent     = findViewById(R.id.splashContent);
        splashLogo        = findViewById(R.id.splashLogo);
        splashAppName     = findViewById(R.id.splashAppName);
        splashDivider     = findViewById(R.id.splashDivider);
        splashWelcome     = findViewById(R.id.splashWelcome);
        splashLoading     = findViewById(R.id.splashLoading);
        splashProgress    = findViewById(R.id.splashProgress);
        splashLoadingText = findViewById(R.id.splashLoadingText);

        // ===== شروع زمان =====
        splashStartTime = System.currentTimeMillis();

        // ===== مخفی کردن اولیه برای انیمیشن =====
        setInitialAlphaZero();

        // ===== اجرای انیمیشن ورود =====
        playEntranceAnimation();

        // ===== شروع لود پس‌زمینه سایت =====
        startBackgroundPreload();

        // ===== تایمر حداکثر انتظار =====
        handler.postDelayed(() -> navigateToMain(false), MAX_WAIT_TIME);
    }

    /**
     * مخفی کردن همه‌ی عناصر برای شروع انیمیشن
     */
    private void setInitialAlphaZero() {
        splashLogo.setAlpha(0f);
        splashAppName.setAlpha(0f);
        splashDivider.setAlpha(0f);
        splashWelcome.setAlpha(0f);
        splashLoading.setAlpha(0f);
        splashContent.setScaleX(0.9f);
        splashContent.setScaleY(0.9f);
    }

    /**
     * اجرای انیمیشن ورود نرم
     * - لوگو با scale + fade
     * - نام اپ با fade + slide up
     * - خط جداکننده با scaleX
     * - متن خوش‌آمد با fade
     * - لودینگ با fade
     */
    private void playEntranceAnimation() {

        // ===== ۱. لوگو: scale + fade =====
        ObjectAnimator logoAlpha = ObjectAnimator.ofFloat(splashLogo, View.ALPHA, 0f, 1f);
        logoAlpha.setDuration(700);
        logoAlpha.setInterpolator(new DecelerateInterpolator());

        ObjectAnimator logoScaleX = ObjectAnimator.ofFloat(splashLogo, View.SCALE_X, 0.85f, 1f);
        logoScaleX.setDuration(700);
        logoScaleX.setInterpolator(new DecelerateInterpolator());

        ObjectAnimator logoScaleY = ObjectAnimator.ofFloat(splashLogo, View.SCALE_Y, 0.85f, 1f);
        logoScaleY.setDuration(700);
        logoScaleY.setInterpolator(new DecelerateInterpolator());

        // ===== ۲. نام اپ: fade + slide up =====
        ObjectAnimator nameAlpha = ObjectAnimator.ofFloat(splashAppName, View.ALPHA, 0f, 1f);
        nameAlpha.setDuration(600);
        nameAlpha.setStartDelay(250);
        nameAlpha.setInterpolator(new DecelerateInterpolator());

        ObjectAnimator nameTranslateY = ObjectAnimator.ofFloat(splashAppName, View.TRANSLATION_Y, 20f, 0f);
        nameTranslateY.setDuration(600);
        nameTranslateY.setStartDelay(250);
        nameTranslateY.setInterpolator(new DecelerateInterpolator());

        // ===== ۳. خط جداکننده: scaleX =====
        ObjectAnimator dividerScaleX = ObjectAnimator.ofFloat(splashDivider, View.SCALE_X, 0f, 1f);
        dividerScaleX.setDuration(500);
        dividerScaleX.setStartDelay(400);
        dividerScaleX.setInterpolator(new DecelerateInterpolator());

        ObjectAnimator dividerAlpha = ObjectAnimator.ofFloat(splashDivider, View.ALPHA, 0f, 0.6f);
        dividerAlpha.setDuration(500);
        dividerAlpha.setStartDelay(400);

        // ===== ۴. متن خوش‌آمد: fade =====
        ObjectAnimator welcomeAlpha = ObjectAnimator.ofFloat(splashWelcome, View.ALPHA, 0f, 0.75f);
        welcomeAlpha.setDuration(600);
        welcomeAlpha.setStartDelay(550);
        welcomeAlpha.setInterpolator(new DecelerateInterpolator());

        ObjectAnimator welcomeTranslateY = ObjectAnimator.ofFloat(splashWelcome, View.TRANSLATION_Y, 12f, 0f);
        welcomeTranslateY.setDuration(600);
        welcomeTranslateY.setStartDelay(550);
        welcomeTranslateY.setInterpolator(new DecelerateInterpolator());

        // ===== ۵. لودینگ: fade =====
        ObjectAnimator loadingAlpha = ObjectAnimator.ofFloat(splashLoading, View.ALPHA, 0f, 1f);
        loadingAlpha.setDuration(500);
        loadingAlpha.setStartDelay(700);
        loadingAlpha.setInterpolator(new DecelerateInterpolator());

        // ===== اجرای همه با هم =====
        AnimatorSet set = new AnimatorSet();
        set.playTogether(
            logoAlpha, logoScaleX, logoScaleY,
            nameAlpha, nameTranslateY,
            dividerScaleX, dividerAlpha,
            welcomeAlpha, welcomeTranslateY,
            loadingAlpha
        );
        set.start();
    }

    /**
     * شروع لود پس‌زمینه‌ی سایت
     * یه WebView مخفی می‌سازیم و سایت رو لود می‌کنیم
     * وقتی onPageFinished صدا زده شد → می‌ریم MainActivity
     */
    private void startBackgroundPreload() {
        // ===== WebView یه کلاس جدا می‌سازیم برای preload =====
        PreloadManager.preload(this, new PreloadManager.PreloadCallback() {
            @Override
            public void onPageLoaded() {
                // ===== اگه هنوز navigate نکردیم =====
                runOnUiThread(() -> navigateToMain(true));
            }

            @Override
            public void onPageFailed() {
                // ===== خطا → بازم بریم MainActivity، اون‌جا خطا نمایش داده می‌شه =====
                runOnUiThread(() -> navigateToMain(false));
            }
        });
    }

    /**
     * رفتن به MainActivity با انیمیشن نرم
     *
     * @param pageLoaded اگه سایت لود شده بود true، اگه نه false
     */
    private void navigateToMain(boolean pageLoaded) {
        // ===== فقط یک بار اجازه navigate =====
        if (!hasNavigated.compareAndSet(false, true)) {
            return;
        }

        // ===== حذف تایمرها =====
        handler.removeCallbacksAndMessages(null);

        // ===== چک حداقل زمان نمایش =====
        long elapsed = System.currentTimeMillis() - splashStartTime;
        long remaining = MIN_DISPLAY_TIME - elapsed;

        if (remaining > 0) {
            // ===== صبر کن تا زمان حداقل برسه =====
            handler.postDelayed(() -> performNavigation(pageLoaded), remaining);
        } else {
            performNavigation(pageLoaded);
        }
    }

    /**
     * اجرای واقعی انتقال
     */
    private void performNavigation(boolean pageLoaded) {
        // ===== انیمیشن خروج نرم =====
        splashContent.animate()
            .alpha(0f)
            .scaleX(1.05f)
            .scaleY(1.05f)
            .setDuration(300)
            .setInterpolator(new AccelerateDecelerateInterpolator())
            .withEndAction(() -> {
                Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                intent.putExtra("page_preloaded", pageLoaded);
                startActivity(intent);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                finish();
            })
            .start();
    }

    @Override
    public void onBackPressed() {
        // ===== توی Splash، back button کاری نکنه =====
        // (کاربر نمی‌تونه برگرده عقب)
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}
