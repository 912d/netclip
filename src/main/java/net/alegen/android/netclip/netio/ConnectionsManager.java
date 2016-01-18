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

import java.util.concurrent.Semaphore;

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

    private List<StringsSocket> sockets;
    private Semaphore socketsSemaphore;
    private Thread listeningThread;
    private Thread checkSocketsThread;
    private List<ConnectionEventsListener> connectionEventsListeners;

    private ConnectionsManager() {
        this.sockets = new ArrayList<StringsSocket>();
        this.socketsSemaphore = new Semaphore(1, true);
        this.connectionEventsListeners = new ArrayList<ConnectionEventsListener>();
        this.listening = false;
        this.checkingSockets = false;
    }

    public void acquireSockets() {
        try {
            this.socketsSemaphore.acquire();
        } catch (InterruptedException ex) {
            Log.e("netclip", "ConnectionsManager.acquireSockets - exception caught - " + ex.getMessage());
        }
    }

    public void releaseSockets() {
        this.socketsSemaphore.release();
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
            this.acquireSockets();
            this.sockets.add(ssocket);
            this.releaseSockets();
            if (this.sockets.size() == 1)
                this.startCheckingSockets();
            this.notifyNewConnectionEvent(ssocket);
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
                        ConnectionsManager.this.acquireSockets();
                        for (int i = 0; i < ConnectionsManager.this.sockets.size(); i++) {
                            boolean closedSocket = false;
                            StringsSocket socket = ConnectionsManager.this.sockets.get(i);

                            try {
                                socket.getSocket().getOutputStream().write( "netclip.ping\r\n".getBytes("UTF-8") );
                            } catch(UnsupportedEncodingException ex) {
                                // should never actually happen
                            } catch(IOException ex) {
                                Log.e("netclip", "ConnectionsManager.startCheckingSockets.Runnable.run - closed socket detected - " + ex.getMessage());
                                closedSocket = true;
                            }

                            if (closedSocket)
                                ConnectionsManager.this.closedSocket(socket);
                        }
                        ConnectionsManager.this.releaseSockets();
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

    private void closedSocket(StringsSocket socket) {
        this.sockets.remove(socket);
        if (this.sockets.size() == 0)
            this.stopCheckingSocketsThread = true;
        this.notifyClosedConnectionEvent(socket);
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

    private void notifyNewConnectionEvent(StringsSocket socket) {
        Log.i("netclip", "ConnectionsManager.notifyNewConnectionEvent - notifying a newly accepted connection");
        for (ConnectionEventsListener listener : this.connectionEventsListeners)
            listener.onNewConnection(socket, this.sockets.size());
    }

    private void notifyClosedConnectionEvent(StringsSocket socket) {
        Log.i("netclip", "ConnectionsManager.notifyClosedConnectionEvent - notifying a closed connection");
        for (ConnectionEventsListener listener : this.connectionEventsListeners)
            listener.onClosedConnection(socket, this.sockets.size());
    }

    public List<StringsSocket> getSockets() {
        return this.sockets;
    }

    public interface ConnectionEventsListener {
        void onNewConnection(StringsSocket socket, int kConnections);
        void onClosedConnection(StringsSocket socket, int kConnections);
    }
}
