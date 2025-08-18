# PowerShell script for manual version bumping
param(
    [Parameter(Mandatory=$false)]
    [ValidateSet("patch", "minor", "major")]
    [string]$BumpType = "patch",
    
    [Parameter(Mandatory=$false)]
    [string]$Message = "",
    
    [Parameter(Mandatory=$false)]
    [switch]$DryRun,
    
    [Parameter(Mandatory=$false)]
    [switch]$SkipTests,
    
    [Parameter(Mandatory=$false)]
    [switch]$SkipCommit
)

Write-Host "🔖 Voice of the Village Version Bump Script" -ForegroundColor Cyan
Write-Host "Bump Type: $BumpType" -ForegroundColor Green

# Check if we're in the right directory
if (-not (Test-Path "gradle.properties")) {
    Write-Error "This script must be run from the project root directory"
    exit 1
}

# Get current version
$gradleProps = Get-Content "gradle.properties"
$currentVersionLine = $gradleProps | Where-Object { $_ -match "mod_version=" }
$currentVersion = $currentVersionLine -replace "mod_version=", ""

Write-Host "Current Version: $currentVersion" -ForegroundColor Blue

# Parse version
$versionParts = $currentVersion.Split('.')
$major = [int]$versionParts[0]
$minor = [int]$versionParts[1]
$patch = [int]$versionParts[2]

# Bump version
switch ($BumpType) {
    "major" {
        $major++
        $minor = 0
        $patch = 0
    }
    "minor" {
        $minor++
        $patch = 0
    }
    "patch" {
        $patch++
    }
}

$newVersion = "$major.$minor.$patch"
Write-Host "New Version: $newVersion" -ForegroundColor Green

if ($DryRun) {
    Write-Host "[DRY RUN] Would update version to $newVersion" -ForegroundColor Yellow
    exit 0
}

# Update gradle.properties
Write-Host "📝 Updating gradle.properties..." -ForegroundColor Blue
$gradleProps = $gradleProps -replace "mod_version=.*", "mod_version=$newVersion"
$gradleProps | Set-Content "gradle.properties"

# Run tests unless skipped
if (-not $SkipTests) {
    Write-Host "🧪 Running tests..." -ForegroundColor Blue
    & ./gradlew test --no-daemon
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Tests failed! Aborting version bump."
        # Restore original version
        $gradleProps = $gradleProps -replace "mod_version=.*", "mod_version=$currentVersion"
        $gradleProps | Set-Content "gradle.properties"
        exit 1
    }
}

# Build the mod
Write-Host "🔨 Building mod..." -ForegroundColor Blue
& ./gradlew build --no-daemon
if ($LASTEXITCODE -ne 0) {
    Write-Error "Build failed! Aborting version bump."
    # Restore original version
    $gradleProps = $gradleProps -replace "mod_version=.*", "mod_version=$currentVersion"
    $gradleProps | Set-Content "gradle.properties"
    exit 1
}

# Update changelog
Write-Host "📋 Updating changelog..." -ForegroundColor Blue
$date = Get-Date -Format "yyyy-MM-dd"
$commitMessage = if ($Message) { $Message } else { "Version bump to $newVersion" }

# Create new changelog content
$changelogContent = @"
# Changelog

All notable changes to the Voice of the Village mod will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [$newVersion] - $date

### Changed
- $commitMessage

"@

# Append existing changelog content (skip header and unreleased)
if (Test-Path "CHANGELOG.md") {
    $existingContent = Get-Content "CHANGELOG.md" -Raw
    $versionPattern = '## \[\d+\.\d+\.\d+\]'
    if ($existingContent -match $versionPattern) {
        $existingVersions = ($existingContent -split '(?=## \[\d+\.\d+\.\d+\])')[1..$($existingContent -split '(?=## \[\d+\.\d+\.\d+\])').Length]
        $changelogContent += ($existingVersions -join "")
    }
}

$changelogContent | Set-Content "CHANGELOG.md"

# Commit changes unless skipped
if (-not $SkipCommit) {
    Write-Host "📤 Committing changes..." -ForegroundColor Blue
    git add gradle.properties CHANGELOG.md
    git commit -m "🔖 Bump version to $newVersion [$BumpType] [skip version]"
    
    Write-Host "🏷️ Creating git tag..." -ForegroundColor Blue
    git tag "v$newVersion"
    
    Write-Host "🚀 Pushing changes..." -ForegroundColor Blue
    git push origin main
    git push origin "v$newVersion"
}

Write-Host "✅ Version bump completed!" -ForegroundColor Green
Write-Host "Version: $currentVersion → $newVersion" -ForegroundColor Cyan

if (-not $SkipCommit) {
    Write-Host "Changes have been committed and tagged." -ForegroundColor Green
    Write-Host "The auto-release workflow will trigger if this version tag matches the release pattern." -ForegroundColor Yellow
}