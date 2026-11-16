# 2.0.0 Release Preparation

English | [简体中文](RELEASE_PREPARATION.zh-CN.md)

Prepared on November 16, 2026. Version checks and the real OSS smoke test are complete. Publishing credentials will be configured later as requested by the maintainer, and the actual release still requires final confirmation.

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
- Maven publishing authentication is not configured on this machine. Credential validity has therefore not been checked, and no artifact deployment has been attempted.

## Remaining release steps

1. Review and merge the release preparation changes and approve the final release notes.
2. Follow the [Jenkins manual release documentation](https://www.jenkins.io/doc/developer/publishing/releasing-manually/) to configure Maven publishing authentication for `raylax`. The parent POM uses server ID `maven.jenkins-ci.org`. Do not put credentials in Git or release notes.
3. After final release confirmation, create the release version and tag and deploy the artifacts.

This preparation has not run release commands, created release tags, or uploaded release artifacts.
