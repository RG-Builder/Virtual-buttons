package com.example.virtualbuttons;

import android.view.View;
import android.widget.AdapterView;

final class SimpleSelectedListener implements AdapterView.OnItemSelectedListener {
    interface Callback { void onSelected(int position); }
    private final Callback callback;
    SimpleSelectedListener(Callback callback) { this.callback = callback; }
    @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { callback.onSelected(position); }
    @Override public void onNothingSelected(AdapterView<?> parent) {}
}
