package de.srlabs.snoopsnitch;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import java.util.LinkedList;
import java.util.List;

import de.srlabs.patchalyzer.NotificationHelper;
import de.srlabs.patchalyzer.PatchalyzerMainActivity;
import de.srlabs.patchalyzer.TestUtils;
import de.srlabs.snoopsnitch.qdmon.MsdSQLiteOpenHelper;
import de.srlabs.snoopsnitch.util.DeviceCompatibilityChecker;
import de.srlabs.snoopsnitch.util.MsdConfig;
import de.srlabs.snoopsnitch.util.MsdDatabaseManager;
import de.srlabs.snoopsnitch.util.MsdDialog;
import de.srlabs.snoopsnitch.util.PermissionChecker;
import de.srlabs.snoopsnitch.util.Utils;

/**
 * This class is launched when starting the App. It will display a dialog if the
 * device is not compatible or if it is the first run of the App (so that the
 * user has to confirm to continue). If the device is compatible and the user
 * has already confirmed the first run dialog, it will directly switch over to
 * DashboardActivity.
 */
public class StartupActivity extends Activity {
    private static final String TAG = "StartupActivity";
    private MsdSQLiteOpenHelper helper;
    private boolean alreadyClicked = false;
    private ProgressDialog progressDialog;
    private static boolean isSNSNCompatible = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String snsnIncompatibilityReason = DeviceCompatibilityChecker.checkDeviceCompatibility(this.getApplicationContext());

        if(snsnIncompatibilityReason != null){
            /*if(snsnIncompatibilityReason.equals(getResources().getString(R.string.compat_no_baseband_messages_in_active_test))){
                showDialogWarningNoBasebandMessages();
            }
            else {
                showDeviceIncompatibleDialog(snsnIncompatibilityReason);
            }*/
        }
        else{
            isSNSNCompatible = true;
        }
        proceedAppFlow();

    }

    private void proceedAppFlow() {
        if(!TestUtils.isTooOldAndroidAPIVersion()) //do not show notification, if users can not use it anyway
            NotificationHelper.showNewPatchalyzerFeatureOnce(this);

        //continue with normal startup
        if (MsdConfig.getFirstRun(this)) {
            showFirstRunDialog();
        } else {
            createDatabaseAndStartDashboard();
        }
    }



    public static boolean isSNSNCompatible(){
        return isSNSNCompatible;
    }



    private void showFirstRunDialog() {
        MsdDialog.makeConfirmationDialog(this, getResources().getString(R.string.alert_first_app_start_message),
                new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (alreadyClicked)
                            return;
                        alreadyClicked = true;
                        // record the fact that the app has been started at least once
                        MsdConfig.setFirstRun(StartupActivity.this, false);
                        createDatabaseAndStartDashboard();
                    }
                },
                new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (alreadyClicked)
                            return;
                        quitApplication();
                    }
                },
                new OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        if (alreadyClicked)
                            return;
                        quitApplication();
                    }
                }, false
        ).show();
    }

    protected void quitApplication() {
        finish();
        System.exit(0);
    }

    private void createDatabaseAndStartDashboard() {
        progressDialog = ProgressDialog.show(this, "Initializing database", "Please wait...", true);
        progressDialog.show();
        final Handler handler = new Handler();
        Thread t = new Thread() {
            @Override
            public void run() {

                try {
                    MsdDatabaseManager.initializeInstance(new MsdSQLiteOpenHelper(StartupActivity.this));
                    MsdDatabaseManager msdDatabaseManager = MsdDatabaseManager.getInstance();
                    SQLiteDatabase db = msdDatabaseManager.openDatabase();
                    //testing DB init
                    db.rawQuery("SELECT * FROM config", null).close();
                    msdDatabaseManager.closeDatabase();

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (progressDialog != null && progressDialog.isShowing ()) {
                                progressDialog.dismiss ();
                            }

                            if(isSNSNCompatible) {
                                //Check for ACCESS_COARSE_PERMISSION neccessary for Recoding in MsdService to function
                                if (PermissionChecker.checkAndRequestPermissionForMsdService(StartupActivity.this)) {
                                    startDashboard();
                                }
                            }else{
                                startDashboard();
                            }

                        }
                    });
                }catch(SQLException e){
                    // Testing if the DB creation worked successfully failed
                    Log.e(TAG,"DB creation failed, maybe App assets are corrupted: "+ e.getMessage());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            showDBCreationFailedDialog();
                        }
                    });

                }
            }
        };
        t.start();
    }

    private void startDashboard() {
        Intent i = new Intent(StartupActivity.this, DashboardActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        StartupActivity.this.startActivity(i);
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode == PermissionChecker.REQUEST_MSDSERVICE_PERMISSIONS) {
            if (grantResults.length > 0) {
                //find all neccessary permissions not granted
                List<String> notGrantedPermissions = new LinkedList<>();
                for (int i = 0; i < permissions.length; i++) {
                    if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                        notGrantedPermissions.add(permissions[i]);
                    }
                }

                if (notGrantedPermissions.isEmpty()) {
                    //Success: All neccessary permissions granted
                    startDashboard();
                } else {

                    //ask again for all not granted permissions
                    boolean showDialog = false;
                    for (String notGrantedPermission : notGrantedPermissions) {
                        showDialog = showDialog || ActivityCompat.shouldShowRequestPermissionRationale(this, notGrantedPermission);
                    }

                    if (showDialog) {
                        showDialogAskingForAllPermissions(getResources().getString(R.string.alert_msdservice_permissions_not_granted));
                    } else {
                        // IF permission is denied (and "never ask again" is checked)
                        // Log.e(TAG, mTAG + ": Permission FAILURE: some permissions are not granted. Asking again.");
                        showDialogPersistentDeniedPermissions(getResources().getString(R.string.alert_msdservice_permissions_not_granted_persistent));

                    }

                }

            }
        }
    }

    private void showDialogAskingForAllPermissions(String message) {
        MsdDialog.makeConfirmationDialog(this, message, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                PermissionChecker.checkAndRequestPermissionForMsdService(StartupActivity.this);
            }
        }, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                showDialogExplainNoRecording(getResources().getString(R.string.alert_msdservice_recording_not_possible));
            }
        }, false).show();
    }

    private void showDialogPersistentDeniedPermissions(String message) {
        /*TODO: Send user to permission settings for SNSN directly? Adapt message accordingly
         startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
				 Uri.parse("package:de.srlabs.snoopsnitch")));*/
        MsdDialog.makeConfirmationDialog(this, message, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                startDashboard();
            }
        }, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                startDashboard();
            }
        }, false).show();
    }

    private void showDialogExplainNoRecording(String message) {
        MsdDialog.makeConfirmationDialog(this, message, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startDashboard();
                    }
                },
                new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startDashboard();
                    }
                }, false).show();
    }

    private void showDBCreationFailedDialog() {
        MsdDialog.makeFatalConditionDialog(this, getResources().getString(R.string.alert_db_creation_failed_detail), new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                quitApplication();
            }
        },null,false).show();
    }

}
