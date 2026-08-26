# Aliyun OSS Uploader Plugin

> **Version**: 1.6.0  
> **Requires**: Jenkins ≥ 2.479.3, JDK 17+

Jenkins plugin that uploads build artifacts to [Aliyun OSS](https://www.alibabacloud.com/product/oss).

This fork updates the plugin for modern Jenkins cores that no longer ship Apache Commons Lang 2.x (`org.apache.commons.lang.StringUtils`), which previously broke the job configuration UI and upload step.

## Changes (1.5-SNAPSHOT → 1.6.0)

- Bump plugin parent POM to `5.31` and import `bom-2.479.x`
- Switch to `commons-lang3` via the `commons-lang3-api` plugin
- Upgrade Aliyun OSS SDK `2.8.3` → `3.18.1` (`OSSClientBuilder`)
- Mark form-validation methods with `@POST`; close upload streams and shut down the OSS client

Job configuration fields and the Pipeline `@Symbol("aliyunOSSUpload")` remain compatible; existing job configs generally do not need XML changes.

## Build

```bash
mvn -U clean package -DskipTests
```

Artifact: `target/aliyun-oss-uploader.hpi`

Maven must be able to resolve artifacts from `https://repo.jenkins-ci.org/public/`. Example `~/.m2/settings.xml` fragments:

```xml
<pluginGroups>
  <pluginGroup>org.jenkins-ci.tools</pluginGroup>
</pluginGroups>

<profiles>
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
</profiles>
```

## Install

1. **Manage Jenkins → Plugins → Advanced settings → Deploy Plugin** and upload `target/aliyun-oss-uploader.hpi`, **or** copy the file into `$JENKINS_HOME/plugins/aliyun-oss-uploader.jpi`.
2. Remove any exploded directory `$JENKINS_HOME/plugins/aliyun-oss-uploader` if present, then restart Jenkins (or use **Download now and install after restart**).
3. Ensure the `commons-lang3-api` plugin is installed (declared as a dependency; Update Center should pull it automatically).
4. Open a job that uses **Aliyun OSS Upload**, confirm the configuration section loads, and run a build that exercises the publisher.

## Usage

Add a post-build action **Aliyun OSS Upload**:

- Local and remote paths must start with `/` (paths are relative to the job workspace)
- Remote path accepts multiple comma-separated destinations
- `maxRetries` may be left empty (default: `3`)

Pipeline example:

```groovy
aliyunOSSUpload(
  endpoint: 'https://oss-cn-hangzhou.aliyuncs.com',
  accessKeyId: '…',
  accessKeySecret: '…',
  bucketName: 'my-bucket',
  localPath: '/dist',
  remotePath: '/releases/${BUILD_NUMBER}'
)
```

## License

This project is licensed under [The MIT License](https://opensource.org/licenses/MIT).
See [LICENSE](LICENSE) for the full license text.
