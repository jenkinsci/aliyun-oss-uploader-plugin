# jenkins阿里云OSS上传插件

## 使用

1.下载hpi文件

[aliyun-oss-uploader.hpi](https://github.com/raylax/jenkins-aliyun-oss-uploader/releases/latest)

2.在plugins管理页面上传hpi文件并安装，重启jenkins

3.修改项目，增加`构建后操作`，选择`阿里云OSS上传`

![](https://github.com/raylax/jenkins-aliyun-oss-plugin/raw/master/image/step1.png)

4.填写阿里云OSS配置信息

![](https://github.com/raylax/jenkins-aliyun-oss-plugin/raw/master/image/step2.png)

> 本地路径为相对于workspace的路径，例如填写为`/abc`，则本地路径为`${WORKSPACE}/abc`
本地路径可以设置为文件或目录。如果设置为文件则上传单个文件，设置为目录上传整个目录

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
mvn clean package -DskipTests
```

3. 运行
```bash
mvn clean hpi:run
```
