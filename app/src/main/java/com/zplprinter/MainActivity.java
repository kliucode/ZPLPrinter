package com.zplprinter;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.drawable.Drawable;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MainActivity extends AppCompatActivity {

    ServerSocket serverSocket;
    Thread ServerThread = null;
    TextView tvAddress, tvPort, tvMessage;
    ImageView ivImage;
    public static final int SERVER_PORT = 9100;
    public static String SERVER_IP = "";
    private BufferedReader input;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvAddress = findViewById(R.id.ipaddress);
        tvPort = findViewById(R.id.port);
        tvMessage = findViewById(R.id.message);
        ivImage = findViewById(R.id.resultImage);

        try {
            SERVER_IP = getLocalIpAddress();
        }
        catch (UnknownHostException e)
        {
            tvPort.setText(e.getMessage());
            tvAddress.setText("Unable to start listening");
            e.printStackTrace();
        }

        ServerThread = new Thread(new ServerThread());
        ServerThread.start();
    }

    private String getLocalIpAddress() throws UnknownHostException {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        assert wifiManager != null;
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipInt = wifiInfo.getIpAddress();
        return InetAddress.getByAddress(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(ipInt).array()).getHostAddress();
    }

    class ServerThread implements Runnable {
        @Override
        public void run() {
            Socket socket;
            try {
                serverSocket = new ServerSocket(SERVER_PORT);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvMessage.setText("Waiting for Connection...");
                        tvAddress.setText("IP: " + SERVER_IP);
                        tvPort.setText("Listening on port " + SERVER_PORT);
                    }
                });
                while(true) {
                    try {
                        socket = serverSocket.accept();
                        input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                tvMessage.setText("Message Received\n");
                            }
                        });

                        while (true) {
                            try {
                                final String message = input.readLine();
                                if (message != null) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            new Thread(new ProcessThread(message)).start();
                                        }
                                    });
                                } else {
                                    break;
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
    }

    private class ProcessThread implements Runnable {
        private String zpl;
        public ProcessThread(String content) {
            super();
            zpl = content;
        }

        @Override
        public void run() {
            ivImage.setImageResource(0);
            try {
                int viewWidth = ivImage.getWidth() > 2000 ? 2000 : ivImage.getWidth();
                int viewHeight = ivImage.getHeight() > 2000 ? 2000 : ivImage.getHeight();
                String url = "https://api.labelary.com/v1/printers/8dpmm/labels/4x6/0/"+zpl;
                InputStream is = (InputStream) new URL(url).getContent();
                Drawable drawable = Drawable.createFromStream(is, "Label");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ivImage.setImageDrawable(drawable);
                        tvMessage.setText("Label Rendered");
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvMessage.setText("Unable to Render ZPL: \n"+e.getClass().getSimpleName()+": "+e.getMessage());
                    }
                });
            }
        }
    }
}