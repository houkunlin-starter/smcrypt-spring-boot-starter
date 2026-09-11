package com.houkunlin.smcrypt;

import org.springframework.core.io.FileSystemResourceLoader;

import java.util.HashMap;
import java.util.Map;

/**
 * 测试用上下文构建工具。
 */
public final class TestContexts {

    private TestContexts() {
    }

    public static SmCryptContext context(Map<String, String> properties) {
        Map<String, String> map = new HashMap<>(properties);
        return new SmCryptContext(map::get, new FileSystemResourceLoader());
    }
}
