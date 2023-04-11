package com.digdes.school;

import java.util.*;

public class JavaSchoolStarter {
    public JavaSchoolStarter() {
    }

    public List<Map<String, Object>> execute(String request) throws Exception {
        return new Executor().apply(request);
    }
}