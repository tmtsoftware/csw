# Releasing

## Prerequisites (This is already configured in `release.yml`)

* Git authentication works by running cmd: `ssh -vT git@github.com`
* Node is installed
* npm module `junit-merge` is installed (for merging multiple xml test reports into one)
* npm module `junit-viewer` is installed (for generating html test report from merged xml)

## Steps to release

### Release msocket

1. Refer RELEASING.md of `msocket` repo.
2. Refer RELEASING.md of `embedded-keycloak` repo
3. Refer RELEASING.md of `sbt-docs` repo.
2. Use the tagged version of above repos in csw repo.

### csw

1. Create a branch named `branch-<major>.<minor>.x` if not already exists from `master`. Example branch name `branch-3.0.x`.
   All subsequent release for this release cycle should be done on this branch. All further steps are to be done on this branch.
2. Update release notes (`notes/<version>.markdown`) in `csw` repo and link the migration guide
 **Note** - The version in `notes` should be of format `v1.0.0`
3. Update top-level `CHANGELOG.md`
4. Update top-level `README.md`
5. Add changes mention in `CHANGELOG.md` of `csw-contract` in top-level `CHANGELOG.md`
6. Add changes mention in `CHANGELOG.md` of `csw-contract` in the change section of `README.md` of `csw-contract`
7. Add changes mention in `CHANGELOG.md` of `csw-contract` in top-level `README.md`
8. Add migration guide to `docs/src/main/migration_guide` if this is the final release.
8. Bump up the `csw-contract` version (if needed)
9. Exclude projects from `build.sbt` which you do not want to release
10. Update versions in `release.yml'` for other repos like `ESW_TS_VERSION` etc. which we will be releasing next. (These versions are used to create parameterized links in the documentation)
11. Commit and push the changes to `branch-<major>.<minor>.x` branch.
12. Run `release.sh $VERSION$` script by providing version number argument (This triggers release workflow)

    **Note:** `PROD=true` environment variable needs to be set before running `release.sh`

