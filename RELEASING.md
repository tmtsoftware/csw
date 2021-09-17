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
4. Refer RELEASING.md of `rtm` repo.
5. Use the tagged version of above repos in csw repo.

### Versioning Strategy

TMT work packages are released in incrementally. (M1 -> RC -> Final)
A milestone release is cut from master branch as we expect bug fixes / feedback before making the final release.
1. While making `Milestone(M*)` release, we follow these ideas:
- Update transitive dependencies.
- Important bug fixes that we want in the major release.
2. While making `RC-*` release, we follow :
- We cut a branch from master, any changes on master from here onwards will not be considered for this current release.
- Do not update any third party dependencies. 
  If Secondary packages has some changes after M1 Release of CSW, we update them & use their latest tags. 
  These secondary packages include `msocket`, `embedded-keycloak`, `sbt-docs` & `rtm`.
- Documentation related updates are allowed on this RC branch, because these changes won't be breaking anything code-wise.
3. While making `Final` release, we follow: 
- RC branch will be considered final & not the master branch.
- After getting the approval, `V*.*.*-Final` tag will be created.


### CSW

1. Create a branch named `branch-<major>.<minor>.x` if not already exists from `master`. Example branch name `branch-3.0.x`.
   All subsequent release for this release cycle should be done on this branch. All further steps are to be done on this branch.
2. Update release notes (`notes/<version>.markdown`) in `csw` repo and link the migration guide
 **Note** - The version in `notes` should be of format `v1.0.0`
3. Update top-level `CHANGELOG.md`
4. Update top-level `README.md`
5. Update top-level `INSTALL.md`
6. Add changes mention in `CHANGELOG.md` of `csw-contract` in top-level `CHANGELOG.md`
7. Add changes mention in `CHANGELOG.md` of `csw-contract` in the change section of `README.md` of `csw-contract`
8. Add changes mention in `CHANGELOG.md` of `csw-contract` in top-level `README.md`
9. Add migration guide to `docs/src/main/migration_guide` if this is the final release.
10. Bump up the `csw-contract` version (if needed)
11. Exclude projects from `build.sbt` which you do not want to release
12. Update secondary packages versions if necessary (this step to be done only in milestone & RC release).
13. Update latest RTM version in github and jenkins workflow files(this step to be done only in milestone & RC release).
14. Update versions in `release.yml'` for other repos like `ESW_TS_VERSION` etc. which we will be releasing next. (These versions are used to create parameterized links in the documentation)
15. Commit and push the changes to `branch-<major>.<minor>.x` branch.
15. Make sure build is green for dev and paradox link check pipeline for this branch.
16. Run `release.sh $VERSION$` script by providing version number argument (This triggers release workflow)
17. Applicable for final release only - Upgrade all the versions of all csw apps in [here](https://github.com/tmtsoftware/osw-apps/blob/master/apps.json)

    **Note:** `PROD=true` environment variable needs to be set before running `release.sh`

