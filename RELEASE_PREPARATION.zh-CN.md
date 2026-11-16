# 2.0.0 发布准备记录

[English](RELEASE_PREPARATION.md) | 简体中文

准备日期：2026 年 11 月 16 日。#17 已合并，版本核对和真实 OSS 烟测已完成，维护者已授权发布 2.0.0。Maven 认证和正式制品部署均于 2026 年 11 月 16 日成功。

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
- 本机已配置 `raylax` 的 Maven 发布认证，向官方仓库发送的只读认证请求成功（HTTP 200）。凭据保存在 Git 之外；该检查未上传制品，后续正式部署成功，已确认上传权限。

## 发布结果

- `release:prepare` 成功，21 项测试通过，失败和错误均为 0；打包和静态检查通过。
- 正式标签：[`aliyun-oss-uploader-2.0.0`](https://github.com/jenkinsci/aliyun-oss-uploader-plugin/releases/tag/aliyun-oss-uploader-2.0.0)。
- `release:perform` 成功，POM、HPI、JAR、源码及 Javadoc 已部署至 [Jenkins 正式制品仓库](https://repo.jenkins-ci.org/releases/io/jenkins/plugins/aliyun-oss-uploader/2.0.0/)。
- Jenkins 父 POM 的发布配置在 `release:perform` 中跳过重复测试；测试已在 `release:prepare` 阶段成功执行。
- 完整下载的 HPI 与本地发布构建的 SHA-256 一致；ZIP 完整性、插件版本 `2.0.0` 和最低 Jenkins 版本 `2.555.3` 均已核验。官方元数据包含 `2.0.0`，并保留所有历史版本。
- `master` 的下一开发版本为 `2.0.1-SNAPSHOT`。

凭据仍保存在 Git 之外的本地 Maven 配置中。已发布的正式制品不得覆盖。
