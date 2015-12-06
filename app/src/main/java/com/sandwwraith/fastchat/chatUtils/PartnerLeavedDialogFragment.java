package com.sandwwraith.fastchat.chatUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

import com.sandwwraith.fastchat.R;

/**
 * Created by sandwwraith(@gmail.com)
 * ITMO University, 2015.
 */
public class PartnerLeavedDialogFragment extends DialogFragment {
    PartnerLeavedDialogListener mListener;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mListener = (PartnerLeavedDialogListener) activity;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.partner_leaved)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mListener.returnToMain(true);
                    }
                })
                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mListener.returnToMain(false);
                    }
                });
        // Create the AlertDialog object and return it
        return builder.create();
    }

    public interface PartnerLeavedDialogListener {
        void returnToMain(boolean enqueueNow);
    }
}
