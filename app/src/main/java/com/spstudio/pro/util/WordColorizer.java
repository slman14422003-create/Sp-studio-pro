package com.spstudio.pro.util;

import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.view.View;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * يبني نصاً قابلاً للنقر كلمة بكلمة (Spannable)، وهو معادل أصلي لآلية
 * enableWordColoring في النسخة القديمة التي كانت تغلّف كل كلمة بعنصر
 * &lt;span class="word-colorable"&gt; قابل للنقر لتلوينه.
 */
public final class WordColorizer {

    /** أنماط لا تُعتبر "كلمة" قابلة للتلوين (تعداد نقطي فقط)، كما في النسخة الأصلية. */
    private static final Pattern BULLET_ONLY = Pattern.compile("^[•★●✓\\s]+$");

    public interface OnWordClickListener {
        void onWordClick(int wordIndex);
    }

    private WordColorizer() {
    }

    public static SpannableStringBuilder buildSpannable(
            String text,
            Map<Integer, Integer> wordColors,
            int defaultColor,
            OnWordClickListener listener) {

        SpannableStringBuilder builder = new SpannableStringBuilder();
        if (text == null || text.isEmpty()) return builder;

        // يحافظ على الفواصل (مسافات وأسطر جديدة) كعناصر منفصلة أثناء التقسيم
        String[] tokens = text.split("((?<=\\s)|(?=\\s))");
        int wordIndex = 0;

        for (String token : tokens) {
            int start = builder.length();
            builder.append(token);
            int end = builder.length();

            boolean isBulletOnly = BULLET_ONLY.matcher(token).matches();
            boolean isBlank = token.trim().isEmpty();

            if (!isBlank && !isBulletOnly) {
                final int index = wordIndex++;
                Integer color = wordColors.get(index);
                builder.setSpan(new ForegroundColorSpan(color != null ? color : defaultColor),
                        start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                builder.setSpan(new ClickableSpan() {
                    @Override
                    public void onClick(View widget) {
                        if (listener != null) listener.onWordClick(index);
                    }

                    @Override
                    public void updateDrawState(android.text.TextPaint ds) {
                        // إبقاء مظهر النص طبيعياً دون تسطير أو تلوين رابط افتراضي
                        ds.setUnderlineText(false);
                    }
                }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        return builder;
    }
}
