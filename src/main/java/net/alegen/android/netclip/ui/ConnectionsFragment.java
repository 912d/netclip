package net.alegen.android.netclip.ui;

import android.app.ListFragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import android.util.Log;

import android.widget.SimpleAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.lang.String;

import net.alegen.android.netclip.netio.ConnectionsManager;
import net.alegen.android.netclip.netio.StringsSocket;

public class ConnectionsFragment
    extends ListFragment
    implements ConnectionsManager.ConnectionEventsListener {

    private static int NEW_CONNECTION = 0;
    private static int CLOSED_CONNECTION = 1;

    private List< Map<String, String> > connections;
    private SimpleAdapter simpleAdapter;
    private Handler handler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i("netclip", "ConnectionsFragment.onCreate");
        super.onCreate(savedInstanceState);

        this.handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message message) {
                ConnectionsFragment.this.handleMessage(message);
            }
        };

        this.connections = new ArrayList< Map<String, String> >();
        this.simpleAdapter = new SimpleAdapter(
            this.getContext(),
            this.connections,
            android.R.layout.simple_list_item_2,
            new String[] {"address", "port"},
            new int[] {android.R.id.text1, android.R.id.text2}
        );
        this.setListAdapter(this.simpleAdapter);

        ConnectionsManager.getInstance().acquireSockets();
        for ( StringsSocket socket : ConnectionsManager.getInstance().getSockets() )
            this.addInfoFromSocket(socket);
        ConnectionsManager.getInstance().releaseSockets();
        this.simpleAdapter.notifyDataSetChanged();

        ConnectionsManager.getInstance().registerConnectionEventsListener(this);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        Log.i("netclip", "ConnectionsFragment.onDestroy");
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onDestroy() {
        Log.i("netclip", "ConnectionsFragment.onDestroy");
        super.onDestroy();
        ConnectionsManager.getInstance().unregisterConnectionEventsListener(this);
    }

    @Override
    public void onNewConnection(StringsSocket socket, int kConnections) {
        Log.i("netclip", "ConnectionsFragment.onNewConnection - kConnections = " + String.valueOf(kConnections));
        Message message = this.handler.obtainMessage();
        message.what = NEW_CONNECTION;
        message.obj = socket;
        message.sendToTarget();
    }

    @Override
    public void onClosedConnection(StringsSocket socket, int kConnections) {
        Log.i("netclip", "ConnectionsFragment.onNewConnection - kConnections = " + String.valueOf(kConnections));
        Message message = this.handler.obtainMessage();
        message.what = CLOSED_CONNECTION;
        message.obj = socket;
        message.sendToTarget();
    }

    public void handleMessage(Message message) {
        Log.i("netclip", "ConnectionsFragment.handleMessage");
        StringsSocket socket = (StringsSocket)message.obj;
        if (message.what == NEW_CONNECTION)
            this.addInfoFromSocket(socket);
        else if (message.what == CLOSED_CONNECTION)
            this.removeInfoAboutSocket(socket);
        this.simpleAdapter.notifyDataSetChanged();
    }

    private void addInfoFromSocket(StringsSocket socket) {
        Map<String, String> hm = new HashMap<>();
        hm.put("address", socket.getSocket().getInetAddress().getHostAddress() );
        hm.put("port", "Remote port: " + String.valueOf(socket.getSocket().getPort()) );
        this.connections.add(hm);
    }

    private void removeInfoAboutSocket(StringsSocket socket) {
        String address = socket.getSocket().getInetAddress().getHostAddress();
        for (int i = 0; i < this.connections.size(); i++) {
            Map<String, String> hm = this.connections.get(i);
            if ( hm.get("address").equals(address) ) {
                this.connections.remove(i);
                return;
            }
        }
    }
}
