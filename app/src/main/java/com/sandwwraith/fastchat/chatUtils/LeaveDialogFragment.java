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
public class LeaveDialogFragment extends DialogFragment {
    LeaveDialogFragmentListener callBack;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.confirm_leave_title)
                .setPositiveButton(R.string.leave_confirm, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        callBack.onLeaveConfirm();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
//                            LeaveDialogFragment.this.getDialog().cancel();
                    }
                });
        // Create the AlertDialog object and return it
        return builder.create();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        callBack = (LeaveDialogFragmentListener) activity;
    }

    public interface LeaveDialogFragmentListener {
        void onLeaveConfirm();
    }
}
