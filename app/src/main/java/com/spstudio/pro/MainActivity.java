package com.spstudio.pro;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.spstudio.pro.databinding.ActivityMainBinding;
import com.spstudio.pro.databinding.ItemEditRowBinding;
import com.spstudio.pro.model.CardState;
import com.spstudio.pro.ui.ColorPickerDialog;
import com.spstudio.pro.util.ImageExporter;
import com.spstudio.pro.util.PrefsManager;
import com.spstudio.pro.util.UpdateChecker;
import com.spstudio.pro.util.WordColorizer;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * الشاشة الرئيسية لتطبيق SP Studio Pro: محرر بطاقات العلاج الطبيعي.
 * <p>
 * إعادة بناء أصلية (Java/Android) كاملة لتطبيق الويب التقدمي الأصلي (PWA)،
 * بدون أي HTML أو CSS أو جافاسكريبت.
 */
public class MainActivity extends AppCompatActivity {

    private static final int[] THEME_PALETTE = {
            0xFF3B82F6, 0xFFEF4444, 0xFF10B981, 0xFFF59E0B, 0xFF8B5CF6, 0xFFEC489A, 0xFF06B6D4
    };

    private static final String[] ASPECT_LABELS = {"1:1", "4:5", "1.91:1", "9:16", "تلقائي"};
    private static final String[] ASPECT_VALUES = {"1:1", "4:5", "1.91:1", "9:16", "auto"};

    private static final String[] FONT_NAMES = {
            "Cairo", "Tajawal", "Noto Kufi Arabic", "Amiri", "Almarai", "El Messiri",
            "Reem Kufi", "Changa", "Sans Serif", "Serif", "Monospace"
    };

    private ActivityMainBinding binding;
    private CardState state;

    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingSave;

    private ActivityResultLauncher<String> storagePermissionLauncher;
    private boolean pendingShareAfterExport = false;

    /** يمنع حلقات إعادة الحفظ أثناء تطبيق الحالة برمجياً على الواجهة. */
    private boolean applyingState = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        state = PrefsManager.load(this);

        storagePermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), granted -> {
                    if (granted) {
                        doExportImage(pendingShareAfterExport);
                    } else {
                        toast(getString(R.string.toast_save_failed));
                    }
                });

        buildModeButtons();
        buildAspectButtons();
        buildColorDots();
        setupFontSpinner();
        setupSliders();
        setupTextWatchers();
        setupButtons();

        applyStateToViews();
        binding.statusBar.setText("✅ التطبيق جاهز | SP Studio Pro (Android)");
    }

    // ========================= بناء عناصر الواجهة الديناميكية =========================

    private void buildModeButtons() {
        binding.topModes.removeAllViews();
        addModeButton(getString(R.string.mode_professional), CardState.MODE_PROFESSIONAL);
        addModeButton(getString(R.string.mode_compare), CardState.MODE_COMPARE);
        addModeButton(getString(R.string.mode_qa), CardState.MODE_QA);
    }

    private void addModeButton(String label, String modeKey) {
        TextView btn = new TextView(this);
        btn.setText(label);
        btn.setTag(modeKey);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        btn.setLayoutParams(lp);
        btn.setPadding(dp(20), dp(9), dp(20), dp(9));
        btn.setTextColor(getColor(R.color.text_light));
        btn.setTextSize(13);
        btn.setTypeface(btn.getTypeface(), Typeface.BOLD);
        btn.setOnClickListener(v -> switchMode(modeKey));
        binding.topModes.addView(btn);
    }

    private void buildAspectButtons() {
        binding.aspectRow.removeAllViews();
        for (int i = 0; i < ASPECT_VALUES.length; i++) {
            String value = ASPECT_VALUES[i];
            TextView btn = new TextView(this);
            btn.setText(ASPECT_LABELS[i]);
            btn.setTag(value);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(dp(6));
            btn.setLayoutParams(lp);
            btn.setPadding(dp(14), dp(7), dp(14), dp(7));
            btn.setTextColor(getColor(R.color.white));
            btn.setTextSize(11);
            btn.setOnClickListener(v -> {
                state.aspectRatio = value;
                applyAspectRatio();
                highlightAspectButtons();
                scheduleSave();
            });
            binding.aspectRow.addView(btn);
        }
    }

    private void highlightAspectButtons() {
        for (int i = 0; i < binding.aspectRow.getChildCount(); i++) {
            TextView btn = (TextView) binding.aspectRow.getChildAt(i);
            boolean active = state.aspectRatio.equals(btn.getTag());
            btn.setBackgroundResource(active ? R.drawable.bg_pill_primary : R.drawable.bg_glass_pill);
        }
    }

    private void buildColorDots() {
        binding.colorGrid.removeAllViews();
        for (int color : THEME_PALETTE) {
            View dot = new View(this);
            GradientDrawable bg = (GradientDrawable) getDrawable(R.drawable.bg_color_dot).mutate();
            bg.setColor(color);
            dot.setBackground(bg);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(34), dp(34));
            lp.setMarginEnd(dp(10));
            dot.setLayoutParams(lp);
            dot.setContentDescription(getString(R.string.color_dot_desc));
            dot.setOnClickListener(v -> applyTheme(color));
            binding.colorGrid.addView(dot);
        }
    }

    private void setupFontSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, FONT_NAMES);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerFont.setAdapter(adapter);
        binding.spinnerFont.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (applyingState) return;
                state.fontName = FONT_NAMES[position];
                applyFont();
                scheduleSave();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });
    }

    private void setupSliders() {
        // حجم العنوان: تدرج 0..24 يقابل 18..42
        binding.seekTitleSize.setMax(24);
        binding.seekTitleSize.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) return;
                state.titleSize = 18 + progress;
                binding.labelTitleSizeVal.setText(String.valueOf(state.titleSize));
                binding.titleEdit.setTextSize(state.titleSize);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                scheduleSave();
            }
        });

        // حجم النص: تدرج 0..12 يقابل 12..24
        binding.seekBodySize.setMax(12);
        binding.seekBodySize.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) return;
                state.bodySize = 12 + progress;
                binding.labelBodySizeVal.setText(String.valueOf(state.bodySize));
                binding.bodyEdit.setTextSize(state.bodySize);
                binding.bodyColorized.setTextSize(state.bodySize);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                scheduleSave();
            }
        });

        // تباعد الأسطر: تدرج 0..16 يقابل 1.20..2.00 بخطوة 0.05
        binding.seekLineHeight.setMax(16);
        binding.seekLineHeight.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) return;
                state.lineHeight = 1.2f + progress * 0.05f;
                binding.labelLineHeightVal.setText(String.format(Locale.US, "%.2f", state.lineHeight));
                binding.bodyEdit.setLineSpacing(0, state.lineHeight);
                binding.bodyColorized.setLineSpacing(0, state.lineHeight);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                scheduleSave();
            }
        });
    }

    private void setupTextWatchers() {
        bindDebouncedWatcher(binding.titleEdit, s -> state.title = s);
        bindDebouncedWatcher(binding.bodyEdit, s -> {
            state.body = s;
            updateWordCount();
        });
        bindDebouncedWatcher(binding.drNameEdit, s -> state.drName = s);
        bindDebouncedWatcher(binding.drSubEdit, s -> state.drSub = s);
    }

    private interface TextSink {
        void set(String value);
    }

    private void bindDebouncedWatcher(EditText editText, TextSink sink) {
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (applyingState) return;
                sink.set(s.toString());
                scheduleSave();
            }
        });
    }

    private void setupButtons() {
        binding.btnSavePng.setOnClickListener(v -> exportImage(false));
        binding.btnCheckUpdates.setOnClickListener(v -> checkForUpdates());
        binding.btnHelp.setOnClickListener(v -> startActivity(new Intent(this, HelpActivity.class)));
        binding.btnShare.setOnClickListener(v -> exportImage(true));
        binding.btnTogglePanel.setOnClickListener(v -> toggleVisibility(binding.smartPanel));
        binding.btnToggleTools.setOnClickListener(v -> toggleVisibility(binding.toolsPanel));

        binding.btnColorize.setOnClickListener(v -> toggleColorize());
        binding.btnResetColors.setOnClickListener(v -> resetWordColors());
        binding.btnResetAll.setOnClickListener(v -> fullReset());

        binding.addProsBtn.setOnClickListener(v -> {
            state.pros.add(getString(R.string.new_pro_item));
            rebuildComparisonList(binding.prosList, state.pros, true);
            scheduleSave();
        });
        binding.addConsBtn.setOnClickListener(v -> {
            state.cons.add(getString(R.string.new_con_item));
            rebuildComparisonList(binding.consList, state.cons, false);
            scheduleSave();
        });
    }

    private void toggleVisibility(View view) {
        view.setVisibility(view.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
    }

    // ========================= الأوضاع =========================

    private void switchMode(String modeKey) {
        state.applyModeContent(this, modeKey);
        applyStateToViews();
        scheduleSave();

        String label = CardState.MODE_PROFESSIONAL.equals(modeKey) ? getString(R.string.mode_professional)
                : CardState.MODE_COMPARE.equals(modeKey) ? getString(R.string.mode_compare)
                : getString(R.string.mode_qa);
        toast("🔄 تم التبديل إلى " + label);
    }

    private void highlightModeButtons() {
        for (int i = 0; i < binding.topModes.getChildCount(); i++) {
            TextView btn = (TextView) binding.topModes.getChildAt(i);
            boolean active = state.mode.equals(btn.getTag());
            btn.setBackgroundResource(active ? R.drawable.bg_pill_primary : R.drawable.bg_mode_inactive);
            btn.setTextColor(active ? Color.WHITE : getColor(R.color.text_light));
        }
    }

    // ========================= التطبيق الكامل للحالة على الواجهة =========================

    private void applyStateToViews() {
        applyingState = true;

        binding.titleEdit.setText(state.title);
        binding.drNameEdit.setText(state.drName);
        binding.drSubEdit.setText(state.drSub);

        binding.titleEdit.setTextSize(state.titleSize);
        binding.bodyEdit.setTextSize(state.bodySize);
        binding.bodyColorized.setTextSize(state.bodySize);
        binding.bodyEdit.setLineSpacing(0, state.lineHeight);
        binding.bodyColorized.setLineSpacing(0, state.lineHeight);

        binding.seekTitleSize.setProgress(state.titleSize - 18);
        binding.seekBodySize.setProgress(state.bodySize - 12);
        binding.seekLineHeight.setProgress(Math.round((state.lineHeight - 1.2f) / 0.05f));
        binding.labelTitleSizeVal.setText(String.valueOf(state.titleSize));
        binding.labelBodySizeVal.setText(String.valueOf(state.bodySize));
        binding.labelLineHeightVal.setText(String.format(Locale.US, "%.2f", state.lineHeight));

        int fontIndex = indexOfFont(state.fontName);
        binding.spinnerFont.setSelection(fontIndex);
        applyFont();

        applyThemeColorsOnly(state.accentColor);
        applyAspectRatio();
        highlightAspectButtons();
        highlightModeButtons();

        boolean isCompare = CardState.MODE_COMPARE.equals(state.mode);
        binding.comparisonContainer.setVisibility(isCompare ? View.VISIBLE : View.GONE);
        if (isCompare) {
            rebuildComparisonList(binding.prosList, state.pros, true);
            rebuildComparisonList(binding.consList, state.cons, false);
        }

        refreshBodyVisibility();
        updateWordCount();

        binding.btnColorize.setText(state.colorizeActive ? R.string.btn_colorize_off : R.string.btn_colorize_on);

        applyingState = false;
    }

    private int indexOfFont(String fontName) {
        for (int i = 0; i < FONT_NAMES.length; i++) {
            if (FONT_NAMES[i].equalsIgnoreCase(fontName)) return i;
        }
        return 0;
    }

    // ========================= المظهر: الألوان، الخط، الأبعاد =========================

    private void applyTheme(int color) {
        state.accentColor = color;
        applyThemeColorsOnly(color);
        scheduleSave();
    }

    private void applyThemeColorsOnly(int color) {
        binding.titleBorder.setBackgroundColor(color);
        binding.titleEdit.setTextColor(color);
        binding.drSubEdit.setTextColor(color);

        ColorStateList tint = ColorStateList.valueOf(color);
        binding.seekTitleSize.setProgressTintList(tint);
        binding.seekTitleSize.setThumbTintList(tint);
        binding.seekBodySize.setProgressTintList(tint);
        binding.seekBodySize.setThumbTintList(tint);
        binding.seekLineHeight.setProgressTintList(tint);
        binding.seekLineHeight.setThumbTintList(tint);
    }

    private void applyFont() {
        Typeface tf;
        switch (state.fontName) {
            case "Serif":
                tf = Typeface.SERIF;
                break;
            case "Monospace":
                tf = Typeface.MONOSPACE;
                break;
            default:
                // الخطوط العربية المخصصة (Cairo, Tajawal...) تُعرض بخط النظام الافتراضي
                // إلى أن تُضاف ملفات .ttf الفعلية ضمن res/font (راجع ملف README).
                tf = Typeface.DEFAULT;
        }
        binding.titleEdit.setTypeface(tf, Typeface.BOLD);
        binding.bodyEdit.setTypeface(tf);
        binding.bodyColorized.setTypeface(tf);
        binding.drNameEdit.setTypeface(tf, Typeface.BOLD);
        binding.drSubEdit.setTypeface(tf, Typeface.BOLD);
    }

    private void applyAspectRatio() {
        androidx.constraintlayout.widget.ConstraintLayout.LayoutParams params =
                (androidx.constraintlayout.widget.ConstraintLayout.LayoutParams) binding.exportArea.getLayoutParams();
        if ("auto".equals(state.aspectRatio)) {
            params.dimensionRatio = null;
            params.height = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT;
        } else {
            params.dimensionRatio = "H," + state.aspectRatio;
            params.height = 0;
        }
        binding.exportArea.setLayoutParams(params);
    }

    // ========================= جدول المقارنة (إيجابيات/سلبيات) =========================

    private void rebuildComparisonList(LinearLayout container, List<String> items, boolean isPro) {
        container.removeAllViews();
        for (int i = 0; i < items.size(); i++) {
            addComparisonRow(container, items, i);
        }
    }

    private void addComparisonRow(LinearLayout container, List<String> items, int index) {
        ItemEditRowBinding rowBinding = ItemEditRowBinding.inflate(LayoutInflater.from(this), container, false);
        rowBinding.itemText.setText(items.get(index));
        rowBinding.itemText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (applyingState) return;
                int pos = container.indexOfChild(rowBinding.getRoot());
                if (pos >= 0 && pos < items.size()) {
                    items.set(pos, s.toString());
                    scheduleSave();
                }
            }
        });
        rowBinding.itemDelete.setOnClickListener(v -> {
            int pos = container.indexOfChild(rowBinding.getRoot());
            if (pos >= 0 && pos < items.size()) {
                items.remove(pos);
                rebuildComparisonList(container, items, container == binding.prosList);
                scheduleSave();
            }
        });
        container.addView(rowBinding.getRoot());
    }

    // ========================= تلوين الكلمات =========================

    private void toggleColorize() {
        state.colorizeActive = !state.colorizeActive;
        binding.btnColorize.setText(state.colorizeActive ? R.string.btn_colorize_off : R.string.btn_colorize_on);
        refreshBodyVisibility();
        scheduleSave();
    }

    private void resetWordColors() {
        state.wordColors.clear();
        refreshBodyVisibility();
        scheduleSave();
        toast(getString(R.string.toast_colors_reset));
    }

    /** يقرر عرض EditText القابل للتحرير أو TextView الملون القابل للنقر بحسب الوضع الحالي. */
    private void refreshBodyVisibility() {
        boolean isCompare = CardState.MODE_COMPARE.equals(state.mode);
        if (isCompare) {
            binding.bodyEdit.setVisibility(View.GONE);
            binding.bodyColorized.setVisibility(View.GONE);
            return;
        }
        if (state.colorizeActive) {
            binding.bodyEdit.setVisibility(View.GONE);
            binding.bodyColorized.setVisibility(View.VISIBLE);
            rebuildColorizedBody();
        } else {
            binding.bodyEdit.setVisibility(View.VISIBLE);
            binding.bodyColorized.setVisibility(View.GONE);
        }
    }

    private void rebuildColorizedBody() {
        SpannableStringBuilder spannable = WordColorizer.buildSpannable(
                state.body, state.wordColors, getColor(R.color.body_dark),
                wordIndex -> ColorPickerDialog.show(this, getString(R.string.label_colors), color -> {
                    state.wordColors.put(wordIndex, color);
                    rebuildColorizedBody();
                    scheduleSave();
                }));
        binding.bodyColorized.setText(spannable);
        binding.bodyColorized.setMovementMethod(LinkMovementMethod.getInstance());
    }

    // ========================= إعادة الضبط الكامل =========================

    private void fullReset() {
        state = CardState.createDefault(this);
        applyStateToViews();
        scheduleSave();
        toast(getString(R.string.toast_reset_done));
    }

    // ========================= عداد الكلمات =========================

    private void updateWordCount() {
        String text = state.body == null ? "" : state.body.replaceAll("[•★●✓]", " ");
        String[] words = text.trim().isEmpty() ? new String[0] : text.trim().split("\\s+");
        int count = words.length;
        int minutes = (int) Math.ceil(count / 50.0);
        binding.wordCounter.setText(getString(R.string.word_counter_format, count, minutes));
    }

    // ========================= الحفظ (مع تأخير بسيط لتقليل الكتابة المتكررة) =========================

    private void scheduleSave() {
        if (pendingSave != null) debounceHandler.removeCallbacks(pendingSave);
        pendingSave = () -> PrefsManager.save(this, state);
        debounceHandler.postDelayed(pendingSave, 350);
    }

    // ========================= التصدير والمشاركة =========================

    private void exportImage(boolean shareAfterExport) {
        pendingShareAfterExport = shareAfterExport;
        boolean needsLegacyPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && !shareAfterExport;
        if (needsLegacyPermission && ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            return;
        }
        doExportImage(shareAfterExport);
    }

    private void doExportImage(boolean shareAfterExport) {
        clearFocusAndHideKeyboard();
        binding.exportArea.post(() -> {
            try {
                Bitmap bitmap = ImageExporter.renderViewToBitmap(binding.exportArea, 2f);
                String fileName = "SP_Studio_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".png";

                if (shareAfterExport) {
                    Uri uri = ImageExporter.saveToCacheForSharing(this, bitmap, fileName);
                    startActivity(Intent.createChooser(ImageExporter.buildShareIntent(uri), getString(R.string.btn_share)));
                } else {
                    ImageExporter.saveToGallery(this, bitmap, fileName);
                    toast(getString(R.string.toast_saved));
                }
            } catch (IOException e) {
                toast(getString(R.string.toast_save_failed));
            }
        });
    }

    private void clearFocusAndHideKeyboard() {
        View focused = getCurrentFocus();
        if (focused != null) {
            focused.clearFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(focused.getWindowToken(), 0);
        }
    }

    // ========================= التحقق من التحديثات (GitHub Releases) =========================

    private void checkForUpdates() {
        toast(getString(R.string.toast_checking_updates));
        UpdateChecker.check(BuildConfig.GITHUB_OWNER, BuildConfig.GITHUB_REPO, BuildConfig.VERSION_NAME,
                new UpdateChecker.Callback() {
                    @Override
                    public void onUpToDate() {
                        toast(getString(R.string.toast_up_to_date));
                    }

                    @Override
                    public void onUpdateAvailable(String latestVersion, String releaseUrl) {
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle(R.string.dialog_update_title)
                                .setMessage(getString(R.string.dialog_update_message, latestVersion))
                                .setPositiveButton(R.string.dialog_yes, (d, w) ->
                                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(releaseUrl))))
                                .setNegativeButton(R.string.dialog_no, null)
                                .show();
                    }

                    @Override
                    public void onError(Exception e) {
                        toast(getString(R.string.toast_update_check_failed));
                    }
                });
    }

    // ========================= أدوات مساعدة =========================

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    /** يبسّط تنفيذ SeekBar.OnSeekBarChangeListener بترك onStartTrackingTouch اختيارياً. */
    private abstract static class SimpleSeekBarListener implements SeekBar.OnSeekBarChangeListener {
        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }
    }
}
