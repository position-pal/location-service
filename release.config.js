const publishCommands = `
echo "Building image \${nextRelease.version}..."
docker build -t \${process.env.DOCKERHUB_USERNAME}/location-service:\${nextRelease.version} -f ./Dockerfile-app . || exit 1
docker tag \${process.env.DOCKERHUB_USERNAME}/location-service:\${nextRelease.version} \${process.env.DOCKERHUB_USERNAME}/location-service:latest || exit 2

echo "Pushing Docker images..."
docker push \${process.env.DOCKERHUB_USERNAME}/location-service:\${nextRelease.version} || exit 3
docker push \${process.env.DOCKERHUB_USERNAME}/location-service:latest || exit 4

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
