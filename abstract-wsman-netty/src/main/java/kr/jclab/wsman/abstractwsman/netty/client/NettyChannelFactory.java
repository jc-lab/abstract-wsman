package kr.jclab.wsman.abstractwsman.netty.client;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import org.apache.cxf.Bus;

import java.net.URI;

public interface NettyChannelFactory {
    ChannelFuture connect(Bus bus, URI url, ChannelHandler channelHandler);

    default boolean getUseAsyncPolicy() {
        return false;
    }
}
