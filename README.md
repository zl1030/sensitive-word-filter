# sensitive-word-filter
提供简单的敏感词过滤web服务

## 构建命令
```bash
./mvnw clean package -Pnative -DskipTests -Dquarkus.native.container-build=true -f pom.xml
```

## 构建Docker命令
```bash
docker build -f src/main/docker/Dockerfile.native-micro -t zl1030/sensitive-word-filter .
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
环境变量WORD_PATH: 指定词库文本文件路径,必要时需要映射卷,以下是使用word.txt文件的例子:
在Windows下:
```bash
docker run --name sensitive-word-filter -d -p 8080:8080 -v d:/:/word -e WORD_PATH=/word/words.txt  zl1030/sensitive-word-filter
```
在Linux下:
```bash
docker run --name sensitive-word-filter -d -p 8080:8080 -v /data:/word -e WORD_PATH=/word/words.txt  zl1030/sensitive-word-filter
```

## 访问接口
浏览器或者Get方式请求http://[服务器IP]:8080/word_filter/[字符串]
返回json格式结果,参数result=0代表未发现敏感词,result=1代表发现敏感词,word为过滤后内容.
例如：
{"result":0,"word":"哈哈哈"}