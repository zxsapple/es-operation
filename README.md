# es-operation

#### 介绍
es查询的框架，复杂的多条件列表查，3层以内聚合查询，满足大部分需求，使用者只用关注自己请求和返回的java实体，封装和请求es 都通过框架来做。

#### 软件架构
此项目采用 client 和server 两端
1 server端 使用HighLevelClient 访问elastic-search，将查询条件，返回的结果集，聚合集进行封装
2 client 对请求参数、结果集进行封装（通过注解反射，将java实体映射到es的hit，agg，condition）
  支持最多3层的嵌套桶聚合，日期单日、单月的聚合，复杂的多条件列表查询
