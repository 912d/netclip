package net.alegen.android.netclip.util;

import android.content.ClipboardManager;
import android.content.ClipData;

import java.lang.String;
import java.lang.Thread;

import net.alegen.android.netclip.ui.MainActivity;

public class ClipboardWrapper implements ClipboardManager.OnPrimaryClipChangedListener {

    private static ClipboardWrapper instance;

    public static ClipboardWrapper getInstance() {
        if (instance == null)
            instance = new ClipboardWrapper();
        return instance;
    }

    private ClipboardManager androidClipboard;
    private ClipboardCleanThread currentCleanThread;
    private volatile boolean primaryClipboardChanged;

    private ClipboardWrapper() {
        this.androidClipboard = MainActivity.getAndroidClipboard();
        this.androidClipboard.addPrimaryClipChangedListener(this);
    }

    public void setClipboardText(String text) {
        this.primaryClipboardChanged = false;
        this.androidClipboard.setPrimaryClip(
            ClipData.newPlainText("netclip", text)
        );
    }

    @Override
    public void onPrimaryClipChanged() {
        this.primaryClipboardChanged = true;
    }

    private class ClipboardCleanThread implements Runnable {

        private volatile boolean replaced;

        public ClipboardCleanThread() {
            this.replaced = false;
        }

        public void setReplaced() {
            this.replaced = true;
        }

        @Override
        public void run() {

        }
    }
}
