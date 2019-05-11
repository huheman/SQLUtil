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
        String conditionSQL = "select * from tab_product" + SQLUtil.createConditionSQL(conditionMap);
        Object[] conditionValues = SQLUtil.createConditionValues(conditionMap);
        System.out.println("conditionSQL = " + conditionSQL);
        System.out.println("conditionValues = " + Arrays.toString(conditionValues));
        ConditionMap countMap = SQLUtil.filterConditionForCount(conditionMap);
        String countSQL = "select count(*) from tab_product" + SQLUtil.createConditionSQL(countMap);
        System.out.println("countSQL = " + countSQL);
        Object[] countValues = SQLUtil.createConditionValues(countMap);
        System.out.println("countValues = " + Arrays.toString(countValues));
        System.out.println(System.currentTimeMillis()-l);
    }
}
