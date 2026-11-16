# 2.0.0 Release Preparation

English | [简体中文](RELEASE_PREPARATION.zh-CN.md)

Prepared on November 16, 2026. PR #17 is merged, version checks and the real OSS smoke test are complete, and the maintainer has authorized the 2.0.0 release. Maven credentials passed a read-only authentication check; publication is now in progress.

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
- Maven publishing authentication is configured locally for `raylax`. A read-only request to the official repository authenticated successfully (HTTP 200). Credentials remain outside Git; this check did not upload artifacts. Actual deployment permission will be verified by the release deployment.

## Remaining release steps

1. Run the authorized [Jenkins manual release procedure](https://www.jenkins.io/doc/developer/publishing/releasing-manually/) for version `2.0.0` and tag `aliyun-oss-uploader-2.0.0`; the next development version is `2.0.1-SNAPSHOT`.
2. Verify the deployed artifacts and the release tag. The parent POM uses server ID `maven.jenkins-ci.org`; credentials stay outside Git.
3. Publish the bilingual release announcement and record the deployment result.

This record describes the checks completed before deployment. Publication will be confirmed after the release commands succeed.
