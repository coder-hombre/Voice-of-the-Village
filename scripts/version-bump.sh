#!/bin/bash

# Bash script for manual version bumping
set -e

BUMP_TYPE="patch"
MESSAGE=""
DRY_RUN=false
SKIP_TESTS=false
SKIP_COMMIT=false

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -t|--type)
            BUMP_TYPE="$2"
            shift 2
            ;;
        -m|--message)
            MESSAGE="$2"
            shift 2
            ;;
        --dry-run)
            DRY_RUN=true
            shift
            ;;
        --skip-tests)
            SKIP_TESTS=true
            shift
            ;;
        --skip-commit)
            SKIP_COMMIT=true
            shift
            ;;
        -h|--help)
            echo "Usage: $0 [-t TYPE] [-m MESSAGE] [--dry-run] [--skip-tests] [--skip-commit]"
            echo "  -t, --type       Version bump type (patch, minor, major) [default: patch]"
            echo "  -m, --message    Commit message [default: auto-generated]"
            echo "  --dry-run        Show what would be done without executing"
            echo "  --skip-tests     Skip running tests"
            echo "  --skip-commit    Skip committing and tagging"
            echo "  -h, --help       Show this help message"
            exit 0
            ;;
        *)
            echo "Unknown option $1"
            exit 1
            ;;
    esac
done

# Validate bump type
if [[ ! "$BUMP_TYPE" =~ ^(patch|minor|major)$ ]]; then
    echo "Error: Bump type must be one of: patch, minor, major"
    exit 1
fi

echo "🔖 Voice of the Village Version Bump Script"
echo "Bump Type: $BUMP_TYPE"

# Check if we're in the right directory
if [[ ! -f "gradle.properties" ]]; then
    echo "Error: This script must be run from the project root directory"
    exit 1
fi

# Get current version
CURRENT_VERSION=$(grep "mod_version=" gradle.properties | cut -d'=' -f2)
echo "Current Version: $CURRENT_VERSION"

# Parse version
IFS='.' read -ra VERSION_PARTS <<< "$CURRENT_VERSION"
MAJOR=${VERSION_PARTS[0]}
MINOR=${VERSION_PARTS[1]}
PATCH=${VERSION_PARTS[2]}

# Bump version
case $BUMP_TYPE in
    "major")
        MAJOR=$((MAJOR + 1))
        MINOR=0
        PATCH=0
        ;;
    "minor")
        MINOR=$((MINOR + 1))
        PATCH=0
        ;;
    "patch")
        PATCH=$((PATCH + 1))
        ;;
esac

NEW_VERSION="$MAJOR.$MINOR.$PATCH"
echo "New Version: $NEW_VERSION"

if [[ "$DRY_RUN" == true ]]; then
    echo "[DRY RUN] Would update version to $NEW_VERSION"
    exit 0
fi

# Update gradle.properties
echo "📝 Updating gradle.properties..."
sed -i.bak "s/mod_version=.*/mod_version=$NEW_VERSION/" gradle.properties
rm gradle.properties.bak 2>/dev/null || true

# Run tests unless skipped
if [[ "$SKIP_TESTS" != true ]]; then
    echo "🧪 Running tests..."
    if ! ./gradlew test --no-daemon; then
        echo "Tests failed! Restoring original version."
        sed -i.bak "s/mod_version=.*/mod_version=$CURRENT_VERSION/" gradle.properties
        rm gradle.properties.bak 2>/dev/null || true
        exit 1
    fi
fi

# Build the mod
echo "🔨 Building mod..."
if ! ./gradlew build --no-daemon; then
    echo "Build failed! Restoring original version."
    sed -i.bak "s/mod_version=.*/mod_version=$CURRENT_VERSION/" gradle.properties
    rm gradle.properties.bak 2>/dev/null || true
    exit 1
fi

# Update changelog
echo "📋 Updating changelog..."
DATE=$(date +%Y-%m-%d)
COMMIT_MESSAGE=${MESSAGE:-"Version bump to $NEW_VERSION"}

# Create new changelog content
cat > temp_changelog.md << EOF
# Changelog

All notable changes to the Voice of the Village mod will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [$NEW_VERSION] - $DATE

### Changed
- $COMMIT_MESSAGE

EOF

# Append existing changelog content (skip header and unreleased)
if [[ -f "CHANGELOG.md" ]]; then
    # Find the first version entry and append everything from there
    awk '/^## \[[0-9]/{found=1} found{print}' CHANGELOG.md >> temp_changelog.md
fi

mv temp_changelog.md CHANGELOG.md

# Commit changes unless skipped
if [[ "$SKIP_COMMIT" != true ]]; then
    echo "📤 Committing changes..."
    git add gradle.properties CHANGELOG.md
    git commit -m "🔖 Bump version to $NEW_VERSION [$BUMP_TYPE] [skip version]"
    
    echo "🏷️ Creating git tag..."
    git tag "v$NEW_VERSION"
    
    echo "🚀 Pushing changes..."
    git push origin main
    git push origin "v$NEW_VERSION"
fi

echo "✅ Version bump completed!"
echo "Version: $CURRENT_VERSION → $NEW_VERSION"

if [[ "$SKIP_COMMIT" != true ]]; then
    echo "Changes have been committed and tagged."
    echo "The auto-release workflow will trigger if this version tag matches the release pattern."
fi