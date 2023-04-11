package com.digdes.school;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.digdes.school.Commands.*;
import static com.digdes.school.Operations.*;

public class PropertyReader {
    private static final char propertyBracket = '\'';
    private static final int readSingle = 1;
    private static final int readNormal = 0;

    private final List<ConditionContainer> properties = new ArrayList<>();


    private StringBuilder incomingRequest;
    private StringBuilder beforeWhere;
    private StringBuilder afterWhere;
    private int stateReaderProperties = 0;
    private String originRequest;


    String originalStringPropertyVal;
    String originalStringCondVal;

    public PropertyReader(String originRequest) {
        this.originRequest = originRequest;
    }

    public List<ConditionContainer> readRequestProperties(String[] request) {
        if (stateReaderProperties == readSingle) {
            incomingRequest = getStringBuilder(request);
        } else {
            cutTheRequest(request);
            incomingRequest = beforeWhere;
        }
        originRequest = originRequest.replaceAll(" ", "");
        if (!request[0].equals(INSERT)) {
            String splitBy = getStrByRegex("where", originRequest);
            if (splitBy.isEmpty()) {
                originalStringPropertyVal = fillForOriginValues(originRequest);
            } else {
                String[] temp = originRequest.split(splitBy);
                originalStringPropertyVal = fillForOriginValues(temp[0]);
                originalStringCondVal = fillForOriginValues(temp[1]);
            }
        } else {
            originalStringPropertyVal = fillForOriginValues(originRequest);
            originalStringCondVal = fillForOriginValues(originRequest);
        }

        if (request[0].equals(DELETE) || request[0].equals(SELECT)) return null;
        StringBuilder property = new StringBuilder();
        StringBuilder value = new StringBuilder();
        StringBuilder operator = new StringBuilder();
        boolean wasOpenBracket = false;
        boolean wasCloseBracket = false;
        boolean isPropertyWriting = false;
        boolean isValueWriting = false;
        boolean isOperatorWriting = false;
        int nextPos;
        for (int i = 0; i < incomingRequest.length(); i++) {
            nextPos = i + 1;
            var currChar = incomingRequest.charAt(i);
            if (currChar == propertyBracket) {
                if (!wasOpenBracket && !property.toString().equals("lastname")) {
                    wasOpenBracket = isPropertyWriting = true;
                    wasCloseBracket = isValueWriting = false;
                } else {
                    wasOpenBracket = isPropertyWriting = false;
                    wasCloseBracket = isValueWriting = true;
                    isOperatorWriting = true;
                }
            }
            if (isOperatorWriting && currChar != propertyBracket) {
                if ((operator.toString().equals(">") || operator.toString().equals("<"))
                        && currChar == '=') operator.append(currChar);
                if (isValidOperatorName(operator) || currChar == ',') {
                    isOperatorWriting = false;
                } else {
                    operator.append(currChar);
                }
            }
            if (!wasOpenBracket && !wasCloseBracket || (wasCloseBracket && currChar == '=')
                    || (i < incomingRequest.length() - 1 && currChar == ','))
                continue; // нужное ли условие?

            if (i == incomingRequest.length() - 2 && isValueWriting && currChar == ','
                    && incomingRequest.charAt(nextPos) == propertyBracket)
                throw new IllegalArgumentException();

            if (currChar != propertyBracket && !isOperatorWriting) {
                if (isPropertyWriting) {
                    property.append(currChar);
                } else if (isValueWriting) {
                    value.append(currChar);
                }
            }
            if (isValueWriting && (i == incomingRequest.length() - 1 || incomingRequest.charAt(nextPos) == ','))
                isValueWriting = false;
            if (!isPropertyWriting && !isValueWriting) {
                if (property.toString().equals("lastname")) {
                    property = new StringBuilder("lastName");
                    if (stateReaderProperties == readSingle) {
                        value = new StringBuilder(originalStringCondVal);
                    } else {
                        value = new StringBuilder(originalStringPropertyVal);
                    }

                }
                final StringBuilder finalProperty = property;
                boolean alreadyExistsProp = properties.stream()
                        .anyMatch(item -> item.getProperty().equals(finalProperty.toString()));
                if (!Data.columns.contains(property.toString()) || alreadyExistsProp) {
                    throw new IllegalArgumentException();
                }
                properties.add(new ConditionContainer(property.toString(), operator.toString(), value.toString()));
                property.setLength(0);
                value.setLength(0);
                operator.setLength(0);
            }
        }
        if (properties.isEmpty()) {
            throw new IllegalArgumentException();
        }
        final var result = new ArrayList<>(properties);
        properties.clear();
        return result;
    }

    private String getStrByRegex(String regex, String str) {
        var p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        var m = p.matcher(str);
        String res = "";
        while (m.find()) {
            res = m.group();
        }
        return res;
    }

    private String fillForOriginValues(String request) {
        String value = getStrByRegex("(?<='lastName'=).*?(?=,)", request);
        if (value.isEmpty())
            value = getStrByRegex("(?<='lastName'=).*(?=and)", request);
        if (value.isEmpty())
            value = getStrByRegex("(?<='lastName'=).*(?=or)", request);
        if (value.isEmpty())
            value = getStrByRegex("(?<='lastName'=).*", request);
        if (value.isEmpty())
            value = getStrByRegex("(?<='lastName'!=).*(?=,)", request);
        if (value.isEmpty())
            value = getStrByRegex("(?<='lastName'!=).*(?=and)", request);
        if (value.isEmpty())
            value = getStrByRegex("(?<='lastName'!=).*(?=or)", request);
        if (value.isEmpty())
            value = getStrByRegex("(?<='lastName'!=).*", request);
        if (value.isEmpty())
            value = getStrByRegex("(?<='lastName'ilike').*?(?=')", request);
        if (value.isEmpty())
            value = getStrByRegex("(?<='lastName'like').*?(?=')", request);
        // рассмотреть все варианты и проверить их.
        return value.trim();
    }

    private boolean isValidOperatorName(StringBuilder operator) {
        return Stream.of(equals, notEquals, greaterThanStrictly, lessThanStrictly, greaterThan, lessThan, like, ilike)
                .anyMatch(op -> op.equals(operator.toString()));
    }

    private void cutTheRequest(String[] request) {
        StringBuilder str = getStringBuilder(request);
        int index = str.indexOf(WHERE);
        if (index == -1) {
            beforeWhere = str;
            afterWhere = null;
            return;
        }
        beforeWhere = new StringBuilder(str.substring(0, index));
        afterWhere = new StringBuilder(str.substring(index + WHERE.length(), str.length()));
    }

    private StringBuilder getStringBuilder(String[] arr) {
        var result = new StringBuilder();
        for (String el : arr) {
            result.append(el);
        }
        return result;
    }

    public List<List<ConditionContainer>> readConditions(String firstRequestWord) {
        if (afterWhere == null) {
            if ((firstRequestWord.equals(DELETE) || firstRequestWord.equals(SELECT))) {
                throw new IllegalArgumentException();
            }
            return null;
        }
        String strCond = afterWhere.toString();
        List<List<ConditionContainer>> allConditions = new ArrayList<>();
        List<ConditionContainer> partsOfConditions = new ArrayList<>();
        var separatedArrForOr = strCond.split("or");
        setReaderState(readSingle);
        for (String str : separatedArrForOr) {
            var temp = str.split("and");
            for (String el : temp) {
                partsOfConditions.addAll(readRequestProperties(new String[]{el}));
            }
            allConditions.add(new ArrayList<>(partsOfConditions));
            partsOfConditions.clear();
        }
        setReaderState(readNormal);
        return allConditions;
    }

    private void setReaderState(int code) {
        stateReaderProperties = code;
    }

    class ConditionContainer {
        private final String property;
        private final String operator;
        private final String value;

        public ConditionContainer(String property, String operator, String value) {
            this.property = property;
            this.operator = operator;
            this.value = value;
        }

        public String getProperty() {
            return property;
        }

        public String getOperator() {
            return operator;
        }

        public String getValue() {
            return value;
        }
    }
}