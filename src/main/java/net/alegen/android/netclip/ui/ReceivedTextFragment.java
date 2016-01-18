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

import net.alegen.android.netclip.netio.CommunicationsManager;
import net.alegen.android.netclip.netio.StringsSocket;

public class ReceivedTextFragment
    extends ListFragment
    implements CommunicationsManager.CommunicationEventsListener {

    private List< Map<String, String> > receivedTexts;
    private SimpleAdapter simpleAdapter;
    private Handler handler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i("netclip", "ReceivedTextFragment.onCreate");
        super.onCreate(savedInstanceState);

        this.handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message message) {
                ReceivedTextFragment.this.handleMessage(message);
            }
        };

        this.receivedTexts = new ArrayList< Map<String, String> >();
        this.simpleAdapter = new SimpleAdapter(
            this.getContext(),
            this.receivedTexts,
            android.R.layout.simple_list_item_2,
            new String[] {"text", "time"},
            new int[] {android.R.id.text1, android.R.id.text2}
        );
        this.setListAdapter(this.simpleAdapter);

        CommunicationsManager.getInstance().acquireReceivedTexts();
        for ( CommunicationsManager.ReceivedText rt : CommunicationsManager.getInstance().getReceivedTexts() )
            this.addReceivedText(rt);
        CommunicationsManager.getInstance().releaseReceivedTexts();
        this.simpleAdapter.notifyDataSetChanged();

        CommunicationsManager.getInstance().registerCommunicationEventsListener(this);
    }

    @Override
    public void onNewReceivedText(CommunicationsManager.ReceivedText rt){
        Log.i("netclip", "ReceivedTextFragment.onNewReceivedText");
        Message message = this.handler.obtainMessage();
        message.obj = rt;
        message.sendToTarget();
    }

    public void handleMessage(Message message) {
        Log.i("netclip", "ReceivedTextFragment.handleMessage");
        CommunicationsManager.ReceivedText rt = (CommunicationsManager.ReceivedText)message.obj;
        this.addReceivedText(rt);
        this.simpleAdapter.notifyDataSetChanged();
    }

    private void addReceivedText(CommunicationsManager.ReceivedText rt) {
        Map<String, String> hm = new HashMap<>();
        hm.put("text", rt.getText() );
        hm.put("time", rt.getTime() );
        this.receivedTexts.add(hm);
    }
}
