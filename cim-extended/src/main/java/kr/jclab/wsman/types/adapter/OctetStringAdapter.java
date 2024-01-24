package kr.jclab.wsman.types.adapter;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

import java.util.Base64;

public class OctetStringAdapter extends XmlAdapter<String, byte[]> {
    @Override
    public byte[] unmarshal(String v) throws Exception {
        return Base64.getDecoder().decode(v);
    }

    @Override
    public String marshal(byte[] v) throws Exception {
        return Base64.getEncoder().encodeToString(v);
    }
}
