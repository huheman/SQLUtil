# SQLUtil

从浏览器接收到参数以后，传统的方法要判断传上来的参数有哪些，然后根据这些参数写service和dao方法，**这会导致dao方法又多，方法重用性又不高**。

> 例如：当有3个需求：模糊查询某些，通过id精确查找某个值，通过指定范围查找某些值，就需要写3条dao方法：模糊查询一个方法，精确查询一个方法，范围查询一个方法。

这样维护起来既不方便，也增加出错的几率，为了解决这个问题。我们可以自己创建一个拼接sql语句的工具。



### 拼接工具的使用

我希望方法接受一个map对象， 就能输出条件选择的sql语句，比如：

> 当接受entry("id",2) 会拼接出 id = 2;

精确查找好搞，想拼接处更复杂的查询语句，就需要定义一些规则：

1. 所有查找条件的键都需要包含冒号`":"`

2. 当冒号在最前面，表示该条件是精确查找，如：`entry(":id",2)  表示 id=2`

3. 当冒号前面的值为ge,表示大于等于，如`entry("ge:price",400)  表示  price>=400`

4. 当冒号前面的值为le，表示小于等于，如`entry("le:price",500) 表示 price <= 500`

5. 当冒号前面的值为gt，表示大于，如`entry("gt:date","2018-01-01") 表示date > '2018-01-01'`

6. 当冒号前面的值为lt，表示小于，如`entry("lt:date","2018-01-01") 表示date < '2019-01-01'`

7. 当冒号前面的值为like，表示like，如`entry("like:name","小雪") 表示 name like '%小雪%'`

8. 当冒号前面的值为order，表示orderby，键如果以desc结尾，表示是降序。如：`entry("order:desc",“count”) 表示 order by count desc`

9. 当冒号前面的值为limit，表示限制搜索多少条数据，如:`entry("limit:",20) 表示 limit 20`

10. 当冒号前面的值为offset， 表示限制开始的位置，如`entry("offset:",3) 表示offset 3`

11. 当冒号前面的值为join\*(星为任意字母)，则需要一个on\*(星为任意字母)，键与之对应，表示查询的另一个表以及所绑定的字段，如：

    ```mysql
    entry("joina:tf","table_favourite");
    entry("ona:uid","table_user");
    -- 两个entry要同时存在，才能表示如下表达式
    JOIN table_favourite tf ON table_user.uid = tf.uid 
    
    ```

    外键查询的规则会比其他查询更复杂，需要仔细核对。

    另外，因为in()条件目前还没用到，先不处理。之后更新再添加



好了！现在你已经掌握了这个工具的使用方式了！现在看看怎样使用吧：

```java
import org.junit.Test;
import utils.SQLUtil;

import java.util.Arrays;
import java.util.TreeMap;

public class TestDemo {
    @Test
    public void testSQL() {
        // 先获取一个专用的键值对Map
        TreeMap<String, Object> conditionMap = SQLUtil.createConditionMap();
        conditionMap.put(":id", 2);    // 按照规则向map放入键值对
        conditionMap.put("gt:price", 3);   
        conditionMap.put("like:product", "烟草");
        String conditionSQL = "select * from tab_product" + SQLUtil.createConditionSQL(conditionMap);   // 工具只拼接条件语句，前面的可以自己定义
        Object[] conditionValues = SQLUtil.createConditionValues(conditionMap);
        System.out.println("conditionSQL = " + conditionSQL);
        System.out.println("Arrays.toString(conditionValues) = " + Arrays.toString(conditionValues));
    }
}


输出结果：
conditionSQL = select * from tab_product where 1=1 and id=? and price>? and product like ?
Arrays.toString(conditionValues) = [2, 3, %烟草%]
```

这样，就可以直接把`conditionSQL`，和`conditionValues`作为参数给jdbcTemplate的query()方法使用啦。

例子使用的是`select` ，你还可以进行任意操作，比如update、delete等等。sql拼接工具，是专门用于拼接条件，也就是拼接 `From some_table` 后面的语句。

这样，你就掌握了sql语句拼接工具的基本使用了



### 使用中要注意的地方

#### 当需要分页查询时

当我们需要分页查询时，除了数据本身，还要查询符合数据的总记录数，为了方便用同一个条件map完成多个查询，SQLUtil提供了方法：`Map<String,Object> filterConditionForCount(Map<String,Object> map)` 他接受一个条件map语句，返回过滤掉不适合执行计总数的entry，比如order:、limit、offset，这样就能用同一个Map执行分页查询和计算总数的语句了

```java
import org.junit.Test;
import utils.SQLUtil;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

public class TestDemo {
    @Test
    public void testSQL() {
        TreeMap<String, Object> conditionMap = SQLUtil.createConditionMap();
        conditionMap.put(":id", 2);
        conditionMap.put("gt:price", 3);
        conditionMap.put("like:product", "烟草");
        conditionMap.put("limit:", 5);
        conditionMap.put("offset:", 10);
        String conditionSQL = "select * from tab_product" + SQLUtil.createConditionSQL(conditionMap);
        Object[] conditionValues = SQLUtil.createConditionValues(conditionMap);
        System.out.println("conditionSQL = " + conditionSQL);
        System.out.println("conditionValues = " + Arrays.toString(conditionValues));
        Map<String, Object> countMap = SQLUtil.filterConditionForCount(conditionMap);
        String countSQL = "select count(*) from tab_product" + SQLUtil.createConditionSQL(countMap);
        System.out.println("countSQL = " + countSQL);
        Object[] countValues = SQLUtil.createConditionValues(countMap);
        System.out.println("countValues = " + Arrays.toString(countValues));
    }
}

运行结果：
conditionSQL = select * from tab_product where 1=1 and id=? and price>? and product like ? limit ? offset ?
conditionValues = [2, 3, %烟草%, 5, 10]
countSQL = select count(*) from tab_product where 1=1 and id=? and price>? and product like ?
countValues = [2, 3, %烟草%]
```

#### SQLUtil的实际应用

我们可以直接在接受数据的时候，就按照这个规则来定义名称，这样拿到parameter之后，稍加加工，加入自己特定的条件，就可以直接传递给dao，但传给dao之前，要通过

`public static Map<String, Object> filterConditionOnly(Map<String, String> map)`

加工一下，他会去掉不符合规范的语句，并且适当地对Entry进行排序，才能拼接处正确的sql条件语句。



这个工具就介绍到这里了。用法也差不多就这些，未来可能加入更多功能，到时候再说吧。



#### 下面演示一个实际应用的例子

有3个表，tab_user记录了用户信息，tab_route记录了路线信息，tab_favourite记录了用户收藏路线的信息，我需要从tab_user、tab_route、tab_favourite三个表中，按收藏时间倒序来获取路线，这就要用到多表查询

```java
// 先通过session获得用户的uid，就能传入这个方法中
@Override
public PageBean<Route> getUserFavRoute(Map<String, String> map, int uid) {
    // 要多表查询
    // join tab_favorite tf on tab_route.rid=tf.rid
    map.put("joina:tf", "tab_favorite");
    map.put("ona:rid", "tab_route");
    
    // join tab_suer tu on tu.uid=tf.uid
    map.put("joinb:tu", "tab_user");
    map.put("onb:uid", "tf");
    
    // 要增加按用户id，按收藏时间倒序这两个条件
    map.put(":tu.uid", uid + "");
    map.put("order:desc", "tf.date");
    return getPageBean(map);
}
```

就是这么简单，就能拼接好一个条件语句，而在`getPageBean(Map)` 方法中，将进一步按照这些逻辑定义sql语句的条件：

```java
@Override
public PageBean<Route> getPageBean(Map<String, String> map) {
    // map只需要提供当前页码，和每一页显示多少条数据，
    // 这些数据都应该是浏览器提供的
    int current = Integer.parseInt(map.get("current"));
    int size = Integer.parseInt(map.get("size"));

    // 生成pageBean后就用数据进行填充
    PageBean<Route> routePageBean = new PageBean<>();
    routePageBean.setCurrent(current);
    routePageBean.setSize(size);

    // 过滤掉不带条件的其他内容，也就是过滤掉没有冒号的其他内容，并且用所需的sortedMap包装好
    Map<String, Object> conditionMap = SQLUtil.filterConditionOnly(map);
    // 把current和size转换成limit 和offset条件
    conditionMap.put("limit:", size);
    conditionMap.put("offset:", size * (current - 1));

    // 条件已经齐全，调用dao方法获取路线对象
    List<Route> data = routeDAO.getRoutesByCondition(conditionMap);
    // 调用dao方法获取路线的总记录数
    int count = routeDAO.getRouteCountOf(conditionMap);

    routePageBean.setCount(count);
    routePageBean.setData(data);
    return routePageBean;
}
```

再来看看DAO层：

```java
public List<Route> getRoutesByCondition(Map<String, Object> condition) {
    String sql = "select tab_route.rid,tab_route.sid,rimage," +
            "tab_route.cid,count,rname,rdate,price,routeIntroduce from tab_route " +
            // 调用SQLUtil方法拼接条件就行
            SQLUtil.createConditionSQL(condition);

    System.out.println("sql = " + sql);
    // 调用SQLUtil方法创建条件参数
    Object[] conditionValues = SQLUtil.createConditionValues(condition);
    System.out.println("values=" + Arrays.toString(conditionValues));
    // 直接交给jdbc就能使用，不用担心顺序之类的问题
    return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(Route.class),
            conditionValues);
}
```

执行项目后打印出来的结果:

```mysql
-- jdbc的sql语句
sql = 
select tab_route.rid,tab_route.sid,rimage,tab_route.cid,count,rname,rdate,price,routeIntroduce 
from tab_route 
JOIN tab_favorite tf ON tab_route.rid=tf.rid 
JOIN tab_user tu ON tf.uid=tu.uid    
where 1=1 and tu.uid=? 
order by tf.date desc 
limit ? offset ?

-- jdbc的参数
values=[1, 12, 0]
```



运行结果：

![1557497061653](C:\Users\huhep\AppData\Roaming\Typora\typora-user-images\1557497061653.png)

源码我放在GitHub中啦，也可以从csdn中下载。
