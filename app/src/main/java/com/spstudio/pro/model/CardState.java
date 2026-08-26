package com.spstudio.pro.model;

import android.content.Context;

import com.spstudio.pro.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * يمثل كامل حالة بطاقة SP Studio Pro القابلة للتحرير: النصوص، الألوان،
 * أحجام الخطوط، الخط المستخدم، نسبة الأبعاد، ألوان الكلمات، وجدول المقارنة.
 * <p>
 * الكائن قابل للتحويل من وإلى JSON ليتم حفظه في {@link com.spstudio.pro.util.PrefsManager}.
 */
public class CardState {

    public static final String MODE_PROFESSIONAL = "prof";
    public static final String MODE_COMPARE = "compare";
    public static final String MODE_QA = "qa";

    public String mode = MODE_PROFESSIONAL;

    public String title = "";
    public String body = "";
    public String drName = "";
    public String drSub = "";

    /** حجم خط العنوان بالـ sp (المدى الأصلي 18-42). */
    public int titleSize = 30;
    /** حجم خط النص بالـ sp (المدى الأصلي 12-24). */
    public int bodySize = 16;
    /** تباعد الأسطر كمُضاعِف (المدى الأصلي 1.2-2.0). */
    public float lineHeight = 1.75f;

    public String fontName = "Cairo";
    public int accentColor = 0xFF3B82F6;

    /** واحدة من: 1:1، 4:5، 1.91:1، 9:16، auto */
    public String aspectRatio = "auto";

    public boolean colorizeActive = false;
    /** فهرس الكلمة (بالترتيب داخل النص) -> لون ARGB. */
    public Map<Integer, Integer> wordColors = new LinkedHashMap<>();

    public List<String> pros = new ArrayList<>();
    public List<String> cons = new ArrayList<>();

    public CardState() {
    }

    /** الحالة الافتراضية الكاملة عند "إعادة ضبط الكل" (تطابق fullReset في النسخة الأصلية). */
    public static CardState createDefault(Context ctx) {
        CardState s = new CardState();
        s.mode = MODE_PROFESSIONAL;
        s.title = ctx.getString(R.string.default_title);
        s.body = ctx.getString(R.string.default_body);
        s.drName = ctx.getString(R.string.default_dr_name);
        s.drSub = ctx.getString(R.string.default_dr_sub);
        s.titleSize = 30;
        s.bodySize = 16;
        s.lineHeight = 1.75f;
        s.fontName = "Cairo";
        s.accentColor = 0xFF3B82F6;
        s.aspectRatio = "auto";
        s.colorizeActive = false;
        s.wordColors = new LinkedHashMap<>();
        s.pros = defaultPros(ctx);
        s.cons = defaultCons(ctx);
        return s;
    }

    public static List<String> defaultPros(Context ctx) {
        List<String> list = new ArrayList<>();
        list.add("العلاج الطبيعي بدون أدوية وآمن");
        list.add("نتائج دائمة وتحسن مستدام");
        list.add("تقنيات حديثة وأجهزة متطورة");
        list.add("تحسين الحركة والمرونة بشكل ملحوظ");
        return list;
    }

    public static List<String> defaultCons(Context ctx) {
        List<String> list = new ArrayList<>();
        list.add("يحتاج التزام طويل الأمد");
        list.add("تكاليف أولية قد تكون مرتفعة");
        list.add("قد يشعر المريض بالإرهاق في البداية");
        list.add("النتائج تختلف حسب الحالة");
        return list;
    }

    /** يطبق محتوى الوضع (مهني/مقارنة/سؤال وجواب) على الحالة الحالية دون المساس بالتنسيق. */
    public void applyModeContent(Context ctx, String newMode) {
        this.mode = newMode;
        if (MODE_PROFESSIONAL.equals(newMode)) {
            title = ctx.getString(R.string.prof_title);
            body = ctx.getString(R.string.prof_body);
            drName = ctx.getString(R.string.prof_dr_name);
            drSub = ctx.getString(R.string.prof_dr_sub);
        } else if (MODE_COMPARE.equals(newMode)) {
            title = ctx.getString(R.string.compare_title);
            body = "";
            drName = ctx.getString(R.string.compare_dr_name);
            drSub = ctx.getString(R.string.compare_dr_sub);
            if (pros.isEmpty()) pros = defaultPros(ctx);
            if (cons.isEmpty()) cons = defaultCons(ctx);
        } else if (MODE_QA.equals(newMode)) {
            title = ctx.getString(R.string.qa_title);
            body = ctx.getString(R.string.qa_body);
            drName = ctx.getString(R.string.qa_dr_name);
            drSub = ctx.getString(R.string.qa_dr_sub);
        }
    }

    // ========================= JSON =========================

    public JSONObject toJson() {
        JSONObject o = new JSONObject();
        try {
            o.put("mode", mode);
            o.put("title", title);
            o.put("body", body);
            o.put("drName", drName);
            o.put("drSub", drSub);
            o.put("titleSize", titleSize);
            o.put("bodySize", bodySize);
            o.put("lineHeight", lineHeight);
            o.put("fontName", fontName);
            o.put("accentColor", accentColor);
            o.put("aspectRatio", aspectRatio);
            o.put("colorizeActive", colorizeActive);

            JSONObject wc = new JSONObject();
            for (Map.Entry<Integer, Integer> e : wordColors.entrySet()) {
                wc.put(String.valueOf(e.getKey()), e.getValue());
            }
            o.put("wordColors", wc);

            o.put("pros", new JSONArray(pros));
            o.put("cons", new JSONArray(cons));
        } catch (JSONException ignored) {
        }
        return o;
    }

    public static CardState fromJson(JSONObject o) {
        CardState s = new CardState();
        s.mode = o.optString("mode", MODE_PROFESSIONAL);
        s.title = o.optString("title", "");
        s.body = o.optString("body", "");
        s.drName = o.optString("drName", "");
        s.drSub = o.optString("drSub", "");
        s.titleSize = o.optInt("titleSize", 30);
        s.bodySize = o.optInt("bodySize", 16);
        s.lineHeight = (float) o.optDouble("lineHeight", 1.75);
        s.fontName = o.optString("fontName", "Cairo");
        s.accentColor = o.optInt("accentColor", 0xFF3B82F6);
        s.aspectRatio = o.optString("aspectRatio", "auto");
        s.colorizeActive = o.optBoolean("colorizeActive", false);

        s.wordColors = new LinkedHashMap<>();
        JSONObject wc = o.optJSONObject("wordColors");
        if (wc != null) {
            java.util.Iterator<String> keys = wc.keys();
            while (keys.hasNext()) {
                String k = keys.next();
                try {
                    s.wordColors.put(Integer.valueOf(k), wc.getInt(k));
                } catch (JSONException | NumberFormatException ignored) {
                }
            }
        }

        s.pros = new ArrayList<>();
        JSONArray prosArr = o.optJSONArray("pros");
        if (prosArr != null) {
            for (int i = 0; i < prosArr.length(); i++) s.pros.add(prosArr.optString(i, ""));
        }

        s.cons = new ArrayList<>();
        JSONArray consArr = o.optJSONArray("cons");
        if (consArr != null) {
            for (int i = 0; i < consArr.length(); i++) s.cons.add(consArr.optString(i, ""));
        }

        return s;
    }
}
