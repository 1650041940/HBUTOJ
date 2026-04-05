# 单体部署②——MySQL更新工具

:::tip
本镜像主要是为了跟随HOJ主仓库更新，使用固定镜像来检查是否有更新，以达到MySQL数据库的平滑升级
:::
## 一、用已有的HOJ镜像部署

可以直接在已有的docker-compose.yml添加以下模块即可，**本容器检查完是否有更新就会正常退出**

```yaml
  hbutoj-mysql-checker:
    image: registry.cn-shenzhen.aliyuncs.com/hcode/hbutoj_database_checker
    container_name: hbutoj-mysql-checker
    depends_on:
      - hbutoj-mysql
    links:
      - hbutoj-mysql:mysql
    environment:
      - MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PASSWORD:-hoj123456} # mysql的数据库密码
```

## 二、自己打包镜像部署

首先 先下载[hbutoj_deplay](https://github.com/1650041940/hbutoj_deplay) 然后进入对应的镜像打包文件夹

```shell
git clone https://github.com/1650041940/hbutoj_deplay.git && cd hbutoj_deplay/src/mysql-checker
```

当前文件夹为打包`hbutoj-mysql-checker`镜像的相关文件，只需将这些文件复制到同一个文件夹内，之后执行以下命令进行打包成镜像。

```shell
docker build -t hbutoj-mysql-checker .
```

docker-compose启动

```yaml
version: "3"
services:
   hbutoj-mysql-checker:
    #image: registry.cn-shenzhen.aliyuncs.com/hcode/hbutoj_database_checker
    image: hbutoj-mysql-checker # 自己的镜像名称
    container_name: hbutoj-mysql-checker
    depends_on:
      - hbutoj-mysql
    links:
      - hbutoj-mysql:mysql
    environment:
      - MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PASSWORD:-hoj123456} # mysql的数据库密码
```



**文件介绍**

#### 1. hoj-update.sql

此文件为检查更新的sql脚本

#### 2. update.sh

此文件为执行脚本

```shell
#!/bin/sh

mysql -h mysql -uroot -p$MYSQL_ROOT_PASSWORD -e "select version();" &> /dev/null
RETVAL=$?

while [ $RETVAL -ne 0 ]
do
	sleep 3
	mysql -h mysql -uroot -p$MYSQL_ROOT_PASSWORD -e "select version();" &> /dev/null
	RETVAL=$?
done
mysql -uroot -h mysql -p$MYSQL_ROOT_PASSWORD -D hoj -e "source /sql/hoj-update.sql"
echo 'Check whether the `hoj` database has been updated successfully!' 
```

#### 3. Dockerfile

```dockerfile
FROM arey/mysql-client

COPY ./hoj-update.sql /sql/

COPY ./update.sh /sql/

ENTRYPOINT ["/bin/sh", "/sql/update.sh"]

```



