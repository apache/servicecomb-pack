package com.github.wenj91.dubbo.provider.service.impl;

import com.github.wenj91.dubbo.api.TestService;

public class TestServiceImpl implements TestService {
    public String sayHello(String name) {
        return "hello, " + name + "!";
    }
}
