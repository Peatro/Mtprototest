package com.peatroxd.mtprototest.service;

import org.springframework.stereotype.Service;

import java.net.InetSocketAddress;
import java.net.Socket;

@Service
public class ProxyChecker {

    public boolean isAlive(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 2000);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
