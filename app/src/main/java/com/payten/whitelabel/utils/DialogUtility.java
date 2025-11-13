package com.payten.whitelabel.utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.payten.whitelabel.R;


public class DialogUtility {

    private static final String TAG = "DialogUtility";

    private Context mContext;
    private DialogInteractionListener mDialogInteractionListener;
    private DialogConfirmListener mConfirmListener;
    private AlertDialog ereceiptDialog;
    private AlertDialog emailDialog;

    public void setmDialogInteractionListener(DialogInteractionListener mDialogInteractionListener) {
        this.mDialogInteractionListener = mDialogInteractionListener;
    }

    public void setmConfirmListener(DialogConfirmListener mConfirmListener) {
        this.mConfirmListener = mConfirmListener;
    }

    public DialogUtility(Context context) {
        mContext = context;
    }

    public void displaySendReportDialog(String dateFrom, String dateTo) {

        View view = LayoutInflater.from(mContext).inflate(R.layout.report_dialog, null);
        TextView mTittle = view.findViewById(R.id.ereceipt_enter_email);
        Button mSendEmailBtn = view.findViewById(R.id.ereceipt_send_btn);
        Button mCancelBtn = view.findViewById(R.id.ereceipt_cancel_send_btn);
        ImageButton mClose = view.findViewById(R.id.close);
        RadioButton mPdfFormat = view.findViewById(R.id.radio_pdf);
        RadioButton mXlsFormat = view.findViewById(R.id.radio_xls);


        EditText emailEditTxt = view.findViewById(R.id.email_edit_txt);
        String period = "";
        period += dateFrom + " - " + dateTo;
        mTittle.setText("Slanje izveštaja za period: \n\n" + period);

        AlertDialog alertDialog = new AlertDialog.Builder(mContext)
                .setView(view)
                .create();
        alertDialog.show();

        mCancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alertDialog.dismiss();
            }
        });
        mClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alertDialog.dismiss();
            }
        });
        mSendEmailBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String userEmail = emailEditTxt.getText().toString().trim();
                if (Utility.isEmailValid(userEmail)) {
                    if (mDialogInteractionListener != null) {
                        if(mPdfFormat.isChecked()){
                            String fileFormat = "pdf";
                            mDialogInteractionListener.onSendReportToEmail(userEmail,dateFrom, dateTo,fileFormat);
                        }else if(mXlsFormat.isChecked()){
                            String fileFormat = "xls";
                            mDialogInteractionListener.onSendReportToEmail(userEmail,dateFrom, dateTo,fileFormat);
                        }
                    }
                    alertDialog.dismiss();
                } else {
                    Toast.makeText(mContext, mContext.getResources().getString(R.string.email_not_valid), Toast.LENGTH_SHORT).show();
                }
            }
        });

    }
}
