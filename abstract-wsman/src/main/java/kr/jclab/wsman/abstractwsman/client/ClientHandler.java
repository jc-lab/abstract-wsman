package kr.jclab.wsman.abstractwsman.client;

public interface ClientHandler {
    void request(ClientRequestContext requestContext, byte[] body);
}
