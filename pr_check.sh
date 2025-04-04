#!/bin/bash
# NOTE: if you need to debug this file, use DRY_RUN=true to echo docker/podman/oc commands without running them

# before we run common consoledot builds, prepare the binary artifacts for quarkus style builds
./podman_run.sh ./gradlew assemble

source cicd_common.sh

export APP_NAME="rhsm"  # name of app-sre "application" folder this component lives in

export IQE_PLUGINS="rhsm-subscriptions"  # name of the IQE plugin for this APP
export IQE_MARKER_EXPRESSION="ephemeral"  # This is the value passed to pytest -m
# export IQE_FILTER_EXPRESSION=""  # This is the value passed to pytest -k
export IQE_CJI_TIMEOUT="30m"  # This is the time to wait for smoke test to complete or fail
# IQE plugin image
export IQE_IMAGE_TAG="rhsm-subscriptions"
# NOTE: workaround for frontend deployment not being ready yet below


# Install bonfire repo/initialize
CICD_URL=https://raw.githubusercontent.com/RedHatInsights/bonfire/master/cicd
curl -s $CICD_URL/bootstrap.sh > .cicd_bootstrap.sh && source .cicd_bootstrap.sh

IMAGES=""

export COMPONENT_NAME="rhsm"  # name of app-sre "resourceTemplate" in deploy.yaml for this component
# prebuild artifacts for quarkus builds
for service in $SERVICES; do
  export IMAGE="quay.io/cloudservices/$service"  # the image location on quay
  export DOCKERFILE="$(get_dockerfile $service)"

  # Build the image and push to quay
  APP_ROOT=$(get_approot $service)
  source $CICD_ROOT/build.sh

  IMAGES=" ${IMAGES} -i ${IMAGE}=${IMAGE_TAG} "
done

APP_ROOT=$PWD

# NOTE: uncomment the following line to test authenticated kafka
#NAMESPACE_POOL="managed-kafka"
EXTRA_DEPLOY_ARGS="--timeout 1800 ${IMAGES}"
OPTIONAL_DEPS_METHOD=none

#each gets appended with --component
export COMPONENTS_W_RESOURCES="rhsm swatch-api swatch-producer-aws swatch-producer-red-hat-marketplace swatch-metrics swatch-subscription-sync swatch-system-conduit swatch-tally"
# NOTE: this ensures that all of the other services end up deployed with the latest template
for EXTRA_COMPONENT_NAME in $COMPONENTS_W_RESOURCES; do
  export EXTRA_DEPLOY_ARGS="${EXTRA_DEPLOY_ARGS} --set-template-ref ${EXTRA_COMPONENT_NAME}=${GIT_COMMIT}"
done
# Deploy to an ephemeral namespace for testing
source $CICD_ROOT/deploy_ephemeral_env.sh

# Run smoke tests with ClowdJobInvocation
 source $CICD_ROOT/cji_smoke_test.sh
# During the PR checks the Ibutsu URL and test run IDs are published as a comment by InsightsDroid account (on GitHub) or iqe-bot (on GitLab).
# This is achieved by adding this line to `pr_check.sh` of the repo:
 source $CICD_ROOT/post_test_results.sh


# Need to make a placeholder results file to make tests pass
# Inspired by https://github.com/RedHatInsights/insights-rbac/blo/243b57a20ea2c1da87fe4292a2df9b19e1157efd/pr_check.sh
# which is listed in the bonfire docs as an example pr_check file
    mkdir -p artifacts
    cat << EOF > artifacts/junit-placeholder.xml
    <testsuite tests="1">
        <testcase classname="placeholder" name="placeholdertest"/>
    </testsuite>
EOF
