package com.peatroxd.mtprototest.parser.source;

import com.peatroxd.mtprototest.parser.model.RawProxy;

import java.util.List;

public interface ProxySource {

    List<RawProxy> fetch();

    String sourceName();
}
