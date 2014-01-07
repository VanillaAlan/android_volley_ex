/**
 * Copyright 2013 Ognyan Bankov Portions copyright 2011 The Android Open Source
 * Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.android.volley.toolbox.https;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Request.Method;
import com.android.volley.toolbox.HttpStack;

public class ExtHttpClientStack implements HttpStack {
    protected final HttpClient mClient;

    private final static String HEADER_CONTENT_TYPE = "Content-Type";
    private static BasicUrlRewriter mRewriter;
    
    public ExtHttpClientStack(HttpClient client) {
        mClient = client;
        mRewriter = new BasicUrlRewriter();
    }

    private static void addHeaders(HttpUriRequest httpRequest, Map<String, String> headers) {
        for (String key : headers.keySet()) {
            httpRequest.setHeader(key, headers.get(key));
        }
    }

    @SuppressWarnings("unused")
    private static List<NameValuePair> getPostParameterPairs(Map<String, String> postParams) {
        List<NameValuePair> result = new ArrayList<NameValuePair>(postParams.size());
        for (String key : postParams.keySet()) {
            result.add(new BasicNameValuePair(key, postParams.get(key)));
        }
        return result;
    }

    @Override
    public HttpResponse performRequest(Request<?> request, Map<String, String> additionalHeaders)
            throws IOException, AuthFailureError {

        HttpUriRequest httpRequest = createHttpRequest(request, additionalHeaders);
        addHeaders(httpRequest, additionalHeaders);
        addHeaders(httpRequest, request.getHeaders());
        onPrepareRequest(httpRequest);
        HttpParams httpParams = httpRequest.getParams();
        int timeoutMs = request.getTimeoutMs();
        // TODO: Reevaluate this connection timeout based on more wide-scale
        // data collection and possibly different for wifi vs. 3G.
        HttpConnectionParams.setConnectionTimeout(httpParams, 120000);
        HttpConnectionParams.setSoTimeout(httpParams, timeoutMs);

        HttpResponse resp = mClient.execute(httpRequest);

        return convertResponseNewToOld(resp);
    }

    private HttpResponse convertResponseNewToOld(HttpResponse resp)
            throws IllegalStateException, IOException {

        ProtocolVersion protocolVersion = new ProtocolVersion(resp.getProtocolVersion()
                .getProtocol(), resp.getProtocolVersion().getMajor(), resp.getProtocolVersion()
                .getMinor());

        StatusLine responseStatus = new BasicStatusLine(protocolVersion, resp.getStatusLine()
                .getStatusCode(), resp.getStatusLine().getReasonPhrase());

        BasicHttpResponse response = new BasicHttpResponse(responseStatus);
        org.apache.http.HttpEntity ent = convertEntityNewToOld(resp.getEntity());
        response.setEntity(ent);

        for (Header h : resp.getAllHeaders()) {
            Header header = convertheaderNewToOld((Header) h);
            response.addHeader(header);
        }

        return response;
    }

    private HttpEntity convertEntityNewToOld(HttpEntity ent)
            throws IllegalStateException, IOException {

        BasicHttpEntity ret = new BasicHttpEntity();
        if (ent != null) {
            ret.setContent(ent.getContent());
            ret.setContentLength(ent.getContentLength());
            Header h;
            h = (Header) ent.getContentEncoding();
            if (h != null) {
                ret.setContentEncoding(convertheaderNewToOld(h));
            }
            h = ent.getContentType();
            if (h != null) {
                ret.setContentType(convertheaderNewToOld(h));
            }
        }

        return ret;
    }

    private Header convertheaderNewToOld(Header header) {
        Header ret = new BasicHeader(header.getName(), header.getValue());
        return ret;
    }

    /**
     * Creates the appropriate subclass of HttpUriRequest for passed in request.
     * @throws IOException 
     */
    /* protected */static HttpUriRequest createHttpRequest(Request<?> request,
            Map<String, String> additionalHeaders) throws AuthFailureError, IOException {
        switch (request.getMethod()) {
        case Method.GET:
            return new HttpGet(mRewriter.rewriteUrl(request));
        case Method.DELETE:
            return new HttpDelete(request.getUrl());
        case Method.POST: {
            HttpPost postRequest = new HttpPost(request.getUrl());
            postRequest.addHeader(HEADER_CONTENT_TYPE, request.getBodyContentType());
            setEntityIfNonEmptyBody(postRequest, request);
            return postRequest;
        }
        case Method.PUT: {
            HttpPut putRequest = new HttpPut(request.getUrl());
            putRequest.addHeader(HEADER_CONTENT_TYPE, request.getBodyContentType());
            setEntityIfNonEmptyBody(putRequest, request);
            return putRequest;
        }
        default:
            throw new IllegalStateException("Unknown request method.");
        }
    }

    private static void setEntityIfNonEmptyBody(HttpEntityEnclosingRequestBase httpRequest,
            Request<?> request) throws AuthFailureError {
        byte[] body = request.getBody();
        if (body != null) {
            HttpEntity entity = new ByteArrayEntity(body);
            httpRequest.setEntity(entity);
        }
    }

    /**
     * Called before the request is executed using the underlying HttpClient.
     * 
     * <p>
     * Overwrite in subclasses to augment the request.
     * </p>
     */
    protected void onPrepareRequest(HttpUriRequest request) throws IOException {
        // Nothing.
    }
    
    /**
     * Class to automatically rewrite URLs for GET methods based on whether the
     * request has params or not
     */
    private static class BasicUrlRewriter implements HttpStack.UrlRewriter {

        @Override
        public String rewriteUrl(Request<?> request) throws IOException {

            switch (request.getMethod()) {

            case Request.Method.GET: {

                String url = request.getUrl();

                try {
                    String encodedParams = request.getEncodedUrlBody();

                    if (encodedParams != null && encodedParams.length() > 0) {
                        if (!url.endsWith("?")) {
                            url += "?";
                        }
                        url += encodedParams;
                    }

                } catch (AuthFailureError e) {
                    return null;
                }
                return url;
            }

            case Request.Method.POST:
            case Request.Method.PUT:
            case Request.Method.DELETE: {
                return request.getUrl();
            }

            default:
                throw new IllegalStateException("Unknown request method.");

            }
        }
    }
}
