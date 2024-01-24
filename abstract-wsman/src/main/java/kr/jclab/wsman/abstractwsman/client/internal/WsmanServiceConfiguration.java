package kr.jclab.wsman.abstractwsman.client.internal;

import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.wsdl.service.factory.DefaultServiceConfiguration;

import javax.xml.namespace.QName;
import java.lang.reflect.Method;

public class WsmanServiceConfiguration extends DefaultServiceConfiguration {
    @Override
    public QName getInputMessageName(OperationInfo op, Method method) {
        return new QName(op.getName().getNamespaceURI(), op.getName().getLocalPart() + "_INPUT");
    }

    @Override
    public QName getOutputMessageName(OperationInfo op, Method method) {
        return new QName(op.getName().getNamespaceURI(), op.getName().getLocalPart() + "_OUTPUT");
    }
}
