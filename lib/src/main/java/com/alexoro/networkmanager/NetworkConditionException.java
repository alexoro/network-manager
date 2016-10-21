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

/**
 * Created by uas.sorokin@gmail.com
 */
public class NetworkConditionException extends Exception {

    static final long serialVersionUID = -1087476901124589102L;

    public enum Reason {
        NO_CONNECTION,
        ROAMING
    }


    private final Reason mReason;

    public NetworkConditionException(Reason reason) {
        mReason = reason;
    }

    @Override
    public String toString() {
        return "NetworkConditionException{" +
                "mReason=" + mReason +
                "} " + super.toString();
    }

}