package fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.style.AlignmentSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.example.serverdatatransfer.DashBoard;
import com.example.serverdatatransfer.R;
import com.example.serverdatatransfer.UserLogin;
import com.nbsp.materialfilepicker.MaterialFilePicker;
import com.nbsp.materialfilepicker.ui.FilePickerActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import dataStorageClasses.Constants;
import dataStorageClasses.MySingleton;
import dataStorageClasses.SharedPrefManager;
import dataStorageClasses.Upload;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Response;

import static android.app.Activity.RESULT_OK;

public class Profile_frag extends Fragment {
    private static final int FILE_PICKER_REQUEST_CODE = 1000;
    private static final int PERMISSION_REQUEST_CODE = 100;
    private TextView tv;
    private Button btn;
    private Intent file_data;
    private int isUploaded = -1;
    private String path = null, userName = null;
    private ProgressDialog progressDialog;
    private TableLayout tableLayout;
    private List<Upload> uploadList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.frag_profile, container, false);

        tv = view.findViewById(R.id.file_location);
        tableLayout=view.findViewById(R.id.jsonOutput);
        Button dwn = view.findViewById(R.id.download);
        dwn.setOnClickListener(v->{
               showMyFiles();
        });

        progressDialog = new ProgressDialog(getContext());
        btn = view.findViewById(R.id.pick_file);

        userName = SharedPrefManager.getInstance(getContext()).getUsername();
        btn.setOnClickListener(v -> {
            if (file_data != null) {
                Upload_File upload_file = new Upload_File(Profile_frag.this);
                upload_file.execute(file_data);
            }
        });
        seekPermission();
        return view;
    }

    private void showMyFiles() {

        StringRequest stringRequest = new StringRequest(Request.Method.GET, Constants.GET_FILE_URL, response -> {
            try {
                JSONObject jsonObject = new JSONObject(response);
                String success = jsonObject.getString("success");
                JSONArray jsonArray = jsonObject.getJSONArray("data");
                if (success.equals("1")) {
                    tableLayout.removeAllViews();
                    for (int j = 0; j < jsonArray.length(); j++) {
                        JSONObject object = jsonArray.getJSONObject(j);
                        final String link = object.getString("filelink");
                        final String title = object.getString("title");
                        Upload upload = new Upload(link,title);
                        uploadList.add(upload);

                        @SuppressLint("InflateParams") View v = getLayoutInflater().inflate(R.layout.files, null);
                        v.setId(j);
                        TableRow tr = new TableRow(getContext());
                        tr.setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams.WRAP_CONTENT, TableLayout.LayoutParams.WRAP_CONTENT));
                        tr.setGravity(Gravity.CENTER);
                        TextView t = v.findViewById(R.id.title);
                        TextView u = v.findViewById(R.id.url);
                        t.setText(title);
                        u.setText(link);

                        tableLayout.addView(v);

                        v.setOnClickListener(v1->{
                            int i =v.getId();
                            startDownload(uploadList.get(i).getLink(),uploadList.get(i).getName());
                        });
                    }
                }
            } catch (JSONException e) {
                Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        }, error -> {
            progressDialog.hide();
            Toast.makeText(getContext(), "Error! " + error.getMessage(), Toast.LENGTH_LONG).show();
        }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("username", userName);
                return params;
            }
        };
        MySingleton.getInstance(getContext()).addToRequestQueue(stringRequest);


    }


    private static class Upload_File extends AsyncTask<Intent, Void, Integer> {

        private WeakReference<Profile_frag> dashBoardWeakReference;

        Upload_File(Profile_frag activity) {
            dashBoardWeakReference = new WeakReference<>(activity);
        }


        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            Profile_frag activity = dashBoardWeakReference.get();
            if (activity == null) {
                return;
            }

            activity.progressDialog.setTitle("Uploading....");
            activity.progressDialog.setCancelable(false);
            activity.progressDialog.show();
            activity.btn.setVisibility(View.INVISIBLE);
            activity.tv.setText(null);
        }

        @Override
        protected Integer doInBackground(Intent... intents) {
            Profile_frag activity = dashBoardWeakReference.get();
            if (activity == null) {
                return null;
            }
            File file;
            if (activity.path != null)
                file = new File(activity.path);
            else
                file = new File(Objects.requireNonNull(intents[0].getStringExtra(FilePickerActivity.RESULT_FILE_PATH)));
            String content_type = getMimeType(file);
            String filePath1 = file.getAbsolutePath();
            OkHttpClient client = new OkHttpClient();
            RequestBody file_body = RequestBody.create(file, MediaType.parse(content_type));
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("username", SharedPrefManager.getInstance(activity.getActivity()).getUsername())
                    .addFormDataPart("password", SharedPrefManager.getInstance(activity.getActivity()).getPassword())
                    .addFormDataPart("uploaded_file", filePath1.substring(filePath1.lastIndexOf('/') + 1), file_body)
                    .build();
            okhttp3.Request request = new okhttp3.Request.Builder()
                    .url(Constants.URL_UPLOAD)
                    .post(requestBody)
                    .build();
            try {
                Response response = client.newCall(request).execute();
                if (response.isSuccessful()) {
                    activity.isUploaded = 1;
                } else {
                    activity.isUploaded = 0;
                }
            } catch (IOException e) {
                activity.isUploaded = 2;
                e.printStackTrace();
            }
            return activity.isUploaded;
        }

        @Override
        protected void onPostExecute(Integer integer) {
            super.onPostExecute(integer);
            Profile_frag activity = dashBoardWeakReference.get();
            if (activity == null) {
                return;
            }
            if (integer == 0) {
                Toast.makeText(activity.getActivity(), "Upload unsuccessful", Toast.LENGTH_SHORT).show();
            } else if (integer == 1) {
                Toast.makeText(activity.getActivity(), "Upload successful", Toast.LENGTH_SHORT).show();
            } else if (integer == 2) {
                Toast.makeText(activity.getActivity(), "Some error occurred in connection with the server", Toast.LENGTH_SHORT).show();
            }
            activity.progressDialog.dismiss();

        }
    }

    @NonNull
    static String getMimeType(@NonNull File file) {
        String type = null;
        final String url = file.toString();
        final String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
        }
        if (type == null) {
            type = "*/*";
        }
        return type;
    }

    private void startFilePicker() {
        new MaterialFilePicker()
                .withActivity(getActivity())
                .withRootPath("/storage/")
                .withRequestCode(FILE_PICKER_REQUEST_CODE)
                .withHiddenFiles(true)
                .withTitle("Sample title")
                .start();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_PICKER_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            String filePath = data.getStringExtra(FilePickerActivity.RESULT_FILE_PATH);
            tv.setText(filePath);
            file_data = data;
            btn.setVisibility(View.VISIBLE);
        }

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.profile_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if (item.getItemId() == R.id.nav_upload) {
            startFilePicker();
        }
        return super.onOptionsItemSelected(item);
    }

    private void seekPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(Objects.requireNonNull(getContext()), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
            }
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED)
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
        }
    }

    private void startDownload(String url,String title) {
        String mimeType=MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(url));
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE | DownloadManager.Request.NETWORK_WIFI);
        request.setTitle("Download");
        request.setDescription(mimeType);
        request.allowScanningByMediaScanner();
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,title);
        request.setAllowedOverMetered(true);
        if(mimeType!=null)
        request.setMimeType(mimeType);
        DownloadManager manager = (DownloadManager) Objects.requireNonNull(getActivity()).getSystemService(Context.DOWNLOAD_SERVICE);
        assert manager != null;
        manager.enqueue(request);

    }
}

