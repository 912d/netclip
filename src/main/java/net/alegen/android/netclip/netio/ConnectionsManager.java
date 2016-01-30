package net.alegen.android.netclip.netio;

import android.util.Log;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import java.lang.IllegalArgumentException;
import java.lang.InterruptedException;
import java.lang.SecurityException;
import java.lang.Thread;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import java.nio.channels.IllegalBlockingModeException;

import java.util.ArrayList;
import java.util.List;

import net.alegen.android.netclip.util.SemaphoreResource;

public class ConnectionsManager {

    private static ConnectionsManager instance;

    public static ConnectionsManager getInstance() {
        if (instance == null) {
            instance = new ConnectionsManager();
            instance.startListening();
        }
        return instance;
    }

    private volatile boolean stopListeningThread;
    private volatile boolean listening;
    private volatile boolean stopCheckingSocketsThread;
    private volatile boolean checkingSockets;

    private SemaphoreResource<List<StringsSocket>> socketsResource;
    private Thread listeningThread;
    private Thread checkSocketsThread;
    private List<ConnectionEventsListener> connectionEventsListeners;

    private ConnectionsManager() {
        this.socketsResource = new SemaphoreResource<List<StringsSocket>>(
            new ArrayList<StringsSocket>(),
            "sockets"
        );
        this.connectionEventsListeners = new ArrayList<ConnectionEventsListener>();
        this.listening = false;
        this.checkingSockets = false;
    }

    public void startListening() {
        if (!this.listening) {
            this.listeningThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        SocketAddress socketAddress = new InetSocketAddress(5541);
                        ServerSocket serverSocket = new ServerSocket();
                        serverSocket.bind(socketAddress);
                        ConnectionsManager.this.listening = true;
                        ConnectionsManager.this.stopListeningThread = false;

                        while (!ConnectionsManager.this.stopListeningThread) {
                            try {
                                Socket socket = serverSocket.accept();
                                Log.i("netclip", "ConnectionsManager.startListening.Runnable.run - accepted a connection");
                                socket.setKeepAlive(true);
                                ConnectionsManager.this.newSocket(socket);
                            } catch (SocketException | SocketTimeoutException | IllegalBlockingModeException ex) {
                                Log.e("netclip", "ConnectionsManager.startListening.Runnable.run - exception caught - " + ex.getMessage());
                            }
                        }

                        serverSocket.close();
                        ConnectionsManager.this.listening = false;
                    } catch (IOException | SecurityException | IllegalArgumentException ex) {
                        Log.e("netclip", "ConnectionsManager.startListening.Runnable.run - exception caught - " + ex.getMessage());
                    }
                }
            });
            this.listeningThread.start();
        }
    }

    private void newSocket(Socket socket) {
        try {
            StringsSocket ssocket = new StringsSocket(socket);
            List<StringsSocket> sockets = this.socketsResource.acquire("ConnectionsManager.newSocket");
            sockets.add(ssocket);
            int kSockets = sockets.size();
            this.socketsResource.release();
            if (kSockets > 0 && this.checkingSockets == false)
                this.startCheckingSockets();
            for (ConnectionEventsListener listener : this.connectionEventsListeners)
                listener.onNewConnection(ssocket, kSockets);
        } catch(IOException ex) {
            Log.e("netclip", "ConnectionsManager.newSocket - exception caught - " + ex.getMessage());
        }
    }

    private void startCheckingSockets() {
        if (!this.checkingSockets) {
            this.checkSocketsThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    ConnectionsManager.this.checkingSockets = true;
                    ConnectionsManager.this.stopCheckingSocketsThread = false;

                    while (!ConnectionsManager.this.stopCheckingSocketsThread) {

                        Log.i("netclip", "ConnectionsManager.startCheckingSockets.Runnable.run - checking closed sockets");
                        List<StringsSocket> closedSockets = new ArrayList<StringsSocket>();
                        List<StringsSocket> sockets = ConnectionsManager.this.socketsResource.acquire("ConnectionsManager.startCheckingSocket.run");

                        for (int i = 0; i < sockets.size(); i++) {
                            boolean closedSocket = false;
                            StringsSocket socket = sockets.get(i);

                            try {
                                socket.getSocket().getOutputStream().write( "netclip.ping\r\n".getBytes("UTF-8") );
                            } catch(UnsupportedEncodingException ex) {
                                // should never actually happen
                            } catch(IOException ex) {
                                Log.e("netclip", "ConnectionsManager.startCheckingSockets.Runnable.run - closed socket detected - " + ex.getMessage());
                                closedSocket = true;
                            }

                            if (closedSocket) {
                                sockets.remove(socket);
                                closedSockets.add(socket);
                                if (sockets.size() == 0)
                                    ConnectionsManager.this.stopCheckingSocketsThread = true;
                            }
                        }
                        int kSockets = sockets.size();

                        ConnectionsManager.this.socketsResource.release();

                        for (StringsSocket socket : closedSockets) {
                            for (ConnectionEventsListener listener : ConnectionsManager.this.connectionEventsListeners)
                                listener.onClosedConnection(socket, kSockets);
                        }

                        try {
                            Thread.sleep(1 * 60 * 1000);
                        } catch (IllegalArgumentException | InterruptedException ex) {
                            Log.e("netclip", "ConnectionsManager.startCheckingSockets.Runnable.run - exception caught - " + ex.getMessage());
                        }
                    }

                    ConnectionsManager.this.checkingSockets = false;
                }
            });
            this.checkSocketsThread.start();
        }
    }

    public void stopListening() {
        this.stopListeningThread = true;
    }

    public boolean isListening() {
        return this.listening;
    }

    public void registerConnectionEventsListener(ConnectionEventsListener listener) {
        if (listener != null)
            this.connectionEventsListeners.add(listener);
    }

    public void unregisterConnectionEventsListener(ConnectionEventsListener listener) {
        if (listener != null)
            this.connectionEventsListeners.remove(listener);
    }

    public SemaphoreResource<List<StringsSocket>> getSocketsResource() {
        return this.socketsResource;
    }

    public interface ConnectionEventsListener {
        void onNewConnection(StringsSocket socket, int kConnections);
        void onClosedConnection(StringsSocket socket, int kConnections);
    }
}
