package com.digdes.school;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.digdes.school.PropertyReader.*;

import static com.digdes.school.Commands.*;
import static com.digdes.school.Operations.*;

public class Executor implements Function<String, List<Map<String, Object>>> {
    List<Map<String, Object>> data = Data.dataTable;

    private final PropertyReader reader = new PropertyReader();

    @Override
    public List<Map<String, Object>> apply(String request) {
        String[] arrayFromRequest = filter(request);
        String firstRequestWord = arrayFromRequest[0];
        if (isSingleCommand(arrayFromRequest, firstRequestWord)) {
            return takeOrDeleteAll(firstRequestWord);
        }
        final List<ConditionContainer> properties;
        List<List<ConditionContainer>> conditions = null;
        switch (firstRequestWord) {
            case INSERT, UPDATE, DELETE, SELECT -> {
                validate(arrayFromRequest, firstRequestWord, request);
                properties = reader.readRequestProperties(arrayFromRequest);
            }
            default -> throw new IllegalArgumentException();
        }
        switch (firstRequestWord) {
            case INSERT -> {
                return insertResolver(properties);
            }
            case UPDATE, SELECT, DELETE -> conditions = reader.readConditions(firstRequestWord);
        }
        if (conditions == null && firstRequestWord.equals(UPDATE)) { // проверку на null заменить на empty collection
            return resolverForUpdateWithoutCond(properties);
        }
        return commonResolver(conditions, firstRequestWord, properties);

//        throw new UnsupportedOperationException();
    }

    private List<Map<String, Object>> resolverForUpdateWithoutCond(List<ConditionContainer> properties) {
        if (isIncorrectOperator(properties)) throw new IllegalArgumentException();
        try {
            for (Map<String, Object> row : Data.dataTable) {
                for (ConditionContainer cond : properties) {
                    putOrUpdateKeyValueIfCorrect(cond.getProperty(), cond.getValue(), row);
                }
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException();
        }
        return Data.dataTable;
    }

    private List<Map<String, Object>> commonResolver(List<List<ConditionContainer>> conditions, String firstRequestWord, List<ConditionContainer> properties) {
        List<Integer> correctRows = new ArrayList<>();
        boolean isValidGroupConds = true;
        // этот метод подсчитывает ряды в таблице, которые подходят под условие
        for (Map<String, Object> row : Data.dataTable) {
            for (var groupCond : conditions) {
                isValidGroupConds = true;
                for (var cond : groupCond) {
                    if (!isRightConditionForRow(cond, row)) {
                        isValidGroupConds = false;
                        break;
                    }
                }
                if (isValidGroupConds) {
                    correctRows.add(Data.dataTable.indexOf(row));
                    break;
                }
            }
        }
        final List<Map<String, Object>> res = new ArrayList<>();
        switch (firstRequestWord) {
            case DELETE -> {
                int countOfDeleted = 0;
                for (int rowIndex : correctRows) {
                    int delIndex = rowIndex - countOfDeleted;
                    res.add(Data.dataTable.get(delIndex));
                    Data.dataTable.remove(delIndex);
                    countOfDeleted++;
                }
            }
            case SELECT -> {
                for (int rowIndex : correctRows) {
                    res.add(Data.dataTable.get(rowIndex));
                }
            }
            case UPDATE -> {
                for (int rowIndex : correctRows) {
                    var row = Data.dataTable.get(rowIndex);
                    for (var property : properties) {
                        putOrUpdateKeyValueIfCorrect(property.getProperty(), property.getValue(), row);
                    }
                    res.add(row);
                }
            }
        }
        return res;
    }

    private boolean isRightConditionForRow(ConditionContainer cond, Map<String, Object> row) {
        int expectedRes = -2;
        int result = -3;
        final String property = cond.getProperty();
        final var propertyValue = row.get(property);
        final var condValue = cond.getValue();
        switch (cond.getOperator()) {
            case equals, notEquals, lessThan, greaterThan, lessThanStrictly, greaterThanStrictly-> {
                return compareResolver(property, propertyValue, condValue, cond.getOperator());
            }
            case like -> {

            }
            case ilike -> {
            }
            default -> throw new IllegalArgumentException();
        }
        return false;
    }

    private boolean compareResolver(String property, Object propertyValue,
                                    String condValue, String op) {
        boolean isStrictly = true;
        int expectedRes = -2;
        switch (op) {
            case equals -> expectedRes = 0;
            case notEquals, lessThanStrictly -> expectedRes = -1;
            case greaterThanStrictly -> expectedRes = 1;
            case greaterThan -> {
                isStrictly = false;
                expectedRes = 1;
            }
            case lessThan -> {
                isStrictly = false;
                expectedRes = -1;
            }
        }
        switch (property) {
            case "age", "id", "cost" -> {
                final int res;
                if (property.equals("cost")) {
                    res = Double.compare(Double.parseDouble(condValue), (Double) propertyValue);
                } else {
                    res = Long.compare(Long.parseLong(condValue), (Long) propertyValue);
                }
                if (!isStrictly) return (res == expectedRes || res == 0);
                return res == expectedRes;
            }
            case "lastName" -> {
                operatorValidate(op);
                return propertyValue.equals(condValue); // сравнение строк, а не ссылок, ведь так?)
            }
            case "active" -> {
                operatorValidate(op);
                if (expectedRes == -1) return propertyValue != valueOfBoolean(condValue);
                return propertyValue == valueOfBoolean(condValue);
            }
            default -> throw new IllegalStateException(new IllegalArgumentException());
        }
    }

    private void operatorValidate(String op) {
        if (op.equals(equals) || op.equals(notEquals)) return;
        throw new IllegalArgumentException();
    }

    private List<Map<String, Object>> insertResolver(List<ConditionContainer> properties) {
        if (isIncorrectOperator(properties)) throw new IllegalArgumentException();
        Map<String, Object> row = properties.stream()
                .collect(Collectors.toMap(ConditionContainer::getProperty, ConditionContainer::getValue));
        try {
            for (var property : properties) {
                String key = property.getProperty();
                String value = property.getValue();
                putOrUpdateKeyValueIfCorrect(key, value, row);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException();
        }
        Data.columns.stream().filter(key -> !row.containsKey(key)).forEach(key -> row.put(key, null));
        Data.dataTable.add(row);
        return Data.dataTable;
    }

    private void putOrUpdateKeyValueIfCorrect(String key, String value, Map<String, Object> row) {
        switch (key) { // проверки типов обернуть в отдельный метод?
            case "id", "age" -> row.put(key, Long.parseLong(value));
            case "cost" -> row.put(key, Double.parseDouble(value));
            case "active" -> row.put(key, valueOfBoolean(value));
            case "lastName" -> row.put(key, value);
            default -> throw new IllegalArgumentException(); // может ли быть тут?
        }
    }

    private Boolean valueOfBoolean(String s) {
        if (s.equalsIgnoreCase("true")) return true;
        if (s.equalsIgnoreCase("false")) return false;
        if (s.equalsIgnoreCase("null")) return null;
        throw new NumberFormatException();
    }

    private boolean isIncorrectOperator(List<ConditionContainer> properties) {
        return properties.stream()
                .map(ConditionContainer::getOperator)
                .anyMatch(operator -> !operator.equals(equals));
    }

    private List<Map<String, Object>> takeOrDeleteAll(String firstRequestWord) {
        if (firstRequestWord.equals(DELETE)) {
            Data.dataTable.clear();
        }
        return Data.dataTable;
    }

    private String[] filter(String request) {
        request = request.trim().toLowerCase();
        String[] arrayFromRequest = request.split(" ");
        arrayFromRequest = deleteEmptyElInArray(arrayFromRequest);
        return arrayFromRequest;
    }

    private String[] deleteEmptyElInArray(String[] arrayFromRequests) {
        List<String> container = new ArrayList<>();
        for (String el : arrayFromRequests) {
            if (el.isEmpty()) continue;
            container.add(el);
        }
        String[] arrContainer = new String[container.size()];
        return container.toArray(arrContainer);
    }

    private void validate(String[] arrayFromRequest, String firstRequestWord, String request) {
        var secRequestWord = arrayFromRequest[1];
        switch (firstRequestWord) {
            case INSERT -> {
                if (!secRequestWord.equals(VALUES) || request.contains(WHERE)) {
                    throw new IllegalArgumentException();
                }
            }
            case UPDATE -> {
                if (!secRequestWord.equals(VALUES)) {
                    throw new IllegalArgumentException();
                }
            }
//            case DELETE, SELECT -> throw new UnsupportedOperationException();
        }
    }

    private boolean isSingleCommand(String[] arrayFromRequest, String firstRequestWord) {
        if (arrayFromRequest.length != 1) {
            return false;
        }
        return firstRequestWord.equals(DELETE) || firstRequestWord.equals(SELECT);
    }
}
