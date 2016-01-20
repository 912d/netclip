package net.alegen.android.netclip.ui;

import android.app.DialogFragment;

import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.TextView;

import net.alegen.android.netclip.R;

public class ReceivedTextDialog extends DialogFragment {

    private TextView lblClipboard;
    private TextView lblDelete;

    public ReceivedTextDialog() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.received_text_dialog, container);
        this.lblClipboard = (TextView)view.findViewById(R.id.lblClipboard);
        this.lblDelete = (TextView)view.findViewById(R.id.lblDelete);
        this.getDialog().setTitle("Text options");
        return view;
    }
}
