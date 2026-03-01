# Release Process

This document describes the release process for GitHub PRs Visualizer.

## Overview

This project uses an automated release workflow. When a PR from a `release/*` branch is merged into `main/master`, builds are automatically executed and artifacts are attached to GitHub Releases.

## Version Management

Version numbers are managed in the `version.properties` file. The format is `major.minor.patch` (e.g., `1.0.0`).

### How to Update Version

1. Open the "Actions" tab in the GitHub repository
2. Select the "Bump Version" workflow
3. Select the version type:
   - `patch`: Bug fixes or minor changes (1.0.0 → 1.0.1)
   - `minor`: New features (1.0.0 → 1.1.0)
   - `major`: Breaking changes (1.0.0 → 2.0.0)
4. Execute "Run workflow"
5. The workflow automatically creates a `release/v{version}` branch and PR

## Release Workflow

### Automatic Release

When a PR from `release/*` is merged into `main/master`, the following happens automatically:

1. **Build**: Builds run in parallel on 3 platforms (Ubuntu, macOS, Windows)
2. **Package Creation**:
   - Ubuntu: `.deb` package
   - macOS: `.dmg` package
   - Windows: `.msi` package
3. **GitHub Release Creation**: Creates a Release with tag `v{version}` based on `version.properties`
4. **Artifact Upload**: Attaches all packages to the Release

### Workflow Files

- `.github/workflows/release.yml`: Release automation workflow
- `.github/workflows/bump-version.yml`: Version bump + release PR creation workflow
- `.github/workflows/build.yml`: Build validation workflow for PRs/pushes

## Release Procedure

Normal release flow:

1. Run the "Bump Version" workflow
2. Review the auto-created `release/v{version}` PR
3. Merge the PR into `main/master`
4. Release is automatically created after the merge

**Note**: If a tag with the same version already exists, the release will be skipped. Update the version before creating a new release.

## Troubleshooting

### Release Not Created

- Check if a tag (`v{version}`) corresponding to the version in `version.properties` already exists
- Check GitHub Actions logs for detailed error messages

### Build Fails

- Run `./gradlew :composeApp:packageReleaseDistributionForCurrentOS` locally to identify the issue
- Check for problems with dependencies or build configuration
