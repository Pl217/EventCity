package pl217.mosis.activity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import pl217.mosis.R;
import pl217.mosis.RESTful;

public class BluetoothActivity extends AppCompatActivity {

    private final UUID APP_UUID = UUID.fromString("c4dfa1c7-17c9-4f75-bc54-86dae60179d4");

    private BluetoothAdapter mBluetoothAdapter;
    private Set<BluetoothDevice> pairedDevices;

    private ArrayList<String> mArrayListPaired = new ArrayList<String>();
    private ArrayAdapter<String> mArrayAdapterPaired;

    private ArrayList<String> mArrayListDiscovered = new ArrayList<String>();
    private ArrayAdapter<String> mArrayAdapterDiscovered;
    // Create a BroadcastReceiver for ACTION_FOUND
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Add the name and address to an array adapter to show in a ListView
                if (!mArrayListDiscovered.contains(device.getName() + "\n" + device.getAddress()))
                    mArrayListDiscovered.add(device.getName() + "\n" + device.getAddress());
                mArrayAdapterDiscovered.notifyDataSetChanged();
            }
        }
    };
    private View mProgressBar;
    private final BroadcastReceiver mFinished = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Button discoverButton = (Button) findViewById(R.id.discoverDevices);
                discoverButton.setEnabled(true);

                mProgressBar.setVisibility(View.GONE);
            }
        }
    };
    private final BroadcastReceiver mStarted = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                Button discoverButton = (Button) findViewById(R.id.discoverDevices);
                discoverButton.setEnabled(false);

                mProgressBar.setVisibility(View.VISIBLE);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_bluetooth);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        setupActionBar();

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        pairedDevices = mBluetoothAdapter.getBondedDevices();
        // If there are paired devices
        if (pairedDevices.size() > 0) {
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {
                // Add the name and address to an array adapter to show in a ListView
                mArrayListPaired.add(device.getName() + "\n" + device.getAddress());
            }
        }

        mProgressBar = findViewById(R.id.bluetooth_progress);

        ListView pairedDevices = (ListView) findViewById(R.id.pairedDevices);
        mArrayAdapterPaired = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mArrayListPaired);
        pairedDevices.setAdapter(mArrayAdapterPaired);

        ListView discoveredDevices = (ListView) findViewById(R.id.discoveredDevices);
        mArrayAdapterDiscovered = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mArrayListDiscovered);
        discoveredDevices.setAdapter(mArrayAdapterDiscovered);

        Button discoverButton = (Button) findViewById(R.id.discoverDevices);
        if (discoverButton != null)
            discoverButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mBluetoothAdapter.startDiscovery();
                }
            });

        pairedDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String device = mArrayListPaired.get(position);

                setConnectThread(device);
            }
        });

        discoveredDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String device = mArrayListDiscovered.get(position);

                setConnectThread(device);
            }
        });

        AcceptThread server = new AcceptThread();
        server.start();
    }

    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void sendToServer(String friend) {
        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        boolean test = sharedPref.getBoolean(getString(R.string.stored_geofence), false);
        RESTful.becomeFriends(friend, mBluetoothAdapter.getName(),
                sharedPref.getBoolean(getString(R.string.stored_geofence), false));

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(BluetoothActivity.this, "You got yourself a friend", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setConnectThread(String device) {
        String address = device.substring(device.indexOf("\n") + 1, device.length());

        BluetoothDevice bluetoothDevice = mBluetoothAdapter.getRemoteDevice(address);
        ConnectThread client = new ConnectThread(bluetoothDevice);
        client.start();
    }

    @Override
    protected void onStart() {
        super.onStart();

        registerReceiver(mReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND)); // Don't forget to unregister during onDestroy
        registerReceiver(mStarted, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED));
        registerReceiver(mFinished, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
    }

    @Override
    protected void onStop() {
        super.onStop();

        unregisterReceiver(mReceiver);
        unregisterReceiver(mFinished);
        unregisterReceiver(mStarted);
    }

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            // Use a temporary object that is later assigned to mmServerSocket,
            // because mmServerSocket is final
            BluetoothServerSocket tmp = null;

            try {
                // MY_UUID is the app's UUID string, also used by the client code
                tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord("EventCity", APP_UUID);
            } catch (IOException e) {
            }
            mmServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned
            while (true) {
                try {
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    break;
                }
                // If a connection was accepted
                if (socket != null) {
                    // Do work to manage the connection (in a separate thread)
                    manageConnectedSocket(socket);
                    cancel();
                    break;
                }
            }
        }

        private void manageConnectedSocket(BluetoothSocket socket) {

            ConnectedThread connectedThread = new ConnectedThread(socket);

            connectedThread.start();
        }

        /**
         * Will cancel the listening socket, and cause the thread to finish
         */
        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.w("Socket exception", e.getMessage());
            }
        }
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;
            mmDevice = device;

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server code
                tmp = device.createRfcommSocketToServiceRecord(APP_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it will slow down the connection
            mBluetoothAdapter.cancelDiscovery();

            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and get out
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    closeException.printStackTrace();
                }
                return;
            }

            // Do work to manage the connection (in a separate thread)
            manageConnectedSocket(mmSocket);
        }

        private void manageConnectedSocket(BluetoothSocket mmSocket) {
            ConnectedThread connectedThread = new ConnectedThread(mmSocket);

            try {
                SharedPreferences sharedPref = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
                connectedThread.write(sharedPref.getString(getString(R.string.stored_username), "Not provided").getBytes("UTF-8"));

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Snackbar snackbar = Snackbar.make(findViewById(R.id.pairedDevices),
                                "Awaiting request confirmation", Snackbar.LENGTH_LONG);
                        snackbar.show();
                    }
                });
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

        /**
         * Will cancel an in-progress connection, and close the socket
         */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[128];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    // Send the obtained bytes to the UI activity
                    /*mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                            .sendToTarget();*/
                    final String text = new String(buffer, "UTF-8").trim();
                    this.showDialog(text);

                } catch (IOException e) {
                    break;
                }
            }
        }

        private void showDialog(final String username) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    AlertDialog.Builder aBuilder = new AlertDialog.Builder(BluetoothActivity.this);
                    aBuilder.setTitle("Friends request");
                    aBuilder.setMessage("Do you want to become friends with " + username + "?");

                    aBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ExecutorService transThread = Executors.newSingleThreadExecutor();
                            transThread.submit(new Runnable() {
                                @Override
                                public void run() {
                                    sendToServer(username);
                                }
                            });
                        }
                    });

                    aBuilder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            //dialog.cancel();
                        }
                    });

                    aBuilder.show();
                }
            });
        }

        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
