package kr.jclab.wsman.abstractwsman.frame;

public class SimpleResponseFrame implements ResponseFrame {
    private final int statusCode;
    private final byte[] body;

    public SimpleResponseFrame(int statusCode, byte[] body) {
        this.statusCode = statusCode;
        this.body = body;
    }

    @Override
    public byte[] getBody() {
        return body;
    }

    @Override
    public int getStatusCode() {
        return statusCode;
    }
}
