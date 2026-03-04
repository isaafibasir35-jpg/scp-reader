package com.example.scpreader;

import android.os.Build;
import android.util.Log;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

public class EdgeTTSClient {
    private static final String TAG = "EdgeTTSClient";
    private static final String WS_URL = "wss://speech.platform.bing.com/consumer/speech/synthesize/readaloud/edge/v1?TrustedClientToken=6A5AA1D4EAFF4E9FB37E23D68491D6F4";
    
    private final OkHttpClient client;
    private WebSocket webSocket;
    private FileOutputStream fileOutputStream;
    private final String outputPath;
    private final TTSCallback callback;
    private final String requestId;

    public interface TTSCallback {
        void onFinish();
        void onError(String error);
    }

    public EdgeTTSClient(String outputPath, TTSCallback callback) {
        this.client = new OkHttpClient();
        this.outputPath = outputPath;
        this.callback = callback;
        this.requestId = UUID.randomUUID().toString().replace("-", "");
    }

    private String getTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss 'GMT'Z (zzzz)", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(new Date());
    }

    public void synthesize(String text) {
        String url = WS_URL + "&ConnectionId=" + requestId;
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Pragma", "no-cache")
                .addHeader("Cache-Control", "no-cache")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36 Edg/130.0.0.0")
                .addHeader("Origin", "https://speech.microsoft.com")
                .addHeader("Accept-Language", "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7")
                .build();

        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                try {
                    fileOutputStream = new FileOutputStream(outputPath);
                    sendConfig(webSocket);
                    sendSSML(webSocket, text);
                } catch (IOException e) {
                    callback.onError(e.getMessage());
                }
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                if (text.contains("Path:turn.end")) {
                    cleanup();
                    callback.onFinish();
                }
            }

            @Override
            public void onMessage(WebSocket webSocket, ByteString bytes) {
                try {
                    int headerLength = ((bytes.getByte(0) & 0xFF) << 8) | (bytes.getByte(1) & 0xFF);
                    String header = bytes.substring(2, headerLength + 2).utf8();
                    if (header.contains("Path:audio")) {
                        byte[] audioData = bytes.substring(headerLength + 2, bytes.size()).toByteArray();
                        if (fileOutputStream != null) {
                            fileOutputStream.write(audioData);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error writing audio data", e);
                }
            }
            
            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                Log.e(TAG, "WebSocket Failure: " + (response != null ? response.message() : ""), t);
                cleanup();
                callback.onError(t.getMessage());
            }
            
            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                cleanup();
            }
        });
    }
            
    private void sendConfig(WebSocket ws) {
        String msg = "X-Timestamp:" + getTimestamp() + "\r\n" +
                "Content-Type:application/json; charset=utf-8\r\n" +
                "Path:speech.config\r\n\r\n" +
                "{\"context\":{\"synthesis\":{\"strategy\":\"uniform\",\"outputFormat\":\"audio-24khz-48kbitrate-mono-mp3\"}}}";
        ws.send(msg);
    }
            
    private void sendSSML(WebSocket ws, String text) {
        // Escape XML special characters
        String escapedText = text.replace("&", "&amp;")
                                 .replace("<", "&lt;")
                                 .replace(">", "&gt;")
                                 .replace("\"", "&quot;")
                                 .replace("'", "&apos;");
                                         
        String ssml = "<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='ru-RU'>" +
                "<voice name='ru-RU-SvetlanaNeural'>" + escapedText + "</voice></speak>";
        String msg = "X-RequestId:" + requestId + "\r\n" +
                "X-Timestamp:" + getTimestamp() + "\r\n" +
                "Content-Type:application/ssml+xml\r\n" +
                "Path:ssml\r\n\r\n" +
                ssml;
        ws.send(msg);
    }

    private void cleanup() {
        if (webSocket != null) {
            webSocket.close(1000, "Done");
        }
        try {
            if (fileOutputStream != null) {
                fileOutputStream.close();
            }
        } catch (IOException e) {}
    }
    
    public void stop() {
        cleanup();
    }
}
