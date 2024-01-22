package kr.jclab.wsman.abstractwsman.client;

import kr.jclab.wsman.abstractwsman.client.internal.ClientResponseHandler;
import kr.jclab.wsman.abstractwsman.frame.ResponseFrame;
import org.apache.cxf.message.Message;

public class ClientRequestContext {
    private final Message outMessage;
    private final ClientResponseHandler clientResponseHandler;

    public ClientRequestContext(Message outMessage, ClientResponseHandler clientResponseHandler) {
        this.outMessage = outMessage;
        this.clientResponseHandler = clientResponseHandler;
    }

    public Message getOutMessage() {
        return outMessage;
    }

    public void emitResponseFrame(ResponseFrame responseFrame) {
        this.clientResponseHandler.onResponse(responseFrame);
    }

    public void emitResponseException(Throwable cause) {
        this.clientResponseHandler.onException(cause);
    }
}
