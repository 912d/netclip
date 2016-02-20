package net.alegen.android.netclip.ui;

import android.app.FragmentManager;
import android.app.ListFragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import android.util.Log;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.lang.String;

import net.alegen.android.netclip.netio.CommunicationsManager;
import net.alegen.android.netclip.netio.ReceivedText;
import net.alegen.android.netclip.netio.StringsSocket;

public class ReceivedTextFragment
    extends ListFragment
    implements CommunicationsManager.CommunicationEventsListener {

    private static int TEXT_ADDED = 0;
    private static int TEXT_DELETED = 1;

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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i("netclip", "ReceivedTextFragment.onCreateView");
        View view = super.onCreateView(inflater, container, savedInstanceState);

        for ( ReceivedText rt : CommunicationsManager.getInstance().getReceivedTextsResource().acquire(
            "ReceivedTextFragment.onCreate"
        ) )
            this.addReceivedText(rt);
        CommunicationsManager.getInstance().getReceivedTextsResource().release();
        this.simpleAdapter.notifyDataSetChanged();
        CommunicationsManager.getInstance().registerCommunicationEventsListener(this);

        return view;
    }

    @Override
    public void onDestroyView() {
        Log.i("netclip", "ReceivedTextFragment.onDestroyView");
        super.onDestroyView();
        CommunicationsManager.getInstance().unregisterCommunicationEventsListener(this);
        this.receivedTexts.clear();
    }

    @Override
    public void onNewReceivedText(ReceivedText rt){
        Log.i("netclip", "ReceivedTextFragment.onNewReceivedText");
        Message message = this.handler.obtainMessage();
        message.obj = rt;
        message.what = TEXT_ADDED;
        message.sendToTarget();
    }

    @Override
    public void onDeletedText(int index) {
        Log.i("netclip", "ReceivedTextFragment.onNewReceivedText");
        Message message = this.handler.obtainMessage();
        message.obj = index;
        message.what = TEXT_DELETED;
        message.sendToTarget();
    }

    public void handleMessage(Message message) {
        Log.i("netclip", "ReceivedTextFragment.handleMessage");
        if (message.what == TEXT_ADDED) {
            ReceivedText rt = (ReceivedText)message.obj;
            this.addReceivedText(rt);
        } else if (message.what == TEXT_DELETED) {
            this.receivedTexts.remove( (int)message.obj );
        }
        this.simpleAdapter.notifyDataSetChanged();
    }

    private void addReceivedText(ReceivedText rt) {
        Log.i("netclip", "ReceivedTextFragment.addReceivedText - adding text - " + rt.getText() );
        Map<String, String> hm = new HashMap<>();
        hm.put("text", rt.getText() );
        hm.put("time", rt.getTime() );
        this.receivedTexts.add(hm);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        FragmentManager fm = this.getFragmentManager();
        ReceivedTextDialog rtdialog = new ReceivedTextDialog();
        Bundle args = new Bundle();
        args.putShort( "index", (short)position );
        args.putCharSequence( "text", this.receivedTexts.get(position).get("text") );
        rtdialog.setArguments(args);
        rtdialog.show(fm, null);
    }
}
