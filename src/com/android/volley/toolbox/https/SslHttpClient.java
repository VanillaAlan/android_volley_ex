/**
 * Copyright 2013 Ognyan Bankov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.volley.toolbox.https;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.client.params.ClientPNames;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;

public class SslHttpClient extends DefaultHttpClient {
    private static final int HTTP_DEFAULT_PORT = 80;
    private static final String HTTP_SCHEME = "http";
    private static final int HTTP_DEFAULT_HTTPS_PORT = 443;
    private static final String HTTP_SSL_SCHEME = "https";
    private int mHttpsPort;


    public SslHttpClient() {
        mHttpsPort = HTTP_DEFAULT_HTTPS_PORT;
    }


    public SslHttpClient(int httpPort) {
        mHttpsPort = httpPort;
    }


    public SslHttpClient(final ClientConnectionManager conman,
            final HttpParams params,
            InputStream keyStore,
            String keyStorePassword) {
        super(conman, checkForInvalidParams(params));
    }


    public SslHttpClient(final HttpParams params) {
        super(null, checkForInvalidParams(params));
    }


    // we check intentionally for an old parameter
    private static HttpParams checkForInvalidParams(HttpParams params) {
        String className = (String) params
                .getParameter(ClientPNames.CONNECTION_MANAGER_FACTORY_CLASS_NAME);
        if (className != null) {
            throw new IllegalArgumentException("Don't try to pass ClientPNames.CONNECTION_MANAGER_FACTORY_CLASS_NAME parameter. We use our own connection manager factory anyway...");
        }

        return params;
    }


    @Override
    protected ClientConnectionManager createClientConnectionManager() {
        SchemeRegistry registry = new SchemeRegistry();

        PlainSocketFactory pfs = PlainSocketFactory.getSocketFactory();
        Scheme s = new Scheme(HTTP_SCHEME, pfs, HTTP_DEFAULT_PORT);
        registry.register(s);

        ThreadSafeClientConnManager ret = null;
        try {
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null);

            SSLSocketFactory sf = new MySSLSocketFactory(trustStore);
            sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            registry.register(new Scheme(HTTP_SSL_SCHEME, sf, mHttpsPort));
            
            ret = new ThreadSafeClientConnManager(new BasicHttpParams(), registry);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return ret;
    }

    private static class MySSLSocketFactory extends SSLSocketFactory {
        SSLContext sslContext = SSLContext.getInstance("TLS");

        public MySSLSocketFactory(KeyStore truststore) throws NoSuchAlgorithmException,
                KeyManagementException, KeyStoreException, UnrecoverableKeyException {
            super(truststore);

            TrustManager tm = new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] chain, String authType)
                        throws CertificateException {
                }

                public void checkServerTrusted(X509Certificate[] chain, String authType)
                        throws CertificateException {
                }

                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            };

            sslContext.init(null, new TrustManager[] { tm }, null);
        }

        @Override
        public Socket createSocket(Socket socket, String host, int port, boolean autoClose)
                throws IOException, UnknownHostException {
            return sslContext.getSocketFactory().createSocket(socket, host, port, autoClose);
        }

        @Override
        public Socket createSocket() throws IOException {
            return sslContext.getSocketFactory().createSocket();
        }
    }
    
    public void setHttpsPort(int httpsPort) {
        mHttpsPort = httpsPort;
    }
}
