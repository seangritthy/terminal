package com.termux.app.updater;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import com.termux.R;
import com.termux.shared.logger.Logger;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executors;

public class TerminalUpdateChecker {

    private static final String LOG_TAG = "TerminalUpdateChecker";
    private static final String GITHUB_RELEASES_API = "https://api.github.com/repos/seangritthy/terminal/releases/latest";
    public static final String CURRENT_VERSION_TAG = "v0.126.0-terminal";

    public static void checkForUpdates(final Activity activity) {
        if (activity == null || activity.isFinishing()) return;

        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(GITHUB_RELEASES_API);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setRequestProperty("User-Agent", "TerminalApp/1.0");
                    connection.setConnectTimeout(8000);
                    connection.setReadTimeout(8000);

                    int responseCode = connection.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        StringBuilder builder = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            builder.append(line);
                        }
                        reader.close();

                        JSONObject json = new JSONObject(builder.toString());
                        final String latestTag = json.optString("tag_name", "");
                        final String releaseNotes = json.optString("body", "");
                        final String downloadUrl = json.optString("html_url", "https://github.com/seangritthy/terminal/releases");

                        if (!latestTag.isEmpty() && !CURRENT_VERSION_TAG.equalsIgnoreCase(latestTag)) {
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    showUpdateDialog(activity, latestTag, releaseNotes, downloadUrl);
                                }
                            });
                        }
                    }
                    connection.disconnect();
                } catch (Exception e) {
                    Logger.logError(LOG_TAG, "Error checking for updates: " + e.getMessage());
                }
            }
        });
    }

    private static void showUpdateDialog(final Activity activity, final String newVersion, String notes, final String downloadUrl) {
        if (activity.isFinishing()) return;

        new AlertDialog.Builder(activity)
            .setTitle("កំណែថ្មីមានស្រាប់ (New Version " + newVersion + ")")
            .setMessage("មានកំណែថ្មីសម្រាប់ Terminal (" + newVersion + ") ត្រូវបានចេញផ្សាយ។\n\n" + notes)
            .setPositiveButton("ទាញយក (Download)", (dialog, which) -> {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl));
                    activity.startActivity(intent);
                } catch (Exception ignored) {}
            })
            .setNegativeButton("ពេលក្រោយ (Later)", null)
            .show();
    }
}
