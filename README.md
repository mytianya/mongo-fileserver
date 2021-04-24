# mongo-fileserver
依赖mongodb的GridFS实现的文件服务器
## docker搭建文件服务器
```
sudo docker run -itd --name mongo -p 27017:27017 mongo
```
## springboot集成mongodb
```xml
 <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-data-mongodb</artifactId>
</dependency>
```
### 已实现功能
- 基本文件上传、下载、删除
- 按需读取fs.chunks文件块，实现文件断点续传