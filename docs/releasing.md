# GitHub release process

This configuration follows the [Jenkins plugin CD guide](https://www.jenkins.io/doc/developer/publishing/releasing-cd/).
CD is enabled through [RPU #5461](https://github.com/jenkins-infra/repository-permissions-updater/pull/5461), and the workflow credentials have been provisioned.
As agreed with raylax, releases use exclusive CD: publication goes through GitHub Actions, and individual Artifactory upload permissions are removed.
Exclusive CD still supports manually triggered releases; it does not require automatic publication after merges.

## Version numbers

Local builds use `999999-SNAPSHOT`. Release builds use a version such as `123.vabcdef456789`, generated from Git history and the commit hash.
This replaces manually incremented versions such as `2.1.1`.
Announce the version-format change in the first release notes. Future published versions must remain newer than previous releases.

To inspect the versions without publishing:

```sh
./mvnw validate help:evaluate -Dexpression=project.version
./mvnw validate help:evaluate -Dexpression=project.version -Dset.changelist -Dignore.dirt
```

## Publishing after approval and setup

1. Review and merge the changes to release, and wait for Jenkins CI to pass on `master`.
2. Apply the standard Jenkins release labels to PRs and review the generated release notes.
3. Open **Actions → cd → Run workflow**, selecting `master`.
4. Keep `validate_only` checked to preview the release draft without publishing.
5. To publish, run again with `validate_only` unchecked after reviewing the draft and CI status.

The workflow has no merge or check-run trigger: merging a PR alone does not publish a release.
Manual triggering is available to users with sufficient GitHub access; it does not enforce a raylax-only publishing policy.
The proposed team agreement is that raylax continues final release review and triggers publication.
