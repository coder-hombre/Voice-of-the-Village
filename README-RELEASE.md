# Release Guide for Voice of the Village

This guide explains how to create and publish new releases of the Voice of the Village mod.

## Prerequisites

Before you can create releases, you need to set up the following secrets in your GitHub repository:

### Required GitHub Secrets

1. **CurseForge Secrets:**
   - `CURSEFORGE_PROJECT_ID`: Your CurseForge project ID
   - `CURSEFORGE_API_TOKEN`: Your CurseForge API token

2. **Modrinth Secrets:**
   - `MODRINTH_PROJECT_ID`: Your Modrinth project ID  
   - `MODRINTH_API_TOKEN`: Your Modrinth API token

### Setting up Secrets

1. Go to your GitHub repository
2. Click on **Settings** → **Secrets and variables** → **Actions**
3. Click **New repository secret**
4. Add each of the required secrets listed above

### Getting API Tokens

**CurseForge:**
1. Go to [CurseForge Console](https://console.curseforge.com/)
2. Navigate to **API Keys**
3. Create a new API key with upload permissions

**Modrinth:**
1. Go to [Modrinth](https://modrinth.com/)
2. Go to your account settings
3. Navigate to **PATs** (Personal Access Tokens)
4. Create a new token with upload permissions

## Creating a Release

### Method 1: Using the Release Script (Recommended)

#### On Windows:
```powershell
# Navigate to your project directory
cd path\to\voice-of-the-village

# Run the release script
.\scripts\release.ps1 -Version "1.0.0" -ReleaseType "release"

# For beta releases:
.\scripts\release.ps1 -Version "1.0.0-beta.1" -ReleaseType "beta"

# For alpha releases:
.\scripts\release.ps1 -Version "1.0.0-alpha.1" -ReleaseType "alpha"

# Dry run to see what would happen:
.\scripts\release.ps1 -Version "1.0.0" -DryRun
```

#### On Linux/macOS:
```bash
# Navigate to your project directory
cd path/to/voice-of-the-village

# Run the release script
./scripts/release.sh -v "1.0.0" -t "release"

# For beta releases:
./scripts/release.sh -v "1.0.0-beta.1" -t "beta"

# For alpha releases:
./scripts/release.sh -v "1.0.0-alpha.1" -t "alpha"

# Dry run to see what would happen:
./scripts/release.sh -v "1.0.0" --dry-run
```

### Method 2: Manual Git Tag

1. Update the version in `gradle.properties`:
   ```properties
   mod_version=1.0.0
   ```

2. Commit and push your changes:
   ```bash
   git add gradle.properties
   git commit -m "Release version 1.0.0"
   git push origin main
   ```

3. Create and push a git tag:
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```

### Method 3: GitHub Actions Manual Trigger

1. Go to your GitHub repository
2. Click on **Actions**
3. Select **Build and Release Mod**
4. Click **Run workflow**
5. Enter the version and release type
6. Click **Run workflow**

## What Happens During Release

The automated release process will:

1. **Run Tests**: Execute all unit and integration tests
2. **Build Mod**: Compile and package the mod JAR file
3. **Upload to CurseForge**: Publish to CurseForge with proper metadata
4. **Upload to Modrinth**: Publish to Modrinth with proper metadata
5. **Create GitHub Release**: Create a GitHub release with the JAR file
6. **Generate Changelog**: Extract changelog from CHANGELOG.md

## Release Types

- **release**: Stable release for general use
- **beta**: Pre-release for testing, marked as beta on platforms
- **alpha**: Early pre-release for development testing

## Versioning

This project follows [Semantic Versioning](https://semver.org/):

- **MAJOR.MINOR.PATCH** (e.g., 1.0.0)
- **MAJOR**: Breaking changes
- **MINOR**: New features, backwards compatible
- **PATCH**: Bug fixes, backwards compatible

For pre-releases, append:
- `-alpha.X` for alpha versions
- `-beta.X` for beta versions

## Updating the Changelog

Before creating a release, update `CHANGELOG.md`:

1. Move items from `[Unreleased]` to a new version section
2. Add the release date
3. Create a new empty `[Unreleased]` section
4. Update the links at the bottom

Example:
```markdown
## [Unreleased]

## [1.0.0] - 2024-01-15

### Added
- New feature X
- New feature Y

### Fixed
- Bug fix A
- Bug fix B
```

## Troubleshooting

### Release Failed

If a release fails:

1. Check the GitHub Actions logs for details
2. Fix any issues (tests, build, etc.)
3. Create a new release with a patch version

### Missing Secrets

If you get authentication errors:
1. Verify all required secrets are set in GitHub
2. Check that API tokens are valid and have correct permissions
3. Ensure project IDs are correct

### Build Failures

If the build fails:
1. Run tests locally: `./gradlew test`
2. Run build locally: `./gradlew build`
3. Fix any issues and commit changes
4. Create a new release

## Monitoring Releases

After triggering a release:

1. Go to **Actions** tab in your GitHub repository
2. Click on the running **Build and Release Mod** workflow
3. Monitor each job's progress
4. Check for any failures and review logs if needed

The entire process typically takes 5-10 minutes to complete.

## Post-Release

After a successful release:

1. Verify the mod appears on CurseForge and Modrinth
2. Test download and installation
3. Update any documentation or announcements
4. Monitor for user feedback and issues

## Quick Reference

| Action | Command |
|--------|---------|
| Create release | `./scripts/release.sh -v "1.0.0"` |
| Create beta | `./scripts/release.sh -v "1.0.0-beta.1" -t "beta"` |
| Dry run | `./scripts/release.sh -v "1.0.0" --dry-run` |
| Run tests | `./gradlew test` |
| Build locally | `./gradlew build` |