package net.alegen.android.netclip.ui;

import android.app.Fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import android.text.Editable;
import android.text.TextWatcher;

import android.util.Log;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import net.alegen.android.netclip.netio.ConnectionsManager;
import net.alegen.android.netclip.netio.StringsSocket;

import net.alegen.android.netclip.R;


public class SendTextFragment
    extends Fragment
    implements TextWatcher, ConnectionsManager.ConnectionEventsListener, View.OnClickListener {

    private static int NEW_CONNECTION = 0;
    private static int CLOSED_CONNECTION = 1;
    private static int TOAST_MESSAGE = 3;
    private static String savedText;

    private Handler handler;
    private EditText edtTextToSend;
    private List<String> spinnerList;
    private ArrayAdapter<String> spinnerAdapter;
    private Spinner spConnectedHosts;
    private TextView lblSendText;
    private TextView lblClearText;

    public SendTextFragment() {
        super();

        this.handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message message) {
                SendTextFragment.this.handleMessage(message);
            }
        };
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i("netclip", "SendTextFragment.onCreateView");
        View view = inflater.inflate(R.layout.send_text_view, container, false);
        this.edtTextToSend = (EditText)view.findViewById(R.id.edtTextToSend);
        this.edtTextToSend.addTextChangedListener(this);
        this.edtTextToSend.setText(savedText);
        this.edtTextToSend.setSelection( savedText.length() );

        this.spinnerList = new ArrayList<String>();
        this.spinnerAdapter = new ArrayAdapter<String>(
            this.getContext(),
            android.R.layout.simple_spinner_dropdown_item,
            this.spinnerList
        );
        this.spConnectedHosts = (Spinner)view.findViewById(R.id.spConnectedHosts);
        this.spConnectedHosts.setAdapter(this.spinnerAdapter);

        ConnectionsManager.getInstance().registerConnectionEventsListener(this);

        List<StringsSocket> sockets = ConnectionsManager.getInstance().getSocketsResource().acquire(
            "SendTextFragment.onCreateView"
        );
        for ( StringsSocket socket : sockets )
            this.spinnerList.add(
                socket.getSocket().getInetAddress().getHostAddress() + " : " +
                String.valueOf(socket.getSocket().getPort())
            );
        ConnectionsManager.getInstance().getSocketsResource().release();
        this.spinnerAdapter.notifyDataSetChanged();

        this.lblSendText = (TextView)view.findViewById(R.id.lblSendText);
        this.lblClearText = (TextView)view.findViewById(R.id.lblClearText);
        this.lblSendText.setOnClickListener(this);
        this.lblClearText.setOnClickListener(this);

        return view;
    }

    @Override
    public void onDestroyView() {
        Log.i("netclip", "SendTextFragment.onDestroyView");
        super.onDestroyView();
        ConnectionsManager.getInstance().unregisterConnectionEventsListener(this);
    }

    @Override
    public void afterTextChanged(Editable s) {
        savedText = s.toString();
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) { }

    @Override
    public void onNewConnection(StringsSocket socket, int kConnections) {
        Log.i("netclip", "SendTextFragment.onNewConnection");
        Message message = this.handler.obtainMessage();
        message.what = NEW_CONNECTION;
        message.obj = socket;
        message.sendToTarget();
    }

    @Override
    public void onClosedConnection(StringsSocket socket, int kConnections) {
        Log.i("netclip", "SendTextFragment.onClosedConnection");
        Message message = this.handler.obtainMessage();
        message.what = CLOSED_CONNECTION;
        message.obj = socket;
        message.sendToTarget();
    }

    private void handleMessage(Message message) {
        if (message.what == NEW_CONNECTION || message.what == CLOSED_CONNECTION) {
            StringsSocket socket = (StringsSocket)message.obj;
            String address = socket.getSocket().getInetAddress().getHostAddress() + " : " +
                String.valueOf(socket.getSocket().getPort());
            if (message.what == NEW_CONNECTION) {
                this.spinnerList.add(address);
            } else if (message.what == CLOSED_CONNECTION) {
                int sckIndex = this.spinnerList.indexOf(address);
                this.spinnerList.remove(sckIndex);
            }
        } else if (message.what == TOAST_MESSAGE) {
            String s = (String)message.obj;
            Toast.makeText(this.getContext(), s, Toast.LENGTH_SHORT).show();
        }
        this.spinnerAdapter.notifyDataSetChanged();
    }

    @Override
    public void onClick(View v) {
        if (v == this.lblSendText) {
            List<StringsSocket> sockets = ConnectionsManager.getInstance().getSocketsResource().acquire(
                "SendTextFragment.onClick"
            );
            int sckIndex = this.spConnectedHosts.getSelectedItemPosition();
            StringsSocket receiver = sockets.get(sckIndex);
            boolean result = receiver.writeString( this.edtTextToSend.getText().toString() );
            Message message = this.handler.obtainMessage();
            message.what = TOAST_MESSAGE;
            if (result == true) {
                this.edtTextToSend.setText("");
                message.obj = "Message has been sent";
            } else
                message.obj = "An error has occured";
            message.sendToTarget();
            ConnectionsManager.getInstance().getSocketsResource().release();
        } else if (v == this.lblClearText) {
            this.edtTextToSend.setText("");
            this.savedText = "";
        }
    }
}
