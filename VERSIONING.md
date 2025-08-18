# Automatic Versioning System

This project uses an automatic versioning system that bumps the version on every commit to the main branch. The system follows [Semantic Versioning](https://semver.org/) principles.

## 🚀 How It Works

### Automatic Version Bumping

Every commit to the `main` or `develop` branch automatically triggers a version bump:

1. **Patch Version** (default): `1.0.0` → `1.0.1`
2. **Minor Version**: `1.0.0` → `1.1.0` 
3. **Major Version**: `1.0.0` → `2.0.0`

### Version Bump Detection

The system analyzes your commit message to determine the bump type:

| Commit Message Contains | Bump Type | Example |
|------------------------|-----------|---------|
| `[major]`, `[breaking]`, `BREAKING CHANGE` | Major | `feat: new API [major]` |
| `[minor]`, `[feature]`, `feat:` | Minor | `feat: add new feature` |
| Everything else | Patch | `fix: resolve bug` |

## 📝 Commit Message Examples

### Patch Bumps (1.0.0 → 1.0.1)
```bash
git commit -m "fix: resolve audio processing bug"
git commit -m "docs: update README"
git commit -m "refactor: improve error handling"
```

### Minor Bumps (1.0.0 → 1.1.0)
```bash
git commit -m "feat: add voice recognition feature"
git commit -m "[feature] implement new GUI component"
git commit -m "[minor] enhance villager AI responses"
```

### Major Bumps (1.0.0 → 2.0.0)
```bash
git commit -m "feat: redesign API structure [major]"
git commit -m "[breaking] remove deprecated methods"
git commit -m "BREAKING CHANGE: update configuration format"
```

## 🛠️ Manual Version Bumping

### Using Scripts

#### Windows (PowerShell):
```powershell
# Patch bump (default)
.\scripts\version-bump.ps1

# Minor bump
.\scripts\version-bump.ps1 -BumpType minor

# Major bump
.\scripts\version-bump.ps1 -BumpType major

# With custom message
.\scripts\version-bump.ps1 -BumpType minor -Message "Add new feature X"

# Dry run to see what would happen
.\scripts\version-bump.ps1 -BumpType major -DryRun

# Skip tests (faster)
.\scripts\version-bump.ps1 -SkipTests

# Skip commit (just update files)
.\scripts\version-bump.ps1 -SkipCommit
```

#### Linux/macOS (Bash):
```bash
# Patch bump (default)
./scripts/version-bump.sh

# Minor bump
./scripts/version-bump.sh -t minor

# Major bump
./scripts/version-bump.sh -t major

# With custom message
./scripts/version-bump.sh -t minor -m "Add new feature X"

# Dry run to see what would happen
./scripts/version-bump.sh -t major --dry-run

# Skip tests (faster)
./scripts/version-bump.sh --skip-tests

# Skip commit (just update files)
./scripts/version-bump.sh --skip-commit
```

### Using GitHub Actions

1. Go to your repository on GitHub
2. Click **Actions** → **Auto Version Bump**
3. Click **Run workflow**
4. Select the bump type (patch, minor, major)
5. Click **Run workflow**

## 🚫 Skipping Version Bumps

To skip automatic version bumping, include `[skip version]` or `[version skip]` in your commit message:

```bash
git commit -m "docs: update README [skip version]"
git commit -m "[version skip] fix typo in comments"
```

## 🔄 What Happens During Auto-Versioning

1. **Analyze Commit**: Determines bump type from commit message
2. **Update Version**: Updates `gradle.properties` with new version
3. **Run Tests**: Ensures all tests pass with new version
4. **Build Mod**: Compiles and packages the mod
5. **Update Changelog**: Adds entry to `CHANGELOG.md`
6. **Commit Changes**: Commits version bump with `[skip version]` tag
7. **Create Tag**: Creates git tag (e.g., `v1.0.1`)
8. **Push Changes**: Pushes commit and tag to repository

## 📦 Automatic Releases

When a version tag is created, the system automatically:

1. **Builds the mod** with the tagged version
2. **Uploads to CurseForge** (if enabled and configured)
3. **Uploads to Modrinth** (if enabled and configured)
4. **Creates GitHub Release** with JAR files and changelog

## ⚙️ Configuration

### Enable/Disable Platform Uploads

Set these repository variables to control uploads:

- `ENABLE_CURSEFORGE_UPLOAD`: Set to `'true'` to enable CurseForge uploads
- `ENABLE_MODRINTH_UPLOAD`: Set to `'true'` to enable Modrinth uploads

### Required Secrets

For automatic uploads, configure these secrets:

- `CURSEFORGE_PROJECT_ID`: Your CurseForge project ID
- `CURSEFORGE_API_TOKEN`: Your CurseForge API token
- `MODRINTH_PROJECT_ID`: Your Modrinth project ID
- `MODRINTH_API_TOKEN`: Your Modrinth API token

## 🔍 Monitoring

### Check Version History
```bash
# View version tags
git tag -l "v*" --sort=-version:refname

# View recent version bumps
git log --oneline --grep="Bump version"
```

### View Current Version
```bash
# From gradle.properties
grep "mod_version=" gradle.properties

# From git tags
git describe --tags --abbrev=0
```

## 🐛 Troubleshooting

### Version Bump Failed

If the auto-version workflow fails:

1. Check the GitHub Actions logs
2. Common issues:
   - Tests failing
   - Build errors
   - Git conflicts

### Manual Recovery

If you need to manually fix versioning:

```bash
# Reset to previous version
git reset --hard HEAD~1
git tag -d v1.0.1  # Delete problematic tag
git push --delete origin v1.0.1

# Fix issues and try again
./scripts/version-bump.sh -t patch
```

### Disable Auto-Versioning

To temporarily disable auto-versioning:

1. Add `[skip version]` to all commit messages, or
2. Disable the workflow in GitHub Actions settings

## 📊 Version Strategy

### Recommended Approach

- **Patch**: Bug fixes, documentation, refactoring
- **Minor**: New features, enhancements, non-breaking changes
- **Major**: Breaking changes, API redesigns, major rewrites

### Development Workflow

1. **Feature branches**: Work on features in separate branches
2. **Pull requests**: Merge to main via PR with appropriate commit message
3. **Automatic versioning**: Let the system handle version bumps
4. **Automatic releases**: Tags trigger releases to mod platforms

## 🎯 Quick Reference

| Action | Command | Result |
|--------|---------|--------|
| Patch bump | `git commit -m "fix: bug"` | 1.0.0 → 1.0.1 |
| Minor bump | `git commit -m "feat: feature"` | 1.0.0 → 1.1.0 |
| Major bump | `git commit -m "feat: api [major]"` | 1.0.0 → 2.0.0 |
| Skip version | `git commit -m "docs: update [skip version]"` | No version change |
| Manual bump | `./scripts/version-bump.sh -t minor` | Interactive bump |
| View version | `grep mod_version gradle.properties` | Show current version |

This system ensures every meaningful change gets a proper version number and can be automatically released to mod platforms! 🚀