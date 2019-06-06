# jenkins阿里云OSS上传插件

## 使用

1.增加`构建后操作`，选择`阿里云OSS上传`

![](https://github.com/raylax/jenkins-aliyun-oss-plugin/raw/master/image/step1.png)

2.填写阿里云OSS配置信息

![](https://github.com/raylax/jenkins-aliyun-oss-plugin/raw/master/image/step2.png)

> 本地路径为相对于workspace的路径，例如填写为`/abc`，则本地路径为`${WORKSPACE}/abc`
本地路径可以设置为文件或目录

## 构建

1. 修改`${USER}/.m2/settings.xml`中的maven配置文件

在`mirrors`节点中增加
```xml
<mirror>
  <id>repo.jenkins-ci.org</id>
  <url>https://repo.jenkins-ci.org/public/</url>
  <mirrorOf>m.g.o-public</mirrorOf>
</mirror>
```
在`pluginGroups`节点中增加
```xml
<pluginGroup>org.jenkins-ci.tools</pluginGroup>
```
在`profiles`节点中增加
```xml
<profile>
  <id>jenkins</id>
  <activation>
    <activeByDefault>true</activeByDefault>
  </activation>
  <repositories>
    <repository>
      <id>repo.jenkins-ci.org</id>
      <url>https://repo.jenkins-ci.org/public/</url>
    </repository>
  </repositories>
  <pluginRepositories>
    <pluginRepository>
      <id>repo.jenkins-ci.org</id>
      <url>https://repo.jenkins-ci.org/public/</url>
    </pluginRepository>
  </pluginRepositories>
</profile>
```

2. 打包
```bash
mvn package
```

3. 运行
```bash
mvn hpi:run
```
