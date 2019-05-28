package com.interswitchgroup.mobpaylib.api.utils;

/**
 * Created by ibrahimlawal on Mar/14/2016.
 *
 * @author fkrauthan
 * @see http://blog.dev-area.net/2015/08/13/android-4-1-enable-tls-1-1-and-tls-1-2/
 * @since 1.2.0
 * <p/>
 * Modified to work with okHttp3.1.2
 * And so it only uses TLSv1.2
 */

import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

public class TLSSocketFactory extends SSLSocketFactory {

    public X509TrustManager getX509TrustManager() {
        return x509TrustManager;
    }

    // Field named delegate so okHttp 3.1.2 will be
    // able to get our trust manager as suggested here:
    // https://github.com/square/okhttp/issues/2323#issuecomment-185055040
    private SSLSocketFactory delegate;
    private X509TrustManager x509TrustManager;

    public TLSSocketFactory() throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
        SSLContext context = SSLContext.getInstance("TLS");

        // Get, so we can use default Trust managers for our Factory
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init((KeyStore) null);
        TrustManager[] tm = trustManagerFactory.getTrustManagers();

        context.init(null, tm, null);
        for (int i = 0; i < tm.length; i++) {
            if (tm[i] instanceof X509TrustManager) {
                x509TrustManager = (X509TrustManager) tm[i];
            }
        }

        delegate = context.getSocketFactory();
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return delegate.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return delegate.getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
        return enableTLSOnSocket(delegate.createSocket(s, host, port, autoClose));
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        return enableTLSOnSocket(delegate.createSocket(host, port));
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
        return enableTLSOnSocket(delegate.createSocket(host, port, localHost, localPort));
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        return enableTLSOnSocket(delegate.createSocket(host, port));
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        return enableTLSOnSocket(delegate.createSocket(address, port, localAddress, localPort));
    }

    private Socket enableTLSOnSocket(Socket socket) {
        if (socket != null && (socket instanceof SSLSocket)) {
            // TODO Remove TLSv1.1 as soon as server starts accepting TLSv1.2 to comply with PCI-DSS
            Set<String> protocols = new HashSet<>(Arrays.asList(((SSLSocket) socket).getEnabledProtocols()));//Get existing protocols
            // Add TLSv1.1 if it was missing, to support old api server
            protocols.add("TLSv1.1");
            protocols.add("TLSv1.2");
            // protocols.remove("SSLv3"); // Remove SSLv3 to fix bug in android 4.4 falling back to something unwanted -> https://stackoverflow.com/a/30302235
            // Set the new set of protocols as the supported ones.
            ((SSLSocket) socket).setEnabledProtocols(protocols.toArray(new String[0]));
            for (String protocol : ((SSLSocket) socket).getEnabledProtocols()) {
                Log.e(getClass().getSimpleName(), protocol);
            }
        }
        return socket;
    }
}
