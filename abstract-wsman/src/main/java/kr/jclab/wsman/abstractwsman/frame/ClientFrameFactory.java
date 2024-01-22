package kr.jclab.wsman.abstractwsman.frame;

public interface ClientFrameFactory {
    Object encodeFrame(RequestFrame frame);
    ResponseFrame decodeFrame(Object input);
}
