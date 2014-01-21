package com.funintended.morsepals;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

/**
 * Created by dustin on 1/20/14. :)
 */
public class MorseHelpDialog extends DialogFragment {

    @InjectView(R.id.tree_button)
    Button mTreeButton;

    @InjectView(R.id.list_button)
    Button mListButton;

    @InjectView(R.id.help_image)
    ImageView mHelpImage;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        View view = inflater.inflate(R.layout.dialog_help, null);
        ButterKnife.inject(this, view);
        builder.setView(view)
                // Add action buttons
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // sign in the user ...
                    }
                });
        return builder.create();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        onClose();
    }

    private void onClose() {
        BusProvider.getInstance().post(new HelpDialogDismissedEvent());
    }

    @OnClick(R.id.tree_button)
    void onTreeButtonClicked() {
        mHelpImage.setImageDrawable(getResources().getDrawable(R.drawable.morse_tree));
    }

    @OnClick(R.id.list_button)
    void onListButtonClicked() {
        mHelpImage.setImageDrawable(getResources().getDrawable(R.drawable.morse_index));
    }

    public static class HelpDialogDismissedEvent {

    }
}
