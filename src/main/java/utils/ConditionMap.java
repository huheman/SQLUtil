package utils;

import java.util.Comparator;
import java.util.TreeMap;

import static utils.SQLUtil.KEY_SEPARTOR;

public  class ConditionMap extends TreeMap<String, Object> {
    // 命令的排序方式
    private static Comparator<String> sqlComparator = (o1, o2) -> {
        if (o1.contains(KEY_SEPARTOR) && o2.contains(KEY_SEPARTOR)) {
            String sub_1 = o1.substring(0, o1.indexOf(KEY_SEPARTOR));
            String sub_2 = o2.substring(0, o2.indexOf(KEY_SEPARTOR));
            int i1 = -1;
            int i2 = -1;
            for (int i = 0; i < SQLUtil.ORDERS.size(); i++) {
                if (sub_1.matches(SQLUtil.ORDERS.get(i))) i1 = i;
                if (sub_2.matches(SQLUtil.ORDERS.get(i))) i2 = i;
            }
            if (i1 == i2) return o1.compareTo(o2);
            return i1 - i2;
        }
        return 1;
    };

    ConditionMap() {
        super(sqlComparator);
    }

    @Override
    public Object put(String key, Object value) {
        // value既不允许为空，也不允许为空字符串
        if (value == null || value.equals("")) {
            return null;
        }
        return super.put(key, value);
    }
}
