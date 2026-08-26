package com.spstudio.pro.util;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * يحوّل أي View (بطاقة المعاينة) إلى صورة PNG، معادلاً وظيفياً لاستدعاء
 * html2canvas في النسخة الأصلية القائمة على الويب، لكن بأدوات Android الأصلية.
 */
public final class ImageExporter {

    private ImageExporter() {
    }

    /** يرسم الـ View بدقة أعلى (scale) إلى Bitmap، مطابقةً لـ {scale:2} في html2canvas. */
    public static Bitmap renderViewToBitmap(View view, float scale) {
        int width = Math.max(1, view.getWidth());
        int height = Math.max(1, view.getHeight());

        Bitmap bitmap = Bitmap.createBitmap(
                Math.round(width * scale),
                Math.round(height * scale),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);
        canvas.scale(scale, scale);
        view.draw(canvas);
        return bitmap;
    }

    /**
     * يحفظ الصورة في معرض الجهاز (Pictures/SP Studio Pro) ويعيد الـ Uri الناتج.
     * يستخدم MediaStore على أندرويد 10+ ويكتب مباشرة إلى المجلد العام قبل ذلك.
     */
    public static Uri saveToGallery(Context ctx, Bitmap bitmap, String fileName) throws IOException {
        ContentResolver resolver = ctx.getContentResolver();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/SP Studio Pro");
            values.put(MediaStore.Images.Media.IS_PENDING, 1);

            Uri collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
            Uri item = resolver.insert(collection, values);
            if (item == null) throw new IOException("تعذر إنشاء ملف الصورة");

            try (OutputStream out = resolver.openOutputStream(item)) {
                if (out == null) throw new IOException("تعذر فتح مخرج الكتابة");
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            }
            values.clear();
            values.put(MediaStore.Images.Media.IS_PENDING, 0);
            resolver.update(item, values, null, null);
            return item;
        } else {
            File dir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES), "SP Studio Pro");
            if (!dir.exists() && !dir.mkdirs()) {
                throw new IOException("تعذر إنشاء المجلد");
            }
            File file = new File(dir, fileName);
            try (FileOutputStream out = new FileOutputStream(file)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            }
            // إشعار الماسح الضوئي للوسائط حتى تظهر الصورة فوراً في المعرض
            android.media.MediaScannerConnection.scanFile(ctx,
                    new String[]{file.getAbsolutePath()}, new String[]{"image/png"}, null);
            return Uri.fromFile(file);
        }
    }

    /** يحفظ الصورة في كاش التطبيق، مفيد للمشاركة الفورية عبر FileProvider دون أذونات تخزين. */
    public static Uri saveToCacheForSharing(Context ctx, Bitmap bitmap, String fileName) throws IOException {
        File dir = new File(ctx.getCacheDir(), "exported");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("تعذر إنشاء مجلد الكاش");
        }
        File file = new File(dir, fileName);
        try (FileOutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        }
        return FileProvider.getUriForFile(ctx, ctx.getPackageName() + ".fileprovider", file);
    }

    /** ينشئ Intent مشاركة جاهز لصورة تم تصديرها مسبقاً. */
    public static Intent buildShareIntent(Uri contentUri) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("image/png");
        intent.putExtra(Intent.EXTRA_STREAM, contentUri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        return intent;
    }
}
