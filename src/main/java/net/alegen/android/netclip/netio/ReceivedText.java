package net.alegen.android.netclip.netio;

import java.lang.String;

public class ReceivedText {

    private String text;
    private String time;

    public ReceivedText(String text, String time) {
        this.text = text;
        this.time = time;
    }

    public String getText() {
        return this.text;
    }

    public String getTime() {
        return this.time;
    }
}
