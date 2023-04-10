package com.digdes.school;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class JavaSchoolStarter {
    public JavaSchoolStarter() {
    }

    public List<Map<String, Object>> execute(String request) throws Exception {
        return new Executor().apply(request);
    }

    public static void main(String[] args) throws Exception {
        JavaSchoolStarter test = new JavaSchoolStarter();


//        System.out.println(test.execute("insert values 'lastName'='vio', 'age'=10, 'cost'=9.6, 'active' = true ,  'id'=1"));
//        System.out.println(test.execute("insert values 'lastName'='chris', 'age'=10, 'cost'=9.6, 'active' = true ,  'id'=2"));
//        System.out.println(test.execute("insert values 'lastName'='egor', 'age'=10, 'cost'=9.6, 'active' = true ,  'id'=16"));
//        System.out.println(test.execute("insert values 'lastName'='kirill', 'age'=10, 'cost'=9.9, 'active' = true ,  'id'=4"));
        System.out.println(test.execute("select where 'lastName'!=vio or 'id'=2 or 'cost'=9.9 and 'id'=4 and 'active'=true"));
        // протестировать все операторы + дописать like + добавить проверки, что like только с String + уточнить по поводу возвращаемых значений + поправить мелкие косяки (комментарии) + код вылизать
//        System.out.println(test.execute("update values 'lastName'='0', 'age'=0, 'cost'=0, 'active'=null where 'active'!=false"));
//        System.out.println(test.execute("insert values 'lastName'='kevin', 'age'=5, 'cost'=10.105, 'active' = false ,  'id'=1"));
//        System.out.println(test.execute("insert values 'lastName'='vio', 'age'=10, 'cost'=9.6, 'active' = true ,  'id'=12"));
//        System.out.println(test.execute("update values 'lastName'='oleg', 'age'=2, 'cost'=0, 'active'=null"));
//        System.out.println(test.execute("select"));
//        System.out.println(test.execute("delete"));
//        System.out.println(test.execute("update values 'lastName'='kevin', 'age'=5 where 'cost'>=10.105 and 'active' != false or  'id'like1"));
//        System.out.println(test.execute("insert values 'lastName'='kevin', 'age'=5, 'cost'=10.105, 'active' = false ,  'id'=1"));
//        System.out.println(test.execute("delete"));

//        System.out.println(test.execute("insert values 'age'=5"));


//        String test1 = "'id'=5 and 'lastName=kevin' or 'active'=false";
//        System.out.println(Arrays.toString(test1.split("and|or")));
//        System.out.println(Data.dataTable.get(1).get("lastName").getClass().getSimpleName());

//        String[] request = {"one", "two", "three"};
//        System.out.println(Arrays.toString(request));

//        Data.dataTable.add(Map.of("id",1));
//        Data.dataTable.add(Map.of("id",2));
//        Data.dataTable.add(Map.of("id",3));
//        System.out.println(Data.dataTable);
//        System.out.println(test.execute("Select"));
    }
}
