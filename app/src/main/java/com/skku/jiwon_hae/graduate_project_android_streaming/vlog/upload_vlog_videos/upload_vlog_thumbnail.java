package com.skku.jiwon_hae.graduate_project_android_streaming.vlog.upload_vlog_videos;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by jiwon_hae on 2017. 11. 13..
 */

public class upload_vlog_thumbnail extends Service {
    private String upload_serverUri = "http://13.125.170.236/streaming_application/redis/vlog/video_thumbnail_upload.php";
    private String fileName;
    private String file_uri;

    private int serverResponseCode = 0;
    int bytesRead, bytesAvailable, bufferSize;
    int maxBufferSize = 1 * 1024 * 1024;

    String lineEnd = "\r\n";
    String twoHyphens = "--";
    String boundary = "*****";

    private String userName;
    private String vlog_file;
    private String registrationNo;
    private String tags;
    private int startId;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        this.startId = startId;

        if(intent != null) {
            fileName = intent.getStringExtra("thumbnail_name");
            file_uri = intent.getStringExtra("thumbnail_addr");
            vlog_file = intent.getStringExtra("vlog_name");

            userName = intent.getStringExtra("userName");

            if (intent.hasExtra("tags")) {
                tags = intent.getStringExtra("tags");
            } else {
                tags = "";
            }
            registrationNo = intent.getStringExtra("registrationNo");

            new Thread(new Runnable() {
                public void run() {
                    uploadFile(file_uri);
                }
            }).start();
        }
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public int uploadFile(String sourceFileUri) {
        HttpURLConnection conn = null;
        DataOutputStream dos = null;
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";
        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1 * 1024 * 1024;
        File sourceFile = new File(sourceFileUri);

        if (!sourceFile.isFile()) {

            return 0;

        } else {
            try {
                // open a URL connection to the Servlet
                FileInputStream fileInputStream = new FileInputStream(sourceFile);
                URL url = new URL(upload_serverUri);

                // Open a HTTP  connection to  the URL
                conn = (HttpURLConnection) url.openConnection();
                conn.setDoInput(true); // Allow Inputs
                conn.setDoOutput(true); // Allow Outputs
                conn.setUseCaches(false); // Don't use a Cached Copy
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Connection", "Keep-Alive");
                conn.setRequestProperty("ENCTYPE", "multipart/form-data");
                conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                conn.setRequestProperty("uploaded_file", fileName);

                dos = new DataOutputStream(conn.getOutputStream());

                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name='uploaded_file';filename='"
                        + fileName + "'" + lineEnd);
                dos.writeBytes(lineEnd);

                // create a buffer of  maximum size
                bytesAvailable = fileInputStream.available();

                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                buffer = new byte[bufferSize];

                // read file and write it into form...
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                while (bytesRead > 0) {

                    dos.write(buffer, 0, bufferSize);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                }
                // send multipart form data necesssary after file data...
                dos.writeBytes(lineEnd);
                dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                // Responses from the server (code and message)
                serverResponseCode = conn.getResponseCode();
                String serverResponseMessage = conn.getResponseMessage();

                Log.i("uploadFile", "HTTP Response is : "
                        + serverResponseMessage + ": " + serverResponseCode);

                if(serverResponseCode == 200){

                    Response.Listener<String> responseListener = new Response.Listener<String>(){
                        @Override
                        public void onResponse(String response) {
                            try{
                                JSONObject jsonObject = new JSONObject(response);
                                boolean success = jsonObject.getBoolean("success");

                            }catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    };

                    vlog_upload upload_vlog = new vlog_upload(registrationNo, userName, vlog_file, tags, responseListener);
                    RequestQueue queue = Volley.newRequestQueue(upload_vlog_thumbnail.this);
                    queue.add(upload_vlog);

                    this.stopSelf(startId);

                }else{
                    Toast.makeText(upload_vlog_thumbnail.this, "VLOG 업로드에 문제가 생겼습니다. 나중에 다시 시도해주세요", Toast.LENGTH_SHORT).show();
                    this.stopSelf(startId);
                }

                fileInputStream.close();
                dos.flush();
                dos.close();

            } catch (MalformedURLException ex) {
                Log.e("Upload file to server", "error: " + ex.getMessage(), ex);
            } catch (Exception e) {
                Log.e("Upload file to server", "Exception : "
                        + e.getMessage(), e);
            }
            return serverResponseCode;

        } // End else block
    }
}