# 2.0.0 发布准备记录

[English](RELEASE_PREPARATION.md) | 简体中文

准备日期：2026 年 11 月 16 日。当前已完成版本核对和真实 OSS 烟测；发布认证按维护者安排稍后配置，正式发布仍需最终确认。

## 版本与制品

- 目标正式版本：`2.0.0`。
- 准备分支的开发版本：`2.0.0-SNAPSHOT`。
- 制品坐标：`io.jenkins.plugins:aliyun-oss-uploader`。
- 最低环境：Jenkins 2.555.3、Java 21；CI 另覆盖 Java 25。
- 准备时已核对 [Jenkins 制品仓库版本记录](https://repo.jenkins-ci.org/releases/io/jenkins/plugins/aliyun-oss-uploader/maven-metadata.xml)及 Git 标签，均未发现已发布的 `2.0.0`。

## 真实 OSS 烟测结果

使用临时 Jenkins 2.555.3 / Java 21 环境，运行待发布代码的实际 Publisher。所有写入均限定在独立随机前缀，不覆盖已有对象或修改存储空间权限。

- 单文件上传：内容回读一致，显式私有 ACL 正确。
- 目录上传：普通文件和嵌套文件的路径、内容及私有 ACL 正确。
- 未指定及显式 `default` ACL：在私有存储空间中上传成功，访问权限符合预期。
- 缺失路径：默认跳过并成功结束；显式启用失败选项时构建失败。
- 清理：5 个上传对象全部删除，并确认没有遗留对象。

本次额外烟测用例执行了 6 次 Jenkins 构建并通过。真实空间未启用公共读或公共读写 ACL；这些取值的请求头由既有自动化 HTTP 服务测试覆盖。凭据仅用于临时环境，测试结束后已移除，未进入 Git。

## 发布权限

GitHub 仓库推送权限与 Jenkins 制品发布权限分别核对：

- 当前 GitHub 账号具有本仓库推送权限，发布说明提交已推送。
- [官方制品发布权限配置](https://github.com/jenkins-infra/repository-permissions-updater/blob/master/permissions/plugin-aliyun-oss-uploader.yml)授予 Jenkins 账号 `raylax` 对 `io/jenkins/plugins/aliyun-oss-uploader` 的发布权限；维护者已确认使用该账号。
- 本机尚未配置 Maven 发布认证，因此尚未验证发布凭据的有效性，也未尝试部署制品。

## 正式发布前的剩余步骤

1. 审阅并合并发布准备改动，确认发布说明的最终措辞。
2. 由维护者按照 [Jenkins 手工发布文档](https://www.jenkins.io/doc/developer/publishing/releasing-manually/)配置 `raylax` 的 Maven 发布认证；父 POM 使用的 server ID 为 `maven.jenkins-ci.org`。凭据不要加入 Git 或发布说明。
3. 获得最终发布确认后，再创建正式版本、发布标签并部署制品。

本次准备没有执行发布命令、创建发布标签或上传发布制品。
