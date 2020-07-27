package com.gaoxh.nettystudy;

import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import javax.crypto.SecretKey;

public class State {

    private int state = 0;

    private SecretKey secretKey;

    public void generateKey() throws NoSuchAlgorithmException {
        secretKey=AESUtil.generateKey();
    }

    public SecretKey getSecretKey() {
        return secretKey;
    }

    final static int IS_CONNECTED = 0x01;
    final static int IS_REGISTERED = 0x02;
    final static int IS_MATCHED = 0x04;
    final static int IS_WAIT_HEARTBEAT = 0x08;
    final static int IS_WAIT_CHECKBEAT = 0x10;

    private State() {
    }

    private static State instance;

    public static synchronized State getInstance() {
        if (instance == null) {
            return instance = new State();
        } else {
            return instance;
        }
    }

    public void init() {
        state = 0;
    }

    public void connected() {
        state |= IS_CONNECTED;
    }

    public void registered() {
        state |= IS_REGISTERED;
    }

    public void matched() {
        state |= IS_MATCHED;
    }


    public void waitCheckBeat() {
        state |= IS_WAIT_CHECKBEAT;
    }

    public void waitHeartBeat() {
        state |= IS_WAIT_HEARTBEAT;
    }


    public void getCheckBeat() {
        state &= ~IS_WAIT_CHECKBEAT;
    }

    public void getHeartBeat() {
        state &= ~IS_WAIT_HEARTBEAT;
    }


    public boolean isWaitHeartbeat() {
        return (state & IS_WAIT_HEARTBEAT) == IS_WAIT_HEARTBEAT;
    }
    public boolean isWaitCheckBeat() {
        return (state & IS_WAIT_CHECKBEAT) == IS_WAIT_CHECKBEAT;
    }
    public boolean isMatched() {
        return (state & IS_MATCHED) == IS_MATCHED;
    }
    public boolean isRegistered() {
        return (state & IS_REGISTERED) == IS_REGISTERED;
    }
    public boolean isConnected() {
        return (state & IS_CONNECTED) == IS_CONNECTED;
    }



}
