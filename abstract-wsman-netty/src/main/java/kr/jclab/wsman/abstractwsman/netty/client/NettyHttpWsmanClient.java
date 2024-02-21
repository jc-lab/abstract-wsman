package kr.jclab.wsman.abstractwsman.netty.client;

import kr.jclab.wsman.abstractwsman.client.AbstractWsmanClient;
import org.apache.cxf.Bus;
import org.apache.cxf.configuration.security.ProxyAuthorizationPolicy;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.ClientImpl;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.jaxws.JaxWsClientFactoryBean;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.transport.http.HttpConduitConfig;
import org.apache.cxf.transport.http.HttpConduitFeature;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;

import java.io.IOException;

public class NettyHttpWsmanClient extends AbstractWsmanClient {
    private final NettyChannelFactory nettyChannelFactory;

    public NettyHttpWsmanClient(Bus bus, NettyChannelFactory nettyChannelFactory, String address) {
        super(bus, address);
        this.nettyChannelFactory = nettyChannelFactory;
    }

    @Override
    public JaxWsClientFactoryBean createClientFactoryBean() {
        return new AbstractNettyClientFactoryBean();
    }

    @Override
    protected void configureJaxWsProxyFactoryBean(JaxWsProxyFactoryBean factoryBean) {
        HttpConduitFeature httpConduitFeature = new HttpConduitFeature();
        httpConduitFeature.setConduitConfig(getHttpConduitConfig());
        factoryBean.getFeatures().add(httpConduitFeature);
    }

    public HttpConduitConfig getHttpConduitConfig() {
        HttpConduitConfig httpConduitConfig = new HttpConduitConfig();
        httpConduitConfig.setClientPolicy(new HTTPClientPolicy());
        httpConduitConfig.setProxyAuthorizationPolicy(new ProxyAuthorizationPolicy());
        return httpConduitConfig;
    }

    private class AbstractNettyClientFactoryBean extends JaxWsClientFactoryBean {
        @Override
        protected Client createClient(Endpoint ep) {
            try {
                AbstractNettyHttpConduit conduit = new AbstractNettyHttpConduit(getBus(), ep.getEndpointInfo(), endpointReference, nettyChannelFactory);
                return new ClientImpl(
                        getBus(),
                        ep,
                        conduit
                );
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
