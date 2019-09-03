# Releasing

## Prerequisites

### Git
* Make sure git authentication works on jenkins agent by running cmd: `ssh -vT git@github.com`

### Node
* Node is installed
* npm module `junit-merge` is installed (for merging multiple xml test reports into one)
* npm module `junit-viewer` is installed (for generating html test report from merged xml)

### Bintray
* Make sure bintray credentials are setup properly by running cmd: `sbt bintrayWhoami`
* Make sure bintray credentials contain API key of bintray user, not password.

## Steps to release

### csw
1. Update release notes (`notes/<version>.markdown`) in `csw` and `csw-acceptance` repo
#### Note - The version in `notes` should be of format `v1.0.0` but while triggering the pipeline build parameter should be of format `1.0.0` 
2. Update top level `CHANGELOG.md`
3. Update top level `README.md`
4. Exclude projects from `build.sbt` which you do not want to release
5. Run `csw-prod` pipeline by providing `VERSION` number. (This automatically triggers `acceptance-release` pipeline)

### csw-acceptance
- Update the release notes (`notes/<version>.markdown`)
- Pipeline will automatically triggered on the successful run of `csw-prod` pipeline

### csw.g8
1. Merge `dev` branch to master
2. Change `csw` version in `src/main/g8/default.properties` and `README.md`
3. Run `giter8-prod` pipeline by providing `VERSION` number

### Release csw-js (if needed)
- Release `csw-js` if keycloak version is updated in csw
1. Update release notes (`notes/<version>.markdown`) in `csw`
2. Update top level `CHANGELOG.md`
3. Update top level `README.md`
4. Refer the RELEASING.md of csw-js

### Release csw-client
1. Update the csw version in `build.sbt`
2. Release `csw-client` with the latest `VERSION` of csw.
    There is not pieline to release csw-client but only `git tag v0.0.0` and `git push origin v0.0.0` 

#### Note - `VERSION` tag is version number with 'v' as prefix. For eg. `v0.0.0`

### More detailed instructions

https://docs.google.com/document/d/1tK9W6NClJOB0wq3bFVEzdrYynCxES6BnPdSLNtyGMXo/edit?usp=sharing