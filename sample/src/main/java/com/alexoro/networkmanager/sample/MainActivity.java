package com.alexoro.networkmanager.sample;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.alexoro.networkmanager.NetworkCondition;
import com.alexoro.networkmanager.NetworkManager;
import com.alexoro.networkmanager.NetworkManagerSingleton;

import java.util.concurrent.Callable;

/**
 * Created by uas.sorokin@gmail.com
 */
public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        NetworkManagerSingleton.init(getApplicationContext());

        Button networkDoNotAwaitView = (Button) findViewById(R.id.network_do_not_await);
        Button networkAwaitView = (Button) findViewById(R.id.network_await);
        networkDoNotAwaitView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doNotAwait();
            }
        });
        networkAwaitView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doAwait();
            }
        });
    }

    private void doNotAwait() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                NetworkManager networkManager = NetworkManagerSingleton.get();
                NetworkCondition condition = new NetworkCondition();
                condition.isAwaitConnection = false;
                condition.isAllowInRoaming = true;

                try {
                    networkManager.executeByCondition(new EmptyCallable(), condition);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Done", Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (final Exception ex) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "" + ex, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }

    private void doAwait() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                NetworkManager networkManager = NetworkManagerSingleton.get();
                NetworkCondition condition = new NetworkCondition();
                condition.isAwaitConnection = true;
                condition.isAllowInRoaming = true;

                try {
                    networkManager.executeByCondition(new EmptyCallable(), condition);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Done", Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (final Exception ex) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "" + ex, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }

    private static class EmptyCallable implements Callable<Void> {
        @Override
        public Void call() throws Exception {
            return null;
        }
    }

}