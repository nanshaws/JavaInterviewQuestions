#   	Mysql笔记



SELECT 
    SUM(IF(`status`= 2,1,0)) as 'a',
    SUM(IF(`status`= 3,1,0)) as 'b',
    SUM(IF(`status`= 4,1,0)) as 'c'
FROM orders 

SELECT 
    IF(`status`= 2,1,0) AS 'a',
    IF(`status`= 3,1,0) AS 'b',
    IF(`status`= 4,1,0) AS 'c'
FROM orders 

##   一、mysql常见工作应用场景                                    

### 1、行转列

​		1）、准备数据sql

```sql
#1、建表
DROP TABLE IF EXISTS tb_score;
CREATE TABLE tb_score(
    id INT(11) NOT NULL auto_increment,
    userid VARCHAR(20) NOT NULL COMMENT '用户id',
    subject VARCHAR(20) COMMENT '科目',
    score DOUBLE COMMENT '成绩',
    PRIMARY KEY(id)
)ENGINE = INNODB DEFAULT CHARSET = utf8;

#2、准备数据
INSERT INTO tb_score(userid,subject,score) VALUES ('张三','Chinese',90);
INSERT INTO tb_score(userid,subject,score) VALUES ('张三','Math',92);
INSERT INTO tb_score(userid,subject,score) VALUES ('张三','English',80);
INSERT INTO tb_score(userid,subject,score) VALUES ('李四','Chinese',88);
INSERT INTO tb_score(userid,subject,score) VALUES ('李四','Math',90);
INSERT INTO tb_score(userid,subject,score) VALUES ('李四','English',75.5);
INSERT INTO tb_score(userid,subject,score) VALUES ('王五','Chinese',70);
INSERT INTO tb_score(userid,subject,score) VALUES ('王五','Math',85);
INSERT INTO tb_score(userid,subject,score) VALUES ('王五','English',90);
INSERT INTO tb_score(userid,subject,score) VALUES ('王五','Music',82);
```

​		2）、方式一：SUM(CASE 表名 WHEN 字段名 THEN score ELSE 0 END) as 字段名

```sql
SELECT userid,
    SUM(CASE `subject` WHEN 'Chinese' THEN score ELSE 0 END) as '语文',
    SUM(CASE `subject` WHEN 'Math' THEN score ELSE 0 END) as '数学',
    SUM(CASE `subject` WHEN 'English' THEN score ELSE 0 END) as '英语',
    SUM(CASE `subject` WHEN 'Music' THEN score ELSE 0 END) as '音乐' 
FROM tb_score 
GROUP BY userid
```

```tex
注意：如果userid ='001' and subject='语文' 的记录有两条，则此时SUM() 的值将会是这两条记录的和，同理，使用Max()的值将会是这两条记录里面值最大的一个。但是正常情况下，一个user对应一个subject只有一个分数，因此可以使用SUM()、MAX()、MIN()、AVG()等聚合函数都可以达到行转列的效果。
```

​		3）、方式二：SUM(IF(表名=字段名,score,0)) as 字段名

```sql
SELECT userid,
    SUM(IF(`subject`='Chinese',score,0)) as '语文',
    SUM(IF(`subject`='Math',score,0)) as '数学',
    SUM(IF(`subject`='English',score,0)) as '英语',
    SUM(IF(`subject`='Music',score,0)) as '音乐' 
FROM tb_score 
GROUP BY userid
```

```tex
使用IF(`subject`='语文',score,0) 作为条件，即对所有subject='语文'的记录的score字段进行SUM()、MAX()、MIN()、AVG()操作，如果score没有值则默认为0
```

​		4）、**利用SUM(IF()) 生成列 + WITH ROLLUP 生成汇总行,并利用 IFNULL将汇总行标题显示为Total**

```sql
SELECT IFNULL(userid,'total') AS userid,
    SUM(IF(`subject`='Chinese',score,0)) AS 语文,
    SUM(IF(`subject`='Math',score,0)) AS 数学,
    SUM(IF(`subject`='English',score,0)) AS 英语,
    SUM(IF(`subject`='Music',score,0)) AS 音乐,
    SUM(IF(`subject`='total',score,0)) AS Total
FROM(
    SELECT userid,IFNULL(`subject`,'total') AS `subject`,SUM(score) AS score
    FROM tb_score
    GROUP BY userid,`subject`
    WITH ROLLUP
    HAVING userid IS NOT NULL
)AS A 
GROUP BY userid
WITH ROLLUP;
```

​		5）、**利用SUM(IF()) 生成列 + UNION 生成汇总行,并利用 IFNULL将汇总行标题显示为 Total**

```sql
SELECT userid,
    SUM(IF(`subject`='Chinese',score,0)) AS 语文,
    SUM(IF(`subject`='Math',score,0)) AS 数学,
    SUM(IF(`subject`='English',score,0)) AS 英语,
    SUM(IF(`subject`='Music',score,0)) AS 音乐,
    SUM(score) AS Total 
FROM tb_score
GROUP BY userid
UNION
SELECT 'Total',
    SUM(IF(`subject`='Chinese',score,0)) AS 语文,
    SUM(IF(`subject`='Math',score,0)) AS 数学,
    SUM(IF(`subject`='English',score,0)) AS 英语,
    SUM(IF(`subject`='Music',score,0)) AS 音乐,
    SUM(score) 
FROM tb_score	
```

​		6）、**利用SUM(IF()) 生成列，直接生成结果不再利用子查询，**

```sql
SELECT IFNULL(userid,'Total') AS userid,
    SUM(IF(`subject`='Chinese',score,0)) AS 语文,
    SUM(IF(`subject`='Math',score,0)) AS 数学,
    SUM(IF(`subject`='English',score,0)) AS 英语,
    SUM(IF(`subject`='Music',score,0)) AS 音乐,
    SUM(score) AS Total 
FROM tb_score
GROUP BY userid WITH ROLLUP;	
```

​		7）、**动态，适用于列不确定情况**

```shell
SET @EE='';
select @EE :=CONCAT(@EE,'sum(if(subject= \'',subject,'\',score,0)) as ',subject, ',') AS aa FROM (SELECT DISTINCT subject FROM tb_score) A ;
SET @QQ = CONCAT('select ifnull(userid,\'TOTAL\')as userid,',@EE,' sum(score) as TOTAL from tb_score group by userid WITH ROLLUP');
-- SELECT @QQ;
PREPARE stmt FROM @QQ;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
```

​		8）、**合并字段显示：利用group_concat()**

```sql
SELECT userid,GROUP_CONCAT(`subject`,":",score)AS 成绩 FROM tb_score
GROUP BY userid
```



### 2、列转行

​		1）、准备数据sql

```sql
#1、建表
DROP TABLE IF EXISTS tb_grade;
CREATE TABLE `tb_grade` (
 `id` int(10) NOT NULL AUTO_INCREMENT,
 `user_name` varchar(20) DEFAULT NULL,
 `chinese_score` float DEFAULT NULL,
 `math_score` float DEFAULT NULL,
 `english_score` float DEFAULT '0',
 PRIMARY KEY (`ID`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

#2、准备数据
insert into tb_grade(user_name, chinese_score, math_score, english_score) values
("张三", 34, 58, 58),
("李四", 45, 87, 45),
("王五", 76, 34, 89);
("赵六", 73, 88, 99);
```

​		2）、方式一：order

 ```sql
 select user_name, '语文' `subject`, chinese_score as score from tb_grade
 union select user_name, '数学' `subject`, math_score as score from tb_grade
 union select user_name, '英语' `subject`, english_score as score from tb_grade
 order by user_name,`subject`;
 ```

​		3）、方式二：group by

```sql
SELECT NAME,'语文' AS subject ,MAX("语文") AS score FROM student1 GROUP BY NAME
UNION
SELECT NAME,'数学' AS subject ,MAX("数学") AS score FROM student1 GROUP BY NAME
UNION
SELECT NAME,'英语' AS subject ,MAX("英语") AS score FROM student1 GROUP BY NAME
```



### 3、排序

​		1）、根据某个字段正序排序，要求：把该字段为null的数据放在最后

```sql
1、写法一
	SELECT * FROM `my_member_record` r ORDER BY r.`trade_type` IS NULL,r.`trade_type`
2、写法二
	SELECT * FROM `my_member_record` r ORDER BY -r.`trade_type` DESC
3、写法三
	SELECT * FROM `my_member_record` r  ORDER BY ISNULL(r.`trade_type`),r.`trade_type`
```

​		2）、根据某个字段按指定顺序排序，要求：把该字段为null的数据放在最后

```sql
1、写法一
    SELECT * FROM `my_member_record` r ORDER BY
    		ISNULL(r.`trade_type`),FIELD(r.`trade_type`,2,3,1,4,5,6,7) 
2、写法二
	SELECT * FROM `my_member_record` r ORDER BY 
	        r.`trade_type` IS NULL,FIELD(r.`trade_type`,2,3,1,4,5,6,7) 
```



### 4、时间段包含查询

```tex
要求：有一个表A，请写出一条SQL语句找出所有根据user_code重复，且open_time与close_time存在时间重叠或者包含的数据，如果close_time为空则默认为关闭时间无穷大。

提示： 表中的数据，第1行与100、第1行与200行虽然user_code重复，但时间不重叠，不应该被查出来；
      100行与200行user_code重复，且open_time与close_time存在重叠；
      3行与101行的存在重叠应该被查出来；
      2行与201行时间存在重叠应该查出来。
```

|  Id  | user_code | …    | open_time  | close_time |
| :--: | --------- | ---- | ---------- | ---------- |
|  1   | A         |      | 2019-01-01 | 2019-12-31 |
|  2   | B         |      | 2019-02-01 |            |
|  3   | C         |      | 2019-02-03 | 2019-12-31 |
|  …   |           |      |            |            |
| 100  | A         |      | 2020-01-01 |            |
| 101  | C         |      | 2018-01-01 | 2020-12-31 |
|  …   |           |      |            |            |
| 200  | A         |      | 2020-06-01 |            |
| 201  | B         |      | 2019-06-01 | 2020-12-31 |
|  …   |           |      |            |            |

```sql
#数据准备
CREATE TABLE `test_user_code` (
  `id` int(11) NOT NULL,
  `user_code` varchar(5) COLLATE utf8_bin DEFAULT NULL,
  `open_time` date DEFAULT NULL,
  `close_time` date DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

insert  into `test_user_code`(`id`,`user_code`,`open_time`,`close_time`) values (1,'A','2019-01-01','2019-12-31'),(2,'B','2019-02-01',NULL),(3,'C','2019-02-03','2019-12-31'),(100,'A','2020-01-01',NULL),(101,'C','2018-01-01','2020-12-31'),(200,'A','2020-06-01',NULL),(201,'B','2019-06-01','2020-12-31');
```

```sql
#方式一
SELECT b.*,a.* 
FROM `test_user_code` a JOIN `test_user_code` b ON a.user_code = b.user_code 
AND( 
 (b.open_time >= a.open_time AND b.`open_time` <= IFNULL(a.close_time, '9999-12-31')) OR
 (b.open_time >= a.open_time AND a.`close_time` IS NULL AND b.`close_time` IS NULL)
)
AND a.`id` <> b.`id`

#方式二
SELECT b.*,a.* 
FROM `test_user_code` a INNER JOIN `test_user_code` b ON b.user_code = a.user_code 
AND b.open_time >= a.open_time AND b.open_time <= IFNULL(a.close_time,CURRENT_TIME)
AND a.id <> b.id 

#
CASE ISNULL(a.close_time)  WHEN 1  THEN CURRENT_TIME ELSE a.close_time END
```



### 5、树形结构查询

```sql
#1、树形结构固定，即固定几层结构，可以采用数据库连接查询，这里以两张表为例
select one.id,
       one.pname,
       two.id,
       two.pname
from test_department one
         inner join test_department two on two.pid = one.id
where one.pid = 2
order by one.id, two.id
```

```sql
#2、树形结构可能变化，采用数据库的递归进行查询

```

```java
//3、Java代码递归查询数据库
    public List<Menu> findMenu(long parentID) {
        QueryWrapper queryWrapper = new QueryWrapper<>();
        //查询条件
        queryWrapper.eq("parentid", parentID);
        //这里传入的值为父节点的id，比如第一次进入网页这里一定是根节点的id 如果点击展开的话就是要展开节点的id 即子节点的父id
 
        List<Menu> list = baseMapper.selectList(queryWrapper);
        for (Menu menu : list) {
        	//递归子类数据
            menu.setChildMenu(findMenu(menu.getID()));
        }
        return list;
    }
```

```java
//一次性全部查询出来，用Java代码实现数据的树形分解
public class TreeDemo{
       public static void main(String[] args) {
        List<TreeEntity> treeList = init();
 
        List<TreeEntity> collect = treeList.stream()
                .filter(item -> item.getPid() == 0)//构造最外层节点，即id=0的节点
                .map(item -> {
                    item.setChildrenList(getChildren(item, treeList));//id=0的节点就为他设置孩子节点
                    return item;
                }).
                collect(Collectors.toList());
        System.out.println(new Gson().toJsonTree(collect));
    }
 
 
 						//获得孩子节点
    private static List<TreeEntity> getChildren(TreeEntity treeEntity, List<TreeEntity> treeEntityList) {
        List<TreeEntity> collect = treeEntityList.stream()
                .filter(item -> item.getPid().equals(treeEntity.getId()))//判断当前节点的父id是不是要设置节点的id
                .map(item -> {
                    item.setChildrenList(getChildren(item, treeEntityList));//如果是 为其设置孩子节点 通过递归 为每个除了最外层节点的节点设置孩子节点
                    return item;
                })
                .collect(Collectors.toList());
        return collect;
    }
 
}
```



### 6、将查询结果插入到另一张表中

```sql
1、insert into target_table select * from srouce_table where 条件;
2、insert into target_table(字段1,字段2...)  select 字段1,字段2... from source_table where..
3、只导入目标表中不存在的记录
    insert into target_table(字段1,字段2...) select 字段1,字段2... from source_table
    where not exists(select * from target_table where target_table.id = source_table.id )
4、select * into target_table from srouce_table where...
5、select 字段1,字段2... into target_table from srouce_table where...
```



### 6、根据已有表创建新表

```sql
1、CREATE TABLE departments1 LIKE departments;
	这种语法，将从源表复制列名、数据类型、大小、非空约束以及索引和主键。而表的内容以及其它约束不会复制，新表是一张空表。
2、create table departments2 as select * from departments
	新表的结构由select列表决定。同时把查询返回的结果集中的行插入到目标表中。
3、CREATE TEMPORARY  TABLE departments3 as  SELECT * FROM departments;
	新表的结构由select列表决定。同时把查询返回的结果集中的行插入到目标表中。
```





### 7、海量数据插入

```sql
-- 创建数据表
create table stu_info (
    id int not null auto_increment,
    first_name varchar(100) null comment '名字',
    last_name varchar(100) null comment '姓氏',
    nation varchar(100) null comment '民族',
    age int null comment '年龄',
    sex varchar(100) null comment '性别',
    iphone varchar(100) null comment '电话',
    birthday varchar(100) null comment '出生年月',
    primary key (id),
    index index_nation_age (nation, age)
) comment='学生信息表';

-- 自定义函数：从给定字符中生成指定长度的字符串
delimiter 
create
    function gen_str(length int, chs varchar(255))
    returns varchar(100)
    comment '从给定字符中生成指定长度的字符串'
    no sql
begin
    declare res varchar(255) default '';
    declare tempStr varchar(255) default '';
    declare i int default 0;

    while i < length do
        set tempStr = substring(chs, floor( rand()*CHAR_LENGTH(chs)+1 ), 1);
        set res = concat(res, tempStr);
        set i = i+1;
    end while;

    return res;
end;
delimiter ;

-- 创建存储过程，用于向表中添加数据
delimiter 
create procedure addStuData(in count int) comment '向stu_info学生信息表中添加数据'
modifies sql data
begin
    declare i int default 0;
    declare names varchar(255) default '长林王发定时要耳根子中奖概率将手动啥沙发为日期望总结注解热额维吾尔人生气哦这里发如今巨峰减少技术接收暗恋拉家带口文旅局将孙楠我年终节点更大';
    declare nations varchar(255) default '汉回藏苗壮满白侗水傣';

    declare my_first_name varchar(100);
    declare my_last_name varchar(100);
    declare my_nation varchar(255);
    declare my_age int;
    declare my_sex varchar(100);
    declare my_iphone varchar(255);
    declare my_birthday varchar(100);

    while i < count do
        set my_first_name = gen_str(rand()*3+1, names);
        set my_last_name = gen_str( 1, names);
        set my_nation = gen_str(1, nations);
        set my_age = ceil( rand()*100 );
        if rand() < 0.5 then
            set my_sex = '男';
        else
            set my_sex = '女';
        end if;
        set my_iphone = cast( ceil(rand()*100000000000) as char );
        set my_birthday = concat( floor(rand()*100+1900), '-', floor(rand()*11+1) );

        insert into stu_info(first_name, last_name, nation, age, sex, iphone, birthday)
            values(my_first_name, my_last_name, my_nation, my_age, my_sex, my_iphone, my_birthday);
        set i = i+1;
    end while;
end
delimiter ;

-- 调用该存储过程，向stu_info学生信息表中添加1500w条数据
call addStuData(1500 * 10000);





CREATE TABLE `my_user` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(255) DEFAULT NULL COMMENT '姓名',
  `create_time` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB COMMENT='用户表';

drop PROCEDURE IF EXISTS insertData;
DELIMITER
create procedure insertData()
begin
 declare i int default 1;
   while i <= 100000 do
         INSERT into my_user (name,create_time) VALUES (CONCAT("name",i), now());
         set i = i + 1; 
   end while; 
end 
DELIMITER ;

call insertData();
```

### 8、插入速度问题

```sql
#插入十万条数据
call addStuData(10 * 10000);
#无任何优化：2分8秒
# set global innodb_flush_log_at_trx_commit = 0; 默认1  25S
# set global sync_binlog = 1000; 默认1    24.4S   
# set session sql_log_bin = 0;  默认1     23.9S
# alter table stu_info DISABLE KEYS;   默认enable  25S
# set global innodb_autoextend_increment = 256; 默认64  23S


核心思想：
    1、尽量使数据库一次性写入Data File
    2、减少数据库的checkpoint次数
    3、程序上尽量缓冲数据，进行批量式插入与提交
    4、减少系统的IO冲突
    
1、修改innodb_flush_log_at_trx_commit参数 (修改my.ini文件也可以)
    set global innodb_flush_log_at_trx_commit = 0; 插入速度会有大幅提高，但Sever断电时有丢失数据风险）
    select @@innodb_flush_log_at_trx_commit;

    1）、当设置为0时，该模式速度最快，但不太安全，mysqld进程的崩溃会导致上一秒钟所有事务数据的丢失。
    2）、当设置为1时，该模式是最安全的，但也是最慢的一种方式。在mysqld 服务崩溃或者服务器主机crash的情况
         下，binary log 只有可能丢失最多一个语句或者一个事务。
    3）、当设置为2时，该模式速度较快，也比0安全，只有在操作系统崩溃或者系统断电的情况下，上一秒钟所有事务数据
         才可能丢失。
 
 
 2、innodb_autoextend_increment 从64M修改为256M （减少tablespace自动扩展次数，避免频繁自动扩展Data File导致 MySQL 的checkpoint 操作）
    SELECT @@innodb_autoextend_increment;
 	set global innodb_autoextend_increment = 256; 
 
 3、innodb_log_buffer_size 从1M修改为16M （根据1秒钟内的事务量情况 适度增大，太大会浪费，因为每1秒钟总会flush一次）
 
 4、innodb_log_file_size 从48M修改为256M （根据服务器内存大小与具体情况设置适合自己环境的值）
 
 5、innodb_log_files_in_group 从2修改为8 (增加Log File数量。此修改主要满足第1、2点)
 
 6、innodb_file_per_table=on & alter table table_name engine=innodb 将大表转变为独立表空并且进行分区，然后将不同分区下挂在多个不同硬盘阵列中，分散IO
 
 7、innodb_write_io_threads & innodb_read_io_threads 从4修改为64 (根据自己的Server CPU核数来更改相应的参数值)
 
 8、innodb_io_capacity & innodb_io_capacity_max 从200修改为10000 (提升 innodb刷脏页的能力，根据自己的的存储IOPS进行对应调整)
 
 384万行数据的插入速度从30小时缩减到了5分20秒，效率得到极大的提升！
 
 9、关闭事务日志
 如果数据的安全性要求不高，可以考虑关闭事务日志功能。关闭事务日志可以减少磁盘IO压力，提高插入速度。但是需要注意的是，关闭事务日志可能会导致数据不可恢复，在使用时需要谨慎操作。关闭事务日志的方法如下
 SET SESSION sql_log_bin = 0;
 
 10、关闭索引维护
 在大批量插入操作之前，可以考虑暂时关闭表的索引，插入完成后再重新开启索引。关闭索引维护可以减少插入操作时的索引更新，从而提高插入速度。关闭索引的方法如下：
 ALTER TABLE my_table DISABLE KEYS;
 -- 执行大批量插入操作
 ALTER TABLE my_table ENABLE KEYS;
 
11、set global sync_binlog = 1000;
    select @@sync_binlog;

https://www.zhihu.com/question/416905226/answer/3041837934
```

### 10、深度分页问题

```sql
-- 1.1、普通分页(其实innodb默认就是按照主键ID列正序排序)
select * from stu_info order by id limit 20 offset 100;  #0.003S

-- 1.2、深分页
select * from stu_info order by id limit 8000000,100;  #5S

-- 2.1、条件查询分页
select * from stu_info where nation='汉' order by id limit 100,20   #0.002S

-- 2.2、条件查询深度分页（即使已经使用到了索引，依然很慢）
select * from stu_info where nation='汉' order by id limit 100000,100; #4S

原理：
    对于分页查询而言，其会先扫描 offset + n 条记录，然后再丢弃掉前offset条记录，最后返回n条记录。故对于深分页场景而言，查询效率会严重下降的原因就在于需要读取大量最终会丢弃掉的前offset条记录。
    即使像上面SQL那样，给nation字段添加、使用了二级索引，也无法显著提高效率。因为其会通过二级索引对 offset+n 条记录全部进行回表，而不仅仅是对n条记录进行回表

优化方案：
# 一、分页游标（推荐）
简单来说就在我们分页查询时，将第1页查询数据作为第2页的查询条件

-- 背景：现在处于第N页： 比如第200000页    3999981 - 4000000 

-- 3.1、查询第N+1页： 4000001  4000020
SELECT * FROM stu_info ORDER BY id LIMIT 4000000,20;  #原始深度分页查询 - 耗时5.6S  
# now_page_max_id 为第N页时查询结果中最大的ID
SELECT * FROM stu_info WHERE id > [now_page_max_id] ORDER BY id LIMIT 20;
SELECT * FROM stu_info WHERE id > 4000000 ORDER BY id LIMIT 20;  #分页游标查询 - 耗时0.001S  

-- 3.2、查询第N-1页： 3999961 - 3999980
SELECT * FROM stu_info ORDER BY id LIMIT 3999960,20;  #原始深度分页查询 - 6S
# now_page_min_id 为第N页时查询结果中最小的ID
SELECT * FROM stu_info WHERE id < [now_page_min_id] ORDER BY id DESC LIMIT 20;
SELECT * FROM stu_info WHERE id < 3999981 ORDER BY id DESC LIMIT 20;#分页游标查询 - 耗时0.001S

SELECT * FROM (   -- 解决查询结果显示的顺序问题
  SELECT * FROM stu_info WHERE id < 3999981 ORDER BY id DESC LIMIT 20
)temp ORDER BY temp.id


注意：
1、该方案适合瀑布流式的业务场景，通过向上、向下滑动来翻页。不适合需要支持随机跳页查询的场景，比如上一次查询第5页数据，这次查询第27页数据
2、该方案下要求表的主键是单调递增。不适合使用UUID等作为主键的表
3、该方案可通过建立二级索引的方式来保障进行业务排序的查询效率。但如果存在需要使用多个业务字段进行排序时，需考虑各种排序字段的组合场景及建立相应索引的成本


#二、子查询
select * from stu_info where age >30 limit 5000000,10; #原始深度分页查询 - 耗时5S

-- 5.1、基于子查询的深分页优化 (方式一)
select * from stu_info 
where id in (
    select id from (
        select id from stu_info 
        where age > 30
        order by id
        limit 5000000,10
    )t  
);    -- 耗时 2.8S

-- 5.2、基于子查询的深分页优化 (方式二)
select * from stu_info where age > 30 and id >= (
    select id from stu_info where age > 30  order by id limit 5000000, 1
) order by id limit 10;



#三、inner join关联查询（延迟关联的深分页优化）
-- 6.1、深度分页
select * from stu_info where age >30 limit 5000000,10; #原始深度分页查询 - 耗时5S

-- 6.2、inner join关联优化
select * from stu_info s
inner join (
   select id from stu_info 
    where age > 30
    order by id
    limit 5000000,10
) as t on s.id = t.id;   -- 耗时 2.8S


-- 7.1、深分页sql
select * from stu_info where nation='汉' order by id limit 250000, 20; -- 耗时9.4S

-- 7.2、inner join关联优化
select * from stu_info s
inner join (
    select id from stu_info where nation='汉' order by id limit 250000, 20
) as t on s.id = t.id;  -- 耗时1.27S 



https://www.zhihu.com/question/432910565
https://zhuanlan.zhihu.com/p/627915190
https://zhuanlan.zhihu.com/p/656097271


CREATE VIEW big_person_company_view AS 
SELECT p.*,c.name AS cname FROM `big_person` p LEFT JOIN big_company c ON p.`id` = c.`id`
WHERE p.`id` < 1000;
```

### 11、多字段数量统计

```sql
SELECT COUNT(IF(`status` = '2', 1, NULL)) toBeConfirmed,
               COUNT(IF(`status` = '3', 1, NULL)) confirmed,
               COUNT(IF(`status` = '4', 1, NULL)) deliveryInProgress
        FROM orders
        
SELECT  COUNT( CASE WHEN STATUS = 2 THEN STATUS END) AS  to_be_confirmed  ,
	COUNT( CASE WHEN STATUS = 3 THEN STATUS END) AS  confirmed ,
	COUNT( CASE WHEN STATUS = 4 THEN STATUS END) AS  delivery_in_progress
FROM orders
```





## 二、索引

#### 2.1 索引介绍 

索引(index)：是帮助数据库高效获取数据的数据结构 。

- 简单来讲，就是使用索引可以提高查询的效率。



测试没有使用索引的查询：

![image-20221209115617429](.\images\image-20221209115617429.png)

添加索引后查询：

~~~mysql
-- 添加索引
create index idx_sku_sn on tb_sku (sn);  #在添加索引时，也需要消耗时间

-- 查询数据（使用了索引）
select * from tb_sku where sn = '100000003145008';
~~~

![image-20221209120107543](.\images\image-20221209120107543.png)



优点：

1. 提高数据查询的效率，降低数据库的IO成本。
2. 通过索引列对数据进行排序，降低数据排序的成本，降低CPU消耗。

缺点：

1. 索引会占用存储空间。
2. 索引大大提高了查询效率，同时却也降低了insert、update、delete的效率。





#### 2.2 索引结构

MySQL数据库支持的索引结构有很多，如：Hash索引、B+Tree索引、Full-Text索引等。

我们平常所说的索引，如果没有特别指明，都是指默认的 B+Tree 结构组织的索引。

在没有了解B+Tree结构前，我们先回顾下之前所学习的树结构：

> 二叉查找树：左边的子节点比父节点小，右边的子节点比父节点大

![image-20221208174135229](.\images\image-20221208174135229.png) 

> 当我们向二叉查找树保存数据时，是按照从大到小(或从小到大)的顺序保存的，此时就会形成一个单向链表，搜索性能会打折扣。

![image-20221208174859866](.\images\image-20221208174859866.png) 

> 可以选择平衡二叉树或者是红黑树来解决上述问题。（红黑树也是一棵平衡的二叉树）

![image-20221209100647867](.\images\image-20221209100647867.png)

> 但是在Mysql数据库中并没有使用二叉搜索数或二叉平衡数或红黑树来作为索引的结构。

思考：采用二叉搜索树或者是红黑树来作为索引的结构有什么问题？

<details>
    <summary>答案</summary>
    最大的问题就是在数据量大的情况下，树的层级比较深，会影响检索速度。因为不管是二叉搜索数还是红黑数，一个节点下面只能有两个子节点。此时在数据量大的情况下，就会造成数的高度比较高，树的高度一旦高了，检索速度就会降低。
</details>




> 说明：如果数据结构是红黑树，那么查询1000万条数据，根据计算树的高度大概是23左右，这样确实比之前的方式快了很多，但是如果高并发访问，那么一个用户有可能需要23次磁盘IO，那么100万用户，那么会造成效率极其低下。所以为了减少红黑树的高度，那么就得增加树的宽度，就是不再像红黑树一样每个节点只能保存一个数据，可以引入另外一种数据结构，一个节点可以保存多个数据，这样宽度就会增加从而降低树的高度。这种数据结构例如BTree就满足。

下面我们来看看B+Tree(多路平衡搜索树)结构中如何避免这个问题：

![image-20221208181315728](.\images\image-20221208181315728.png)

B+Tree结构：

- 每一个节点，可以存储多个key（有n个key，就有n个指针）
- 节点分为：叶子节点、非叶子节点
  - 叶子节点，就是最后一层子节点，所有的数据都存储在叶子节点上
  - 非叶子节点，不是树结构最下面的节点，用于索引数据，存储的的是：key+指针
- 为了提高范围查询效率，叶子节点形成了一个双向链表，便于数据的排序及区间范围查询



> **拓展：**
>
> 非叶子节点都是由key+指针域组成的，一个key占8字节，一个指针占6字节，而一个节点总共容量是16KB，那么可以计算出一个节点可以存储的元素个数：16*1024字节 / (8+6)=1170个元素。
>
> - 查看mysql索引节点大小：show global status like 'innodb_page_size';    -- 节点大小：16384
>
> 当根节点中可以存储1170个元素，那么根据每个元素的地址值又会找到下面的子节点，每个子节点也会存储1170个元素，那么第二层即第二次IO的时候就会找到数据大概是：1170*1170=135W。也就是说B+Tree数据结构中只需要经历两次磁盘IO就可以找到135W条数据。
>
> 对于第二层每个元素有指针，那么会找到第三层，第三层由key+数据组成，假设key+数据总大小是1KB，而每个节点一共能存储16KB，所以一个第三层一个节点大概可以存储16个元素(即16条记录)。那么结合第二层每个元素通过指针域找到第三层的节点，第二层一共是135W个元素，那么第三层总元素大小就是：135W*16结果就是2000W+的元素个数。
>
> 结合上述分析B+Tree有如下优点：
>
> - 千万条数据，B+Tree可以控制在小于等于3的高度
> - 所有的数据都存储在叶子节点上，并且底层已经实现了按照索引进行排序，还可以支持范围查询，叶子节点是一个双向链表，支持从小到大或者从大到小查找

#### 2.3 索引类型

```sql
#索引类型
  主键索引：全表只能有一个，值必须是唯一的，且不能为NULL，
  唯一索引：全表可以有多个，值必须是唯一的，可以为NULL，
  联合索引：全表可以有多个，值不要求唯一，参与的字段有多个，可以为NULL，
  普通索引：全表可以有多个，值不要求唯一，可以为NULL，
  全文索引：该语句指定了索引为FULLTEXT， 用于全文索引（很少用，以后用es代替）
```

#### 2.4 索引语法

**创建索引**

~~~mysql
create  [ unique ]  index 索引名 on  表名 (字段名,... ) ;
~~~

案例：为tb_emp表的name字段建立一个索引

~~~mysql
create index idx_emp_name on tb_emp(name);
~~~

![image-20221209105119159](.\images\image-20221209105119159.png)

> 在创建表时，如果添加了主键和唯一约束，就会默认创建：主键索引、唯一约束
>
> ![image-20221209105846211](.\images\image-20221209105846211.png)



**查看索引**

~~~mysql
show  index  from  表名;
~~~

案例：查询 tb_emp 表的索引信息

~~~mysql
show  index  from  tb_emp;
~~~

![image-20221209110317092](.\images\image-20221209110317092.png)



**删除索引**

~~~mysql
drop  index  索引名  on  表名;
~~~

案例：删除 tb_emp 表中name字段的索引

~~~mysql
drop index idx_emp_name on tb_emp;
~~~



> 注意事项：
>
> - 主键字段，在建表时，会自动创建主键索引
>
> - 添加唯一约束时，数据库实际上会添加唯一索引

```sql
#1、新建索引
ALTER TABLE tb_name ADD PRIMARY KEY(column_list);  #主键索引
ALTER TABLE tb_name ADD UNIQUE index_name(column_list);  #唯一索引
ALTER TABLE tb_name ADD INDEX index_name(column_list);  #普通索引

ALTER TABLE tb_name ADD FULLTEXT index_name(column_list); #全文索引
 
#另外2种创建语法 
#1）、CRATEC方式创建：CREATE INDEX indexName ON tableName(columnName(length));
#2）、建表的时候直接指定
	# 唯一索引：UNIQUE KEY `height` (`height`)
	# 普通索引：KEY `height` (`height`)
	
#2、查询索引
SHOW INDEX FROM table_name;

#3、删除索引(三者)
DROP INDEX index_name ON table_name;
DROP PRIMARY KEY index_name ON table_name;
ALTER TABLE table_name DROP INDEX index_name
```

#### 2.5 索引优化

```sql
0、准备工作（导入百万数据做测试）
    1）、在 [mysqld] 下my.ini文件最后添加'secure_file_priv=',然后services下重启 MySQL 服务器，让选项生效，该选项作用：允许在利用load命令导入任意目录的数据。
    2）、建表
    	CREATE TABLE `big_person` (
              `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
              `NAME` varchar(6) DEFAULT NULL,
              `first_name` varchar(16) DEFAULT NULL,
              `last_name` varchar(16) DEFAULT NULL,
              `married` tinyint(4) DEFAULT NULL,
              `birthday` date DEFAULT NULL,
              `province` varchar(16) DEFAULT NULL,
              `city` varchar(16) DEFAULT NULL,
              `county` varchar(16) DEFAULT NULL,
              `email` varchar(32) DEFAULT NULL,
              `phone` char(11) DEFAULT NULL,
              `zip` char(6) DEFAULT NULL,
              `card` char(18) DEFAULT NULL,
              `avatar` varchar(256) DEFAULT NULL,
              `intro` text,
              PRIMARY KEY (`id`),
              KEY `index_name` (`NAME`),
              KEY `province_city_county_idx` (`province`,`city`,`county`)
        ) ENGINE=InnoDB AUTO_INCREMENT=1000001 DEFAULT CHARSET=utf8mb4
    3）、执行 LOAD DATA INFILE 'D:\\big_person.txt' INTO TABLE big_person; 
       注意实际路径根据情况修改测试表 big_person（此表数据量较大，如果与其它表数据一起提供不好管理，故单独提供），数据行数 100 万条，列个数 15 列。为了更快速导入数据，这里采用了 load data infile 命令配合 *.txt 格式数据
       
1、为合适的列添加索引(主键、唯一索引、组合索引、普通索引)；
    1)、索引字段不宜过大
    2)、查询频繁字段建索引
    3)、区分度要高
    4)、索引不是越多越好
2、尽量建立联合索引，也省空间成本；
3、尽量使用覆盖索引；
4、避免以下会使索引失效的操作
	#0）、建立索引
    #普通索引
    ALTER TABLE big_person DROP INDEX idx_name;												
    ALTER TABLE big_person DROP INDEX idx_first;
    ALTER TABLE big_person DROP INDEX idx_last;
    ALTER TABLE big_person DROP INDEX idx_phone;
    
    #联合索引
    ALTER TABLE big_person DROP INDEX idx_last_first;
    ALTER TABLE big_person DROP INDEX idx_name_lastname_province;
    ALTER TABLE big_person DROP INDEX idx_province_city_county;
    
    #索引初步测试
    EXPLAIN SELECT * FROM big_person WHERE `name` = '戴杰';
    ALTER TABLE big_person ADD INDEX idx_name(`name`);

    EXPLAIN SELECT * FROM `big_person` p WHERE  province = '上海' AND city='宜兰县' AND county='中西区';
    ALTER TABLE big_person ADD INDEX `idx_province_city_county` (province,city,county);

    SELECT `name`,last_name,province FROM big_person WHERE NAME = '唐军';
    SELECT `name`,last_name,province,COUNT(1) AS num
    FROM big_person GROUP BY `name`,last_name,province 
    HAVING num > 1
    ALTER TABLE big_person ADD INDEX `idx_name_lastname_province` (`name`,last_name,province);
    
    #1）、使用is not null不走索引
    EXPLAIN SELECT * FROM big_person p WHERE p.`name` IS NOT NULL;
    EXPLAIN SELECT * FROM big_person p WHERE p.`name` IS NULL;

    #2）、各种负向查询not ，not in， not like ，<> ,!=  不会使用索引
    EXPLAIN SELECT * FROM big_person p WHERE p.`name`  NOT IN ('高洋','卢娟');
    EXPLAIN SELECT * FROM big_person p WHERE p.`name` NOT LIKE '卢%';
    EXPLAIN SELECT * FROM big_person p WHERE p.`name` <> '高洋';
    EXPLAIN SELECT * FROM big_person p WHERE p.`name` != '高洋';

    #3）、like将%放左边不走索引
    ALTER TABLE big_person ADD INDEX idx_first(first_name);
    ALTER TABLE big_person ADD INDEX idx_last(last_name);
    
    EXPLAIN SELECT * FROM big_person WHERE first_name LIKE '%dav' LIMIT 5;
    EXPLAIN SELECT * FROM big_person WHERE last_name LIKE '%dav%' LIMIT 5;
    EXPLAIN SELECT * FROM big_person WHERE last_name LIKE 'dav%' LIMIT 5;
    
    #4）、查询条件的数据类型做了隐式转换(比如varchar类型字段用int去查)
    CREATE INDEX idx_phone ON big_person(phone);
    EXPLAIN SELECT * FROM big_person WHERE phone = 13000013934;
    EXPLAIN SELECT * FROM big_person WHERE phone = '13000013934';

    #5）、使用union代替or，or两侧都是索引列才会走索引，其他情况不会走索引
    EXPLAIN SELECT * FROM big_person  WHERE `name` = '唐军' OR    
      `card` = '330000201903245429';

    EXPLAIN SELECT * FROM big_person WHERE `name` = 'RIMOWA 我的测试数据 - 21寸拉杆箱' 
    UNION   SELECT * FROM big_person WHERE `card` = '330000201903245429';

    #6）、尽量保持索引列干净，不在索引列上使用函数转换、运算
    CREATE INDEX idx_birthday ON big_person(birthday);
    EXPLAIN SELECT * FROM big_person WHERE ADDDATE(birthday,1)='2005-02-10';
    EXPLAIN SELECT * FROM big_person WHERE birthday=ADDDATE('2005-02-10',-1);

    #7）、联合索引要遵循最左匹配原则.
    EXPLAIN SELECT * FROM big_person WHERE province = '上海' AND city='宜兰县' AND county='中西区';
    EXPLAIN SELECT * FROM big_person WHERE county='中西区' AND city='宜兰县' AND province = '上海';
    EXPLAIN SELECT * FROM big_person WHERE city='宜兰县' AND county='中西区';
    EXPLAIN SELECT * FROM big_person WHERE county='中西区';	
    EXPLAIN SELECT * FROM big_person WHERE province = '上海' AND county='中西区';	


    #8）、使用比较运算或between会使联合索引从使用比较运算的下一个索引处断开
     ALTER TABLE tb_item ADD INDEX `idx_brand_price_category` (`brand`,`price`,`category`);   
    EXPLAIN SELECT * FROM tb_item ti WHERE ti.`brand` = 'RIMOWA' AND ti.`price` > 18000 AND ti.`category` = '拉杆箱1' ;

    #9）、多列排序需要用联合索引（如果需要用到多列联合排序就要针对排序字段顺序建立联合索引）
    EXPLAIN SELECT * FROM big_person ORDER BY last_name, first_name LIMIT 10; 

    ALTER TABLE big_person DROP INDEX idx_first;
    ALTER TABLE big_person DROP INDEX idx_last;
    
    CREATE INDEX idx_last_first ON big_person(last_name,first_name);

    #10）、多列排序需要遵循最左前缀原则
    EXPLAIN SELECT * FROM big_person ORDER BY last_name, first_name LIMIT 10; 
    EXPLAIN SELECT * FROM big_person ORDER BY first_name, last_name LIMIT 10; 
    EXPLAIN SELECT * FROM big_person ORDER BY first_name LIMIT 10; 


    #11）、多列排序升降序需要一致
    EXPLAIN SELECT * FROM big_person ORDER BY last_name DESC, first_name DESC LIMIT 10; 
    EXPLAIN SELECT * FROM big_person ORDER BY last_name DESC, first_name ASC LIMIT 10;
    
    
4、避免以下会使索引失效的操作
	#0）、建立索引
    #普通索引
    ALTER TABLE tb_item DROP INDEX idx_name;
    ALTER TABLE tb_item DROP INDEX idx_stock;
    
    #联合索引
    ALTER TABLE tb_item DROP INDEX idx_brand_price_category;

    #索引初步测试
    EXPLAIN SELECT * FROM `tb_item` ti WHERE ti.`name` = 'RIMOWA 我的测试数据 - 21寸拉杆箱';
    ALTER TABLE `tb_item` ADD INDEX idx_name(`name`);
    
    EXPLAIN SELECT * FROM `tb_item` ti WHERE brand = 'RIMOWA' AND category = '拉杆箱' AND price = '18000';
    ALTER TABLE tb_item ADD INDEX `idx_brand_price_category` (`brand`,`price`,`category`);   

    #1）、使用is not null不走索引
    EXPLAIN SELECT * FROM tb_item ti WHERE ti.`name` IS NOT NULL;
    EXPLAIN SELECT * FROM tb_item ti WHERE ti.`name` IS NULL;

    #2）、各种负向查询not ，not in， not like ，<> ,!= ,!> ,!<  不会使用索引
    EXPLAIN SELECT * FROM tb_item ti WHERE ti.`name` NOT IN ('拉杆箱','牛奶');
    EXPLAIN SELECT * FROM tb_item ti WHERE ti.`name` NOT LIKE '拉杆箱%';
    EXPLAIN SELECT * FROM tb_item ti WHERE ti.`name` <> '拉杆箱';
    EXPLAIN SELECT * FROM tb_item ti WHERE ti.`name` != '拉杆箱';

    #3）、like将%放左边不走索引
    EXPLAIN SELECT * FROM tb_item ti WHERE ti.`name` LIKE '%拉杆箱%';

    #4）、查询条件的数据类型做了隐式转换(比如varchar类型字段用int去查)
    EXPLAIN SELECT * FROM tb_item ti WHERE ti.`name` = 1;
    EXPLAIN SELECT * FROM tb_item ti WHERE ti.`name` = '1';

    ALTER TABLE tb_item ADD INDEX idx_stock(stock);
    EXPLAIN SELECT * FROM tb_item ti WHERE ti.`stock` = '1';
    EXPLAIN SELECT * FROM tb_item ti WHERE ti.`stock` = 1;
    
    #5）、使用union代替or，or两侧都是索引列才会走索引，其他情况不会走索引
    EXPLAIN SELECT * FROM tb_item ti WHERE ti.`name` = 'RIMOWA 我的测试数据 - 21寸拉杆箱' OR ti.`isAD` = 1;

    EXPLAIN SELECT * FROM tb_item ti WHERE ti.`name` = 'RIMOWA 我的测试数据 - 21寸拉杆箱' 
    UNION   SELECT * FROM tb_item ti WHERE ti.`isAD` = 1;

    #6）、尽量保持索引列干净，不在索引列上使用函数转换、运算
    EXPLAIN SELECT * FROM tb_item ti WHERE CONCAT(ti.`name`,'666') = 'RIMOWA 我的测试数据 - 21寸拉杆箱666';
    EXPLAIN SELECT * FROM tb_item ti WHERE ti.`name` =  REPLACE('RIMOWA 我的测试数据 - 21寸拉杆箱666','666','');

    #7）、联合索引要遵循最左匹配原则.
    KEY `idx_brand_price_category` (`brand`,`price`,`category`)
        #如建立联合索引（A,B,C）,查询顺序如下：
        #ABC会走索引，AB会走索引，A也会走索引，但是不能断开，如|BC|CB|B|C都不会走索引，AC、CA也会走索引，但是只会走A这部分的索引
    EXPLAIN SELECT * FROM tb_item ti WHERE ti.`price` = 18000 AND ti.`brand` = 'RIMOWA' AND ti.`category` = '拉杆箱' ;
    EXPLAIN SELECT * FROM tb_item ti WHERE ti.`brand` = 'RIMOWA' AND ti.`price` = 18000;
    EXPLAIN SELECT * FROM tb_item ti WHERE ti.`brand` = 'RIMOWA';
    EXPLAIN SELECT * FROM tb_item ti WHERE ti.`category` = '拉杆箱' AND ti.`brand` = 'RIMOWA' ;

    EXPLAIN SELECT * FROM tb_item ti WHERE  ti.`price` = 18000 AND ti.`category` = '拉杆箱';
    EXPLAIN SELECT * FROM tb_item ti WHERE ti.`price` = 18000;
    EXPLAIN SELECT * FROM tb_item ti WHERE ti.`category` = '拉杆箱';

    #8）、使用比较运算或between会使联合索引从使用比较运算的下一个索引处断开
    EXPLAIN SELECT * FROM tb_item ti WHERE ti.`brand` = 'RIMOWA' AND ti.`price` > 18000 AND ti.`category` = '拉杆箱1' ;

    #9）、多列排序需要用联合索引（如果需要用到多列联合排序就要针对排序字段顺序建立联合索引）
    EXPLAIN SELECT * FROM big_person ORDER BY last_name, first_name LIMIT 10; 

    ALTER TABLE big_person DROP INDEX idx_first;
    ALTER TABLE big_person DROP INDEX idx_last;
    
    CREATE INDEX idx_last_first ON big_person(last_name,first_name);


    #10）、多列排序需要遵循最左前缀原则
    EXPLAIN SELECT * FROM big_person ORDER BY last_name, first_name LIMIT 10; 
    EXPLAIN SELECT * FROM big_person ORDER BY first_name, last_name LIMIT 10; 
    EXPLAIN SELECT * FROM big_person ORDER BY first_name LIMIT 10; 


    #11）、多列排序升降序需要一致
    EXPLAIN SELECT * FROM big_person ORDER BY last_name DESC, first_name DESC LIMIT 10; 
    EXPLAIN SELECT * FROM big_person ORDER BY last_name DESC, first_name ASC LIMIT 10;

5、其他
    二级索引覆盖的例子
    explain SELECT * FROM big_person WHERE province = '上海' AND city='宜兰县' AND county= '中西区';
    explain SELECT id,province,city,county FROM big_person WHERE province = '上海' AND city='宜兰县' AND county='中西区';
    表连接需要在连接字段上建立索引
    不要迷信网上说法，具体情况具体分析
    create index first_idx on big_person(first_name);
    explain SELECT * FROM big_person WHERE first_name > 'Jenni';
    explain SELECT * FROM big_person WHERE first_name > 'Willia';
    explain select * from big_person where id = 1 or id = 190839;
    explain select * from big_person where first_name = 'David' or last_name = 'Thomas';
    explain select * from big_person where first_name in ('Mark', 'Kevin','David'); 
    explain select * from big_person where first_name not in ('Mark', 'Kevin','David');
    explain select id from big_person where first_name not in ('Mark', 'Kevin','David');
    以上实验基于 5.7.27，其它如 !=、is null、is not null 是否使用索引都会跟版本、实际数据相关，以优化器结果为准

```



#### 2.6 索引下推  

```sql
1、哪些条件能利用索引
    explain SELECT * FROM big_person WHERE province = '上海';
    explain SELECT * FROM big_person WHERE province = '上海' AND city='嘉兴市';
    explain SELECT * FROM big_person WHERE province = '上海' AND city='嘉兴市' AND county='中西区';
    explain SELECT * FROM big_person WHERE province = '上海' AND county='中西区';

2、原理：MySQL 执行条件判断的时机有两处：引擎层（下）（包括了索引实现）和服务层(上)
	上面第 4 条 SQL 中仅有 province 条件能够利用索引，在引擎层执行，但 county 条件仍然要交给服务层处理
	1）、在 5.6 之前，服务层需要判断所有记录的 county 条件，性能非常低
	     原理举例：从联合索引中拿到所有省为上海的id，比如有5000条数据，再拿这5000个id去主键索引树上逐一回表查询，最后留下country为中西区的数据
	     
	2）、5.6 以后，引擎层会先根据 province 条件过滤，满足条件的记录才在服务层处理county条件，索引条件下推
		原理举例：从联合索引中拿到所有省为上海的索引数据，判断county是不是中西区，如果是，才拿着主键ID去回表查询，否则不会回表查，比如在联合索引信息上查得有5条数据county是中西区，只用拿这5个id去主键索引树上逐一回表查询数据即可。
		SELECT * FROM big_person WHERE province = '上海' AND county='中西区';
		SET optimizer_switch = 'index_condition_pushdown=off';  #关闭索引下推后性能急剧下降
		SET optimizer_switch = 'index_condition_pushdown=on';   #默认开启
```

#### 2.7 explain详解

```sql
1、id列：该列是select的序列号，有几个select就有几个id，id列的值越大语句执行的优先级越高，id相同则是按照执行计划列从上往下执行，id为空则是最后执行。
2、select_type列：
	1）、simple：不包含子查询和union的简单查询
		SELECT * FROM big_person WHERE   city='宜兰县' AND county= '中西区';
	2）、primary：复杂查询中最外层的select
	3）、subquery：包含在select中的子查询（不在from的子句中）
		EXPLAIN  SELECT * FROM gd_member_record WHERE member_id = 
              (SELECT 
                member_id 
              FROM
                `gd_member_account` 
              WHERE `id` = '7355da1e591f4ca086a426a7754f4c2c')
		AND STATUS = 3
	3）、union：union后的select，UNION RESULT为合并的结果
	EXPLAIN 
     SELECT * FROM big_person WHERE province = '上海' AND city='宜兰县' AND county='中西区'
     UNION 
     SELECT * FROM big_person WHERE province = '上海'  AND city='宜兰县' 
	4）、derived：包含在from子句中的子查询。mysql会将查询结果放入一个临时表中，此临时表也叫衍生表。
	
3、table列：  表示当前行访问的是哪张表。
4、partitions列：查询将匹配记录的分区。 对于非分区表，该值为 NULL。
5、type列：
	此列表示关联类型或访问类型。也就是MySQL决定如何查找表中的行。依次从最优到最差分别为：
	system > const > eq_ref > ref > range > index > all。
	1）、system、const：MySQL对查询的某部分进行优化并把其转化成一个常量, system是const的一个特例，表
	    示表里只有一条元组匹配时为system。
	2）、const：一般为主键查询
	2）、eq_ref：一般为唯一索引查询，最多只会返回一条符合条件的记录。
	3）、ref：一般为普通索引或者唯一索引的部分前缀，会找到多个符合条件的行。
	4）、range：通常出现在范围查询中，比如in、between、大于、小于等。使用索引来检索给定范围的行。
	5）、index：扫描全索引拿到结果，一般是扫描某个二级索引，二级索引一般比较少，所以通常比ALL快一点。
	6）、all：全表扫描，扫描聚簇索引的所有叶子节点。
6、possible_keys列：显示在查询中可能被用到的索引。
7、key列：显示MySQL在查询时实际用到的索引。在执行计划中可能出现possible_keys列有值，而key列为null，这种情		     况可能是表中数据不多，MySQL认为索引对当前查询帮助不大而选择了全表查询。如果想强制MySQL使用或忽
          视possible_keys列中的索引，在查询时可使用force index、ignore index。
8、key_len列：显示MySQL在索引里使用的字节数，索引最大长度为768字节，当长度过大时，MySQL会做一个类似最左
             前缀处理，将前半部分字符提取出做索引。
9、ref列：显示key列记录的索引中，表查找值时使用到的列或常量。常见的有const、字段名
10、rows列：此列是MySQL在查询中估计要读取的行数。注意这里不是结果集的行数。
11、filtered：显示本次查询过滤掉数据的百分比。
12、Extra列：此列是一些额外信息。常见的重要值如下：
	 1）、Using index：使用覆盖索引（如果select后面查询的字段都可以从这个索引的树中获取，不需要通过辅助索
	          引树找到主键，再通过主键去主键索引树里获取其它字段值，这种情况一般可以说是用到了覆盖索引）。
	 2）、Using where：使用 where 语句来处理结果，并且查询的列以及查询的条件列未被索引覆盖。
	 3）、Using index condition：索引下推，where条件中索引下推前是一个查询的范围。
     4）、Using temporary：MySQL需要创建一张临时表来处理查询。出现这种情况一般是要进行优化的。
    
     5）、Using filesort：将使用外部排序而不是索引排序，数据较小时从内存排序，否则需要在磁盘完成排序。
     6）、Select tables optimized away：使用某些聚合函数（比如 max、min）来访问存在索引的某个字段时。
```



## 三、SQL执行过程

![image-20230105201549053](.\images\image-20230105201549053.png)

```sql
1、连接器：负责建立连接、检查权限、连接超时时间由 wait_timeout 控制，默认 8 小时
2、查询缓存：会将 SQL 和查询结果以键值对方式进行缓存，修改操作会以【表单位】导致缓存失效
3、分析器：词法、语法分析
4、优化器：决定用哪个索引，决定表的连接顺序等
5、执行器：根据存储引擎类型，调用存储引擎接口
6、存储引擎：数据的读写接口，索引、表都在此层实现
```

​		SQL解析顺序

```sql
SELECT DISTINCT
    < select_list >
FROM
    < left_table > < join_type >
JOIN < right_table > ON (a.id = b.aid and b.status =3)
WHERE
    < where_condition >
GROUP BY
    < group_by_list >
HAVING
    < having_condition >
ORDER BY
    < order_by_condition >
LIMIT < limit_number >

1 FROM <left_table>
 2 ON <join_condition>
 3 <join_type> JOIN <right_table>
 4 WHERE <where_condition>
 5 GROUP BY <group_by_list>
 6 HAVING <having_condition>
 7 SELECT 
 8 DISTINCT <select_list>
 9 ORDER BY <order_by_condition>
10 LIMIT <limit_number>
```





## 四、事务测试

0、知识储备

```sql
--一、查看mysql数据库版本
	select version();
--二、查看数据库隔离级别
    1、查看系统隔离级别：	
    	1）、5.0以上版本
    		select @@global.tx_isolation;
    	2）、8.0以上版本
    		select @@global.transaction_isolation;
    2、查看会话隔离级别：
    	1)、5.0以上版本
    		select @@tx_isolation;  
    		select @@session.tx_isolation;
    		show variables like '%tx_isolation%';
    	2）、8.0以上版本
    		select @@transaction_isolation;
    		select @@session.transaction_isolation;
    		show variables like '%transaction_isolation%';
    		
--三、修改mysql中的事务隔离级别：
	1、设置会话隔离级别
		set session transaction isolation level read uncommitted; 设置会话隔离级别为读未提交
		set session transaction isolation level read committed; 设置会话隔离级别为读已提交
		set session transaction isolation level repeatable read; 设置会话隔离级别为可重复读
		set session transaction isolation level serializable; 设置会话隔离级别为串行化
		以下设置方式也可以：
		set tx_isolation='read uncommitted';
		
	2、设置系统隔离级别
		set global transaction isolation level read uncommitted; 设置会话隔离级别为读未提交
		set global transaction isolation level read committed; 设置会话隔离级别为读已提交
		set global transaction isolation level repeatable read; 设置会话隔离级别为可重复读
	
	注意：
    	1、SESSION：表示修改的事务隔离级别将应用于当前 session（当前 cmd 窗口）内的所有事务；
		2、GLOBAL：表示修改的事务隔离级别将应用于所有 session（全局）中的所有事务，且当前已经存在的 
				session 不受影响；
		3、如果省略 SESSION 和 GLOBAL，表示修改的事务隔离级别将应用于当前 session 内的下一个还未开始的
		   事务。
		4、任何用户都能改变会话的事务隔离级别，但是只有拥有 SUPER 权限的用户才能改变全局的事务隔离级别。
		
--四、查看当前事务是否是自动提交 1表示开启，0表示关闭
	select @@autocommit;
--五、设置关闭自动提交事务
	set autocommit = 0;
```

1、 关闭自动提交事务

```sql
set autocommit = 0;
```

2、数据准备

```sql
--准备数据
 	create table person(id int primary key,name varchar(10)) engine=innodb;
--插入数据
    insert into person values(1,'zhangsan');
    insert into person values(2,'lisi');
    insert into person values(3,'wangwu');
	commit;
```

3、事务隔离级别

```sql
--事务包含四个隔离级别：从上往下，隔离级别越来越高，意味着数据越来越安全
read uncommitted; 	--读未提交
read commited;		--读已提交
repeatable read;	--可重复读
seariable			--序列化执行，串行执行
--产生数据不一致的情况：
脏读
不可重复读
幻读
```

|              |              |            |      |
| ------------ | ------------ | ---------- | ---- |
| **隔离级别** | **异常情况** |            |      |
| 读未提交     | 脏读         | 不可重复读 | 幻读 |
| 读已提交     |              | 不可重复读 | 幻读 |
| 可重复读     |              |            | 幻读 |
| 序列化       |              |            |      |



4、测试1：当使用【read uncommitted】的时候，会出现【脏读】问题

```sql
set session transaction isolation level read uncommitted;
A:	start transaction;
A:	select * from person;

B:	start transaction;
B:	select * from person;

A:	update person set name='111' where id = 1;
A:	select * from person;

B:	select * from person;  --读取的结果111。产生脏读，因为A事务并没有commit，读取到了不存在的数据

A:	commit;

B:	select * from person; --读取的数据是111,因为A事务已经commit，数据永久的被修改

```

5、测试2：当使用【read committed】的时候，就不会出现【脏读】的情况了，但是会出现【不可重复读】的问题

```sql
set session transaction isolation level read committed;
A:start transaction;
A:select * from person;

B:start transaction;
B:select * from person;

--执行到此处的时候，两个窗口读取的数据是一致的

A:update person set name ='222' where id = 2;
A:select * from person;

B:select * from person;

A:commit;

B:select * from person; --读取到B更新的数据，数据跟之前读取的不一致，不可重复读
                      	--同一个事务中多次读取数据出现不一致的情况
```

6、测试3：当使用【repeatable read】的时候(按照上面的步骤操作)，就不会出现【不可重复读】的问题，但是会出现    【幻读】的问题

```sql
set session transaction isolation level repeatable read;
A:start transaction;
A:select * from person;

B:start transaction;
B:insert into person values(4,'44');
B:commit;

A:select * from person; --读取不到添加的数据
A:insert into person values(4,'44');--报错，无法插入数据
--此时发现读取不到数据，但是在插入的时候不允许插入，出现了幻读，设置更高级别的隔离级别即可解决


--幻读的另外两个现象
--场景1
A:start transaction;
A:select * from person; 

B:start transaction;
B:insert into person values(5,'55');
B:commit;

A: select * from person ; -- 看不到B事务提交的5号数据
A: update person set name = '55' where id = 5; -- 看不见5号数据，但是能更新，更新会重新建立快照
A: select * from person; -- 看到了B事务提交的5号数据，出现了幻读

--场景2
A:start transaction;
A:select * from person where id > 1;

B:start transaction;
B:insert into person values(6,'66');
B:update person set name = '33' where id = 3;
B:commit;

A:select * from person where id> 1 for update; -- 看到了B提交的新增和修改操作，出现了幻读
```

7、测试4：当使用【serializable】的时候，不会出现【幻读】问题，因为这个隔离级别的所有的读写、写写操作都会【锁表】

```sql
set session transaction isolation level serializable;
```

## 五、几个不同

​	   1、exists和in的区别？

```sql
示例：AB两表，两表都有一个id字段，两个表都为id字段建立了索引
1、in 
	select * from A where id in (select id from B)
    这句sql等价于两个循环：
    	for select id from B
	    for select * from A where A.id = id
	    
	 List<String> result = new Arraylist<>();   
	 for(String bid:bids){
	 	for(String aid:aids){
	 		if(bid = aid){
	 			result.add(aid);
	 		}
	 	}
	 }   
	原理：对B表的id做个外层循环，而内层再嵌套一层A表的id循环，内层循环里判断A表和B表的id是否相等，相等的话
	     就是要返回的数据。
	      
2、exists
	select * from A where exists(select * from B where B.id = A.id)
	这句sql等价于两个循环：
	for select * from A
	for select * from B where B.id = A.id
   原理：对A表的id做个外层循环，而内层再嵌套一层B表的id循环，内层循环里判断B表和A表的id是否相等，相等的话
        就是要返回的数据。
        
3、推理
	1、sql优化规则：以小表驱动大表，mysql连接数会更少，sql性能会更佳
	2、用in时，是in里面的表驱动外面的表，所以如果B表相对于A表是小表，用in比较好。
	   而用exists时，是外面的表驱动exists里面的表，所以如果A表相对于B表是小表，则用exists比较好。
	   
4、结论
	​IN​​​适合于外表大而内表小的情况，而​​EXISTS​​适合于外表小而内表大的情况。
	 涉及表关联查询时可以用EXISTS代替IN，但是很多时候是没办法替代的，比如：
	 select * from A where id in (1,2,3)
```



​		2、count(1)、count(列名)、count(*) 的区别？

```sql
1、执行效果上：
	1）、count(*)包括了所有的列，相当于行数，在统计结果的时候，不会忽略为NULL的值。
    2）、count(1)忽略所有列，用1代表行，在统计结果的时候，不会忽略为NULL的值。
    3）、count(列名)只包括列名那一列，在统计结果的时候，会忽略列值为空（这里的空不是指空字符串或者0，而是
        表示null）的计数，即某个字段值为NULL时，不统计。
2、执行效率上
	1）、列名为主键，count(列名)会比count(1)快
    2）、列名不为主键，count(1)会比count(列名)快
    3）、如果表有多个列并且没有主键，则 count(1) 的执行效率优于 count（*）
    4）、如果有主键，则 select count（主键）的执行效率是最优的
    5）、如果表只有一个字段，则 select count（*）最优。
```



​		3、where 和 on的区别

```sql
1、在多表查询时，on和where都表示筛选条件，on先执行，where后执行。
2、on 后面跟两表的连接条件，然后再加的筛选条件只针对关联表（从表）
3、where 则针对连接后产生的临时表进行筛选执行顺序：先连接再筛选
4、执行顺序：从表按照条件筛选，然后再进行连接；即先筛选再连接

5、on条件是在生成临时表时使用的条件，它不管on中的条件是否为真，都会返回左边表中的记录。而where条件是在临时
   表生成好后，再对临时表进行过滤的条件。
6、on 后跟关联表（从表）的过滤条件，where 后跟主表或临时表的筛选条件（左连接为例，主表的数据都会查询到，所
   以临时表中必定包含主表所有的字段，需要给主表加什么筛选条件，直接给临时表加效果相同）
7、所有的连接条件都必须放在 ON 后面，否则无论是 LEFT 还是 RIGHT 连接都将不起作用，而where 作用的是连接后
   的临时表，与连接已经无关）
例（mytest库）:
#ON条件是在合并两张表形成临时表【前】进行的条件筛选（左表所有数据会查询出来，右表只会有符合条件的数据）
SELECT * FROM `tb_user` u LEFT JOIN `tb_order`  o 
ON u.`id` = o.`userId` AND u.`username` = '华沉鱼';

uid  uname     oid  ono
u1   张三    	  o1   001
u2   李四    	 null  null
u3   华沉鱼     o2   002
u4   王五       o3   003

#WHERE是在临时表创建成功后，再次对临时表进行筛选（左右表都只会展示符合条件的数据）
SELECT * FROM `tb_user` u LEFT JOIN `tb_order`  o 
ON u.`id` = o.`userId` WHERE  u.`username` = '华沉鱼';

#内连接，只展示符合条件的数据
SELECT * FROM `tb_user` u INNER JOIN `tb_order`  o 
ON u.`id` = o.`userId` AND u.`username` = '华沉鱼';

#内连接，只展示符合条件的数据
SELECT * FROM `tb_user` u,`tb_order`  o 
WHERE u.`id` = o.`userId` AND u.`username` = '华沉鱼';




```

4、union和union all的区别

```tex
union会对所有查询的列进行合并去重和进行默认排序，而union all不会进行去重和排序
```



## 六、当前读与快照读

```sql
1、当前读，即读取最新提交的数据
	1）、select … for update
	2）、insert、update、delete，都会按最新提交的数据进行操作
	
2、快照读，读取某一个快照建立时（可以理解为某一时间点）的数据
	快照读主要体现在 select 时，不同隔离级别下，select 的行为不同
	1）、在 Serializable 隔离级别下 - 普通 select 也变成当前读
	2）、在 RC 隔离级别下 - 每次 select 都会建立新的快照，【效果等同于当前读】
	3）、在 RR 隔离级别下
		1、事务启动后，首次 select 会建立快照
		2、如果事务启动选择了 with consistent snapshot，事务启动时就建立快照
			start transaction with consistent snapshot
		3、基于旧数据（别的事务新增的数据）的修改操作，会重新建立快照
```

![image-20230105183458707](.\images\image-20230105183458707.png)			

```sql
答案：B
解析：
	1、A客户端 start transaction 手动开启事务1，默认隔离级别为RR
	2、B客户端通过update修改了id为1的姓名为：‘张四’，并且自动提交了事务
	3、A客户端的事务1 开始执行第一个select 语句，此时这个select为快照读，读取的结果是最新结果：‘张四’，
	  并且此时开始建立快照；
	4、B客户端通过insert语句插入id为2的新数据并且提交事务
	5、A客户端执行select count 查询，由于从上一次A客户端select时开启了快照，所以此次读取查询不到客户端B
	   新增的数据，所以结果为1，但是如果此时客户端A执行插入id为2的数据，会报重复的主键错误，即：幻读。
```

## 七、日志

### 7.1、undolog

```sql
undolog的作用有两个：
1、回滚数据，以行为单位，记录数据每次的变更，一行记录有多个版本并存
2、多版本并发控制，即快照读（也称为一致性读），让查询操作可以去访问历史版本
```

![image-20230105202719447](.\images\image-20230105202719447.png)

```tex
1、开启 id为100的事务，开启快照
2、在id为101、102的事务中分别做两次余额更新
3、在100事务中去做快照读，会根据trx id去找，一直会找到比100更小的那次记录
```

### 7.2、redolog

```tex
redo log 的作用主要是实现 ACID 中的持久性，保证提交的数据不丢失
1、它记录了事务提交的变更操作，服务器意外宕机重启时，利用 redo log 进行回放，重新执行已提交的变更操作
2、事务提交时，首先将变更写入 redo log，事务就视为成功。至于数据页（表、索引）上的变更，可以放在后面慢慢做
   1）、数据页上的变更宕机丢失也没事，因为 redo log 里已经记录了
   2）、数据页在磁盘上位置随机，写入速度慢，redo log 的写入是顺序的速度快
3、它由两部分组成，内存中的 redo log buffer，磁盘上的 redo log file
   1）、redo log file 由一组文件组成，当写满了会循环覆盖较旧的日志，这意味着不能无限依赖 redo log，更早的数据恢复需要 binlog
   2）、buffer 和 file 两部分组成意味着，写入了文件才真正安全，同步策略由下面的参数控制
   3）、innodb_flush_log_at_trx_commit 
        0 - 每隔 1s 将日志 write and flush 到磁盘 
        1 - 每次事务提交将日志 write and flush（默认值）
        2 - 每次事务提交将日志 write，每隔 1s flush 到磁盘，意味着 write 意味着写入操作系统缓存，如果 MySQL 挂了，而操作系统没挂，那么数据不会丢失 
```

## 八、mysql多表查询练习

```sql
-- 1、查询学生表和老师表的笛卡尔积（注意：开发中，一定要避免出现笛卡尔积。）
SELECT * FROM student s,teacher t;

-- 2、查询所有任教中的老师的教师号、姓名以及对应教的课程
SELECT t.`id`,t.`name`,c.`name` FROM teacher t,course c WHERE t.`id` = c.`t_id`;

-- 3、查询比王小虎年龄大的学生所有信息（标量子查询）
SELECT * FROM student WHERE age > (
	SELECT age FROM student WHERE `name` = '王小虎'
);

-- 4、查询至少有一门学科分数大于60分的学生所有信息（列子查询）
SELECT * FROM student WHERE id IN(
	SELECT DISTINCT(s.`s_id`) FROM scores s WHERE s.`score` > 60;
)

-- 5、查询男生中年龄最大的学生所有信息（行子查询）
SELECT * FROM student s WHERE (s.`gender`,s.`age`) = 
(SELECT gender,MAX(age) FROM student WHERE s.`gender` = '男');
 
-- 6、查询数学成绩排名前2的学生所有信息，按分数倒序排列 
-- 前两名 (如有2个100,2个90，取的是前两个100)
SELECT s.*,sc.`score` FROM scores sc,course c,student s 
WHERE sc.`c_id` = c.`id`
AND c.`name` = '数学'
AND sc.`s_id` = s.`id`
ORDER BY sc.`score` DESC LIMIT 2;
-- 真正的前两名 (如有2个100,2个90，取的是这四条信息)
SELECT s.*,sc.`score` FROM student s,`scores` sc,( 
	SELECT DISTINCT sc.`score`,c.`id` FROM scores sc,course c
	WHERE sc.`c_id` = c.`id`
	AND c.`name` = '数学'
	ORDER BY sc.`score` DESC LIMIT 2
  )AS temp 
WHERE sc.`c_id` = temp.id 
AND sc.`score` = temp.score
AND s.`id` = sc.`s_id`
ORDER BY sc.`score` DESC;

-- 7、查询每个老师的代客数量，列出老师的ID、姓名、代课数
SELECT t.`id`,t.`name`,COUNT(c.`t_id`) AS cnum FROM teacher t LEFT JOIN course c ON t.`id` = c.`t_id`
GROUP BY t.id,t.name;
         
-- 8、查询“4”号学生的姓名和各科成绩
SELECT 
       s.`name` sname,
       c.`name` cname,
       sc.score
FROM student s
         LEFT JOIN scores sc ON s.id = sc.s_id
         LEFT JOIN course c ON c.id = sc.c_id
WHERE s.id = 4;

SELECT 
  (SELECT 
    NAME 
  FROM
    student 
  WHERE id = 4) AS `name`,
  c.`name` AS cname,
  IFNULL(sc.`score`,'缺考')  AS score 
FROM
  course c 
  LEFT JOIN scores sc 
    ON sc.`c_id` = c.`id` 
    AND sc.`s_id` = 4;

-- 9、查询各个学科的平均成绩和最高成绩、最低成绩
SELECT c.`name`,MAX(sc.`score`),MIN(sc.`score`) FROM course c LEFT JOIN scores sc 
ON sc.`c_id` = c.`id`
GROUP BY c.id,c.`name`;
         
-- 10、查询每个同学的最高成绩和科目名称
select temp.name,cc.`name` as cname,c.`score` from scores c,(
	select s.`id`,s.`name`,max(sc.`score`) as score from scores sc,student s
	where sc.`s_id` = s.`id`
	group by sc.`s_id`)temp,course cc
where c.`s_id` = temp.id
and c.`score` = temp.score 
and c.`c_id` = cc.`id`;
         
-- 11、查询每个课程的最高分的学生姓名、课程名称、分数
SELECT s.`name`,c.`name`,ss.`score`  FROM scores ss,(
  SELECT sc.`c_id`,MAX(sc.`score`) AS score FROM scores sc GROUP BY sc.`c_id`
)temp,student s,course c WHERE ss.`c_id` = temp.c_id
AND ss.`score` = temp.score
AND s.`id` = ss.`s_id`
AND c.`id` = ss.`c_id`
ORDER BY c.`name`;

-- 12、查询平均成绩大于70的同学的姓名以及平均分数。
SELECT t.`name`,temp.avg_score FROM student t,
(SELECT sc.s_id,AVG(sc.`score`) AS avg_score FROM scores sc GROUP BY sc.`s_id` HAVING avg_score > 70)temp
WHERE t.`id` = temp.s_id;

-- 13、将学生按照总分数进行排名。（从高到低）
SELECT s.id,
       s.NAME,
       SUM(sc.score) score
FROM student s
         LEFT JOIN scores sc ON s.id = sc.s_id
GROUP BY s.id,
         s.NAME
ORDER BY score DESC,
         s.id ASC;
        
-- 14、查询数学成绩的最高分、最低分、平均分。
SELECT c.`name`,MAX(sc.score),MIN(sc.score),AVG(sc.score) 
FROM scores sc,course c WHERE sc.`c_id` = c.`id`
AND c.`name` = '数学';

SELECT  c.`name`,MAX(sc.score),MIN(sc.score),AVG(sc.score) 
FROM
  scores sc,course c 
WHERE sc.`c_id` = c.`id` 
GROUP BY c.`id`,c.`name` 
HAVING c.`name` = '数学'; 

-- 15、将各科目按照平均降序排序，列出科目名称和平均分。
select 
  c.`name`,
  avg(sc.`score`) as avg_score 
from
  scores sc,course c
where sc.`c_id` = c.`id`
group by c.`name` 
order by avg_score desc; 

-- 16、查询老师的信息和他所带的科目的平均分
SELECT t.`name`,c.`name`,AVG(sc.`score`) FROM teacher t
LEFT JOIN course c ON t.`id` = c.`t_id`
LEFT JOIN scores sc ON  sc.`c_id` = c.`id` 
GROUP BY t.id,t.name,c.`id`,c.`name`;
         
-- 17、查询"Tom"和"Jerry"两位老师所教课程中的学生最高分和最低分
SELECT t.name,c.name cname,MAX(r.score),
       MIN(r.score)
FROM teacher t
         LEFT JOIN course c ON t.id = c.t_id
         LEFT JOIN scores r ON r.c_id = c.id
GROUP BY t.id,t.name,c.id,c.name
HAVING t.name IN ('Tom', 'Jerry');

-- 18、查询每个学生的最好成绩的科目名称（子查询）
SELECT t.`name`,t.id,c.`name`,s.`score` FROM scores s,
	(SELECT sc.`s_id`,MAX(sc.`score`) AS max_score FROM `scores` sc GROUP BY sc.`s_id`) temp,
course c,student t 
WHERE s.`score` = temp.max_score
AND s.`s_id` = temp.s_id
AND c.`id` = s.`c_id`
AND s.`s_id` = t.`id`
         
-- 19、查询所有学生的课程及分数
SELECT s.NAME,c.NAME,r.score
FROM student s
         LEFT JOIN scores r ON s.id = r.s_id
         LEFT JOIN course c ON c.id = r.c_id;
         
-- 20、查询课程编号为1且课程成绩在60分以上的学生的学号和姓名（子查询）
SELECT s.id,s.name,r.score FROM student s LEFT JOIN scores r ON s.id = r.s_id
WHERE r.c_id = 1
  AND r.score > 60
  
-- 21、查询平均成绩大于等于70的所有学生姓名和平均成绩
select s.name,temp.avg_score from student s  inner join 
	(select sc.`s_id`,AVG(sc.`score`) AS avg_score from `scores` sc  
		group by sc.`s_id` having avg_score > 70) temp
ON temp.`s_id` = s.`id`;
	
-- 22、查询有不及格课程的学生信息
SELECT * FROM student s 
WHERE id IN ( SELECT r.s_id FROM scores r GROUP BY r.s_id HAVING min( r.score ) < 60 );
	
-- 23、查询每门课程有成绩的学生人数
SELECT
	c.id,
	c.NAME,
	count(*) 
FROM
	course c
	LEFT JOIN scores r ON c.id = r.c_id 
GROUP BY
	c.id,
	c.NAME;
	
-- 24、查询每门课程的平均成绩，结果按照平均成绩降序排列，如果平均成绩相同，再按照课程编号升序排列
SELECT
	c.id,
	c.NAME,
	avg( score ) score 
FROM
	course c
	LEFT JOIN scores r ON c.id = r.c_id 
GROUP BY
	c.id,
	c.NAME 
ORDER BY
	score DESC,
	c.id ASC;
	
-- 25、查询平均成绩大于60分的同学的学生编号和学生姓名和平均成绩
SELECT
	s.id,
	s.NAME sname,
	avg( r.score ) score 
FROM
	student s
	LEFT JOIN scores r ON r.s_id = s.id
	LEFT JOIN course c ON c.id = r.c_id 
GROUP BY
	s.id,
	s.NAME 
HAVING
	score > 65;
	
-- 26、查询有且仅有一门课程成绩在80分以上的学生信息
SELECT
	s.id,
	s.NAME,
	s.gender 
FROM
	student s
	LEFT JOIN scores r ON s.id = r.s_id 
WHERE
	r.score > 80 
GROUP BY
	s.id,
	s.NAME,
	s.gender 
HAVING
	count(*) = 1;
	
-- 27、查询出只有三门课程的学生的学号和姓名
SELECT s.id, s.NAME,s.gender FROM student s
	 LEFT JOIN scores r ON s.id = r.s_id 
GROUP BY s.id,s.NAME,s.gender 
HAVING count(*) = 3;
	
-- 28、查询有不及格课程的课程信息
SELECT * FROM course c WHERE id IN (
	SELECT
		r.c_id 
	FROM
		scores r 
	GROUP BY
		r.c_id 
	HAVING
	min( r.score ) < 60 
);
	
-- 29、查询至少选择4门课程的学生信息
SELECT s.id,s.NAME FROM student s
	LEFT JOIN scores r ON s.id = r.s_id 
GROUP BY
	s.id,
	s.NAME 
HAVING count(*) >= 4;
	
-- 30、查询没有选全所有课程的同学的信息
SELECT * FROM student 
WHERE id IN (
	SELECT
		r.s_id 
	FROM
		scores r 
	GROUP BY
		r.s_id 
	HAVING
	count(*) != 5
);
	
-- 31、查询选全所有课程的同学的信息 
SELECT
	s.id,
	s.NAME,
	count(*) number 
FROM
	student s
	LEFT JOIN scores r ON s.id = r.s_id 
GROUP BY
	s.id,
	s.NAME 
HAVING
	number = ( SELECT count(*) FROM course );
	
-- 32、查询各学生都选了多少门课
SELECT
	s.id,
	s.NAME,
	count(*) number 
FROM
	student s
	LEFT JOIN scores r ON s.id = r.s_id 
GROUP BY
	s.id,
	s.NAME;
	
-- 33、查询课程名称为"java"，且分数低于60分的学生姓名和分数
SELECT
	s.id,
	s.NAME,
	r.score 
FROM
	student s
	LEFT JOIN scores r ON s.id = r.s_id
	LEFT JOIN course c ON r.c_id = c.id 
WHERE
	c.NAME = 'java' 
	AND r.score < 60;
	
-- 34、查询学过"Tony"老师授课的同学的信息
SELECT
	s.id,
	s.NAME 
FROM
	student s
	LEFT JOIN scores r ON r.s_id = s.id
	LEFT JOIN course c ON c.id = r.c_id
	LEFT JOIN teacher t ON t.id = c.t_id 
WHERE
	t.NAME = 'Tom';
	
-- 35、查询没学过"Tony"老师授课的学生信息
SELECT * FROM student WHERE id NOT IN (
	SELECT DISTINCT
		s.id 
	FROM
		student s
		LEFT JOIN scores r ON r.s_id = s.id
		LEFT JOIN course c ON c.id = r.c_id
		LEFT JOIN teacher t ON t.id = c.t_id 
	WHERE
	t.NAME = 'Tom' 
)
```

```sql
#0）、建立索引
    #普通索引
    ALTER TABLE `tb_item` ADD INDEX idx_name (`name`);
    ALTER TABLE big_person ADD INDEX idx_name(`name`)

    ALTER TABLE tb_item DROP INDEX idx_name;
    ALTER TABLE big_person DROP INDEX idx_name;

    SELECT * FROM `tb_item` WHERE `name` = 'RIMOWA 我的测试数据 - 21寸拉杆箱';
    SELECT * FROM big_person WHERE `name` = '戴杰';
		
		
    #联合索引
    ALTER TABLE tb_item ADD INDEX `idx_brand_price_category` (`brand`,`price`,`category`);
    ALTER TABLE big_person ADD INDEX `idx_province_city_county` (province,city,county);
    ALTER TABLE big_person ADD INDEX `idx_name_lastname_province` (`name`,last_name,province);

    ALTER TABLE tb_item DROP INDEX idx_brand_price_category;
    ALTER TABLE big_person DROP INDEX idx_province_city_county;
    ALTER TABLE big_person DROP INDEX idx_name_lastname_province;

    SELECT * FROM `tb_item` ti WHERE brand = 'RIMOWA' AND category = '拉杆箱' AND price = '18000';
    
    SELECT * FROM `big_person` p WHERE  province = '上海' AND city='宜兰县' AND county='中西区';

    SELECT `name`,last_name,province FROM big_person WHERE NAME = '唐军';
    
    SELECT `name`,last_name,province,COUNT(1) AS num
    FROM big_person GROUP BY `name`,last_name,province 
    HAVING num > 1


    #1）、使用is not null不走索引
    EXPLAIN SELECT * FROM tb_item  WHERE `name` IS NOT NULL;
    EXPLAIN SELECT * FROM tb_item  WHERE `name` IS NULL;


    #2）、各种负向查询not ，not in not like，<> ,!= ,!> ,!<  不会使用索引
    EXPLAIN SELECT * FROM tb_item  WHERE `name` NOT IN ('拉杆箱','牛奶');
    EXPLAIN SELECT * FROM tb_item  WHERE `name` NOT LIKE '拉杆箱%';
    EXPLAIN SELECT * FROM tb_item  WHERE `name` <> '拉杆箱';
    EXPLAIN SELECT * FROM tb_item  WHERE `name` != '拉杆箱';


    #3）、like将%放左边不走索引
    EXPLAIN SELECT * FROM tb_item WHERE .`name` LIKE '%拉杆箱%';

    EXPLAIN SELECT * FROM big_person WHERE `name` LIKE '%丁';
    EXPLAIN SELECT * FROM big_person WHERE `name` LIKE '%丁%';
    EXPLAIN SELECT * FROM big_person WHERE `name` LIKE '丁%';

    #4）、查询条件的数据类型做了隐式转换(比如varchar类型字段用int去查)
    EXPLAIN SELECT * FROM tb_item ti WHERE ti.`name` = 1;
    EXPLAIN SELECT * FROM tb_item ti WHERE ti.`name` = '1';

    ALTER TABLE tb_item ADD INDEX idx_stock(stock);
    EXPLAIN SELECT * FROM tb_item ti WHERE ti.`stock` = '1';
    EXPLAIN SELECT * FROM tb_item ti WHERE ti.`stock` = 1;

    CREATE INDEX idx_phone ON big_person(phone);
    EXPLAIN SELECT * FROM big_person WHERE phone = 13000013934;
    EXPLAIN SELECT * FROM big_person WHERE phone = '13000013934';


    #5）、使用union代替or，or两侧都是索引列才会走索引，其他情况不会走索引
    EXPLAIN SELECT * FROM tb_item ti WHERE ti.`name` = 'RIMOWA 我的测试数据 - 21寸拉杆箱' OR ti.`isAD` = 1;

    EXPLAIN SELECT * FROM tb_item ti WHERE ti.`name` = 'RIMOWA 我的测试数据 - 21寸拉杆箱' 
    UNION   SELECT * FROM tb_item ti WHERE ti.`isAD` = 1;


    #6）、尽量保持索引列干净，不在索引列上使用函数转换、运算
    EXPLAIN SELECT * FROM tb_item ti WHERE CONCAT(ti.`name`,'666') = 'RIMOWA 我的测试数据 - 21寸拉杆箱666';
    EXPLAIN SELECT * FROM tb_item ti WHERE ti.`name` =  REPLACE('RIMOWA 我的测试数据 - 21寸拉杆箱666','666','');

    CREATE INDEX idx_birthday ON big_person(birthday);
    EXPLAIN SELECT * FROM big_person WHERE ADDDATE(birthday,1)='2005-02-10';
    EXPLAIN SELECT * FROM big_person WHERE birthday=ADDDATE('2005-02-10',-1);


    #7）、联合索引要遵循最左匹配原则.
    KEY `index_brand_price_category` (`brand`,`price`,`category`)
        #如建立联合索引（A,B,C）,查询顺序如下：
        #ABC会走索引，AB会走索引，A也会走索引，但是不能断开，如|BC|CB|B|C都不会走索引，AC、CA也会走索引，但是只会走A这部分的索引
    EXPLAIN SELECT * FROM tb_item ti WHERE ti.`price` = 18000 AND ti.`brand` = 'RIMOWA' AND ti.`category` = '拉杆箱' ;
    EXPLAIN SELECT * FROM tb_item ti WHERE ti.`brand` = 'RIMOWA' AND ti.`price` = 18000;
    EXPLAIN SELECT * FROM tb_item ti WHERE ti.`brand` = 'RIMOWA';
    EXPLAIN SELECT * FROM tb_item ti WHERE ti.`category` = '拉杆箱' AND ti.`brand` = 'RIMOWA' ;

    EXPLAIN SELECT * FROM tb_item ti WHERE  ti.`price` = 18000 AND ti.`category` = '拉杆箱';
    EXPLAIN SELECT * FROM tb_item ti WHERE ti.`price` = 18000;
    EXPLAIN SELECT * FROM tb_item ti WHERE ti.`category` = '拉杆箱';

    EXPLAIN SELECT * FROM big_person WHERE province = '上海' AND city='宜兰县' AND county='中西区';
    EXPLAIN SELECT * FROM big_person WHERE county='中西区' AND city='宜兰县' AND province = '上海';
    EXPLAIN SELECT * FROM big_person WHERE city='宜兰县' AND county='中西区';
    EXPLAIN SELECT * FROM big_person WHERE county='中西区';	
    EXPLAIN SELECT * FROM big_person WHERE province = '上海' AND county='中西区';	


    #8）、使用比较运算或between会使联合索引从使用比较运算的下一个索引处断开
    EXPLAIN SELECT * FROM tb_item ti WHERE ti.`brand` = 'RIMOWA' AND ti.`price` > 18000 AND ti.`category` = '拉杆箱1' ;


    #9）、多列排序需要用联合索引（如果需要用到多列联合排序就要针对排序字段顺序建立联合索引）
    ALTER TABLE big_person ADD INDEX idx_last_name(`last_name`)
    ALTER TABLE big_person ADD INDEX idx_first_name(`first_name`)
    
    EXPLAIN SELECT * FROM big_person ORDER BY last_name, first_name LIMIT 10; 
		
    ALTER TABLE big_person DROP INDEX first_idx;
    ALTER TABLE big_person DROP INDEX last_idx;
    
    CREATE INDEX last_first_idx ON big_person(last_name,first_name);


    #10）、多列排序需要遵循最左前缀原则
    EXPLAIN SELECT * FROM big_person ORDER BY last_name, first_name LIMIT 10; 
    EXPLAIN SELECT * FROM big_person ORDER BY first_name, last_name LIMIT 10; 
    EXPLAIN SELECT * FROM big_person ORDER BY first_name LIMIT 10; 


    #11）、多列排序升降序需要一致
    EXPLAIN SELECT * FROM big_person ORDER BY last_name DESC, first_name DESC LIMIT 10; 
    EXPLAIN SELECT * FROM big_person ORDER BY last_name DESC, first_name ASC LIMIT 10;
```

