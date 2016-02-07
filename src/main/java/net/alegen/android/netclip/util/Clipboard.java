package net.alegen.android.netclip.util;

import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.Context;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import android.util.Log;

import android.widget.Toast;

import java.lang.String;
import java.lang.Thread;

import net.alegen.android.netclip.ui.MainActivity;

public class Clipboard implements ClipboardManager.OnPrimaryClipChangedListener {

    private static Clipboard instance;

    public static synchronized Clipboard getInstance(Context context) {
        if (instance == null)
            instance = new Clipboard(context);
        if (context != null)
            instance.context = context;
        return instance;
    }

    private volatile Context context;
    private ClipboardManager androidClipboard;
    private ClipboardCleanRunnable currentCleanRunnable;
    private Thread currentCleanThread;
    private volatile boolean appChangedClipboard;
    private Handler handler;

    private Clipboard(Context context) {
        this.context = context;
        this.androidClipboard = (ClipboardManager)this.context.getSystemService(context.CLIPBOARD_SERVICE);
        this.androidClipboard.addPrimaryClipChangedListener(this);
        this.appChangedClipboard = false;

        this.handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message message) {
                Clipboard.this.handleMessage(message);
            }
        };
    }

    public void setClipboardText(String text) {
        Log.i("netclip", "Clipboard.setClipboardText - " + text);
        this.stopCurrentCleanThread();
        this.appChangedClipboard = true;
        this.androidClipboard.setPrimaryClip(
            ClipData.newPlainText("netclip", text)
        );
        this.currentCleanRunnable = new ClipboardCleanRunnable();
        this.currentCleanThread = new Thread(this.currentCleanRunnable);
        Log.i("netclip", "Clipboard.setClipboardText - starting new clipboard clean thread");
        this.currentCleanThread.start();

        Message message = this.handler.obtainMessage();
        message.obj = "Text copied to clipboard";
        message.sendToTarget();
    }

    @Override
    public void onPrimaryClipChanged() {
        Log.i("netclip", "Clipboard.onPrimaryClipChanged - " + String.valueOf(this.appChangedClipboard));
        if (this.appChangedClipboard == false)
            this.stopCurrentCleanThread();
        else
            this.appChangedClipboard = false;
    }

    private synchronized void stopCurrentCleanThread() {
        Log.i("netclip", "Clipboard.stopCurrentCleanThread");
        if ( this.currentCleanThread != null && this.currentCleanThread.isAlive() ) {
            try {
                this.currentCleanRunnable.setReplaced();
                this.currentCleanThread.join();
            } catch (InterruptedException ex) { }
        }
    }

    public synchronized void reset() {
        this.appChangedClipboard = true;
        this.androidClipboard.setPrimaryClip(
            ClipData.newPlainText("netclip", "")
        );

        Message message = this.handler.obtainMessage();
        message.obj = "Clipboard has been cleared";
        message.sendToTarget();
    }

    public void handleMessage(Message message) {
        String toastMessage = (String)message.obj;
        Toast.makeText(this.context, toastMessage, Toast.LENGTH_SHORT).show();
    }

    private class ClipboardCleanRunnable implements Runnable {

        private volatile boolean replaced;

        public ClipboardCleanRunnable() {
            this.replaced = false;
        }

        public void setReplaced() {
            this.replaced = true;
        }

        @Override
        public void run() {
            Log.i("netclip", "ClipboardCleanRunnable.run");
            int k = 0;
            while (k <= 15 * 10 && this.replaced == false) {
                Log.i("netclip", "ClipboardCleanRunnable.run - iteration");
                try {
                    Thread.sleep(100);
                    k++;
                } catch (InterruptedException ex) { }
            }
            if (this.replaced == false) {
                Log.i("netclip", "ClipboardCleanRunnable.run - cleaning clipboard");
                Clipboard.getInstance(null).reset();
            }
            Log.i("netclip", "ClipboardCleanRunnable.run - finished execution");
        }
    }
}
