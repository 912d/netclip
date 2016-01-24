package net.alegen.android.netclip.netio;

import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

import java.net.Socket;

public class StringsSocket {

    private Socket socket;
    private BufferedReader reader;

    public StringsSocket(Socket socket) throws IOException {
        this.socket = socket;
        this.reader = new BufferedReader(new InputStreamReader( socket.getInputStream() ));
    }

    public Socket getSocket() {
        return this.socket;
    }

    public boolean isReadReady() {
        try {
            return this.reader.ready();
        } catch (IOException ex) {
            Log.e("netclip", "StringsSocket.isReadReady - exception caught - " + ex.getMessage());
            return false;
        }
    }

    public int readAvailable() {
        try {
            return this.socket.getInputStream().available();
        } catch (IOException ex) {
            Log.e("netclip", "StringsSocket.readAvailable - exception caught - " + ex.getMessage());
            return -1;
        }
    }

    public String readString() {
        try {
            return this.reader.readLine();
        } catch (IOException ex) {
            Log.e("netclip", "StringsSocket.readString - exception caught - " + ex.getMessage());
            return null;
        }
    }
}
