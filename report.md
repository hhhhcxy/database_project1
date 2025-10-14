# Project Report: DBMS Performance Evaluation 
#### by 成轩宇 12412908

## 设计

### 数据使用
数据选择第一节课下发的航班数据 flights.sql，数据规模较大（12.5 MB），结构较简单，满足实验所需。

### 数据库搭建
使用windows11系统。
postgreSQL 搭建在本地，opengauss 通过 docker 部署在本地，通过多次实验抵消连接延迟

### 实验内容
* 指定出发地，查找对应航班。
* 指定字符串，查找航班号包含该子串的航班。
* 讲所有的指定字符都替换为另一个字符串。

## 实现

### 准备工作

### 代码
我利用了 lab3 课上给出的 postgresql.jar 与 DataFactory 等代码框架，补充完成了对 opengauss 的操作和文件操作。
添加了与 flights 有关的查询操作。
在 Client 中添加了计时器，用来记录查询操作用时。
使用 java 将 flights.sql 转换为了更适合文件查找的格式。

### 

## 结论

## 评价
