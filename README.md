# SP Studio Pro (Android — Java)

إعادة بناء **أصلية بالكامل** لتطبيق "PT.semo" (SP Studio Pro) كتطبيق أندرويد
حقيقي بلغة **Java**، دون أي HTML أو CSS أو JavaScript. التطبيق الأصلي كان
تطبيق ويب تقدمي (PWA) بصفحة HTML واحدة ضخمة (973 سطراً) تجمع بين الواجهة
والمنطق؛ هذه النسخة تعيد تنظيمه بالكامل ضمن هيكلة مشروع أندرويد قياسية مع
فصل واضح بين الواجهة (`res/layout`, `res/values`) والمنطق (`java/...`).

## ما الذي تغيّر عن النسخة الأصلية؟

| النسخة الأصلية (PWA) | النسخة الجديدة (Android/Java) |
|---|---|
| صفحة `index.html` واحدة (HTML+CSS+JS) | مشروع Gradle منظم بحزم Java منفصلة |
| `localStorage` / `IndexedDB` / `localforage` | `SharedPreferences` + JSON (`PrefsManager`) |
| `html2canvas` لتصدير الصورة | رسم الـ `View` مباشرة إلى `Bitmap` (`ImageExporter`) |
| Service Worker / كاش المتصفح | غير مطلوب؛ التطبيق أصلي مثبّت فعلياً |
| فحص التحديث عبر `SU.html` / `updates.json` | فحص عبر GitHub Releases API (`UpdateChecker`) |
| 50 خط عبر Google Fonts CDN | قائمة خطوط قابلة للتوسعة (راجع قسم "الخطوط" أدناه) |
| أخطاء بسيطة (مثل علامة استفهام صينية "？" بدل "؟") | تم تصحيحها بالكامل في النصوص |

## هيكلة المشروع

```
SPStudioPro/
├── app/
│   ├── build.gradle                 # إعدادات وحدة التطبيق + معلومات GitHub للتحديثات
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── java/com/spstudio/pro/
│   │   │   ├── SPStudioApp.java         # فئة التطبيق
│   │   │   ├── MainActivity.java        # الشاشة الرئيسية (محرر البطاقة)
│   │   │   ├── HelpActivity.java        # شاشة المساعدة
│   │   │   ├── model/CardState.java     # نموذج حالة البطاقة الكاملة + JSON
│   │   │   ├── util/PrefsManager.java   # حفظ/استرجاع الحالة (SharedPreferences)
│   │   │   ├── util/ImageExporter.java  # تصدير View إلى PNG وحفظه/مشاركته
│   │   │   ├── util/UpdateChecker.java  # التحقق من GitHub Releases
│   │   │   ├── util/WordColorizer.java  # بناء نص قابل للنقر لتلوين الكلمات
│   │   │   └── ui/ColorPickerDialog.java# نافذة اختيار لون
│   │   └── res/
│   │       ├── layout/                  # activity_main.xml, activity_help.xml, item_edit_row.xml
│   │       ├── values/                  # strings.xml (عربي), colors.xml, styles.xml, themes.xml
│   │       ├── values-en/               # strings.xml (إنجليزي، اختياري)
│   │       ├── drawable/                # خلفيات زجاجية، أزرار، بطاقة التصدير...
│   │       └── xml/                     # file_paths.xml, locales_config.xml
├── .github/workflows/release.yml    # بناء ونشر APK تلقائياً على GitHub Releases
├── settings.gradle / build.gradle / gradle.properties
├── LICENSE
└── README.md
```

## الميزات المعاد بناؤها

- **ثلاثة أوضاع للبطاقة**: مهني، مقارنة تفاعلية (إيجابيات/سلبيات قابلة
  للتحرير والإضافة والحذف)، وسؤال وجواب.
- **تحرير مباشر** للعنوان والنص واسم الأخصائي والتخصص داخل البطاقة نفسها.
- **تخصيص كامل**: حجم خط العنوان، حجم خط النص، تباعد الأسطر، نوع الخط،
  لون التمييز (7 ألوان)، ونسبة الأبعاد (1:1، 4:5، 1.91:1، 9:16، تلقائي).
- **تلوين كلمات فردية** داخل النص عبر النقر على أي كلمة واختيار لون من
  لوحة تضم 24 لوناً.
- **تصدير PNG** حقيقي عبر رسم الواجهة إلى Bitmap وحفظه في المعرض
  (Pictures/SP Studio Pro)، أو مشاركته مباشرة عبر أي تطبيق.
- **حفظ تلقائي** لكامل حالة البطاقة محلياً، بحيث تبقى عند إعادة فتح التطبيق.
- **التحقق من التحديثات** عبر GitHub Releases API.
- دعم كامل للغة العربية واتجاه RTL.

## البناء محلياً

1. افتح مجلد `SPStudioPro` في **Android Studio** (Hedgehog أو أحدث).
2. اسمح لـ Android Studio بتوليد ملف `gradle-wrapper.jar` تلقائياً إن طُلب
   ذلك (لم يُضمَّن الملف الثنائي هنا لأسباب تتعلق بحجم وبيئة الإنشاء).
3. Build → Make Project، ثم Run على جهاز أو محاكي.

للبناء من سطر الأوامر بعد توليد الـ wrapper:

```bash
./gradlew assembleDebug     # نسخة تجريبية
./gradlew assembleRelease   # نسخة إصدار (غير موقّعة افتراضياً، راجع أدناه)
```

## إعداد اسم مستودع GitHub للتحديثات

عدّل هذين السطرين في `app/build.gradle` ليطابقا مستودعك على GitHub:

```groovy
buildConfigField "String", "GITHUB_OWNER", '"your-github-username"'
buildConfigField "String", "GITHUB_REPO", '"PT-semo-android"'
```

## إصدار (Release) تلقائي على GitHub

يتضمن المشروع سير عمل جاهز في `.github/workflows/release.yml` يقوم تلقائياً بـ:

1. بناء المشروع بـ JDK 17.
2. توليد ملف APK (وسمه إذا توفرت أسرار التوقيع، أو دون توقيع إن لم تتوفر).
3. رفع الـ APK كأداة (Artifact) للتشغيل.
4. عند دفع وسم إصدار مثل `v1.0.0`، إنشاء **GitHub Release** تلقائياً وإرفاق
   ملف الـ APK به.

### لإصدار نسخة جديدة

```bash
git tag v1.0.0
git push origin v1.0.0
```

### لتوقيع الإصدار رسمياً (اختياري لكن موصى به)

أضف الأسرار التالية في إعدادات المستودع (Settings → Secrets → Actions):

- `RELEASE_KEYSTORE_BASE64` — محتوى ملف الـ keystore مُرمّزاً بـ base64
  (`base64 -w0 release.keystore`)
- `RELEASE_STORE_PASSWORD`
- `RELEASE_KEY_ALIAS`
- `RELEASE_KEY_PASSWORD`

عند توفر هذه الأسرار سيقوم السير تلقائياً بتوقيع الإصدار؛ وبدونها يُبنى
APK غير موقّع صالح للاختبار فقط (لا يمكن نشره على متجر Google Play دون توقيع).

## الخطوط

للحفاظ على موثوقية البناء دون اتصال بالإنترنت أو شهادات غير مُتحقَّق منها،
تعرض قائمة الخطوط حالياً أسماء الخطوط العربية الأصلية (Cairo, Tajawal,
Amiri...) لكنها تُعرض فعلياً بخط النظام الافتراضي حتى تُضاف ملفات `.ttf`
حقيقية. لإضافة خط حقيقي:

1. نزّل ملف `.ttf` (مثلاً من Google Fonts) وضعه في `app/src/main/res/font/`
   باسم صغير الحروف بدون مسافات، مثل `cairo_bold.ttf`.
2. في `MainActivity.applyFont()`، أضف حالة جديدة في الـ `switch` تُحمّل
   الخط عبر `ResourcesCompat.getFont(this, R.font.cairo_bold)`.

## الأذونات

- `INTERNET`: للتحقق من التحديثات عبر GitHub API فقط (لا تحليلات ولا إعلانات).
- `WRITE_EXTERNAL_STORAGE` (حتى Android 9 فقط): لحفظ الصورة المصدَّرة في
  المعرض العام. من Android 10 فما فوق يُستخدم `MediaStore` مباشرة دون أي إذن.

## الرخصة

MIT — راجع ملف `LICENSE`.
