# 2.0.0 Release Preparation

English | [简体中文](RELEASE_PREPARATION.zh-CN.md)

Prepared on November 16, 2026. PR #17 is merged, version checks and the real OSS smoke test are complete, and the maintainer authorized the 2.0.0 release. Maven authentication and the formal artifact deployment succeeded on November 16, 2026.

## Version and artifact

- Target release: `2.0.0`.
- Development version on the preparation branch: `2.0.0-SNAPSHOT`.
- Artifact coordinates: `io.jenkins.plugins:aliyun-oss-uploader`.
- Minimum environment: Jenkins 2.555.3 and Java 21; CI also covers Java 25.
- At preparation time, neither the [Jenkins artifact repository version list](https://repo.jenkins-ci.org/releases/io/jenkins/plugins/aliyun-oss-uploader/maven-metadata.xml) nor Git tags contained a published `2.0.0`.

## Real OSS smoke test

The test ran the actual Publisher code in a temporary Jenkins 2.555.3 / Java 21 environment. Writes were restricted to a unique random prefix, without overwriting existing objects or changing bucket permissions.

- Single file: downloaded content matched and the explicit private ACL was correct.
- Directory: regular and nested files had the expected paths, content, and private ACLs.
- Unset and explicit `default` ACLs: uploads to a private bucket succeeded with the expected access permissions.
- Missing paths: the default behavior skipped the path and succeeded; enabling the failure option failed the build.
- Cleanup: all five uploaded objects were deleted and no test objects remained.

This additional smoke-test case completed six Jenkins builds successfully. It did not enable public-read or public-read-write on the real bucket; their request headers are covered by existing automated HTTP tests. Credentials were used only in the temporary environment, removed after testing, and never added to Git.

## Publishing permissions

GitHub push access and Jenkins artifact publishing rights were checked separately:

- The current GitHub account has push access to this repository, and the release-notes commit has been pushed.
- The [official artifact permission configuration](https://github.com/jenkins-infra/repository-permissions-updater/blob/master/permissions/plugin-aliyun-oss-uploader.yml) grants Jenkins account `raylax` permission to publish `io/jenkins/plugins/aliyun-oss-uploader`. The maintainer confirmed this account will be used.
- Maven publishing authentication is configured locally for `raylax`. A read-only request to the official repository authenticated successfully (HTTP 200). Credentials remain outside Git; this check did not upload artifacts. The subsequent release deployment succeeded, confirming upload permission.

## Release result

- `release:prepare` succeeded with 21 tests passing and no failures or errors; packaging and static checks passed.
- Release tag: [`aliyun-oss-uploader-2.0.0`](https://github.com/jenkinsci/aliyun-oss-uploader-plugin/releases/tag/aliyun-oss-uploader-2.0.0).
- `release:perform` succeeded and deployed the POM, HPI, JAR, sources, and Javadoc to the [Jenkins releases repository](https://repo.jenkins-ci.org/releases/io/jenkins/plugins/aliyun-oss-uploader/2.0.0/).
- The Jenkins parent release profile skips repeated tests during `release:perform`; the tests ran successfully during `release:prepare`.
- The downloaded HPI matches the local release build by SHA-256; ZIP integrity, plugin version `2.0.0`, and minimum Jenkins version `2.555.3` were verified. Official metadata includes `2.0.0` and retains all previous versions.
- The next development version on `master` is `2.0.1-SNAPSHOT`.

Credentials remain in the local Maven configuration outside Git. Published release artifacts must not be overwritten.
