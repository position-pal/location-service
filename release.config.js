const publishCommands = `
DOCKER_IMAGE_NAME=\${process.env.GITHUB_REPOSITORY#*/}
echo "Building image \${DOCKER_IMAGE_NAME}:\${nextRelease.version}..."
docker build -t \${process.env.DOCKERHUB_USERNAME}/\${DOCKER_IMAGE_NAME}:\${nextRelease.version} -f ./Dockerfile-app . || exit 1
docker tag \${process.env.DOCKERHUB_USERNAME}/\${DOCKER_IMAGE_NAME}:\${nextRelease.version} \${process.env.DOCKERHUB_USERNAME}/\${DOCKER_IMAGE_NAME}:latest || exit 2

echo "Pushing Docker images..."
docker push \${process.env.DOCKERHUB_USERNAME}/\${DOCKER_IMAGE_NAME}:\${nextRelease.version} || exit 3
docker push \${process.env.DOCKERHUB_USERNAME}/\${DOCKER_IMAGE_NAME}:latest || exit 4

echo "release_status=released" >> $GITHUB_ENV
echo "CONTAINER_VERSION="\${nextRelease.version} >> $GITHUB_ENV
`
const releaseBranches = ["main"]
const config = require('semantic-release-preconfigured-conventional-commits')
config.branches = releaseBranches
config.plugins.push(
    // Custom release commands
    ["@semantic-release/exec", {
        "publishCmd": publishCommands,
    }],
    "@semantic-release/github",
    "@semantic-release/git",
)
module.exports = config
