package com.simant.permissions;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import java.util.ArrayList;

public class PermissionHelper {

    private static final String TAG = PermissionHelper.class.getName();

    public static boolean requestPermissions(String[] permissions, Activity callingActivity, int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Store the permissions not currently granted
            ArrayList<String> permissionsNeedRequesting = new ArrayList<>();
            for (String permission : permissions) {
                // Check each permission
                if (!checkPermission(permission, callingActivity)) {
                    permissionsNeedRequesting.add(permission);
                }
            }
            // Check if any permissions are currently not granted
            if (!permissionsNeedRequesting.isEmpty()) {
                // Request permissions not currently granted
                callingActivity.requestPermissions(permissionsNeedRequesting.toArray(new String[permissionsNeedRequesting.size()]), requestCode);
                return false;
            }
            else {
                // All permissions already granted.
                return true;
            }
        }
        else {
            // Permission management not supported on pre Android M devices
            return true;
        }
    }

    public static boolean checkPermission(String permission, Activity callingActivity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Check to see if permission already granted.
            int permissionStatus = callingActivity.checkSelfPermission(permission);

            return permissionStatus == PackageManager.PERMISSION_GRANTED;
        }
        else {
            // Permission management not supported on pre Android M devices
            return true;
        }
    }

    public static boolean requestPermissions(String permission, Activity callingActivity, int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!checkPermission(permission, callingActivity)) {
                // Permission not already granted - request permission
                Log.d(TAG, "Permission " + permission + " not granted. Requesting.");

                String[] permissionArray = {permission};
                callingActivity.requestPermissions(permissionArray, requestCode);
                return false;
            } else {
                Log.d(TAG, "Permission " + permission + " already granted.");
                // Permission already granted, call listener
                return true;
            }
        }
        else {
            // Permission management not supported on pre Android M devices
            return true;
        }
    }
}
