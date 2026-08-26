package com.spstudio.pro.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.spstudio.pro.model.CardState;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * يحفظ ويستعيد {@link CardState} بصيغة JSON داخل SharedPreferences،
 * وهو المعادل الأصلي لاستخدام localStorage في النسخة القديمة.
 */
public final class PrefsManager {

    private static final String PREFS_NAME = "sp_studio_prefs";
    private static final String KEY_STATE = "card_state_json";

    private PrefsManager() {
    }

    public static void save(Context ctx, CardState state) {
        SharedPreferences prefs = ctx.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_STATE, state.toJson().toString()).apply();
    }

    /** يعيد الحالة المحفوظة، أو الحالة الافتراضية الكاملة إن لم توجد بيانات محفوظة. */
    public static CardState load(Context ctx) {
        SharedPreferences prefs = ctx.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_STATE, null);
        if (json == null) {
            return CardState.createDefault(ctx);
        }
        try {
            return CardState.fromJson(new JSONObject(json));
        } catch (JSONException e) {
            return CardState.createDefault(ctx);
        }
    }
}
