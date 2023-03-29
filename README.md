# sensitive-word-filter

提供简单的敏感词过滤web服务
JavaSDK：graalvm
Java编译等级：Java11

## 构建命令

```bash
./mvnw clean package -Pnative -DskipTests -Dquarkus.native.container-build=true -f pom.xml
```

## 构建Docker命令

```bash
docker build -f src/main/docker/Dockerfile.native-micro -t zl1030/sensitive-word-filter:0.6 .
```

## 准备词库文件

创建words.txt文件,UTF-8编码格式,格式类似下面一词一行:

```
我
是
例子
次库列表
```

## 运行Docker

把宿主机上目录映射到/word卷，默认会读取/word目录下words.txt和addon.txt，如果需要可以通过设置环境变量WORD_PATH和ADDON_WORD_PATH特殊指定词库文件:
在Windows下:

```bash
docker run --name sensitive-word-filter -d --restart=always -p 8080:8080 -v d:/:/word zl1030/sensitive-word-filter:0.6
```

在Linux下:

```bash
docker run --name sensitive-word-filter -d --restart=always -p 8080:8080 -v /data:/word zl1030/sensitive-word-filter:0.6
```

## OpenAPI协议
浏览器访问http://[服务器IP]:8080/q/swagger-ui/
