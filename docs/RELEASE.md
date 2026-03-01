# Release Process

## Trigger

`release.yml` runs only when a PR from `release/*` is merged into `main` or `master`.

## Steps

1. Run the "Bump Version" workflow and choose `patch`, `minor`, or `major`.
2. The workflow updates `version.properties`, creates `release/v{version}`, and opens a PR.
3. Review and merge that PR into `main` or `master`.
4. The release workflow builds artifacts (Ubuntu/macOS/Windows) and creates GitHub Release `v{version}`.

## Workflow Files

- `.github/workflows/bump-version.yml`
- `.github/workflows/release.yml`
