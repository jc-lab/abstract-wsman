package kr.jclab.wsman.types.adapter;

import org.junit.jupiter.api.Test;

class OffsetDateTimeAdapterTest {
    OffsetDateTimeAdapter adapter = new OffsetDateTimeAdapter();

    @Test
    void unmarshal() throws Exception {
        adapter.unmarshal("2023-02-01T00:00:00Z");
    }
}