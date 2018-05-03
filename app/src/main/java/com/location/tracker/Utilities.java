package com.location.tracker;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.os.Build;
import android.view.ContextThemeWrapper;

import java.util.Date;

/**
 * Created by Tapan Kshatriya on 8/22/2017.
 */

public class Utilities {
    private ProgressDialog progressDialog;
    private Context context;

    public Utilities(Context context) {
        this.context = context;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            progressDialog = new ProgressDialog(new ContextThemeWrapper(context, android.R.style.Theme_Holo_Light_Dialog));
        } else {
            progressDialog = new ProgressDialog(context);
        }

    }

    public void showProgressDialog(String title, String message) {
        if (title != null)
            progressDialog.setTitle(title);
        progressDialog.setMessage(message);
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    public void dismissProgressDialog() {
        if (progressDialog != null)
            if (progressDialog.isShowing())
                progressDialog.dismiss();
    }

    public void showSimpleMessageDialog(String title, String message) {
        ContextThemeWrapper themedContext;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            themedContext = new ContextThemeWrapper(context, android.R.style.Theme_Holo_Light_Dialog_NoActionBar);
        } else {
            themedContext = new ContextThemeWrapper(context, android.R.style.Theme_Light_NoTitleBar);
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(themedContext);
        if (title != null)
            builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    public boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null;
    }

    public String getTimeDifference(Date startDate, Date endDate) {
        String timeDifference = "";
        //milliseconds
        long different = endDate.getTime() - startDate.getTime();

        System.out.println("startDate : " + startDate);
        System.out.println("endDate : " + endDate);
        System.out.println("different : " + different);

        long secondsInMilli = 1000;
        long minutesInMilli = secondsInMilli * 60;
        long hoursInMilli = minutesInMilli * 60;
        long daysInMilli = hoursInMilli * 24;

        long elapsedDays = different / daysInMilli;
        different = different % daysInMilli;

        long elapsedHours = different / hoursInMilli;
        different = different % hoursInMilli;

        long elapsedMinutes = different / minutesInMilli;
        different = different % minutesInMilli;

        long elapsedSeconds = different / secondsInMilli;
//        timeDifference = elapsedDays + " d " + elapsedHours + " h " + elapsedMinutes + " m " + elapsedSeconds + " s ";

        if (elapsedDays < 10 && elapsedDays > 0)
            timeDifference = "0" + elapsedDays + "d ";
        else if (elapsedDays >= 10)
            timeDifference = elapsedDays + "d ";

        if (elapsedHours < 10 && elapsedHours > 0)
            timeDifference = timeDifference + "0" + elapsedHours + "h ";
        else if (elapsedHours >= 10)
            timeDifference = timeDifference + elapsedHours + "h ";

        if (elapsedMinutes < 10 && elapsedMinutes > 0)
            timeDifference = timeDifference + "0" + elapsedMinutes + "m ";
        else if (elapsedMinutes >= 10)
            timeDifference = timeDifference + elapsedMinutes + "m ";

        if (elapsedSeconds < 10 && elapsedSeconds > 0)
            timeDifference = timeDifference + "0" + elapsedSeconds + "s ";
        else if (elapsedSeconds >= 10)
            timeDifference = timeDifference + elapsedSeconds + "s ";


        System.out.printf(
                "%d days, %d hours, %d minutes, %d seconds%n",
                elapsedDays, elapsedHours, elapsedMinutes, elapsedSeconds);
        return timeDifference;
    }
}
