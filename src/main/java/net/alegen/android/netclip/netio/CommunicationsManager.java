package net.alegen.android.netclip.netio;

import android.util.Log;

import java.io.IOException;

import java.lang.IllegalArgumentException;
import java.lang.InterruptedException;
import java.lang.Thread;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import java.util.concurrent.Semaphore;

public class CommunicationsManager implements ConnectionsManager.ConnectionEventsListener {

    private static CommunicationsManager instance;

    public static CommunicationsManager getInstance() {
        if (instance == null) {
            instance = new CommunicationsManager();
            ConnectionsManager.getInstance().registerConnectionEventsListener(instance);
        }
        return instance;
    }

    private volatile boolean reading;
    private volatile boolean stopReadingThread;

    private Thread readingThread;
    private Semaphore receivedTextsSemaphore;
    private List<ReceivedText> receivedTexts;
    private List<CommunicationEventsListener> communicationEventsListeners;

    private CommunicationsManager() {
        this.receivedTexts = new ArrayList<ReceivedText>();
        this.communicationEventsListeners = new ArrayList<CommunicationEventsListener>();
        this.reading = false;
        this.receivedTextsSemaphore = new Semaphore(1, true);
    }

    private void startReading() {
        if (!this.reading) {
            this.readingThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    CommunicationsManager.this.reading = true;
                    CommunicationsManager.this.stopReadingThread = false;

                    while (!CommunicationsManager.this.stopReadingThread) {
                        ConnectionsManager.getInstance().acquireSockets();
                        List<StringsSocket> sockets = ConnectionsManager.getInstance().getSockets();
                        for (int i = 0; i < sockets.size(); i++) {
                            StringsSocket socket = sockets.get(i);
                            if (socket.isReadReady())
                                CommunicationsManager.this.newReceivedText( socket.readString() );
                        }
                        ConnectionsManager.getInstance().releaseSockets();
                        try {
                            Thread.sleep(500);
                        } catch (IllegalArgumentException | InterruptedException ex) {
                            Log.e("netclip", "CommunicationsManager.startReading.Runnable.run - exception caught - " + ex.getMessage());
                        }
                    }

                    CommunicationsManager.this.reading = false;
                }
            });
            this.readingThread.run();
        }
    }

    public List<ReceivedText> getReceivedTexts() {
        return this.receivedTexts;
    }

    private void newReceivedText(String text) {
        ReceivedText rt = new ReceivedText(
            text,
            new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date())
        );
        this.acquireReceivedTexts();
        this.receivedTexts.add(rt);
        this.releaseReceivedTexts();
        this.notifyNewCommunicationEvent(rt);
    }

    private void notifyNewCommunicationEvent(ReceivedText rt) {
        Log.i("netclip", "CommunicationsManager.notifyNewCommunicationEvent - notifying a newly received text");
        for (CommunicationEventsListener listener : this.communicationEventsListeners)
            listener.onNewReceivedText(rt);
    }

    public void acquireReceivedTexts() {
        try {
            this.receivedTextsSemaphore.acquire();
        } catch (InterruptedException ex) {
            Log.e("netclip", "CommunicationsManager.acquireReceivedTexts - exception caught - " + ex.getMessage());
        }
    }

    public void releaseReceivedTexts() {
        this.receivedTextsSemaphore.release();
    }

    @Override
    public void onNewConnection(StringsSocket socket, int kConnections) {
        Log.i("netclip", "CommunicationsManager.onNewConnection - kConnections = " + String.valueOf(kConnections));
        if (kConnections == 1) {
            Log.i("netclip", "CommunicationsManager.onNewConnection - starting reading thread");
            this.startReading();
        }
    }

    @Override
    public void onClosedConnection(StringsSocket socket, int kConnections) {
        Log.i("netclip", "CommunicationsManager.onClosedConnection - kConnections = " + String.valueOf(kConnections));
        if (kConnections == 0)
            this.stopReadingThread = true;
    }

    public void registerCommunicationEventsListener(CommunicationEventsListener listener) {
        if (listener != null)
            this.communicationEventsListeners.add(listener);
    }

    public interface CommunicationEventsListener {
        void onNewReceivedText(ReceivedText rt);
    }
}
