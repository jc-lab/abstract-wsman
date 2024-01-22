package kr.jclab.wsman.abstractwsman.frame;

public class SimpleRequestFrame implements RequestFrame {
    private final byte[] body;

    public SimpleRequestFrame(byte[] body) {
        this.body = body;
    }

    @Override
    public byte[] getBody() {
        return body;
    }
}
