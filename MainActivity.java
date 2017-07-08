package com.example.safur.smartshare;

import android.Manifest;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import static android.R.attr.permission;

public class MainActivity extends AppCompatActivity implements ServiceListFragment.DeviceActionListener, WifiP2pManager.ConnectionInfoListener {
    public static final String TAG = "SmartShare";

    public static final String TXTRECORD_PROP_AVAILABLE = "available";
    public static final String SERVICE_INSTANCE = "_smartshare";
    public static final String SERVICE_REG_TYPE = "_presence._tcp";
    protected static final int CHOOSE_FILE_RESULT_CODE = 20;
    static final int SERVER_PORT = 7777;
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private final IntentFilter intentFilter = new IntentFilter();
    Context context = this;
    String imageuri;
    String path;
    String info1;
    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    private BroadcastReceiver receiver = null;
    private WifiP2pDnsSdServiceRequest serviceRequest;
    private ServiceListFragment serviceListFragment;
    private TextView statusTextView;

    //#################################################################
    public static boolean copyFile(InputStream inputStream, OutputStream out) {
        Log.d(TAG, "copyFile: starts");
        byte buf[] = new byte[1024];
        int len;
        try {
            while ((len = inputStream.read(buf)) != -1) {
                out.write(buf, 0, len);

            }
            out.close();
            inputStream.close();
        } catch (IOException e) {
            Log.d(MainActivity.TAG, e.toString());
            return false;
        }
        Log.d(TAG, "copyFile: ends");

        return true;


    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: ma");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        statusTextView = (TextView) findViewById(R.id.status_text_view);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);
        startRegistration();

        // serviceListFragment= new ServiceListFragment().getSupportFragmentManager().beginTransaction.add.


        serviceListFragment = new ServiceListFragment();
        getSupportFragmentManager().beginTransaction()
                .add(R.id.container_root, serviceListFragment, "smartshare").commit();
    }

    private void startRegistration() {
        Log.d(TAG, "startRegistration: ma");
        Map<String, String> record = new HashMap<>();
        record.put(TXTRECORD_PROP_AVAILABLE, "visible");
        final WifiP2pDnsSdServiceInfo serviceInfo = WifiP2pDnsSdServiceInfo.newInstance(
                SERVICE_INSTANCE, SERVICE_REG_TYPE, record);
        /////////here before add service, do clear service and add service in the on success method of clearservice
//        you should use the WifiP2pManager's clearLocalServices method.
//        And it is important, that you should only call addLocalService if the listener passed
//        in the clearLocalServices returned with the onSuccess callback.

        manager.clearLocalServices(channel, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                manager.addLocalService(channel, serviceInfo, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        appendStatus("Added Local Service");
                    }

                    @Override
                    public void onFailure(int reason) {
                        appendStatus("Failed to add a Service");
                    }
                });
            }

            @Override
            public void onFailure(int reason) {

            }
        });


        startDiscovery();

    }

    private void startDiscovery() {
        Log.d(TAG, "startDiscovery: ma");
        manager.setDnsSdResponseListeners(channel, new WifiP2pManager.DnsSdServiceResponseListener() {
            @Override
            public void onDnsSdServiceAvailable(String instanceName, String registrationType, WifiP2pDevice srcDevice) {
                if (instanceName.equalsIgnoreCase(SERVICE_INSTANCE)) {
                    ServiceListFragment fragment = (ServiceListFragment) getSupportFragmentManager()
                            .findFragmentByTag("smartshare");
                    Log.d(TAG, "onDnsSdServiceAvailable:Starts ");
                    Log.d(TAG, "onDnsSdServiceAvailable: " + fragment);
                    if (fragment != null) {
                        Log.d(TAG, "onDnsSdServiceAvailable: fragment not null");
//                        ServiceListFragment.WiFiDevicesAdapter adapter = ((ServiceListFragment.WiFiDevicesAdapter)
//                                fragment.getListAdapter());
                        WiFiP2pService service = new WiFiP2pService();
                        service.device = srcDevice;
                        //CHECK HERE IF THERE IS AN UPDATED CONTENT, CONCATENATE THE NAME TO THE INSTANCE NAME AND APPLY THE LOGIC HERE.
                        service.instanceName = instanceName;
                        service.serviceRegistrationType = registrationType;
//                        adapter.add(service);
//                        adapter.notifyDataSetChanged();
                        connect(service);
                        Log.d(TAG, "onDnsSdServiceAvailable: " + instanceName + "  " + registrationType +
                                "  " + srcDevice);
                    }
                }

            }
        }, new WifiP2pManager.DnsSdTxtRecordListener() {
            @Override
            public void onDnsSdTxtRecordAvailable(String fullDomainName, Map<String, String> txtRecordMap, WifiP2pDevice srcDevice) {
                Log.d(TAG, "onDnsSdTxtRecordAvailable: " + srcDevice.deviceName +
                        " is " + txtRecordMap.get(TXTRECORD_PROP_AVAILABLE) + fullDomainName);
            }
        });

        serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();
        manager.addServiceRequest(channel, serviceRequest, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                appendStatus("Added Service Request");
            }

            @Override
            public void onFailure(int reason) {
                appendStatus("Service Request Failed");
            }
        });

        manager.discoverServices(channel, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                appendStatus("Service discovery initiated");
            }

            @Override
            public void onFailure(int arg0) {
                appendStatus("Service discovery failed");

            }
        });
    }

    public void appendStatus(String status) {
        String current = statusTextView.getText().toString();
        statusTextView.setText(current + "\n" + status);
    }

    @Override
    public void connect(WiFiP2pService wifiP2pService) {
        Log.d(TAG, "connect: ma");
        final WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = wifiP2pService.device.deviceAddress;
        config.wps.setup = WpsInfo.PBC;
        //  config.groupOwnerIntent=15;

        Log.d(TAG, "connect: " + config.deviceAddress);


        if (serviceRequest != null) {
            Log.d(TAG, "connect: service request ma");
            manager.removeServiceRequest(channel, serviceRequest, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {

                }

                @Override
                public void onFailure(int reason) {

                }
            });
            Log.d(TAG, "connect: b4manager.connect ma");
            manager.connect(channel, config, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    appendStatus("Connecting To Service");
                    Log.d(TAG, "onSuccess: connectonsuccess/create group");
//                    manager.createGroup(channel, new WifiP2pManager.ActionListener(){
//
//                        @Override
//                        public void onSuccess() {
//                            Log.d(TAG, "creategrouponSuccess: ");
////                switch(config.deviceAddress) {
////                    case "ea:50:8b:e6:9f:70":
//                            Log.d(TAG, "connect: " + config.groupOwnerIntent);
////                        config.groupOwnerIntent = 15;
////                        Log.d(TAG, "connect: " + config.groupOwnerIntent);
////                        break;
////                    default:
////                        config.groupOwnerIntent = 0;
////                }
//                        }
//                        @Override
//                        public void onFailure(int reason) {
//
//                        }
//                    });
                }

                @Override
                public void onFailure(int reason) {
                    appendStatus("Failed Connecting To Service");

                }
            });
        }

    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {
        Log.d(TAG, "onConnectionInfoAvailable: ma");
        if (info.groupFormed && info.isGroupOwner) {
            if (permission != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        PERMISSIONS_STORAGE,
                        REQUEST_EXTERNAL_STORAGE
                );
            }

            new FileServerAsyncTask(this, statusTextView).execute();
        } else if (info.groupFormed) {
            info1 = info.groupOwnerAddress.getHostAddress();
            Uri uri = Uri.parse(Environment.getExternalStorageDirectory()
                    + "/Pictures/wifip2p/");
            Log.d(TAG, "onClick: Gallery " + uri);
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setDataAndType(uri, "image/*");
            startActivityForResult(intent, CHOOSE_FILE_RESULT_CODE);
            statusTextView.setText("Connected As Client");
        }

    }


//

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult: called");
        Uri uri = data.getData();
        path = uri.toString();
        Log.d(TAG, "onActivityResult: " + path);

        Log.d(TAG, "onActivityResult: server started");
        ClientThread client;
        client = new ClientThread(this, info1, SERVER_PORT, path);
        new Thread(client).start();
        Log.d(TAG, "onActivityResult: client started");

    }

//    //########################################################SERVER

    @Override
    protected void onRestart() {
        Log.d(TAG, "onRestart: called");
        Fragment frag = getFragmentManager().findFragmentByTag("smartshare");
        if (frag != null) {
            getFragmentManager().beginTransaction().remove(frag).commit();
        }
        super.onRestart();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop: called");
        if (manager != null && channel != null) {
            manager.removeGroup(channel, new WifiP2pManager.ActionListener() {

                @Override
                public void onFailure(int reasonCode) {
                    Log.d(TAG, "Disconnect failed. Reason :" + reasonCode);
                }

                @Override
                public void onSuccess() {
                }

            });
        }
        super.onStop();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause: called");
        super.onPause();
        unregisterReceiver(receiver);
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume: called");
        super.onResume();
        receiver = new SmartShareBroadcastReceiver(manager, channel, this);
        registerReceiver(receiver, intentFilter);
    }

    public static class FileServerAsyncTask extends AsyncTask<Void, Void, String> {

        private Context context;
        private TextView statusText;

        /**
         * @param context
         * @param statusText
         */
        public FileServerAsyncTask(Context context, View statusText) {
            this.context = context;
            this.statusText = (TextView) statusText;
        }


        @Override
        protected String doInBackground(Void... params) {
            Log.d(TAG, "doInBackground: starts");
            try {
                ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
                Log.d(MainActivity.TAG, "Server: Socket opened");
                Socket client = serverSocket.accept();
                Log.d(MainActivity.TAG, "Server: connection done");
                Log.d(TAG, "doInBackground: Client Name is " + client.getLocalAddress().getHostName());
                final File f = new File(Environment.getExternalStorageDirectory() + "/DCIM/"
                        + context.getPackageName() + "/wifip2pshared-" + System.currentTimeMillis()
                        + ".jpg");
                Log.d(TAG, "doInBackground: newfile created");
                Log.d(TAG, "doInBackground: " + f);

                File dirs = new File(f.getParent());
                Log.d(TAG, "doInBackground: " + dirs.exists());
                if (!dirs.exists())
                    dirs.mkdirs();
                Log.d(TAG, "doInBackground: " + dirs.exists());

                f.createNewFile();

                Log.d(MainActivity.TAG, "server: copying files " + f.toString());
                InputStream inputstream = client.getInputStream();
                // Log.d(TAG, "doInBackground: IP ADDS" + getIpAddress() );
                copyFile(inputstream, new FileOutputStream(f));

                Log.d(TAG, "doInBackground: clientadd " + client.getRemoteSocketAddress());
                // Long fsize=f.length()/1024;
                //   Long timeelp=(timeend-timestart)/1000;

//                File file= new File(Environment.getExternalStorageDirectory().getPath() + "/ti-" + System.currentTimeMillis() +".txt");
//                Log.d(TAG, "copyFile: path ===" + file);
//
//
//                FileWriter writer = new FileWriter(file);
//                writer.write("Host Ip address: " + hostadd + "\n" +"Host mac address: " + hostmacad
//                        +"\n" +"Peer mac add: "+ peeradd  +"\n"+ "File transferred: " + f.getAbsolutePath().toString()
//                        + "\n"+"File size: " + fsize+ "KB" + "\n" + "Transfer Time: " +timeelp+"seconds");
//                writer.close();

                Log.d(TAG, "doInBackground: file finished copying");
                serverSocket.close();
                Log.d(TAG, "doInBackground: serversocketclosed");
                return f.getAbsolutePath();
            } catch (IOException e) {
                Log.e(MainActivity.TAG, e.getMessage());
                return null;
            }
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(String result) {

            Log.d(TAG, "onPostExecute: starts");
            if (result != null) {
                statusText.setText("File copied - " + result);
                Intent intent = new Intent();
                intent.setAction(android.content.Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse("file://" + result), "image/*");
                context.startActivity(intent);
                Log.d(TAG, "onPostExecute: ends");
            }

        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPreExecute()
         */
        @Override
        protected void onPreExecute() {
            statusText.setText("Opening a server socket");

        }

    }

    /////////////////////////////////////////////////////////////client
    public class ClientThread implements Runnable {


        Socket socket = new Socket();
        private Context context;
        private String host;
        private int port;
        private String path;
        private int SOCKET_TIMEOUT = 5000;

        public ClientThread(Context context, String host, int port, String path) {
            this.context = context;
            this.host = host;
            this.port = port;
            this.path = path;
        }

        @Override
        public void run() {
            Log.d(TAG, "run: statrted");


            try {
                Log.d(MainActivity.TAG, "Opening client socket - ");
                socket.bind(null);
                socket.connect((new InetSocketAddress(host, port)));

                Log.d(MainActivity.TAG, "Client socket - " + socket.isConnected());
                OutputStream stream = socket.getOutputStream();
                //ContentResolver cr = context.getContentResolver();
                ContentResolver cr = context.getContentResolver();
                InputStream is = null;
                Log.d(TAG, "run: " + path);
                try {
                    is = cr.openInputStream(Uri.parse(path));
                } catch (FileNotFoundException e) {
                    Log.d(MainActivity.TAG, e.toString());
                }
                copyFile(is, stream);
                Log.d(MainActivity.TAG, "Client: Data written");
            } catch (IOException e) {
                Log.e(MainActivity.TAG, e.getMessage());
            } finally {
                if (socket != null) {
                    if (socket.isConnected()) {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            // Give up
                            e.printStackTrace();
                        }
                    }
                }

            }
        }
    }


}

