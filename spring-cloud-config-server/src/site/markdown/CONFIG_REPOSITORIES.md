# config repositories

## I. Repository locations

### 1.Global config repository for all projects of organization
git@{git-service}:spring-cloud-config-server/{organization}-global-config.git

### 2.Project config repositories
git@{git-service}:spring-cloud-config-server/{project}-config.git


## II. Branch model
We use GitFlow to control global/project config versions in develop, staging and production environments

Environment    | config-server         | Global config label (Branch/Tag)                  | project               | Project config label (default)
---------------|-----------------------|---------------------------------------------------|-----------------------|-----------------------------------------------------------|
develop.env    | on develop machine    | branch feature/{feature} during develop           |                       |                                                           |
               | in office LAN         | branch develop available for project developers   | on developer hosts    | branch feature/{feature} during feature development       |
               | in office LAN         | branch develop available for project developers   | on developer hosts    | branch develop when feature merged                        |
               |                       |                                                   | on CI slave/runner    | branch develop on CI build and publish snapshots          |
               |                       |                                                   |                       |                                                           |
staging.env    | in staging cluster    | branch release/{version} during pre-release tests |                       |                                                           |
               | in staging cluster    | branch master or tag v{version} after released    | on CI slave/runner    | branch release/{version} on CI build and publish releases |
               |                       |                                                   | in staging cluster    | branch master or tag v{version} after released            |
               |                       |                                                   |                       |                                                           |
production.env | in production cluster | branch master or tag v{version} after released    | in production cluster | branch master or tag v{version} after released            |

## III. Config repository security, keystore, password management

Environment    | config-server         | Global config label (Branch/Tag)                  | project               | Project config label (default)
---------------|-----------------------|---------------------------------------------------|-----------------------|-----------------------------------------------------------|
develop.env    | on develop machine    | branch feature/{feature} during develop           |                       |                                                           |
               | in office LAN         | branch develop available for project developers   | on developer hosts    | branch feature/{feature} during feature development       |
               | in office LAN         | branch develop available for project developers   | on developer hosts    | branch develop when feature merged                        |
               |                       |                                                   | on CI slave/runner    | branch develop on CI build and publish snapshots          |
               |                       |                                                   |                       |                                                           |
staging.env    | in staging cluster    | branch release/{version} during pre-release tests |                       |                                                           |
               | in staging cluster    | branch master or tag v{version} after released    | on CI slave/runner    | branch release/{version} on CI build and publish releases |
               |                       |                                                   | in staging cluster    | branch master or tag v{version} after released            |
               |                       |                                                   |                       |                                                           |
production.env | in production cluster | branch master or tag v{version} after released    | in production cluster | branch master or tag v{version} after released            |
