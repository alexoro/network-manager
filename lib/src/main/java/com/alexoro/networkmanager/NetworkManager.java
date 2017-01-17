/*
 * Copyright (C) 2015 Alexander Sorokin (alexoro)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.alexoro.networkmanager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * Created by uas.sorokin@gmail.com
 */
public class NetworkManager {

    private static final IntentFilter INTENT_FILTER_NETWORK;
    static {
        INTENT_FILTER_NETWORK = new IntentFilter();
        INTENT_FILTER_NETWORK.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
    }

    private static final NetworkCondition COND_AWAIT_NETWORK_FALSE;
    private static final NetworkCondition COND_AWAIT_NETWORK_TRUE;
    static {
        COND_AWAIT_NETWORK_FALSE = new NetworkCondition();
        COND_AWAIT_NETWORK_FALSE.isAllowInRoaming = true;
        COND_AWAIT_NETWORK_FALSE.isAwaitConnection = false;

        COND_AWAIT_NETWORK_TRUE = new NetworkCondition();
        COND_AWAIT_NETWORK_TRUE.isAllowInRoaming = true;
        COND_AWAIT_NETWORK_TRUE.isAwaitConnection = true;
    }


    private final Context mAppContext;
    private final ConnectivityManager mConnectivityManager;
    private final NetworkBroadcastReceiver mNetworkBroadcastReceiver;

    private final ArrayList<OnNetworkStateUpdatedListener> mStateListenerList;
    private final Set<Object> mConsumerTags;
    private final Object mIntegrityLock;
    private final NetworkState mStateForListener;


    public NetworkManager(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("context is null");
        }

        mAppContext = context.getApplicationContext();
        mConnectivityManager = (ConnectivityManager) mAppContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        mNetworkBroadcastReceiver = new NetworkBroadcastReceiver();

        mStateListenerList = new ArrayList<>();
        mConsumerTags = new HashSet<>();
        mIntegrityLock = new Object();
        mStateForListener = new NetworkState();
    }

    public boolean isConnected() {
        NetworkInfo netInfo = mConnectivityManager.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnected();
    }

    public boolean isRoaming() {
        NetworkInfo netInfo = mConnectivityManager.getActiveNetworkInfo();
        return netInfo != null && netInfo.isRoaming();
    }

    public void registerStateListener(OnNetworkStateUpdatedListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener is null");
        }

        synchronized (mIntegrityLock) {
            mStateListenerList.add(listener);
            startObserveImpl(listener);
        }
    }

    public void unregisterStateListener(OnNetworkStateUpdatedListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener is null");
        }

        synchronized (mIntegrityLock) {
            stopObserveImpl(listener);
            mStateListenerList.remove(listener);
        }
    }

    private void startObserveImpl(Object consumerTag) {
        int oldSize = mConsumerTags.size();
        mConsumerTags.add(consumerTag);
        int newSize = mConsumerTags.size();
        if (oldSize == 0 && newSize > 0) {
            mAppContext.registerReceiver(
                    mNetworkBroadcastReceiver,
                    INTENT_FILTER_NETWORK);
        }
    }

    private void stopObserveImpl(Object consumerTag) {
        int oldSize = mConsumerTags.size();
        mConsumerTags.remove(consumerTag);
        int newSize = mConsumerTags.size();
        if (oldSize > 0 && newSize == 0) {
            mAppContext.unregisterReceiver(mNetworkBroadcastReceiver);
        }
    }

    private class NetworkBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null
                    && INTENT_FILTER_NETWORK.hasAction(intent.getAction())) {
                synchronized (mIntegrityLock) {
                    mIntegrityLock.notifyAll();
                    mStateForListener.isConnected = isConnected();
                    mStateForListener.isRoaming = isRoaming();
                    for (int i = 0; i < mStateListenerList.size(); i++) {
                        mStateListenerList.get(i).onNetworkStateUpdated(mStateForListener);
                    }
                }
            }
        }
    }


    // ====================================================================

    public <T> T execute(Callable<T> callable, boolean isAwaitNetwork) throws Exception {
        return executeByCondition(
                callable,
                isAwaitNetwork ? COND_AWAIT_NETWORK_TRUE : COND_AWAIT_NETWORK_FALSE);
    }

    public <T> T executeByCondition(Callable<T> callable,
                                    NetworkCondition condition)
            throws Exception {

        if (callable == null) {
            throw new IllegalArgumentException("callable is null");
        }
        if (condition == null) {
            throw new IllegalArgumentException("condition is null");
        }

        boolean isHasInternalConsumer = condition.isAwaitConnection;
        if (isHasInternalConsumer) {
            startObserveImpl(callable);
        }

        while (true) {
            synchronized (mIntegrityLock) {
                if (!isConnected()) {
                    if (condition.isAwaitConnection) {
                        mIntegrityLock.wait();
                        continue;
                    } else {
                        throw new NetworkConditionException(NetworkConditionException.Reason.NO_CONNECTION);
                    }
                }
            }

            if (isRoaming()) {
                if (!condition.isAllowInRoaming) {
                    throw new NetworkConditionException(NetworkConditionException.Reason.ROAMING);
                }
            }

            try {
                return callable.call();
            } catch (IOException ex) {
                // Any IOException is treated as NetworkException
                // So, let's retry
                continue;
            } catch (Exception ex) {
                if (isHasInternalConsumer) {
                    stopObserveImpl(callable);
                }
                throw ex;
            }
        }
    }

}