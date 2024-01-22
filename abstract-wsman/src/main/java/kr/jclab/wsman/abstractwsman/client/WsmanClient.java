package kr.jclab.wsman.abstractwsman.client;

import kr.jclab.wsman.abstractwsman.WSManConstants;
import kr.jclab.wsman.abstractwsman.client.internal.AbstractBridgedClientFactoryBean;
import org.apache.cxf.binding.soap.SoapBindingConstants;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientFactoryBean;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.interceptor.transform.TransformInInterceptor;
import org.apache.cxf.interceptor.transform.TransformOutInterceptor;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.ws.addressing.JAXWSAConstants;
import org.apache.cxf.ws.addressing.WSAddressingFeature;
import org.xmlsoap.schemas.ws._2004._09.enumeration.DataSource;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class WsmanClient {
    // private static final org.apache.cxf.ws.addressing.v200408.ObjectFactory WSA_OBJECT_FACTORY = new org.apache.cxf.ws.addressing.v200408.ObjectFactory();

    private final ClientFactoryBean clientFactoryBean;
    private final String address;

    public WsmanClient(ClientFactoryBean clientFactoryBean, String address) {
        this.clientFactoryBean = clientFactoryBean;
        this.address = address;
    }

    public WsmanClient(ClientHandler clientHandler, String address) {
        this.clientFactoryBean = new AbstractBridgedClientFactoryBean(clientHandler);
        this.address = address;
    }

    public JaxWsProxyFactoryBean createFactoryFor(Class<?> serviceClass, String address) {
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean(clientFactoryBean);
        factory.setServiceClass(serviceClass);
        factory.setAddress(address);

        // Create a new ExtensionManagerBus to be used by this factory
        // This is necessary since the bus holds a reference to the org.apache.cxf.ws.policy.PolicyRegistryImpl, which contains
        // policies created by the Wsdl11AttachmentPolicyProvider, which are never removed. If we don't create our own
        // bus, the same instance will be shared across all factories, and the policies will continue to accumulate.
        factory.setBus(clientFactoryBean.getBus());

        WSAddressingFeature feature = new WSAddressingFeature();
        feature.setResponses(WSAddressingFeature.AddressingResponses.ANONYMOUS);
        factory.getFeatures().add(feature);

        // Force the client to use SOAP v1.2, as per:
        // R13.1-1: A service shall at least receive and send SOAP 1.2 SOAP Envelopes.
        factory.setBindingId(SoapBindingConstants.SOAP12_BINDING_ID);

        return factory;
    }

    public <T> T createProxyFor(
            Class<T> serviceClass,
            Map<String, String> outTransformMap,
            Map<String, String> inTransformMap
    ) {
        T proxyService = createFactoryFor(serviceClass, address).create(serviceClass);

        // Retrieve the underlying client, so we can fine tune it
        Client cxfClient = ClientProxy.getClient(proxyService);
        Map<String, Object> requestContext = cxfClient.getRequestContext();

        // Add static name-space mappings, this helps when manually inspecting the XML
        Map<String, String> nsMap = new HashMap<>();
        nsMap.put("wsa", WSManConstants.XML_NS_WS_2004_08_ADDRESSING);
        nsMap.put("wsen", WSManConstants.XML_NS_WS_2004_09_ENUMERATION);
        nsMap.put("wsman", WSManConstants.XML_NS_DMTF_WSMAN_V1);
        nsMap.put("wsmid", WSManConstants.XML_NS_DMTF_WSMAN_IDENTITY_V1);
        cxfClient.getRequestContext().put("soap.env.ns.map", nsMap);

        // Set the Reply-To header to the anonymous address
//        requestContext.put(JAXWSAConstants.CLIENT_ADDRESSING_PROPERTIES, createAddressingPropertiesMap());

        // if (m_endpoint.getServerVersion() == WSManVersion.WSMAN_1_0) {
            // WS-Man 1.0 does not support the W3C WS-Addressing, so we need to change the namespace
            // "http://www.w3.org/2005/08/addressing" becomes "http://schemas.xmlsoap.org/ws/2004/08/addressing"
            outTransformMap.put(
                    "{" + JAXWSAConstants.NS_WSA + "}*",
                    "{" + WSManConstants.XML_NS_WS_2004_08_ADDRESSING + "}*"
            );
        // }

        // Optionally apply any in and/or out transformers
        if (!outTransformMap.isEmpty()) {
            final TransformOutInterceptor transformOutInterceptor = new TransformOutInterceptor();
            transformOutInterceptor.setOutTransformElements(outTransformMap);
            cxfClient.getOutInterceptors().add(transformOutInterceptor);
        }

        if (!inTransformMap.isEmpty()) {
            final TransformInInterceptor transformInInterceptor = new TransformInInterceptor();
            transformInInterceptor.setInTransformElements(inTransformMap);
            cxfClient.getInInterceptors().add(transformInInterceptor);
        }

//        // Remove the action attribute from the Content-Type header.
//        // By default, CXF will add the action to the Content-Type header, generating something like:
//        // Content-Type: application/soap+xml; action="http://schemas.xmlsoap.org/ws/2004/09/enumeration/Enumerate"
//        // Windows Server 2008 barfs on the action=".*" attribute and none of the other servers
//        // seem to care whether it's there or not, so we remove it.
//        Map<String, List<String>> headers = Maps.newHashMap();
//        headers.put(CONTENT_TYPE_HEADER, Collections.singletonList(MEDIA_TYPE_SOAP_UTF8));
//        requestContext.put(Message.PROTOCOL_HEADERS, headers);
//
//        // Log incoming and outgoing requests
//        LoggingInInterceptor loggingInInterceptor = new LoggingInInterceptor();
//        loggingInInterceptor.setPrettyLogging(true);
//        cxfClient.getInInterceptors().add(loggingInInterceptor);
//
//        LoggingOutInterceptor loggingOutInterceptor = new LoggingOutInterceptor();
//        loggingOutInterceptor.setPrettyLogging(true);
//        cxfClient.getOutInterceptors().add(loggingOutInterceptor);

        return proxyService;
    }

//    private AddressingProperties createAddressingPropertiesMap() {
//        AddressingProperties maps = new AddressingProperties();
//        AttributedURI address = WSA_OBJECT_FACTORY.createAttributedURI();
//        EndpointReferenceType ref = WSA_OBJECT_FACTORY.createEndpointReferenceType();
//        address.setValue(VersionTransformer.Names200408.WSA_ANONYMOUS_ADDRESS);
//        ref.setAddress(address);
//        maps.setReplyTo(ref);
//        maps.setFaultTo(ref);
//        return maps;
//    }

    public <T> T createResource(String resourceUri, Class<T> clazz) {
        // Relocate the Filter element to the WS-Man namespace.
        // Our WSDLs generate it one package but the servers expect it to be in the other

        // Relocate the Filter element to the WS-Man namespace.
        // Our WSDLs generate it one package but the servers expect it to be in the other
        HashMap<String, String> outTransformMap = new HashMap<>();
        outTransformMap.put(
                "{" + WSManConstants.XML_NS_WS_2004_09_ENUMERATION + "}Filter",
                "{" + WSManConstants.XML_NS_DMTF_WSMAN_V1 + "}Filter"
        );

        T resource = createProxyFor(
                clazz,
                outTransformMap,
                Collections.emptyMap()
        );

        Client cxfClient = ClientProxy.getClient(resource);

        // Add the WS-Man ResourceURI to the SOAP header
        WSManHeaderInterceptor interceptor = new WSManHeaderInterceptor(resourceUri);
        cxfClient.getOutInterceptors().add(interceptor);

        return resource;
    }
}
