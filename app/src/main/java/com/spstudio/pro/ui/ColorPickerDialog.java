package com.spstudio.pro.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.GridLayout;

import androidx.appcompat.app.AlertDialog;

/**
 * نافذة اختيار لون بسيطة (شبكة دوائر ملونة)، تُستخدم لتلوين الكلمات المحددة
 * داخل نص البطاقة، مطابقة لوظيفة showWordColorModal في النسخة الأصلية.
 */
public final class ColorPickerDialog {

    public static final int[] WORD_COLOR_PALETTE = {
            0xFF3B82F6, 0xFFEF4444, 0xFF10B981, 0xFFF59E0B, 0xFF8B5CF6, 0xFFEC489A,
            0xFF06B6D4, 0xFFF97316, 0xFF14B8A6, 0xFF000000, 0xFFFFFFFF, 0xFF64748B,
            0xFFDC2626, 0xFFEA580C, 0xFFD97706, 0xFFCA8A04, 0xFF65A30D, 0xFF16A34A,
            0xFF0284C7, 0xFF2563EB, 0xFF4F46E5, 0xFF7C3AED, 0xFFDB2777, 0xFFE11D48
    };

    public interface OnColorSelected {
        void onColorSelected(int color);
    }

    private ColorPickerDialog() {
    }

    public static void show(Context ctx, String title, OnColorSelected listener) {
        GridLayout grid = new GridLayout(ctx);
        grid.setColumnCount(6);
        int pad = dp(ctx, 16);
        grid.setPadding(pad, pad, pad, pad);

        AlertDialog dialog = new AlertDialog.Builder(ctx)
                .setTitle(title)
                .setView(grid)
                .setNegativeButton(android.R.string.cancel, null)
                .create();

        int swatchSize = dp(ctx, 36);
        int margin = dp(ctx, 6);

        for (int color : WORD_COLOR_PALETTE) {
            View swatch = new View(ctx);
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.OVAL);
            bg.setColor(color);
            bg.setStroke(dp(ctx, 1), color == Color.WHITE ? 0xFF333333 : 0x80FFFFFF);
            swatch.setBackground(bg);

            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = swatchSize;
            lp.height = swatchSize;
            lp.setMargins(margin, margin, margin, margin);
            swatch.setLayoutParams(lp);

            swatch.setOnClickListener(v -> {
                if (listener != null) listener.onColorSelected(color);
                dialog.dismiss();
            });

            grid.addView(swatch);
        }

        dialog.show();
    }

    private static int dp(Context ctx, int value) {
        float density = ctx.getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }
}
