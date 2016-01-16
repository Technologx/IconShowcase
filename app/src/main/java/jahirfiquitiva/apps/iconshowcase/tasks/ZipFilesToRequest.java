package jahirfiquitiva.apps.iconshowcase.tasks;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import com.afollestad.materialdialogs.MaterialDialog;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;

import jahirfiquitiva.apps.iconshowcase.R;
import jahirfiquitiva.apps.iconshowcase.models.RequestItem;

public class ZipFilesToRequest extends AsyncTask<Void, String, Boolean> {

    private MaterialDialog dialog;
    public ArrayList<RequestItem> appsListFinal = new ArrayList<>();

    private static final int BUFFER = 2048;
    private String zipLocation, zipFilePath;
    private Context context;

    public static String filesLocation;

    public static ArrayList<String> appsNames = new ArrayList<String>();
    public static ArrayList<String> appsPackages = new ArrayList<String>();
    public static ArrayList<String> appsClasses = new ArrayList<String>();
    public static ArrayList<Drawable> appsIcons = new ArrayList<Drawable>();

    private StringBuilder emailContent = new StringBuilder();
    private boolean worked;

    private int selected;

    private WeakReference<Activity> wrActivity;

    private Activity activity;

    public ZipFilesToRequest(Activity activity, MaterialDialog dialog, ArrayList<RequestItem> appsListFinal) {
        this.wrActivity = new WeakReference<>(activity);
        this.dialog = dialog;
        this.appsListFinal = appsListFinal;
    }

    @Override
    protected void onPreExecute() {

        final Activity act = wrActivity.get();
        if (act != null) {
            this.context = act.getApplicationContext();
            this.activity = act;
        }

        zipLocation = context.getString(R.string.request_save_location,
                Environment.getExternalStorageDirectory().getAbsolutePath());
        filesLocation = zipLocation + "Files/";

        SimpleDateFormat date = new SimpleDateFormat("yyyyMMdd_hhmmss", Locale.getDefault());
        String appNameCorrected = context.getResources().getString(R.string.app_name).replace(" ", "");

        zipFilePath = zipLocation + appNameCorrected
                + "_" + date.format(new Date()) + ".zip";

        appsNames.clear();
        appsPackages.clear();
        appsClasses.clear();
        appsIcons.clear();
        selected = 0;

        for (int a = 0; a < appsListFinal.size(); a++) {
            if (!appsListFinal.get(a).isSelected()) {
                selected += 1;
            }
        }

        if (selected >= appsListFinal.size()) {
            for (int b = 0; b < appsListFinal.size(); b++) {
                appsNames.add(appsListFinal.get(b).getAppName());
                appsPackages.add(appsListFinal.get(b).getPackageName());
                appsClasses.add(appsListFinal.get(b).getClassName());
                appsIcons.add(appsListFinal.get(b).getIcon());
            }
        } else {
            for (int c = 0; c < appsListFinal.size(); c++) {
                if (appsListFinal.get(c).isSelected()) {
                    appsNames.add(appsListFinal.get(c).getAppName());
                    appsPackages.add(appsListFinal.get(c).getPackageName());
                    appsClasses.add(appsListFinal.get(c).getClassName());
                    appsIcons.add(appsListFinal.get(c).getIcon());
                }
            }
        }
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        try {
            final File zipFolder = new File(zipLocation);
            final File filesFolder = new File(filesLocation + "/");

            deleteDirectory(zipFolder);
            deleteDirectory(filesFolder);

            zipFolder.mkdirs();
            filesFolder.mkdirs();

            StringBuilder sb = new StringBuilder();
            StringBuilder appFilterBuilder = new StringBuilder();
            StringBuilder appMapBuilder = new StringBuilder();
            StringBuilder themeResourcesBuilder = new StringBuilder();

            int appsCount = 0;
            sb.append("These apps have no icons, please add some for them:\n\n");

            for (int i = 0; i < appsNames.size(); i++) {

                appFilterBuilder.append("<!-- " + appsNames.get(i) +
                        " -->\n<item component=\"ComponentInfo{" +
                        appsPackages.get(i) + "/" + appsClasses.get(i) + "}\"" +
                        "drawable=\"" + appsNames.get(i).replace(" ", "_").toLowerCase() + "\"/>" + "\n");

                appMapBuilder.append("<!-- " + appsNames.get(i) +
                        " -->\n<item name=\"" + appsNames.get(i).replace(" ", "_").toLowerCase() +
                        "\" class=\"" + appsClasses.get(i) + "\" />" + "\n");

                themeResourcesBuilder.append("<!-- " + appsNames.get(i) +
                        " -->\n<AppIcon name=\"" +
                        appsPackages.get(i) + "/" + appsClasses.get(i) +
                        "\" image=\"" + appsNames.get(i).replace(" ", "_").toLowerCase() + "\"/>" + "\n");

                sb.append("App Name: " + appsNames.get(i) + "\n");
                sb.append("App Info: " + appsPackages.get(i) + "/" + appsClasses.get(i) + "\n");
                sb.append("App Link: " + "https://play.google.com/store/apps/details?id=" + appsPackages.get(i) + "\n");
                sb.append("\n");
                sb.append("\n");

                Bitmap bitmap = ((BitmapDrawable) (appsIcons.get(i))).getBitmap();

                FileOutputStream fileOutputStream;
                try {
                    fileOutputStream = new FileOutputStream(filesLocation + "/" + appsNames.get(i).replace(" ", "_").toLowerCase() + ".png");
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
                    fileOutputStream.flush();
                    fileOutputStream.close();
                } catch (FileNotFoundException e) {
                } catch (IOException e) {
                }

                appsCount++;
            }

            sb.append("\nOS Version: " + System.getProperty("os.version") + "(" + Build.VERSION.INCREMENTAL + ")");
            sb.append("\nOS API Level: " + Build.VERSION.SDK_INT);
            sb.append("\nDevice: " + Build.DEVICE);
            sb.append("\nManufacturer: " + Build.MANUFACTURER);
            sb.append("\nModel (and Product): " + Build.MODEL + " (" + Build.PRODUCT + ")");

            try {
                PackageInfo appInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                sb.append("\nApp Version Name: " + appInfo.versionName);
                sb.append("\nApp Version Code: " + appInfo.versionCode);
            } catch (Exception e) {
            }

            if (appsCount != 0) {

                try {
                    FileWriter fileWriter1 = new FileWriter(filesLocation + "/appfilter.xml");
                    BufferedWriter bufferedWriter1 = new BufferedWriter(fileWriter1);
                    bufferedWriter1.write(appFilterBuilder.toString());
                    bufferedWriter1.close();
                } catch (Exception e) {
                    return null;
                }

                try {
                    FileWriter fileWriter2 = new FileWriter(filesLocation + "/appmap.xml");
                    BufferedWriter bufferedWriter2 = new BufferedWriter(fileWriter2);
                    bufferedWriter2.write(appMapBuilder.toString());
                    bufferedWriter2.close();
                } catch (Exception e) {
                    return null;
                }

                try {
                    FileWriter fileWriter3 = new FileWriter(filesLocation + "/theme_resources.xml");
                    BufferedWriter bufferedWriter3 = new BufferedWriter(fileWriter3);
                    bufferedWriter3.write(themeResourcesBuilder.toString());
                    bufferedWriter3.close();
                } catch (Exception e) {
                    return null;
                }

                createZipFile(filesLocation, true, zipFilePath);
                deleteDirectory(filesFolder);

            }

            worked = true;
            emailContent = sb;

        } catch (Exception e) {
            worked = false;
            emailContent = null;
            e.getLocalizedMessage();
        }
        return worked;

    }

    @Override
    protected void onPostExecute(Boolean worked) {

        if (worked) {

            if (emailContent != null) {
                dialog.dismiss();
                final Uri uri = Uri.parse("file://" + zipFilePath);

                String[] recipients = new String[]{context.getString(R.string.email_id)};

                Intent sendIntent = new Intent(Intent.ACTION_SEND);
                sendIntent.setType("application/zip");
                sendIntent.putExtra(Intent.EXTRA_STREAM, uri);
                sendIntent.putExtra("android.intent.extra.EMAIL", recipients);
                sendIntent.putExtra("android.intent.extra.SUBJECT",
                        context.getString(R.string.app_name) + " Icon Request");
                sendIntent.putExtra("android.intent.extra.TEXT", emailContent.toString());
                sendIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                try {
                    activity.startActivity(Intent.createChooser(sendIntent, "Send mail..."));
                } catch (ActivityNotFoundException e) {
                }
            }

        } else {
            dialog.setContent(R.string.error);
        }

    }

    public static boolean deleteDirectory(File dir) {
        if (dir.exists()) {
            File[] files = dir.listFiles();
            for (int j = 0; j < files.length; j++) {
                if (files[j].isDirectory()) {
                    deleteDirectory(files[j]);
                    String[] children = dir.list();
                    for (int i = 0; i < children.length; i++) {
                        new File(dir, children[i]).delete();
                    }
                } else {
                    files[j].delete();
                }
            }

        }
        return (dir.delete());
    }

    public static boolean createZipFile(final String path, final boolean keepDirectoryStructure, final String outputFile) {
        final File filesFolder = new File(path);

        if (!filesFolder.canRead() || !filesFolder.canWrite()) {
            return false;
        }

        try {
            ZipOutputStream zipOutputStream = new ZipOutputStream(
                    new BufferedOutputStream(
                            new FileOutputStream(outputFile), BUFFER));
            if (keepDirectoryStructure) {
                zipFile(path, zipOutputStream, "");
            } else {
                final File files[] = filesFolder.listFiles();
                for (final File file : files) {
                    zipFolder(file, zipOutputStream);
                }
            }
            zipOutputStream.close();
        } catch (FileNotFoundException e) {
            Log.e("File not found", e.getMessage());
            return false;
        } catch (IOException e) {
            Log.e("IOException", e.getMessage());
            return false;
        }

        return true;
    }

    public static void zipFile(final String zipFilesPath, final ZipOutputStream zipOutputStream, final String zipPath) throws IOException {
        final File file = new File(zipFilesPath);

        if (!file.exists()) {
            return;
        }

        final byte[] buf = new byte[1024];
        final String[] files = file.list();

        if (file.isFile()) {
            FileInputStream in = new FileInputStream(file.getAbsolutePath());
            try {
                zipOutputStream.putNextEntry(new ZipEntry(zipPath + file.getName()));
                int len;
                while ((len = in.read(buf)) > 0) {
                    zipOutputStream.write(buf, 0, len);
                }
                zipOutputStream.closeEntry();
                in.close();
            } catch (ZipException e) {
            } finally {
                if (zipOutputStream != null) zipOutputStream.closeEntry();
                if (in != null) in.close();
            }
        } else if (files.length > 0) {
            for (int i = 0, length = files.length; i < length; ++i) {
                zipFile(zipFilesPath + "/" + files[i], zipOutputStream, zipPath + file.getName() + "/");
            }
        }
    }

    private static void zipFolder(File file, ZipOutputStream zipOutputStream) throws IOException {
        byte[] data = new byte[BUFFER];
        int read;

        if (file.isFile()) {
            ZipEntry zipEntry = new ZipEntry(file.getName());
            zipOutputStream.putNextEntry(zipEntry);
            BufferedInputStream bufferedInputStream = new BufferedInputStream(
                    new FileInputStream(file));
            while ((read = bufferedInputStream.read(data, 0, BUFFER)) != -1)
                zipOutputStream.write(data, 0, read);
            zipOutputStream.closeEntry();
            bufferedInputStream.close();
        } else if (file.isDirectory()) {
            String[] list = file.list();
            int listLength = list.length;
            for (int i = 0; i < listLength; i++)
                zipFolder(new File(file.getPath() + "/" + list[i]), zipOutputStream);
        }
    }

}