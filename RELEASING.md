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

1. Update release notes (`notes/<version>.markdown`) in `csw` repo and link the migration guide
 **Note** - The version in `notes` should be of format `v1.0.0`
2. Update top-level `CHANGELOG.md`
3. Update top-level `README.md`
4. Add changes mention in `CHANGELOG.md` of `csw-contract` in top-level `CHANGELOG.md`
5. Add changes mention in `CHANGELOG.md` of `csw-contract` in the change section of `README.md` of `csw-contract`
6. Add changes mention in `CHANGELOG.md` of `csw-contract` in top-level `README.md`
7. Bump up the `csw-contract` version (if needed)
8. Exclude projects from `build.sbt` which you do not want to release
8. Update versions in `release.ym'` for other repos like `ESW_TS_VERSION` etc. which we will be releasing next. (These versions are used to create parameterized links in documentation)
9. Run `release.sh $VERSION$` script by providing version number argument (This triggers release workflow)

    **Note:** `PROD=true` environment variable needs to be set before running `release.sh`

