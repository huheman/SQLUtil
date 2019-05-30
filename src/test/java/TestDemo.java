import org.junit.Test;
import utils.ConditionMap;
import utils.SQLUtil;

import java.util.Arrays;

public class TestDemo {
    @Test
    public void testSQL() {
        long l = System.currentTimeMillis();
        ConditionMap conditionMap = SQLUtil.createConditionMap();
        conditionMap.put(":id", 2);
        conditionMap.put("gt:price", 3);
        conditionMap.put("like:product", "烟草");
        conditionMap.put("limit:", 5);
        conditionMap.put("offset:", 10);
        conditionMap.put("rlike:name", "小王");
        conditionMap.put("in:rid", "2,5,7,9");
        ConditionMap havingMap = SQLUtil.createConditionMap();
        havingMap.put("ge:date", "1990-09-20");
        conditionMap.put("having:", havingMap);
        conditionMap.put("group:", "cid");
        String conditionSQL = "select * from tab_product" + SQLUtil.createConditionSQL(conditionMap);
        Object[] conditionValues = SQLUtil.createConditionValues(conditionMap);
        System.out.println("conditionSQL = " + conditionSQL);
        System.out.println("conditionValues = " + Arrays.toString(conditionValues));
        ConditionMap countMap = SQLUtil.filterConditionForCount(conditionMap);
        String countSQL = "select count(*) from tab_product" + SQLUtil.createConditionSQL(countMap);
        System.out.println("countSQL = " + countSQL);
        Object[] countValues = SQLUtil.createConditionValues(countMap);
        System.out.println("countValues = " + Arrays.toString(countValues));
        System.out.println(System.currentTimeMillis() - l);
    }

    @Test
    public void testBatisSQL() {
        /*ConditionMap conditionMap = SQLUtil.createConditionMap();
        conditionMap.put(":id", 2);
        conditionMap.put(":cid", 4);
        conditionMap.put("order:desc", "id");
        conditionMap.put("limit:", 4);
        conditionMap.put("offset:", 0);
        conditionMap.put("in:rid", Arrays.asList("4", "8", "9", "10"));
        String finalResult = SQLUtil.createBatisStyleSQL(conditionMap);
        String conditionSQL = SQLUtil.createConditionSQL(conditionMap);
        Object[] conditionValues = SQLUtil.createConditionValues(conditionMap);
        System.out.println("finalResult : " + finalResult);
        System.out.println("conditionSQL = " + conditionSQL);
        System.out.println("Arrays.toString(conditionValues) = " + Arrays.toString(conditionValues));*/
    }
}
