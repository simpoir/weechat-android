package com.ubergeek42.WeechatAndroid.media;

// this  class is a modified version of the same class from the sources of Glide
// https://github.com/bumptech/glide/blob/master/integration/okhttp3/src/main/java/com/bumptech/glide/integration/okhttp3/OkHttpStreamFetcher.java

// Copyright 2014 Google, Inc. All rights reserved.

// Redistribution and use in source and binary forms, with or without modification, are
// permitted provided that the following conditions are met:

// 1. Redistributions of source code must retain the above copyright notice, this list of
// conditions and the following disclaimer.

// 2. Redistributions in binary form must reproduce the above copyright notice, this list
// of conditions and the following disclaimer in the documentation and/or other materials
// provided with the distribution.

// THIS SOFTWARE IS PROVIDED BY GOOGLE, INC. ``AS IS'' AND ANY EXPRESS OR IMPLIED
// WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
// FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL GOOGLE, INC. OR
// CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
// ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
// NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
// ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

// note that when using GlideUrl, calling onLoadFailed results in an additional call, see
// https://github.com/bumptech/glide/issues/2943 -- something to potentially worry about?

import androidx.annotation.NonNull;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.HttpException;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.util.Preconditions;
import com.ubergeek42.cats.Cat;
import com.ubergeek42.cats.Kitty;
import com.ubergeek42.cats.Root;

import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import static com.ubergeek42.WeechatAndroid.utils.Utils.readInputStream;

public class OkHttpStreamFetcher implements DataFetcher<InputStream> {
    final private static @Root Kitty kitty = Kitty.make();

    final private Call.Factory client;
    final private StrategyUrl url;

    private DataCallback<? super InputStream> callback;

    OkHttpStreamFetcher(Call.Factory client, StrategyUrl url) {
        this.client = client;
        this.url = url;
    }

    @Override @Cat public void loadData(@NonNull Priority priority, @NonNull DataCallback<? super InputStream> callback) {
        this.callback = callback;
        String requestUrl = url.getStrategy().getRequestUrl(url.getOriginalUrl());
        if (url.getStrategy().needsToProcessBody()) intermediate.fire(requestUrl);
        else main.fire(requestUrl);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////

    private abstract class MyCallback implements Callback {
        // call may be accessed on the main thread while the object is in use on other threads. all
        // other accesses to variables may occur on different threads, but only one at a time.
        private volatile Call call;

        private InputStream stream;
        private ResponseBody responseBody;

        // use glide headers? see com.bumptech.glide.load.model.Headers.DEFAULT
        // use proper request headers for html/images
        @Cat void fire(String url) {
            Request request = new Request.Builder().url(url).build();
            call = client.newCall(request);
            call.enqueue(this);
        }

        @Override @Cat public void onFailure(@NonNull Call call, @NonNull IOException e) {
            callback.onLoadFailed(e);
        }

        @Override @Cat public void onResponse(@NonNull Call call, Response response)  {
            responseBody = response.body();
            if (!response.isSuccessful()) {
                callback.onLoadFailed(new HttpException(response.message(), response.code()));
                return;
            }

            // contentLength will be -1 if not present in the request. it's often missing if the
            // request is served with gzip, as the resulting file size might not be known in advance
            long contentLength = Preconditions.checkNotNull(responseBody).contentLength();
            if (contentLength > Engine.MAXIMUM_BODY_SIZE) {
                callback.onLoadFailed(new Exceptions.ContentLengthExceedsLimitException(contentLength, Engine.MAXIMUM_BODY_SIZE));
                return;
            }

            stream = new LimitedLengthInputStream(responseBody.byteStream(), contentLength, Engine.MAXIMUM_BODY_SIZE);
            onStreamReady(responseBody, stream);
        }

        abstract void onStreamReady(ResponseBody response, InputStream stream);

        void cleanup() {
            try {
                if (stream != null) stream.close();
            } catch (IOException ignored) {}
            if (responseBody != null) responseBody.close();
        }

        void cancel() {
            Call local = call;
            if (local != null) {
                local.cancel();
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    private MyCallback intermediate = new MyCallback() {
        final private @Root Kitty intermediate_kitty = kitty.kid("Intermediate");

        @Override @Cat void onStreamReady(ResponseBody response, InputStream stream) {
            CharSequence body;

            try {
                body = readInputStream(stream, url.getStrategy().wantedBodySize());
            } catch (IOException e) {
                callback.onLoadFailed(e);
                return;
            }

            String newRequestUrl = url.getStrategy().getRequestUrlFromBody(body);
            if (newRequestUrl == null) {
                callback.onLoadFailed(new Exceptions.HtmlBodyLacksRequiredDataException(body));
                return;
            }

            main.fire(newRequestUrl);
        }
    };

    ////////////////////////////////////////////////////////////////////////////////////////////////

    private MyCallback main = new MyCallback() {
        final private @Root Kitty main_kitty = kitty.kid("Main");

        @Override @Cat void onStreamReady(ResponseBody responseBody, InputStream stream) {
            callback.onDataReady(stream);
        }
    };

    ////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////

    @Override @Cat public void cleanup() {
        intermediate.cleanup();
        main.cleanup();
        callback = null;
    }

    @Override @Cat public void cancel() {
        intermediate.cancel();
        main.cancel();
    }

    @NonNull @Override public Class<InputStream> getDataClass() {
        return InputStream.class;
    }

    @NonNull @Override public DataSource getDataSource() {
        return DataSource.REMOTE;
    }
}