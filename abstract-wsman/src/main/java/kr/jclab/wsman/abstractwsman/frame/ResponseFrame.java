package kr.jclab.wsman.abstractwsman.frame;

public interface ResponseFrame {
    byte[] getBody();
    int getStatusCode();
}
