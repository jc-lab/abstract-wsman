package kr.jclab.wsman.abstractwsman.client.internal;

import kr.jclab.wsman.abstractwsman.frame.ResponseFrame;

public interface ClientResponseHandler {
    public void onResponse(ResponseFrame responseFrame);
    public void onException(Throwable cause);
}
