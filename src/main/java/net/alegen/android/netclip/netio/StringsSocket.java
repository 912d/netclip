package net.alegen.android.netclip.netio;

import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintWriter;

import java.net.Socket;

public class StringsSocket {

    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;

    public StringsSocket(Socket socket) throws IOException {
        this.socket = socket;
        this.reader = new BufferedReader(new InputStreamReader( socket.getInputStream() ));
        this.writer = new PrintWriter( socket.getOutputStream(), true );
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

    public synchronized String readString() {
        try {
            return this.reader.readLine();
        } catch (IOException ex) {
            Log.e("netclip", "StringsSocket.readString - exception caught - " + ex.getMessage());
            return null;
        }
    }

    public synchronized boolean writeString(String s) {
        this.writer.write(s + "\n");
        return this.writer.checkError();
    }
}
