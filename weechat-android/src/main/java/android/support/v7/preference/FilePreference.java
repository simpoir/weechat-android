/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package android.support.v7.preference;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.text.ClipboardManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Base64;
import android.widget.Toast;

import com.ubergeek42.WeechatAndroid.utils.Utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class FilePreference extends DialogPreference {
    private static Logger logger = LoggerFactory.getLogger("FilePreference");

    public FilePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override public CharSequence getSummary() {
        return super.getSummary() + (Utils.isEmpty(getData(getPersistedString(null))) ? " (not set)" : " (set)");
    }

    protected void saveData(@Nullable byte[] bytes) {
        if (callChangeListener(bytes)) {
            persistString(bytes == null ? null : Base64.encodeToString(bytes, Base64.NO_WRAP));
            notifyChanged();
        }
    }

    public static @Nullable byte[] getData(String data) {
        try {return Base64.decode(data.getBytes(), Base64.NO_WRAP);}
        catch (IllegalArgumentException | NullPointerException ignored) {return null;}
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    // this gets called when a file has been picked
    public void onActivityResult(@NonNull Intent intent) {
        try {
            saveData(Utils.readFromUri(getContext(), intent.getData()));
            Toast.makeText(getContext(), "File imported. You can delete it now.", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            logger.error("onActivityResult()", e);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    public static class FilePreferenceFragment extends PreferenceDialogFragmentCompat {

        public static FilePreferenceFragment newInstance(String key, int code) {
            FilePreferenceFragment fragment = new FilePreferenceFragment();
            Bundle b = new Bundle(1);
            b.putString("key", key);
            b.putInt("code", code);
            fragment.setArguments(b);
            return fragment;
        }

        @Override protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
            builder.setNeutralButton("Clear", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) {
                        ((FilePreference) getPreference()).saveData(null);
                        Toast.makeText(getContext(), "Cleared", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Paste", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) {
                        // noinspection deprecation
                        ClipboardManager cm = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                        CharSequence clip = cm.getText();
                        if (TextUtils.isEmpty(clip))
                            Toast.makeText(getContext(), "Clipboard is empty", Toast.LENGTH_SHORT).show();
                        else {
                            ((FilePreference) getPreference()).saveData(clip.toString().getBytes());
                            Toast.makeText(getContext(), "Pasted from clipboard", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setPositiveButton("Choose file", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("*/*");
                        getTargetFragment().startActivityForResult(intent, getArguments().getInt("code"));
                    }
                });
        }

        @Override public void onDialogClosed(boolean b) {}
    }
}