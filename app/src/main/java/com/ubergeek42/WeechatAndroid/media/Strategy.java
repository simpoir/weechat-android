package com.ubergeek42.WeechatAndroid.media;

import androidx.annotation.Nullable;

import java.util.List;

// validates and converts urls to more usable image urls
public interface Strategy {

    // get all host this strategy is handling in no particular order. "*" can replace parts one or
    // more or all leftmost parts of the host. e.g. "one.two.com", "*.two.com", "*"
    List<String> getHosts();

    // return true if the response body needs additional processing
    boolean needsToProcessBody();

    // if this strategy needs to process the body, this returns the number of bytes we want to
    // request from the server. this is just a guideline, the actual number of transferred bytes
    // might be smaller or greater than this
    int wantedBodySize();

    // given an url, get the same or a different url to fetch. the new url might be an http+s url,
    // a direct image url constructed from a non-direct url, or a web page for further processing.
    // returns null if no url could be constructed
    @Nullable String getRequestUrl(String url);

    // get the final image url given the body, or null if no image url could be constructed
    @Nullable String getRequestUrlFromBody(CharSequence body);
}
