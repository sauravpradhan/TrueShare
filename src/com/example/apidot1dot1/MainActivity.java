package com.example.apidot1dot1;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.squareup.picasso.Picasso;

public class MainActivity extends Activity {

    static String id = null;
    final static String CONSUMER_KEY = "XD8F4hjAt10dw6vOzIMSA";
    final static String CONSUMER_SECRET = "yW6iBJP3Ch5TG9gjaZoVSQvbJaDVTFz8GSI71SRid4s";
    final static String TwitterTokenURL = "https://api.twitter.com/oauth2/token";
    final static String TwitterStreamURL = "https://api.twitter.com/1.1/statuses/show.json?id=";
    Boolean haveCalledShare = false;
    Boolean excepCaught = false;
    static int count = 0;
    String url = null;
    private TextView mUrlTextView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Start:code for fetching the intent from twitter App
        mUrlTextView  = (TextView)findViewById(R.id.urlTextview);
        mUrlTextView.setText("Report Bug");
        mUrlTextView.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                Intent sendIntent = new Intent(Intent.ACTION_SEND);
               // sendIntent.setClassName("com.google.android.gm","com.google.android.gm.ComposeActivityGmail");
                sendIntent.putExtra(Intent.EXTRA_EMAIL, new String[] { "blogger.saurav@gmail.com" });
                sendIntent.setData(Uri.parse("blogger.saurav@gmail.com"));
                sendIntent.putExtra(Intent.EXTRA_SUBJECT, "Bug on TrueShare V 1.0");
                sendIntent.setType("plain/text");
                sendIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                sendIntent.putExtra(Intent.EXTRA_TEXT, "Describe Bug Here...");
               getApplicationContext(). startActivity(sendIntent);
            }
        });
        Intent intent = getIntent();
        String action = intent.getAction();
        Bundle bundle = intent.getExtras();
        String type = intent.getType();
        Log.d("Saurav", "Inside OnCreate");
        
        if(intent.getAction().equalsIgnoreCase("android.intent.action.MAIN"))
        {
            Toast.makeText(getApplicationContext(), "Blind Application Launch| Nothing to share as of now!", Toast.LENGTH_LONG).show();
        }
        else
        {	
            if (Intent.ACTION_SEND.equals(action) && type != null) {	
                Log.d("Saurav","The connection status now is:"+checkMyConnection());
                if(!checkMyConnection())
                {
                    Toast.makeText(getApplicationContext(), "Internet connection is not active!Please switch either data or WiFi to "
                            + "use this app!", Toast.LENGTH_LONG).show();
                    finish();
                } 
                else {
                    if ("text/plain".equals(type)) {       	
                        count++;
                        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
                        Log.d("Saurav","The entired Shared Text is:"+sharedText);
                        if(count > 2)
                        {
                            Toast.makeText(getApplicationContext(), "Probably you're not playing 'Hazy maze cave' here!", Toast.LENGTH_LONG).show();
                        }
                        String[] filteredStr = sharedText.split("/");
                        String formattedString = filteredStr[5];
                        Log.d("Saurav","Link to extract:"+filteredStr[5]);

                        String[] removed_extra_param = formattedString.split("\\?");
                        Log.d("Saurav","The Actual ID is:"+removed_extra_param[0]);

                        id = removed_extra_param[0];
                        new FetchTwitterData().execute();
                    } else if (type.startsWith("image/")) {
                        count++;
                        if(count > 2)
                        {
                            Toast.makeText(getApplicationContext(), "Probably you're not playing 'Hazy maze cave' here!", Toast.LENGTH_LONG).show();
                        }
                        Log.d("Saurav","A image to share, dont process directly forward!"+count
                                );
                        //if image process directly
                        if(haveCalledShare)
                        {
                            Toast.makeText(getApplicationContext(), "Probably you're not playing 'Hazy maze cave' here!", Toast.LENGTH_LONG).show();
                        }
                        Uri imageUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
                        if (imageUri != null) {

                            Intent shareIntent = new Intent();
                            shareIntent.setAction(Intent.ACTION_SEND);
                            shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
                            shareIntent.setType("image/jpeg");
                            startActivity(Intent.createChooser(shareIntent, "Share with which app?"));
                        }
                    }

                    //End:code for fetching the intent from twitter App

                }
            }
        }
    }
    private void handleShare(Intent sourceIntent){
    }
    class FetchTwitterData extends AsyncTask<String, String, String>{

        ProgressDialog fetchData = new ProgressDialog(MainActivity.this);
        @Override
        protected void onPreExecute() {
            // TODO Auto-generated method stub
            fetchData.setMessage("Invoking Twitter API!");
            fetchData.show();
            super.onPreExecute();
        }
        @Override
        protected String doInBackground(String... params) {

            // TODO Auto-generated method stub
            String result = null;


            result = getTwitterStream();
            return result;
        }
        @Override
        protected void onPostExecute(String result) {
            Log.d("Saurav", "JSON is:"+result);
            fetchData.dismiss();
            if(result == null)
            {
                Toast.makeText(getApplicationContext(), "This doesn't look like a tweet", Toast.LENGTH_SHORT).show();
                System.exit(0);
            }
            try {
                JSONObject jobObject = new JSONObject(result);
                if (jobObject != null) {
                    JSONObject object = jobObject
                            .optJSONObject("extended_entities");
                    if (object != null) {
                        JSONArray mediaArray = object.optJSONArray("media");
                        if (mediaArray != null) {
                            JSONObject mediaObject = mediaArray
                                    .optJSONObject(0);
                            if (mediaObject != null) {
                                url = mediaObject.optString("media_url");
                            }
                        }
                    }
                }
                Log.d("Saurav", "URl: " + url);
            } catch (JSONException e) {
                Log.d("Saurav","Tweet doesn't contain extended entity!");

            }
            if(!TextUtils.isEmpty(url)) {
                new FetchImageFromLink().execute();
            } else {
                Toast.makeText(getApplicationContext(), "This is not a picture you're trying to share!", Toast.LENGTH_LONG).show();
                finish();
            }
        }

    }
    //No need to touch the code below as these are phases of twitter authentication took almost a week to understand
    private String getTwitterStream() {
        String results = null;

        // Step 1: Encode consumer key and secret
        try {
            // URL encode the consumer key and secret
            String urlApiKey = URLEncoder.encode(CONSUMER_KEY, "UTF-8");
            String urlApiSecret = URLEncoder.encode(CONSUMER_SECRET, "UTF-8");

            // Concatenate the encoded consumer key, a colon character, and the
            // encoded consumer secret
            String combined = urlApiKey + ":" + urlApiSecret;

            // Base64 encode the string
            String base64Encoded = Base64.encodeToString(combined.getBytes(), Base64.NO_WRAP);

            // Step 2: Obtain a bearer token
            HttpPost httpPost = new HttpPost(TwitterTokenURL);
            httpPost.setHeader("Authorization", "Basic " + base64Encoded);
            httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
            httpPost.setEntity(new StringEntity("grant_type=client_credentials"));
            String rawAuthorization = getResponseBody(httpPost);
            Authenticated auth = jsonToAuthenticated(rawAuthorization);

            // Applications should verify that the value associated with the
            // token_type key of the returned object is bearer
            if (auth != null && auth.token_type.equals("bearer")) {

                // Step 3: Authenticate API requests with bearer token
                HttpGet httpGet = new HttpGet(TwitterStreamURL + id);

                // construct a normal HTTPS request and include an Authorization
                // header with the value of Bearer <>
                httpGet.setHeader("Authorization", "Bearer " + auth.access_token);
                httpGet.setHeader("Content-Type", "application/json");
                // update the results with the body of the response
                results = getResponseBody(httpGet);
            }
        } catch (UnsupportedEncodingException ex) {
        } catch (IllegalStateException ex1) {
        }
        return results;
    }
    private String getResponseBody(HttpRequestBase request) {
        StringBuilder sb = new StringBuilder();
        try {

            DefaultHttpClient httpClient = new DefaultHttpClient(new BasicHttpParams());
            HttpResponse response = httpClient.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();
            String reason = response.getStatusLine().getReasonPhrase();

            if (statusCode == 200) {

                HttpEntity entity = response.getEntity();
                InputStream inputStream = entity.getContent();

                BufferedReader bReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"), 8);
                String line = null;
                while ((line = bReader.readLine()) != null) {
                    sb.append(line);
                }
            } else {
                sb.append(reason);
            }
        } catch (UnsupportedEncodingException ex) {
        } catch (ClientProtocolException ex1) {
        } catch (IOException ex2) {
        }
        return sb.toString();
    }
    // convert a JSON authentication object into an Authenticated object
    private Authenticated jsonToAuthenticated(String rawAuthorization) {
        Authenticated auth = null;
        if (rawAuthorization != null && rawAuthorization.length() > 0) {
            try {
                Gson gson = new Gson();
                auth = gson.fromJson(rawAuthorization, Authenticated.class);
            } catch (IllegalStateException ex) {
                // just eat the exception
            }
        }
        return auth;
    }
    class FetchImageFromLink extends AsyncTask<Void, String, Uri>{
        @Override
        protected void onPreExecute() {
            // TODO Auto-generated method stub 
            super.onPreExecute();
        }
        @Override
        protected Uri doInBackground(Void... params) {
            Uri dummyUri = null;
            Uri bmpUri = null;
            if (!TextUtils.isEmpty(url))
                dummyUri = Uri.parse(url); // just dummy URI'
            if(dummyUri !=null) {
                Bitmap bitmap = null;
                try {
                    bitmap = Picasso.with(getApplicationContext()).load(dummyUri).get();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                if (bitmap != null) {
                    try {
                        //File file = new File(Environment.getDownloadCacheDirectory(),"share_image_" + System.currentTimeMillis() + ".png");
                        File file =  new File(Environment.getExternalStoragePublicDirectory(  
                                Environment.DIRECTORY_DOWNLOADS), "share_image_" + System.currentTimeMillis() + ".png");
                        file.getParentFile().mkdirs();
                        FileOutputStream out = new FileOutputStream(file);
                        bitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
                        out.close();
                        bmpUri = Uri.fromFile(file);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            // TODO Auto-generated method stub
            return bmpUri;
        }
        @Override
        protected void onPostExecute(Uri result) {
            // TODO Auto-generated method stub
            super.onPostExecute(result);
            if(result !=null) {
                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.putExtra(Intent.EXTRA_STREAM, result);
                shareIntent.setType("image/*");
                startActivity(Intent.createChooser(shareIntent," Share Image to"));
            } else {
                finish();
            }
        }

    }
    //Function that returns if connection is active or not
    public Boolean checkMyConnection()
    {
        ConnectivityManager cmgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if(cmgr.getActiveNetworkInfo() != null)
            return cmgr.getActiveNetworkInfo().isConnected();
        return false;	

    }
}
