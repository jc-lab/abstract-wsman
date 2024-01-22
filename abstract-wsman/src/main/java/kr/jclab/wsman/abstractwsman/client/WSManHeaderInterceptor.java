package kr.jclab.wsman.abstractwsman.client;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.binding.soap.interceptor.SoapPreProtocolOutInterceptor;
import org.apache.cxf.headers.Header;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.jaxb.JAXBDataBinding;
import org.apache.cxf.phase.Phase;
import org.dmtf.schemas.wbem.wsman._1.wsman.AttributableURI;
import org.dmtf.schemas.wbem.wsman._1.wsman.ObjectFactory;
import org.dmtf.schemas.wbem.wsman._1.wsman.SelectorSetType;
import org.dmtf.schemas.wbem.wsman._1.wsman.SelectorType;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class WSManHeaderInterceptor extends AbstractSoapInterceptor {
    private final String m_resourceUri;
    private final Map<String, String> m_selectors;
    private final ObjectFactory factory = new ObjectFactory();

    public WSManHeaderInterceptor(String resourceUri) {
        this(resourceUri, Collections.emptyMap());
    }

    public WSManHeaderInterceptor(String resourceUri, Map<String, String> selectors) {
        super(Phase.POST_LOGICAL);
        addAfter(SoapPreProtocolOutInterceptor.class.getName());
        m_resourceUri = Objects.requireNonNull(resourceUri, "resourceUri cannot be null");
        m_selectors = Objects.requireNonNull(selectors, "selector cannot be null");
    }

    @Override
    public void handleMessage(SoapMessage message) throws Fault {
        // Retrieve the current list of headers
        List<Header> headers = message.getHeaders();
        // Always add the resourceUri header
        headers.add(getResourceUriHeader());
        // Add the selectorSet header iff have one or more selectors
        if (!m_selectors.isEmpty()) {
            headers.add(getSelectorSetHeader());
        }
        message.put(Header.HEADER_LIST, headers);
    }

    private Header getResourceUriHeader() {
        AttributableURI uri = new AttributableURI();
        uri.setValue(m_resourceUri);
        JAXBElement<AttributableURI> resourceURI = factory.createResourceURI(uri);
        try {
            return new Header(resourceURI.getName(), resourceURI, new JAXBDataBinding(AttributableURI.class));
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    private Header getSelectorSetHeader() {
        SelectorSetType selectorSetType = factory.createSelectorSetType();
        for (Map.Entry<String, String> selectorEntry : m_selectors.entrySet()) {
            SelectorType selector = factory.createSelectorType();
            selector.setName(selectorEntry.getKey());
            selector.getContent().add(selectorEntry.getValue());
            selectorSetType.getSelector().add(selector);
        }
        JAXBElement<SelectorSetType> el = factory.createSelectorSet(selectorSetType);
        try {
            return new Header(el.getName(), el, new JAXBDataBinding(el.getValue().getClass()));
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }
}
