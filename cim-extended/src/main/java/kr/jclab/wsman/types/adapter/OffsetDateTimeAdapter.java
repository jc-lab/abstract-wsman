package kr.jclab.wsman.types.adapter;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

import java.time.OffsetDateTime;

public class OffsetDateTimeAdapter extends XmlAdapter<String, OffsetDateTime> {
    @Override
    public OffsetDateTime unmarshal(String v) throws Exception {
        if (v == null || v.isEmpty()) {
            return null;
        }
        return OffsetDateTime.parse(v);
    }

    @Override
    public String marshal(OffsetDateTime v) throws Exception {
        return v.toString();
    }
}