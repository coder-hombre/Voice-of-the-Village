#!/bin/bash

# Bash script to create a new release
set -e

VERSION=""
RELEASE_TYPE="release"
DRY_RUN=false

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -v|--version)
            VERSION="$2"
            shift 2
            ;;
        -t|--type)
            RELEASE_TYPE="$2"
            shift 2
            ;;
        --dry-run)
            DRY_RUN=true
            shift
            ;;
        -h|--help)
            echo "Usage: $0 -v VERSION [-t TYPE] [--dry-run]"
            echo "  -v, --version    Version to release (e.g., 1.0.0)"
            echo "  -t, --type       Release type (release, beta, alpha) [default: release]"
            echo "  --dry-run        Show what would be done without executing"
            echo "  -h, --help       Show this help message"
            exit 0
            ;;
        *)
            echo "Unknown option $1"
            exit 1
            ;;
    esac
done

# Validate required parameters
if [[ -z "$VERSION" ]]; then
    echo "Error: Version is required. Use -v or --version"
    exit 1
fi

# Validate version format
if [[ ! "$VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    echo "Error: Version must be in format X.Y.Z (e.g., 1.0.0)"
    exit 1
fi

# Validate release type
if [[ ! "$RELEASE_TYPE" =~ ^(release|beta|alpha)$ ]]; then
    echo "Error: Release type must be one of: release, beta, alpha"
    exit 1
fi

echo "🚀 Voice of the Village Release Script"
echo "Version: $VERSION"
echo "Release Type: $RELEASE_TYPE"

# Check if we're in the right directory
if [[ ! -f "build.gradle" ]]; then
    echo "Error: This script must be run from the project root directory"
    exit 1
fi

# Check if git is clean
if [[ -n $(git status --porcelain) ]]; then
    echo "Warning: Git working directory is not clean:"
    git status --porcelain
    read -p "Continue anyway? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "Aborted by user"
        exit 1
    fi
fi

# Update version in gradle.properties
echo "📝 Updating version in gradle.properties..."
if [[ "$DRY_RUN" == true ]]; then
    echo "[DRY RUN] Would update mod_version to $VERSION in gradle.properties"
else
    sed -i.bak "s/mod_version=.*/mod_version=$VERSION/" gradle.properties
    rm gradle.properties.bak 2>/dev/null || true
fi

# Run tests
echo "🧪 Running tests..."
if [[ "$DRY_RUN" == true ]]; then
    echo "[DRY RUN] Would run: ./gradlew test"
else
    ./gradlew test --no-daemon
fi

# Build the mod
echo "🔨 Building mod..."
if [[ "$DRY_RUN" == true ]]; then
    echo "[DRY RUN] Would run: ./gradlew build"
else
    ./gradlew build --no-daemon
fi

# Create git tag
TAG_NAME="v$VERSION"
echo "🏷️ Creating git tag: $TAG_NAME..."
if [[ "$DRY_RUN" == true ]]; then
    echo "[DRY RUN] Would run: git tag $TAG_NAME"
    echo "[DRY RUN] Would run: git push origin $TAG_NAME"
else
    git add gradle.properties
    git commit -m "Release version $VERSION"
    git tag "$TAG_NAME"
    git push origin main
    git push origin "$TAG_NAME"
fi

echo "✅ Release process completed!"
echo "The GitHub Actions workflow will now:"
echo "  1. Run all tests"
echo "  2. Build the mod"
echo "  3. Upload to CurseForge"
echo "  4. Upload to Modrinth"
echo "  5. Create GitHub release"
echo ""

# Extract repository info from git remote
REPO_URL=$(git config --get remote.origin.url)
if [[ "$REPO_URL" =~ github\.com[:/]([^/]+)/([^/]+)(\.git)?$ ]]; then
    REPO_PATH="${BASH_REMATCH[1]}/${BASH_REMATCH[2]}"
    echo "Monitor the progress at: https://github.com/$REPO_PATH/actions"
fi