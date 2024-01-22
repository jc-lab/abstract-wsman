package kr.jclab.wsman.abstractwsman.client.internal;

import kr.jclab.wsman.abstractwsman.client.ClientHandler;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.ClientImpl;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.jaxws.JaxWsClientFactoryBean;

public class AbstractBridgedClientFactoryBean extends JaxWsClientFactoryBean {
    private final ClientHandler clientHandler;

    public AbstractBridgedClientFactoryBean(ClientHandler clientHandler) {
        this.clientHandler = clientHandler;
    }

    @Override
    protected Client createClient(Endpoint ep) {
        return new ClientImpl(
                getBus(),
                ep,
                new AbstractBridgedConduit(clientHandler, getBus(), ep.getEndpointInfo(), endpointReference)
        );
    }
}
