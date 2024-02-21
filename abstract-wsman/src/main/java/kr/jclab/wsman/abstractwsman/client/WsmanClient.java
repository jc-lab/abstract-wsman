package kr.jclab.wsman.abstractwsman.client;

import kr.jclab.wsman.abstractwsman.client.internal.AbstractBridgedClientFactoryBean;
import org.apache.cxf.Bus;
import org.apache.cxf.jaxws.JaxWsClientFactoryBean;

public class WsmanClient extends AbstractWsmanClient {
    private final ClientHandler clientHandler;

    public WsmanClient(Bus bus, ClientHandler clientHandler, String address) {
        super(bus, address);
        this.clientHandler = clientHandler;
    }

    @Override
    public JaxWsClientFactoryBean createClientFactoryBean() {
        AbstractBridgedClientFactoryBean clientFactoryBean = new AbstractBridgedClientFactoryBean(clientHandler);
        clientFactoryBean.setBus(bus);
        return clientFactoryBean;
    }
}
