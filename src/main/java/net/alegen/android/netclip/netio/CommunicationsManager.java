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

import net.alegen.android.netclip.util.SemaphoreResource;

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
    private SemaphoreResource<List<ReceivedText>> receivedTextsResource;
    private List<CommunicationEventsListener> communicationEventsListeners;

    private CommunicationsManager() {
        this.receivedTextsResource = new SemaphoreResource<List<ReceivedText>>(
            new ArrayList<ReceivedText>(),
            "received texts"
        );
        this.communicationEventsListeners = new ArrayList<CommunicationEventsListener>();
        this.reading = false;
    }

    private void startReading() {
        if (!this.reading) {
            this.readingThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    CommunicationsManager.this.reading = true;
                    CommunicationsManager.this.stopReadingThread = false;

                    while (!CommunicationsManager.this.stopReadingThread) {
                        List<StringsSocket> sockets = ConnectionsManager.getInstance().getSocketsResource().acquire(
                            "CommunicationsManager.startReading.run"
                        );
                        for (int i = 0; i < sockets.size(); i++) {
                            StringsSocket socket = sockets.get(i);
                            if (socket.isReadReady() && socket.readAvailable() > 0)
                                CommunicationsManager.this.newReceivedText( socket.readString() );
                        }
                        ConnectionsManager.getInstance().getSocketsResource().release();
                        try {
                            Thread.sleep(500);
                        } catch (IllegalArgumentException | InterruptedException ex) {
                            Log.e(
                                "netclip", "CommunicationsManager.startReading.Runnable.run - exception caught - "
                                + ex.getMessage()
                            );
                        }
                    }

                    CommunicationsManager.this.reading = false;
                }
            });
            this.readingThread.start();
        }
    }

    public SemaphoreResource<List<ReceivedText>> getReceivedTextsResource() {
        return this.receivedTextsResource;
    }

    private void newReceivedText(String text) {
        ReceivedText rt = new ReceivedText(
            text,
            new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date())
        );
        this.receivedTextsResource.acquire("CommunicationsManager.newReceivedText").add(rt);
        this.receivedTextsResource.release();
        this.notifyNewCommunicationEvent(rt);
    }

    private void notifyNewCommunicationEvent(ReceivedText rt) {
        Log.i("netclip", "CommunicationsManager.notifyNewCommunicationEvent - notifying a newly received text");
        for (CommunicationEventsListener listener : this.communicationEventsListeners)
            listener.onNewReceivedText(rt);
    }

    @Override
    public void onNewConnection(StringsSocket socket, int kConnections) {
        Log.i("netclip", "CommunicationsManager.onNewConnection - kConnections = " + String.valueOf(kConnections));
        if (kConnections > 0 && this.reading == false) {
            Log.i("netclip", "CommunicationsManager.onNewConnection - starting reading thread");
            this.startReading();
        }
    }

    @Override
    public void onClosedConnection(StringsSocket socket, int kConnections) {
        Log.i("netclip", "CommunicationsManager.onClosedConnection - kConnections = " + String.valueOf(kConnections));
        if (kConnections == 0 && this.reading == true)
            this.stopReadingThread = true;
    }

    public void deleteText(int index) {
        this.receivedTextsResource.acquire("CommunicationsManager.newReceivedText").remove(index);
        this.receivedTextsResource.release();
        for (CommunicationEventsListener listener : this.communicationEventsListeners)
            listener.onDeletedText(index);
    }

    public void registerCommunicationEventsListener(CommunicationEventsListener listener) {
        if (listener != null)
            this.communicationEventsListeners.add(listener);
    }

    public void unregisterCommunicationEventsListener(CommunicationEventsListener listener) {
        if (listener != null)
            this.communicationEventsListeners.remove(listener);
    }

    public interface CommunicationEventsListener {
        void onNewReceivedText(ReceivedText rt);
        void onDeletedText(int index);
    }
}
