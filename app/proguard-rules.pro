# =========================================================
# proguard-rules.pro  —  قوانین ProGuard
# مسیر: app/proguard-rules.pro
# =========================================================

# ===== حفظ WebView و JavaScript Interface =====
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

-keepattributes JavascriptInterface
-keepattributes *Annotation*

# ===== حفظ AndroidX =====
-keep class androidx.** { *; }
-keep interface androidx.** { *; }
-dontwarn androidx.**

# ===== حفظ کلاس‌های اصلی اپ =====
-keep class app.vista.** { *; }

# ===== حفظ WebView =====
-keep class android.webkit.** { *; }
-dontwarn android.webkit.**

# ===== حفظ AppCompat =====
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

# ===== حفظ View ها با پارامتر constructor =====
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# ===== حفظ متدهای Native =====
-keepclasseswithmembernames class * {
    native <methods>;
}

# ===== حفظ Enum =====
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ===== حفظ Parcelable =====
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# ===== حذف Log در Release =====
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# ===== حفظ اطلاعات دیباگ =====
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ===== R8 Optimization =====
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification

# ===== حذف هشدارها =====
-dontwarn java.lang.invoke.**
-dontwarn javax.annotation.**
-dontwarn org.jetbrains.annotations.**
