package com.ywesee.amiko.hinclient;

public interface HINClientResponseCallback<T> {
    void onResponse(T res);

    void onError(Exception err);
}
