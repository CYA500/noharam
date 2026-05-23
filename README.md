# الحارس (Guardian App)

تطبيق Android للضبط الذاتي مع استجابة فورية ثلاثية المستويات: كلمات مشبوهة، روابط محجوبة، وصور صريحة.

## المتطلبات

- Android Studio Hedgehog أو أحدث
- JDK 17
- minSdk 26 / targetSdk 34

## البناء المحلي

```bash
./gradlew assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

## الأذونات (من شاشة الإعداد)

1. إمكانية الوصول — مراقبة النصوص
2. VPN — تصفية DNS
3. مسؤول الجهاز — قفل الشاشة الفوري (المستوى 3)
4. عرض فوق التطبيقات — رسائل التحذير
5. الوصول إلى الصور — تحليل وحذف الصور

## نموذج TFLite (اختياري)

ضع `guardian_classifier.tflite` في `app/src/main/assets/`. بدون الملف يعمل التطبيق عبر ML Kit.

## CI

GitHub Actions يبني APK تلقائياً عند كل push إلى `main` / `master` / `dev`.
