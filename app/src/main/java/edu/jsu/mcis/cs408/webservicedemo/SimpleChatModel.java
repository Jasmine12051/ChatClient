package edu.jsu.mcis.cs408.webservicedemo;

import android.util.Log;
import androidx.lifecycle.MutableLiveData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.net.ssl.HttpsURLConnection;

public class SimpleChatModel extends AbstractModel {

    private static final String TAG = "ExampleWebServiceModel";

    private static final String GET_URL = "https://testbed.jaysnellen.com:8443/SimpleChat/board";
    private static final String POST_URL = "https://testbed.jaysnellen.com:8443/SimpleChat/board";
    private static final String DELETE_URL = "https://testbed.jaysnellen.com:8443/SimpleChat/board";
    private MutableLiveData<JSONObject> jsonData;
    private String outputText;

    private final ExecutorService requestThreadExecutor;
    private final Runnable httpGetRequestThread, httpPostRequestThread, httpDeleteRequestThread;
    private Future<?> pending;

    private String postData; // Added to store JSON data for POST request

    public SimpleChatModel() {
        requestThreadExecutor = Executors.newSingleThreadExecutor();

        httpGetRequestThread = () -> {
            if (pending != null) { pending.cancel(true); }
            try {
                pending = requestThreadExecutor.submit(new HTTPRequestTask("GET", GET_URL, null));
            } catch (Exception e) { Log.e(TAG, " Exception: ", e); }
        };

        httpPostRequestThread = () -> {
            if (pending != null) { pending.cancel(true); }
            try {
                pending = requestThreadExecutor.submit(new HTTPRequestTask("POST", POST_URL, postData));
            } catch (Exception e) { Log.e(TAG, " Exception: ", e); }
        };

        httpDeleteRequestThread = () -> {
            if (pending != null) { pending.cancel(true); }
            try {
                // Create a new HTTPRequestTask with DELETE method and URL
                pending = requestThreadExecutor.submit(new HTTPRequestTask("DELETE", DELETE_URL, null));
            } catch (Exception e) {
                Log.e(TAG, " Exception: ", e);
            }
        };
    }

    public void initDefault() {
        sendGetRequest();
    }

    public String getOutputText() {
        return outputText;
    }

    public void setOutputText(String newText) {
        String oldText = this.outputText;
        this.outputText = newText; // Store the original newText

        // Convert the new text to a map
        try {
            JSONObject jsonObject = new JSONObject(newText);
            Map<String, String> dataMap = new HashMap<>();

            // Get an iterator for the keys
            Iterator<String> keys = jsonObject.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                String value = jsonObject.getString(key);
                dataMap.put(key, value);
            }

            // Replace newText with the extracted data from the map
            StringBuilder extractedData = new StringBuilder();
            for (Map.Entry<String, String> entry : dataMap.entrySet()) {
                String value = entry.getValue();
                extractedData.append(value).append("\n");
            }
            this.outputText = extractedData.toString(); // Replace newText with extracted data
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing JSON: " + e.getMessage());
        }

        Log.i(TAG, "Output Text Change: From " + oldText + " to " + this.outputText);

        firePropertyChange(DefaultController.ELEMENT_OUTPUT_PROPERTY, oldText, this.outputText);
    }

    public void sendGetRequest() {
        httpGetRequestThread.run();
    }

    public void sendPostRequest(String jsonData) {
        this.postData = jsonData; // Set JSON data to postData
        httpPostRequestThread.run();
    }

    public void sendDeleteRequest() {
        httpDeleteRequestThread.run();
    }

    private void setJsonData(JSONObject json) {
        this.getJsonData().postValue(json);
        setOutputText(json.toString());
    }

    public MutableLiveData<JSONObject> getJsonData() {
        if (jsonData == null) {
            jsonData = new MutableLiveData<>();
        }
        return jsonData;
    }

    private class HTTPRequestTask implements Runnable {

        private final String method, urlString;
        private final String jsonData;

        HTTPRequestTask(String method, String urlString, String jsonData) {
            this.method = method;
            this.urlString = urlString;
            this.jsonData = jsonData;
        }

        @Override
        public void run() {
            JSONObject results = doRequest(urlString, jsonData);
            setJsonData(results);
        }

        private JSONObject doRequest(String urlString, String jsonData) {
            StringBuilder r = new StringBuilder();
            String line;
            HttpURLConnection conn = null;
            JSONObject results = null;

            try {
                if (Thread.interrupted()) throw new InterruptedException();

                URL url = new URL(urlString);
                conn = (HttpURLConnection) url.openConnection();

                conn.setReadTimeout(10000);
                conn.setConnectTimeout(15000);

                // Set the request method based on the 'method' parameter
                conn.setRequestMethod(method);
                conn.setDoInput(true);

                // Check if it's a POST request to include JSON data
                if (method.equals("POST")) {
                    conn.setDoOutput(true);
                    OutputStream out = conn.getOutputStream();
                    out.write(jsonData.getBytes());
                    out.flush();
                    out.close();
                }

                // Connect to the URL
                conn.connect();

                // Get the response code
                int code = conn.getResponseCode();

                // Handle DELETE request separately
                if (method.equals("DELETE")) {
                    if (code == HttpURLConnection.HTTP_OK) {
                        Log.d(TAG, "Delete operation successful");
                    } else {
                        Log.e(TAG, "Delete operation failed");
                    }
                }
                    // Handle other HTTP methods (GET, POST)
                if (code == HttpsURLConnection.HTTP_OK || code == HttpsURLConnection.HTTP_CREATED ) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));

                    do {
                        line = reader.readLine();
                        if (line != null) r.append(line);
                    } while (line != null);
                }

                results = new JSONObject(r.toString());
                Log.d(TAG, "Here are the results" + results);


            } catch (Exception e) {
                Log.e(TAG, " Exception: ", e);
            } finally {
                if (conn != null) { conn.disconnect(); }
            }

            Log.d(TAG, " JSON: " + r.toString());
            return results;
        }

    }

}
