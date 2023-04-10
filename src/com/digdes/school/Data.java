package com.digdes.school;

import java.util.*;

public class Data { // инкапсулировать
    static final List<Map<String, Object>> dataTable = new ArrayList<>();
    static final Set<String> columns = Set.of("id", "lastName", "age", "cost", "active");
}
