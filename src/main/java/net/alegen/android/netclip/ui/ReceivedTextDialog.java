package net.alegen.android.netclip.ui;

import android.app.DialogFragment;

import android.content.res.Configuration;

import android.os.Bundle;

import android.util.Log;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.TextView;

import net.alegen.android.netclip.R;
import net.alegen.android.netclip.netio.CommunicationsManager;
import net.alegen.android.netclip.util.Clipboard;

public class ReceivedTextDialog
    extends DialogFragment
    implements View.OnClickListener {

    private TextView lblClipboard;
    private TextView lblDelete;
    private int index;
    private String text;
    private int width;
    private int height;

    public ReceivedTextDialog() {
        super();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.received_text_dialog, container);

        this.index = this.getArguments().getShort("index");
        this.text = this.getArguments().getCharSequence("text").toString();
        if ( MainActivity.getCurrOrientation() == Configuration.ORIENTATION_PORTRAIT ) {
            this.width = (int)( MainActivity.getCurrWidth() * 0.8 );
            this.height = (int)( MainActivity.getCurrHeight() * 0.6 );
        } else {
            this.width = (int)( MainActivity.getCurrWidth() * 0.5 );
            this.height = (int)( MainActivity.getCurrHeight() * 0.9 );
        }

        this.lblClipboard = (TextView)view.findViewById(R.id.lblClipboard);
        this.lblClipboard.setOnClickListener(this);
        this.lblDelete = (TextView)view.findViewById(R.id.lblDelete);
        this.lblDelete.setOnClickListener(this);
        this.getDialog().setTitle("Text options");
        return view;
    }

    @Override
    public void onClick(View v) {
        if (v == this.lblClipboard)
            Clipboard.getInstance( this.getActivity() ).setClipboardTextAndClean(this.text);
        else if (v == this.lblDelete)
            CommunicationsManager.getInstance().deleteText(this.index);
        this.dismiss();
    }

    @Override
    public void onResume() {
        super.onResume();
        // set the size of the dialog window
        // https://stackoverflow.com/questions/14946887
        // https://stackoverflow.com/questions/12478520
        this.getDialog().getWindow().setLayout(this.width, this.height);
        this.getDialog().getWindow().setGravity(Gravity.CENTER);
    }
}
