# Releasing

## Prerequisites (This is configured in `release.yml`)

* Git authentication works by running cmd: `ssh -vT git@github.com`
* Node is installed
* npm module `junit-merge` is installed (for merging multiple xml test reports into one)
* npm module `junit-viewer` is installed (for generating html test report from merged xml)

## Steps to release

### Release msocket

1. Refer RELEASING.md of `msocket` repo.
2. Use the tagged version in csw repo.

### csw

1. Update release notes (`notes/<version>.markdown`) in `csw` repo and link the migration guide
 **Note** - The version in `notes` should be of format `v1.0.0`
1. Update top-level `CHANGELOG.md`
1. Update top-level `README.md`
1. Bump up the `csw-contract` version (if needed)
1. Exclude projects from `build.sbt` which you do not want to release
1. Run `release.sh $VERSION$` script by providing version number argument (This triggers release workflow)
    **Note:** `PROD=true` environment varibale needs to be set before running `release.sh`

### csw.g8

Refer **RELEASING.md** in `csw.g8` repo.

### Release csw-js (if needed)

Release `csw-js` if keycloak version is updated in csw. Refer the **RELEASING.md** in `csw-js`.

### Release esw

Refer to **RELEASING.md** in `esw` repo.

### Release csw-shell

Refer **RELEASING.md** of `csw-shell` repo.
