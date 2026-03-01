# Release Process

This document describes the release process for GitHub PRs Visualizer.

## Overview

This project uses an automated release workflow. When merged to the master branch, builds are automatically executed and artifacts are attached to GitHub Releases.

## Version Management

Version numbers are managed in the `version.properties` file. The format is `major.minor.patch` (e.g., `1.0.0`).

### How to Update Version

There are two ways to update the version:

#### Method 1: Using GitHub Actions (Recommended)

1. Open the "Actions" tab in the GitHub repository
2. Select the "Bump Version" workflow
3. Click "Run workflow"
4. Select the version type:
   - `patch`: Bug fixes or minor changes (1.0.0 → 1.0.1)
   - `minor`: New features (1.0.0 → 1.1.0)
   - `major`: Breaking changes (1.0.0 → 2.0.0)
5. Execute "Run workflow"

#### Method 2: Manual Update

1. Edit the `version.properties` file
2. Update the `version=` value
3. Commit the change and merge to master

## Release Workflow

### Automatic Release

When pushed to the master branch, the following happens automatically:

1. **Build**: Builds run in parallel on 3 platforms (Ubuntu, macOS, Windows)
2. **Package Creation**:
   - Ubuntu: `.deb` package
   - macOS: `.dmg` package
   - Windows: `.msi` package
3. **GitHub Release Creation**: Creates a Release with tag `v{version}` based on `version.properties`
4. **Artifact Upload**: Attaches all packages to the Release

### Workflow Files

- `.github/workflows/release.yml`: Release automation workflow
- `.github/workflows/bump-version.yml`: Version bump workflow
- `.github/workflows/build.yml`: Build validation workflow for PRs/pushes

## Release Procedure

Normal release flow:

1. Develop features in PRs
2. Merge PR to master
3. Run "Bump Version" workflow if needed to update the version
4. Release is automatically created when pushed to master

**Note**: If a tag with the same version already exists, the release will be skipped. Update the version before creating a new release.

## Troubleshooting

### Release Not Created

- Check if a tag (`v{version}`) corresponding to the version in `version.properties` already exists
- Check GitHub Actions logs for detailed error messages

### Build Fails

- Run `./gradlew :composeApp:packageReleaseDistributionForCurrentOS` locally to identify the issue
- Check for problems with dependencies or build configuration
