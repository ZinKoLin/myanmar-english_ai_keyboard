package com.zrok.myanmaraikeyboard;

import android.app.Instrumentation;
import android.app.Activity;
import android.app.UiAutomation;
import android.content.Intent;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import java.util.ArrayList;
import java.util.List;

/** Device-only probe: edits the IME draft and never inserts or sends a message. */
public class KeyboardTranslationProbe extends Instrumentation {
    private Bundle arguments;

    @Override public void onCreate(Bundle args) { arguments = args; start(); }

    private List<AccessibilityNodeInfo> nodes() {
        List<AccessibilityNodeInfo> result = new ArrayList<>();
        for (AccessibilityWindowInfo window : getUiAutomation().getWindows()) {
            if (window.getType() == AccessibilityWindowInfo.TYPE_INPUT_METHOD) {
                collect(window.getRoot(), result);
            }
        }
        return result;
    }

    private void collect(AccessibilityNodeInfo node, List<AccessibilityNodeInfo> result) {
        if (node == null) return;
        result.add(node);
        for (int i = 0; i < node.getChildCount(); i++) collect(node.getChild(i), result);
    }

    private AccessibilityNodeInfo find(String label) {
        for (AccessibilityNodeInfo node : nodes()) {
            if (label.equals(String.valueOf(node.getText())) ||
                    label.equals(String.valueOf(node.getContentDescription()))) return node;
        }
        return null;
    }

    private void click(String label) throws java.util.concurrent.TimeoutException {
        long deadline = SystemClock.uptimeMillis() + 3000;
        boolean clicked = false;
        while (!clicked && SystemClock.uptimeMillis() < deadline) {
            AccessibilityNodeInfo node = find(label);
            clicked = node != null && node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            if (!clicked) SystemClock.sleep(100);
        }
        if (!clicked) throw new IllegalStateException("Missing or unclickable: " + label);
        getUiAutomation().waitForIdle(300, 3000);
    }

    private AccessibilityNodeInfo editor() {
        for (AccessibilityNodeInfo node : nodes()) {
            if ("android.widget.EditText".contentEquals(node.getClassName()) && node.isVisibleToUser()) return node;
        }
        throw new IllegalStateException("IME editor missing");
    }

    private EditText findSearch(View view) {
        if (view instanceof EditText) return (EditText) view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                EditText result = findSearch(group.getChildAt(i));
                if (result != null) return result;
            }
        }
        return null;
    }

    private void screenshot(String name) throws java.io.IOException {
        java.io.File file = new java.io.File(getTargetContext().getExternalFilesDir(null), name);
        try (java.io.FileOutputStream stream = new java.io.FileOutputStream(file)) {
            android.graphics.Bitmap bitmap = getUiAutomation().takeScreenshot();
            if (bitmap == null || !bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)) {
                throw new java.io.IOException("Cannot capture UI");
            }
        }
    }

    private void setDraft(String value) {
        Bundle text = new Bundle();
        text.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value);
        if (!editor().performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, text)) throw new IllegalStateException("Cannot set draft");
        SystemClock.sleep(150);
    }

    private void draftEquals(String value) {
        if (!value.equals(String.valueOf(editor().getText()))) throw new IllegalStateException("Draft mismatch: " + editor().getText());
    }

    @Override public void onStart() {
        Bundle result = new Bundle();
        try {
            if ("mon".equals(arguments.getString("action"))) {
                String report = MonTranslationChecks.run(getTargetContext(), line -> {
                    Bundle progress = new Bundle(); progress.putString("stream", line); sendStatus(0, progress);
                });
                result.putString("stream", "PASS: Mon dictionary and whole-request timing checks\n" + report);
                finish(-1, result);
                return;
            }
            if ("feedback".equals(arguments.getString("action"))) {
                com.zrok.myanmaraikeyboard.data.AppPreferences preferences = new com.zrok.myanmaraikeyboard.data.AppPreferences(getTargetContext());
                preferences.setKeyboardSoundEnabled(true);
                preferences.setKeyboardVibrationEnabled(true);
                preferences.setKeyboardTone(3);
                preferences.setKeyboardSoundVolume(0.5f);
                preferences.setSystemKeyboardHaptic(true);
            }
            if ("providers".equals(arguments.getString("action"))) {
                result.putString("stream", SuggestionProviderChecks.run(getTargetContext()));
                finish(-1, result);
                return;
            }
            Activity activity = startActivitySync(new Intent(getTargetContext(), LanguageSettingsActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            runOnMainSync(() -> {
                EditText search = findSearch(activity.getWindow().getDecorView());
                search.requestFocus();
                ((InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE))
                    .showSoftInput(search, InputMethodManager.SHOW_IMPLICIT);
            });
            UiAutomation ui = getUiAutomation();
            AccessibilityServiceInfo info = ui.getServiceInfo();
            info.flags |= AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
            ui.setServiceInfo(info);
            long keyboardDeadline = SystemClock.uptimeMillis() + 10000;
            while (nodes().isEmpty() && SystemClock.uptimeMillis() < keyboardDeadline) SystemClock.sleep(250);
            ui.waitForIdle(300, 3000);
            if ("feedback".equals(arguments.getString("action"))) {
                if (find("Open translation") != null) click("Open translation");
                click("Clear");
                click("မူရင်း (Auto)");
                if (find("ဪ") == null) click("Switch Myanmar / English; hold to use another keyboard");
                click("က"); click("မ"); click("Space"); click("Enter, new line"); click("Delete");
                draftEquals("ကမ ");
                click("Clear");
                click("Switch Myanmar / English; hold to use another keyboard");
                click("q; hold for 1"); click("w; hold for 2"); click("Space"); click("Enter, new line"); click("Delete");
                draftEquals("qw ");
                click("Clear");
                click("Switch Myanmar / English; hold to use another keyboard");
                screenshot("keyboard-feedback-enabled.png");
                result.putString("stream", "PASS: Sound and system haptic enabled; Myanmar/English letter, Space, Enter and Delete keys pressed; both drafts matched; test text cleared.\n");
            } else if ("dump".equals(arguments.getString("action"))) {
                StringBuilder dump = new StringBuilder();
                for (AccessibilityNodeInfo node : nodes()) {
                    if (node.getText() != null || node.getContentDescription() != null) {
                        dump.append(node.getClassName()).append(" | ").append(node.getText())
                            .append(" | ").append(node.getContentDescription()).append('\n');
                    }
                }
                result.putString("stream", dump.toString());
            } else if ("revision".equals(arguments.getString("action"))) {
                final java.util.concurrent.atomic.AtomicReference<String> receivedMime = new java.util.concurrent.atomic.AtomicReference<>();
                final java.util.concurrent.atomic.AtomicInteger receivedBytes = new java.util.concurrent.atomic.AtomicInteger();
                runOnMainSync(() -> {
                    EditText old = findSearch(activity.getWindow().getDecorView());
                    ViewGroup parent = (ViewGroup) old.getParent();
                    int index = parent.indexOfChild(old);
                    android.view.ViewGroup.LayoutParams layout = old.getLayoutParams();
                    EditText receiver = new EditText(activity) {
                        @Override public android.view.inputmethod.InputConnection onCreateInputConnection(android.view.inputmethod.EditorInfo out) {
                            android.view.inputmethod.InputConnection base = super.onCreateInputConnection(out);
                            out.contentMimeTypes = new String[]{"image/png", "image/gif"};
                            return new android.view.inputmethod.InputConnectionWrapper(base, false) {
                                @Override public boolean commitContent(android.view.inputmethod.InputContentInfo content, int flags, Bundle opts) {
                                    try {
                                        content.requestPermission();
                                        byte[] bytes;
                                        try (java.io.InputStream stream = activity.getContentResolver().openInputStream(content.getContentUri())) { bytes = stream.readAllBytes(); }
                                        receivedMime.set(content.getDescription().getMimeType(0));
                                        receivedBytes.set(bytes.length);
                                        content.releasePermission();
                                        return true;
                                    } catch (Exception error) { return false; }
                                }
                            };
                        }
                    };
                    parent.removeView(old); parent.addView(receiver, index, layout);
                    receiver.requestFocus();
                    ((InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE)).showSoftInput(receiver, InputMethodManager.SHOW_IMPLICIT);
                });
                SystemClock.sleep(500);
                runOnMainSync(() -> {
                    EditText receiver = findSearch(activity.getWindow().getDecorView());
                    receiver.requestFocus();
                    InputMethodManager manager = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
                    manager.restartInput(receiver);
                    manager.showSoftInput(receiver, InputMethodManager.SHOW_IMPLICIT);
                });
                long receiverDeadline = SystemClock.uptimeMillis() + 10000;
                while (find("Open translation") == null && find("Close translation") == null && SystemClock.uptimeMillis() < receiverDeadline) SystemClock.sleep(200);
                runOnMainSync(() -> findSearch(activity.getWindow().getDecorView()).setText("external draft"));
                if (find("Open translation") != null) click("Open translation");
                click("မူရင်း (Auto)");
                setDraft("source draft");
                String previewTab = null;
                for (AccessibilityNodeInfo node : nodes()) {
                    String description = String.valueOf(node.getContentDescription());
                    if (description.endsWith(" preview")) { previewTab = description; break; }
                }
                if (previewTab == null) throw new IllegalStateException("Preview tab missing");
                click(previewTab);
                setDraft("translated draft");
                click("Clear");
                if (!editor().isShowingHintText()) throw new IllegalStateException("Source survived clear");
                click(previewTab);
                if (!editor().isShowingHintText()) throw new IllegalStateException("Preview survived clear");
                click("Undo");
                if (!editor().isShowingHintText()) throw new IllegalStateException("Undo restored clear");
                final java.util.concurrent.atomic.AtomicBoolean externalEmpty = new java.util.concurrent.atomic.AtomicBoolean();
                runOnMainSync(() -> externalEmpty.set(findSearch(activity.getWindow().getDecorView()).getText().length() == 0));
                if (!externalEmpty.get()) throw new IllegalStateException("External text survived clear");
                click("မူရင်း (Auto)");
                setDraft("မင်္ဂလာပါ၊ နေကောင်းလား။");
                click("Translate →");
                click("Clear");
                SystemClock.sleep(2000);
                if (!editor().isShowingHintText()) throw new IllegalStateException("Cancelled request restored text");
                click(previewTab);
                if (!editor().isShowingHintText()) throw new IllegalStateException("Cancelled request restored preview");
                click("မူရင်း (Auto)");
                screenshot("keyboard-clear-placeholder.png");
                click("Close translation"); click("Open translation");
                if (!editor().isShowingHintText()) throw new IllegalStateException("Reopen restored draft");
                click("Close translation");
                if (find("ဪ") == null) click("Switch Myanmar / English; hold to use another keyboard");
                find("၊; hold for emoji").performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK);
                click("Open stickers");
                if (find("Import sticker") == null) throw new IllegalStateException("Sticker import missing");
                long stickerDeadline = SystemClock.uptimeMillis() + 5000;
                while (find("Insert sticker မင်္ဂလာပါ") == null && SystemClock.uptimeMillis() < stickerDeadline) SystemClock.sleep(100);
                screenshot("keyboard-stickers.png");
                click("Insert sticker ကျေးဇူးပါ");
                long commitDeadline = SystemClock.uptimeMillis() + 5000;
                while (receivedBytes.get() == 0 && SystemClock.uptimeMillis() < commitDeadline) SystemClock.sleep(100);
                if (!"image/png".equals(receivedMime.get()) || receivedBytes.get() < 100) throw new IllegalStateException("Sticker content was not delivered to editor");
                click("Return to keys");
                result.putString("stream", "PASS: Clear removed source, preview, external text and undo; in-flight translation stayed cleared; reopen stayed empty; sticker PNG committed to test editor\n" + RevisionChecks.run(getTargetContext()));
            } else if ("layout".equals(arguments.getString("action"))) {
                if (find("Open translation") != null) click("Open translation");
                click("မူရင်း (Auto)");
                if (find("ဪ") == null) click("Switch Myanmar / English; hold to use another keyboard");
                setDraft("");
                click("က");
                click("Shift");
                click("ဃ");
                click("Space");
                click("Enter, new line");
                draftEquals("ကဃ \n");
                click("Switch Myanmar / English; hold to use another keyboard");
                setDraft("");
                click("q; hold for 1");
                click("w; hold for 2");
                click("e; hold for 3");
                click("Delete");
                draftEquals("qw");
                if (!find("q; hold for 1").performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)) throw new IllegalStateException("Number hold failed");
                SystemClock.sleep(200);
                draftEquals("qw1");
                if (!find(",; hold for emoji").performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)) throw new IllegalStateException("Emoji hold failed");
                click("Insert emoji 😃");
                click("ABC");
                draftEquals("qw1😃");
                setDraft("abcdefghij");
                android.graphics.Rect bounds = new android.graphics.Rect();
                find("Delete").getBoundsInScreen(bounds);
                long downTime = SystemClock.uptimeMillis();
                android.view.MotionEvent down = android.view.MotionEvent.obtain(downTime, downTime, android.view.MotionEvent.ACTION_DOWN, bounds.centerX(), bounds.centerY(), 0);
                down.setSource(android.view.InputDevice.SOURCE_TOUCHSCREEN);
                ui.injectInputEvent(down, true); down.recycle();
                SystemClock.sleep(650);
                android.view.MotionEvent up = android.view.MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), android.view.MotionEvent.ACTION_UP, bounds.centerX(), bounds.centerY(), 0);
                up.setSource(android.view.InputDevice.SOURCE_TOUCHSCREEN);
                ui.injectInputEvent(up, true); up.recycle();
                if (String.valueOf(editor().getText()).length() >= 9) throw new IllegalStateException("Held delete did not repeat");
                setDraft("");
                click("Close translation");
                screenshot("keyboard-layout-english.png");
                click("Switch Myanmar / English; hold to use another keyboard");
                screenshot("keyboard-layout-myanmar.png");
                click("Shift");
                screenshot("keyboard-layout-myanmar-shift.png");
                click("Shift");
                result.putString("stream", "PASS: Myanmar keys/Shift, English QWERTY, Space, Enter, number hold, emoji hold, delete and repeated delete; layout screenshots captured\n");
            } else if ("suggestions".equals(arguments.getString("action")) || "enhanced".equals(arguments.getString("action"))) {
                String providerReport = "enhanced".equals(arguments.getString("action")) ? SuggestionProviderChecks.run(getTargetContext()) : "";
                if (find("Open translation") != null) click("Open translation");
                click("မူရင်း (Auto)");
                if (find("မြန်မာ") != null) click("Switch Myanmar / English; hold to use another keyboard");
                Bundle warmup = new Bundle();
                warmup.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "architecture");
                editor().performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, warmup);
                long dictionaryDeadline = SystemClock.uptimeMillis() + 10000;
                while (find("Known English word architecture") == null && SystemClock.uptimeMillis() < dictionaryDeadline) SystemClock.sleep(100);
                String[][] samples = {
                    {"helo", "Use suggestion hello"},
                    {"recieve", "Use suggestion receive"},
                    {"archtecture", "Use suggestion architecture"},
                    {"hipopatamus", "Use suggestion hippopotamus"},
                    {"architecture", "Known English word architecture"}
                };
                StringBuilder report = new StringBuilder();
                for (String[] sample : samples) {
                    Bundle text = new Bundle();
                    text.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, sample[0]);
                    if (!editor().performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, text)) throw new IllegalStateException("Cannot set English source");
                    long deadline = SystemClock.uptimeMillis() + 10000;
                    while (find(sample[1]) == null && SystemClock.uptimeMillis() < deadline) SystemClock.sleep(100);
                    if (find(sample[1]) == null) throw new IllegalStateException("Missing suggestion: " + sample[1]);
                    report.append(sample[0]).append(" => ").append(sample[1]).append('\n');
                    if (sample[0].equals("helo")) {
                        click(sample[1]);
                        if (!"hello".equals(String.valueOf(editor().getText()))) throw new IllegalStateException("Suggestion did not replace the typo");
                    }
                }
                screenshot("keyboard-probe-english.png");
                click("Close translation");
                click("Switch Myanmar / English; hold to use another keyboard");
                if (find("ဪ") == null) throw new IllegalStateException("Myanmar key missing after language switch");
                screenshot("keyboard-probe-myanmar.png");
                result.putString("stream", providerReport + report);
            } else {
                if (find("Open translation") != null) click("Open translation");
                String[][] samples = {
                    {"Spanish", "Hola, ¿cómo estás?"},
                    {"French", "Bonjour, comment allez-vous ?"},
                    {"Thai", "สวัสดี คุณสบายดีไหม"},
                    {"Japanese", "こんにちは、お元気ですか？"},
                    {"Arabic", "مرحبا، كيف حالك؟"}
                };
                StringBuilder report = new StringBuilder();
                for (String[] sample : samples) {
                    click("မူရင်း (Auto)");
                    Bundle text = new Bundle();
                    text.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, sample[1]);
                    if (!editor().performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, text)) {
                        throw new IllegalStateException("Cannot set source text");
                    }
                    ui.waitForIdle(300, 3000);
                    if (!sample[1].equals(String.valueOf(editor().getText()))) throw new IllegalStateException("Source mismatch");
                    click("Translate →");
                    long deadline = SystemClock.uptimeMillis() + 65000;
                    String output = "";
                    while (SystemClock.uptimeMillis() < deadline) {
                        SystemClock.sleep(500);
                        AccessibilityNodeInfo field = editor();
                        if (!field.isShowingHintText()) output = String.valueOf(field.getText());
                        if (!output.isEmpty() && !output.equals(sample[1])) break;
                    }
                    String row = sample[0] + ": " + sample[1] + " => " + output + "\n";
                    report.append(row);
                    Bundle progress = new Bundle(); progress.putString("stream", row); sendStatus(0, progress);
                    if (output.isEmpty() || output.equals(sample[1])) throw new IllegalStateException("Translation did not complete: " + sample[0]);
                }
                result.putString("stream", report.toString());
            }
            finish(-1, result);
        } catch (Throwable error) {
            try { screenshot("keyboard-layout-failure.png"); } catch (Throwable ignored) {}
            StringBuilder details = new StringBuilder();
            for (AccessibilityNodeInfo node : nodes()) {
                if (node.getText() != null || node.getContentDescription() != null)
                    details.append(node.getText()).append(" | ").append(node.getContentDescription()).append('\n');
            }
            result.putString("stream", "FAILED: " + error.toString() + "\n" + details);
            finish(0, result);
        }
    }
}
