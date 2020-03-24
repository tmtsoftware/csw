# Releasing

## Prerequisites

### Git
* Make sure git authentication works on jenkins agent by running cmd: `ssh -vT git@github.com`

### Node
* Node is installed
* npm module `junit-merge` is installed (for merging multiple xml test reports into one)
* npm module `junit-viewer` is installed (for generating html test report from merged xml)

## Steps to release

### Release msocket
1. Refer RELEASING.md of `msocket` repo.
2. Use the tagged version in csw repo.
    
### csw
1. Update release notes (`notes/<version>.markdown`) in `csw` repo and link the migration guide
#### Note - The version in `notes` should be of format `v1.0.0` but while triggering the pipeline build parameter should be of format `1.0.0` 
2. Update top-level `CHANGELOG.md`
3. Update top-level `README.md`
4. Bump up the `csw-contract` version (if needed). 
5. Exclude projects from `build.sbt` which you do not want to release
6. Remove targets of newly added js projects in jenkins prod file (if needed).  
7. Run `csw-prod` pipeline by providing `VERSION` number.

### csw.g8
Refer RELEASING.md in `csw.g8` repo.

### Release csw-js (if needed)
Release `csw-js` if keycloak version is updated in csw. Refer the RELEASING.md in `csw-js`.

### Release esw
Refer to RELEASING.md in `esw` repo.

### Release csw-shell
Refer RELEASING.md of `csw-shell` repo.

#### Note - `VERSION` tag is version number with 'v' as prefix. For eg. `v0.0.0`