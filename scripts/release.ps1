# PowerShell script to create a new release
param(
    [Parameter(Mandatory=$true)]
    [string]$Version,
    
    [Parameter(Mandatory=$false)]
    [ValidateSet("release", "beta", "alpha")]
    [string]$ReleaseType = "release",
    
    [Parameter(Mandatory=$false)]
    [switch]$DryRun
)

Write-Host "🚀 Voice of the Village Release Script" -ForegroundColor Cyan
Write-Host "Version: $Version" -ForegroundColor Green
Write-Host "Release Type: $ReleaseType" -ForegroundColor Green

# Validate version format
if ($Version -notmatch '^\d+\.\d+\.\d+$') {
    Write-Error "Version must be in format X.Y.Z (e.g., 1.0.0)"
    exit 1
}

# Check if we're in the right directory
if (-not (Test-Path "build.gradle")) {
    Write-Error "This script must be run from the project root directory"
    exit 1
}

# Check if git is clean
$gitStatus = git status --porcelain
if ($gitStatus) {
    Write-Warning "Git working directory is not clean:"
    Write-Host $gitStatus -ForegroundColor Yellow
    $continue = Read-Host "Continue anyway? (y/N)"
    if ($continue -ne "y" -and $continue -ne "Y") {
        Write-Host "Aborted by user" -ForegroundColor Red
        exit 1
    }
}

# Update version in gradle.properties
Write-Host "📝 Updating version in gradle.properties..." -ForegroundColor Blue
$gradleProps = Get-Content "gradle.properties"
$gradleProps = $gradleProps -replace "mod_version=.*", "mod_version=$Version"
$gradleProps | Set-Content "gradle.properties"

# Run tests
Write-Host "🧪 Running tests..." -ForegroundColor Blue
if ($DryRun) {
    Write-Host "[DRY RUN] Would run: ./gradlew test" -ForegroundColor Yellow
} else {
    & ./gradlew test --no-daemon
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Tests failed! Aborting release."
        exit 1
    }
}

# Build the mod
Write-Host "🔨 Building mod..." -ForegroundColor Blue
if ($DryRun) {
    Write-Host "[DRY RUN] Would run: ./gradlew build" -ForegroundColor Yellow
} else {
    & ./gradlew build --no-daemon
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Build failed! Aborting release."
        exit 1
    }
}

# Create git tag
$tagName = "v$Version"
Write-Host "🏷️ Creating git tag: $tagName..." -ForegroundColor Blue
if ($DryRun) {
    Write-Host "[DRY RUN] Would run: git tag $tagName" -ForegroundColor Yellow
    Write-Host "[DRY RUN] Would run: git push origin $tagName" -ForegroundColor Yellow
} else {
    git add gradle.properties
    git commit -m "Release version $Version"
    git tag $tagName
    git push origin main
    git push origin $tagName
}

Write-Host "✅ Release process completed!" -ForegroundColor Green
Write-Host "The GitHub Actions workflow will now:" -ForegroundColor Cyan
Write-Host "  1. Run all tests" -ForegroundColor White
Write-Host "  2. Build the mod" -ForegroundColor White
Write-Host "  3. Upload to CurseForge" -ForegroundColor White
Write-Host "  4. Upload to Modrinth" -ForegroundColor White
Write-Host "  5. Create GitHub release" -ForegroundColor White
Write-Host ""
Write-Host "Monitor the progress at: https://github.com/$(git config --get remote.origin.url | Select-String -Pattern '([^/]+)/([^/]+)\.git$' | ForEach-Object { $_.Matches[0].Groups[1].Value + '/' + $_.Matches[0].Groups[2].Value })/actions" -ForegroundColor Blue