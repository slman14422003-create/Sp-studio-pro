package com.spstudio.pro.util;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * يتحقق من وجود إصدار أحدث عبر GitHub Releases API، وهو البديل الأصلي (Native)
 * لآلية التحقق القديمة القائمة على SU.html / updates.json.
 * <p>
 * مرجع الواجهة: https://api.github.com/repos/{owner}/{repo}/releases/latest
 */
public final class UpdateChecker {

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    public interface Callback {
        void onUpToDate();

        void onUpdateAvailable(String latestVersion, String releaseUrl);

        void onError(Exception e);
    }

    private UpdateChecker() {
    }

    public static void check(String owner, String repo, String currentVersionName, Callback callback) {
        EXECUTOR.execute(() -> {
            try {
                String urlStr = "https://api.github.com/repos/" + owner + "/" + repo + "/releases/latest";
                HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
                conn.setRequestProperty("Accept", "application/vnd.github+json");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);

                int code = conn.getResponseCode();
                if (code != 200) {
                    postError(callback, new IOException("HTTP " + code));
                    return;
                }

                StringBuilder sb = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                }

                JSONObject json = new JSONObject(sb.toString());
                String tagName = json.optString("tag_name", "");
                String releaseUrl = json.optString("html_url", urlStr);
                String latest = tagName.startsWith("v") ? tagName.substring(1) : tagName;

                if (isNewer(latest, currentVersionName)) {
                    postUpdateAvailable(callback, latest, releaseUrl);
                } else {
                    postUpToDate(callback);
                }
            } catch (Exception e) {
                postError(callback, e);
            }
        });
    }

    /** مقارنة إصدارات دلالية بسيطة على شكل x.y.z. */
    private static boolean isNewer(String remote, String local) {
        String[] r = remote.split("\\.");
        String[] l = local.split("\\.");
        int len = Math.max(r.length, l.length);
        for (int i = 0; i < len; i++) {
            int rv = i < r.length ? safeInt(r[i]) : 0;
            int lv = i < l.length ? safeInt(l[i]) : 0;
            if (rv != lv) return rv > lv;
        }
        return false;
    }

    private static int safeInt(String s) {
        try {
            return Integer.parseInt(s.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static void postUpToDate(Callback cb) {
        MAIN_HANDLER.post(cb::onUpToDate);
    }

    private static void postUpdateAvailable(Callback cb, String version, String url) {
        MAIN_HANDLER.post(() -> cb.onUpdateAvailable(version, url));
    }

    private static void postError(Callback cb, Exception e) {
        MAIN_HANDLER.post(() -> cb.onError(e));
    }
}
