package com.peatroxd.mtprototest.service.source;

import com.peatroxd.mtprototest.dto.RawProxy;

import java.util.List;

public interface ProxySource {

    List<RawProxy> fetch();

    String sourceName();
}
