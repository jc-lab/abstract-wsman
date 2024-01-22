package kr.jclab.wsman.abstractwsman.client.internal;

import jakarta.annotation.Nullable;
import kr.jclab.wsman.abstractwsman.client.ClientHandler;
import kr.jclab.wsman.abstractwsman.client.ClientRequestContext;
import kr.jclab.wsman.abstractwsman.frame.ResponseFrame;
import org.apache.cxf.Bus;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.AbstractConduit;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.http.*;
import org.apache.cxf.ws.addressing.EndpointReferenceType;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.logging.Logger;

import static org.apache.cxf.transport.http.HTTPConduit.HTTP_RESPONSE_MESSAGE;
import static org.apache.cxf.transport.http.HTTPConduit.SET_HTTP_RESPONSE_MESSAGE;

public class AbstractBridgedConduit extends AbstractConduit {
    /**
     * The Logger for this class.
     */
    protected static final Logger LOG = LogUtils.getL7dLogger(AbstractBridgedConduit.class);

    private final ClientHandler bridgeHandler;


    private final Bus bus;

    /**
     * This field is used for two reasons. First it provides the base name for
     * the conduit for Spring configuration. The other is to hold default
     * address information, should it not be supplied in the Message Map, by the
     * Message.ENDPOINT_ADDRESS property.
     */
    protected final EndpointInfo endpointInfo;

    /**
     * This field holds the "default" URI for this particular conduit, which
     * is created on demand.
     */
    protected volatile Address defaultAddress;
    protected boolean fromEndpointReferenceType;


    public AbstractBridgedConduit(ClientHandler bridgeHandler, Bus bus, EndpointInfo endpointInfo, @Nullable EndpointReferenceType t) {
        super(getTargetReference(endpointInfo, t, bus));

        this.bridgeHandler = bridgeHandler;
        this.bus = bus;
        this.endpointInfo = endpointInfo;

        if (t != null) {
            fromEndpointReferenceType = true;
        }
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    @Override
    public void prepare(Message message) throws IOException {
        // This call can possibly change the conduit endpoint address and
        // protocol from the default set in EndpointInfo that is associated
        // with the Conduit.
        Address currentAddress;
        try {
            currentAddress = setupAddress(message);
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }

        // set the OutputStream on the ProxyOutputStream
        ProxyOutputStream pos = message.getContent(ProxyOutputStream.class);
        if (pos != null && message.getContent(OutputStream.class) != null) {
            pos.setWrappedOutputStream(createOutputStream(message));
        } else {
            message.setContent(OutputStream.class, createOutputStream(message));
        }
        // We are now "ready" to "send" the message.
    }

    @Override
    public void close(Message msg) throws IOException {
        InputStream in = msg.getContent(InputStream.class);
        try {
            if (in != null) {
                int count = 0;
                byte[] buffer = new byte[1024];
                while (in.read(buffer) != -1
                        && count < 25) {
                    //don't do anything, we just need to pull off the unread data (like
                    //closing tags that we didn't need to read

                    //however, limit it so we don't read off gigabytes of data we won't use.
                    ++count;
                }
            }
        } finally {
            super.close(msg);
        }
    }

    private OutputStream createOutputStream(Message inMessage) {
        return new WrappedOutputStream(inMessage);
    }

    class WrappedOutputStream extends ByteArrayOutputStream implements ClientResponseHandler {
        private final ClientRequestContext requestContext;
        private final Message outMessage;

        WrappedOutputStream(Message inMessage) {
            this.requestContext = new ClientRequestContext(inMessage, this);
            this.outMessage = requestContext.getOutMessage();
        }

        @Override
        public void close() throws IOException {
            bridgeHandler.request(requestContext, this.toByteArray());
        }

        @Override
        public void onResponse(ResponseFrame responseFrame) {
            this.onResponseOrException(responseFrame, null);
        }

        @Override
        public void onException(Throwable cause) {
            this.onResponseOrException(null, cause);
        }

        private void onResponseOrException(ResponseFrame responseFrame, Throwable cause) {
            Message inMessage = handleResponseInternal(responseFrame, cause);
            Exchange exchange = outMessage.getExchange();
            exchange.setInMessage(inMessage);
            incomingObserver.onMessage(inMessage);
        }

        protected Message handleResponseInternal(ResponseFrame res, Throwable ex) {
            Message inMessage = new MessageImpl();

            Exchange exchange = outMessage.getExchange();

            if (ex != null) {
                inMessage.setContent(Exception.class, ex);
                return inMessage;
            }

            int responseCode = res.getStatusCode();

            inMessage.setExchange(exchange);
            updateResponseHeaders(inMessage);
            inMessage.put(Message.RESPONSE_CODE, responseCode);
            if (MessageUtils.getContextualBoolean(outMessage, SET_HTTP_RESPONSE_MESSAGE, false)) {
                inMessage.put(HTTP_RESPONSE_MESSAGE, res.getBody());
            }
            propagateConduit(exchange, inMessage);

            outMessage.removeContent(OutputStream.class);

            inMessage.setContent(InputStream.class, new ByteArrayInputStream(res.getBody()));

            return inMessage;
        }

        protected void updateResponseHeaders(Message inMessage) {
            Headers h = new Headers(inMessage);
            inMessage.put(Message.CONTENT_TYPE, "application/soap+xml;charset=UTF-8");
        }

        /**
         * This predicate returns true if the exchange indicates
         * a oneway MEP.
         *
         * @param exchange The exchange in question
         */
        private boolean isOneway(Exchange exchange) {
            return exchange != null && exchange.isOneWay();
        }

        private boolean doProcessResponse(Message message, int responseCode) {
            // 1. Not oneWay
            if (!isOneway(message.getExchange())) {
                return true;
            }
            // 2. Robust OneWays could have a fault
            return responseCode == 500 && MessageUtils.getContextualBoolean(message, Message.ROBUST_ONEWAY, false);
        }
    }

    /**
     * This function sets up a URL based on ENDPOINT_ADDRESS, PATH_INFO,
     * and QUERY_STRING properties in the Message. The QUERY_STRING gets
     * added with a "?" after the PATH_INFO. If the ENDPOINT_ADDRESS is not
     * set on the Message, the endpoint address is taken from the
     * "defaultEndpointURL".
     * <p>
     * The PATH_INFO is only added to the endpoint address string should
     * the PATH_INFO not equal the end of the endpoint address string.
     *
     * @param message The message holds the addressing information.
     *
     * @return The full URL specifying the HTTP request to the endpoint.
     *
     * @throws MalformedURLException
     * @throws URISyntaxException
     */
    private Address setupAddress(Message message) throws URISyntaxException {
        String result = (String)message.get(Message.ENDPOINT_ADDRESS);
        String pathInfo = (String)message.get(Message.PATH_INFO);
        String queryString = (String)message.get(Message.QUERY_STRING);
        setAndGetDefaultAddress();
        if (result == null) {
            if (pathInfo == null && queryString == null) {
                if (defaultAddress != null) {
                    message.put(Message.ENDPOINT_ADDRESS, defaultAddress.getString());
                }
                return defaultAddress;
            }
            if (defaultAddress != null) {
                result = defaultAddress.getString();
                message.put(Message.ENDPOINT_ADDRESS, result);
            }
        }

        // REVISIT: is this really correct?
        if (null != pathInfo && !result.endsWith(pathInfo)) {
            result = result + pathInfo;
        }
        if (queryString != null) {
            result = result + "?" + queryString;
        }
        if (defaultAddress == null) {
            return setAndGetDefaultAddress(result);
        }
        return result.equals(defaultAddress.getString()) ? defaultAddress : new Address(result);
    }

    private Address setAndGetDefaultAddress() throws URISyntaxException {
        if (defaultAddress == null) {
            synchronized (this) {
                if (defaultAddress == null) {
                    if (fromEndpointReferenceType && getTarget().getAddress().getValue() != null) {
                        defaultAddress = new Address(this.getTarget().getAddress().getValue());
                    } else if (endpointInfo.getAddress() != null) {
                        defaultAddress = new Address(endpointInfo.getAddress());
                    }
                }
            }
        }
        return defaultAddress;
    }

    private Address setAndGetDefaultAddress(String curAddr) throws URISyntaxException {
        if (defaultAddress == null) {
            synchronized (this) {
                if (defaultAddress == null) {
                    if (curAddr != null) {
                        defaultAddress = new Address(curAddr);
                    } else {
                        throw new URISyntaxException("<null>",
                                "Invalid address. Endpoint address cannot be null.", 0);
                    }
                }
            }
        }
        return defaultAddress;
    }

    protected void propagateConduit(Exchange exchange, Message in) {
        if (exchange != null) {
            Message out = exchange.getOutMessage();
            if (out != null) {
                in.put(Conduit.class, out.get(Conduit.class));
            }
        }
    }
}
