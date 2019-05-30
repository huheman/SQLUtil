package utils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class SQLUtil {
    private static final String JOIN = "join\\w*";
    private static final String ON = "on\\w*";
    private static final String EQUAL = "";
    private static final String GREATER_THAN = "gt";
    private static final String LESS_THAN = "lt";
    private static final String GREATER_EQUAL = "ge";
    private static final String LESS_EQUAL = "le";
    private static final String LIKE = "like";
    private static final String LLIKE = "llike";
    private static final String RLIKE = "rlike";
    private static final String IN = "in";
    private static final String ORDER = "order";
    private static final String LIMIT = "limit";
    private static final String OFFSET = "offset";
    private static final String GROUP = "group";
    private static final String HAVING = "having";
    public static final String KEY_SEPARTOR = "$";

    static final List<String> ORDERS = Arrays.asList(JOIN, ON, EQUAL, GREATER_THAN, LESS_THAN,
            GREATER_EQUAL, LESS_EQUAL, RLIKE, LLIKE, LIKE, IN, GROUP, HAVING, ORDER, LIMIT, OFFSET);
    private static final List<String> ONE_PAR = Arrays.asList(ORDER, LIMIT, OFFSET, GROUP, HAVING); // 可以冒号后面不接其他的命令
    private static final List<String> UNCOUNTABLE = Arrays.asList(LIMIT, ORDER, OFFSET);  // 不能用于select count(*) 语句的条件
    private static final List<String> COLLAPSE = Arrays.asList(ORDER, JOIN, ON, GROUP);  // 不能占位的条件

    private static final String MARK_QUESTION = "question";  // 用问号填充sql语句
    private static final String MARK_KEY = "key";  // 用#{key}填充sql语句

    /**
     * 返回一个用户装条件参数的MAP，必须用这个Map来装各种参数
     */
    public static ConditionMap createConditionMap() {
        return new ConditionMap();
    }

    /**
     * 因为用NamedTemplate非常严格，要求sql中的别名要完全符合，包括大小写和必须在map中有指定，
     * 所以我们把大小写和在map中没出现的别名，全部替换成null，来解决这个问题
     * <p>
     * 此方法适用于insert into。
     */
    /*public static String formatSQL(String sql, Map<String, String> map) {
        Pattern pattern = Pattern.compile(":(\\w+)[,)]");
        Matcher matcher = pattern.matcher(sql);
        boolean result = matcher.find();
        if (result) {
            StringBuilder sb = new StringBuilder();
            do {
                String inside = matcher.group(1);
                if (!map.keySet().contains(inside) || map.get(inside) == null || map.get(inside).isEmpty()) {
                    // 如果map中没有这个值,就在map中加入这个值，并且值为“null”
                    // 目前是没这个值就把这个别名变为null，一样的。
                    matcher.appendReplacement(sb, matcher.group().replace(":" + matcher.group(1), "null"));
                }
                result = matcher.find();
            } while (result);
            matcher.appendTail(sb);
            return sb.toString();
        }
        return sql;
    }*/

    /**
     * 能够把map中符合格式的键值对转化为可用的sql语句
     */
    private static String createConditionSQL(ConditionMap condition, boolean needWhere, String replacementMark) {
        return condition.entrySet().stream().map(new Function<Map.Entry<String, Object>, String>() {
            boolean flag_isFirst = true;

            @Override
            public String apply(Map.Entry<String, Object> entry) {
                StringBuilder sb = new StringBuilder();
                // 先把key分割好
                String[] split = entry.getKey().split(KEY_SEPARTOR);
                // 核实该有两个值的要有两个值
                if (!enSureKey(split)) return "";

                // 核实用那种类型作为占位符
                String replaceVal;
                switch (replacementMark) {
                    case MARK_QUESTION:
                        replaceVal = "?";
                        break;
                    case MARK_KEY:
                        replaceVal = "#{" + entry.getKey() + "}";
                        break;
                    default:
                        throw new RuntimeException("要求replaceVal为question或key之一");
                }
                Object eval = entry.getValue();
                switch (split[0]) {
                    case EQUAL:
                        // 如果是精确查找，则只需添加=?即可
                        flag_isFirst = checkNeedWhere(flag_isFirst, needWhere, sb);
                        sb.append(split[1]).append("=").append(replaceVal);
                        break;
                    case GREATER_THAN:
                        // 处理大于符号
                        flag_isFirst = checkNeedWhere(flag_isFirst, needWhere, sb);
                        sb.append(split[1]).append(">").append(replaceVal);
                        break;
                    case GREATER_EQUAL:
                        // 处理大于等于符号
                        flag_isFirst = checkNeedWhere(flag_isFirst, needWhere, sb);
                        sb.append(split[1]).append(">=").append(replaceVal);
                        break;
                    case LESS_THAN:
                        // 处理小于符号
                        flag_isFirst = checkNeedWhere(flag_isFirst, needWhere, sb);
                        sb.append(split[1]).append("<").append(replaceVal);
                        break;
                    case LESS_EQUAL:
                        // 处理小于等于符号
                        flag_isFirst = checkNeedWhere(flag_isFirst, needWhere, sb);
                        sb.append(split[1]).append("<=").append(replaceVal);
                        break;
                    case LIKE:
                    case RLIKE:
                    case LLIKE:
                        // 处理Like，Like分lLike和rlike。在sql语句是一样的
                        flag_isFirst = checkNeedWhere(flag_isFirst, needWhere, sb);
                        sb.append(split[1]).append(" LIKE ").append(replaceVal);
                        break;
                    case ORDER:
                        // 处理order by，不用检测是否添加WHERE
                        sb.append(" ORDER BY ").append(eval);
                        if (split.length > 1 && split[1].equalsIgnoreCase("DESC"))
                            sb.append(" DESC");
                        break;
                    case LIMIT:
                        // limit也是不用检测是否添加where的
                        sb.append(" LIMIT ").append(replaceVal);
                        break;
                    case OFFSET:
                        // offset 不用检测是否需要添加where
                        sb.append(" OFFSET ").append(replaceVal);
                        break;
                    case IN:
                        flag_isFirst = checkNeedWhere(flag_isFirst, needWhere, sb);
                        String[] eValStrs = makeListToArray(eval);
                        sb.append(split[1]).append(" IN (");
                        int varibleCount = eValStrs.length;
                        for (int i = 0; i < varibleCount; i++) {
                            if (replacementMark.equalsIgnoreCase(MARK_KEY)) {
                                replaceVal = "#{" + entry.getKey() + "[" + i + "]}";
                            }
                            sb.append(replaceVal).append(",");
                        }
                        sb.setCharAt(sb.length() - 1, ')');
                        break;
                    case GROUP:
                        sb.append(" GROUP BY ").append(eval);
                        break;
                    case HAVING:
                        // 先保证有GROUP才继续，否则就不管这个了
                        if (condition.get(GROUP + KEY_SEPARTOR) == null) break;
                        ConditionMap map;
                        try {
                            // 把Having的val转成ConditionMap先
                            map = (ConditionMap) condition.get(HAVING + KEY_SEPARTOR);
                            sb.append(" HAVING ").append(createConditionSQL(map, false, replacementMark));
                        } catch (Exception e) {
                            break;
                        }
                        break;
                    default:
                        if (split[0].startsWith("JOIN")) {
                            // 通过正则表达式，找到对应的ON键
                            Pattern pattern = Pattern.compile("^on" + split[0].substring(4) + ":(\\w+)$");
                            condition.forEach((key, valueOfOn) -> {
                                Matcher matcher = pattern.matcher(key);
                                if (matcher.find()) {
                                    String field = matcher.group(1);
                                    // 找到就可以添加啦
                                    sb.append(" JOIN ").append(eval).append(" ")
                                            .append(split[1]).append(" ON ").append(valueOfOn)
                                            .append(".").append(field).append("=").append(split[1])
                                            .append(".").append(field);
                                }
                            });
                        }
                        break;
                }
                return sb.toString();
            }

            private boolean checkNeedWhere(boolean isFirst, boolean needWhere, StringBuilder sb) {
                if (isFirst) {
                    if (needWhere)
                        sb.append(" WHERE ");
                } else sb.append("AND ");
                return false;
            }
        }).collect(Collectors.joining(" "));
    }

    private static String[] makeListToArray(Object eval) {
        String[] eValStrs;
        if (eval instanceof List) {
            eValStrs = (String[]) ((List) eval).toArray(new String[0]);
        } else {
            eValStrs = (String[]) eval;
        }
        return eValStrs;
    }

    private static boolean enSureKey(String[] split) {
        return ONE_PAR.contains(split[0]) || split.length >= 2;
    }

    public static String createConditionSQL(ConditionMap conditionMap) {
        return createConditionSQL(conditionMap, true, MARK_QUESTION);
    }

    /**
     * 把map中不符合规则的entry去掉，只留下可以写sql的语句
     * 这个方法也能返回一个适用于createConditionSQL的MAP
     * <p>
     * 此方法等同于新建一个ConditionMap，把符合规则的entry放进去。
     */
    public static ConditionMap makeConditionMap(Map<String, ?> map) {
        return SQLUtil.filterCondition(map, ORDERS, true);
    }

    /**
     * 把condition去掉filter里面有的key,或者选择保留
     */
    public static ConditionMap filterCondition(Map<String, ?> condition, List<String> filter, boolean isKeep) {
        return condition.entrySet().stream().filter(entry -> {
            String split = entry.getKey().split(KEY_SEPARTOR)[0];
            for (String s : filter) {
                if (split.matches(s))
                    return isKeep;
            }
            return !isKeep;
        }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (object, object2) -> object, SQLUtil::createConditionMap));
    }

    /**
     * 把参数变成适合求count的，把limit，order，offset这些key去掉
     * <p>
     * 要求参数是已经被过滤好的
     */
    public static ConditionMap filterConditionForCount(ConditionMap orgCondition) {
        return filterCondition(orgCondition, UNCOUNTABLE, false);
    }

    /**
     * 把condition去掉不适合填问号的键，再把值变成适合jdbcTemplate的值
     */
    public static Object[] createConditionValues(ConditionMap condition) {
        return filterCondition(condition, COLLAPSE, false).entrySet().stream()
                // 先处理LIKE键
                .peek(new Consumer<Map.Entry<String, Object>>() {
                    @Override
                    public void accept(Map.Entry<String, Object> entry) {
                        String val = entry.getValue().toString();
                        switch (entry.getKey().split(KEY_SEPARTOR)[0]) {
                            case LIKE:
                                val = addPrefix(val);
                                entry.setValue(addPosfix(val));
                                break;
                            case LLIKE:
                                entry.setValue(addPrefix(val));
                                break;
                            case RLIKE:
                                entry.setValue(addPosfix(val));
                                break;
                            default:
                                break;
                        }
                    }

                    private String addPrefix(String val) {
                        if (!val.startsWith("%")) return "%" + val;
                        return val;
                    }

                    private String addPosfix(String val) {
                        if (!val.endsWith("%")) return val + "%";
                        return val;
                    }
                })
                // 再把IN键的值拆开
                .flatMap(entry -> {
                    String tag = entry.getKey().split(KEY_SEPARTOR)[0];
                    if (tag.equalsIgnoreCase(IN)) {
                        return Arrays.stream(makeListToArray(entry.getValue()));
                    }
                    // 把HAVING的值拆开
                    if (tag.equalsIgnoreCase(HAVING)) {
                        return Arrays.stream(createConditionValues((ConditionMap) entry.getValue()));
                    }
                    return Stream.of(entry.getValue());
                }).toArray();
    }

    /**
     * 通过ConditionMap生成一个mybatis适用的sql语句
     */
    public static String createBatisStyleSQL(ConditionMap conditionMap) {
        return createConditionSQL(conditionMap, true, MARK_KEY);
    }


    /*public static String createBatisStyleSQL(SQL sql, ConditionMap conditionMap) {
        boolean hasLimit = false;
        boolean hasOffset = false;
        conditionMap.entrySet().forEach((entry) -> {
            String[] split = entry.getKey().split(KEY_SEPARTOR);
            String tag = split[0];
            // 核实该有两个值的要有两个值
            if (!enSureKey(split)) return;
            String replaceVal;

            // 确定替换的值
            if (COLLAPSE.contains(tag)) {
                replaceVal = "${" + entry.getKey() + "}";
            } else {
                replaceVal = "#{" + entry.getKey() + "}";
            }
            // 因为JOIN ON涉及myBatis中的不同resultMap，所以这个框架不考虑JOIN ON。
            switch (tag) {
                case EQUAL:
                    sql.WHERE(split[1] + "=" + replaceVal);
                    break;
                case GREATER_THAN:
                    sql.WHERE(split[1] + ">" + replaceVal);
                    break;
                case LESS_THAN:
                    sql.WHERE(split[1] + "<" + replaceVal);
                    break;
                case GREATER_EQUAL:
                    sql.WHERE(split[1] + ">=" + replaceVal);
                    break;
                case LESS_EQUAL:
                    sql.WHERE(split[1] + "<=" + replaceVal);
                    break;
                case RLIKE:
                    entry.setValue(entry.getValue() + "%");
                    sql.WHERE(split[1] + " LIKE " + replaceVal);
                    break;
                case LLIKE:
                    entry.setValue("%" + entry.getValue());
                    sql.WHERE(split[1] + " LIKE " + replaceVal);
                    break;
                case LIKE:
                    entry.setValue("%" + entry.getValue() + "%");
                    sql.WHERE(split[1] + " LIKE " + replaceVal);
                    break;
                case IN:
                    // 为了防止注入，应该把In的值变成一个Map
                    Map<String, String> map = Arrays.stream(((String) entry.getValue())
                            .split(","))
                            .collect(Collectors.toMap(s -> s, s -> s));
                    entry.setValue(map);
                    replaceVal = map.keySet().stream().map(s -> "#{" + entry.getKey() + "." + s + "}")
                            .collect(Collectors.joining(",", "(", ")"));
                    sql.WHERE(split[1] + " IN " + replaceVal);
                    break;
                case GROUP:
                    sql.GROUP_BY(replaceVal);
                    break;
                case HAVING:

                    break;
                case ORDER:
                    if (split.length > 1 && split[1].equalsIgnoreCase("DESC"))
                        sql.ORDER_BY(replaceVal + " DESC");
                    else
                        sql.ORDER_BY(replaceVal);
                    break;
                default:
                    break;
            }
        });
        String sqlRet = sql.toString();
        if (conditionMap.get("limit:") != null) {
            sqlRet += "limit #{limit:}";
        }
        if (conditionMap.get("offset:") != null) {
            sqlRet += "offset #{offset:}";
        }
        return sqlRet;
    }*/
}
