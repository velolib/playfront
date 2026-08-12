# 1. Determine the root folder of the repository
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
$repoRoot = Split-Path -Parent $scriptDir

# 2. Define relative paths based on the repo root
$projectDir = Join-Path $repoRoot "PlayfrontSMTC\windows"
$targetDir = Join-Path $repoRoot "src\main\resources\assets\playfront"
$targetExe = Join-Path $targetDir "PlayfrontSMTC.exe"

Write-Host "Starting build for PlayfrontSMTC..." -ForegroundColor Cyan

# 3. Enter the C# project directory and build
Push-Location $projectDir

dotnet publish -c Release -r win-x64 --self-contained true -p:PublishSingleFile=true -p:EnableCompressionInSingleFile=true

$buildExitCode = $LASTEXITCODE

Pop-Location # Return to original directory

# Check if the build was successful
if ($buildExitCode -ne 0) {
    Write-Host "Build failed with exit code $buildExitCode. Exiting..." -ForegroundColor Red
    exit $buildExitCode
}

Write-Host "`nBuild successful. Locating executable..." -ForegroundColor Green

# 4. Find the executable (using a wildcard for the framework folder so it's future-proof)
$exePath = Get-ChildItem -Path (Join-Path $projectDir "bin\Release\*\win-x64\publish\PlayfrontSMTC.exe") -ErrorAction SilentlyContinue | Select-Object -First 1

if ($exePath) {
    # 5. Ensure the destination resources directory exists
    if (-not (Test-Path $targetDir)) {
        Write-Host "Creating target directory: $targetDir" -ForegroundColor DarkGray
        New-Item -ItemType Directory -Force -Path $targetDir | Out-Null
    }

    # 6. Copy the file
    Write-Host "Copying to resources..." -ForegroundColor Cyan
    Copy-Item -Path $exePath.FullName -Destination $targetExe -Force
    Write-Host "Successfully copied PlayfrontSMTC.exe to: src\main\resources\assets\playfront\" -ForegroundColor Green
} else {
    Write-Host "Error: Could not find the published PlayfrontSMTC.exe!" -ForegroundColor Red
    exit 1
}