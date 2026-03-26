param(
    [string]$ProjectRoot = "",
    [string]$Registry = "harbor.ahtongtu.cn",
    [string]$Repository = "ai-req-tool",
    [string]$BackendImageName = "backend",
    [string]$FrontendImageName = "frontend",
    [string]$Tag = "",
    [string]$Username = $env:HARBOR_USERNAME,
    [string]$Password = $env:HARBOR_PASSWORD,
    [switch]$SkipLatest,
    [switch]$SkipLogin,
    [switch]$DryRun
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Write-Step {
    param([string]$Message)
    Write-Host ""
    Write-Host ("[STEP] " + $Message) -ForegroundColor Cyan
}

function Write-Ok {
    param([string]$Message)
    Write-Host ("[ OK ] " + $Message) -ForegroundColor Green
}

function Test-ProjectRootCandidate {
    param([string]$Root)
    if ([string]::IsNullOrWhiteSpace($Root)) {
        return $false
    }
    if (-not (Test-Path $Root)) {
        return $false
    }
    $resolved = (Resolve-Path $Root).Path
    return (Test-Path (Join-Path $resolved "backend\Dockerfile")) -and
           (Test-Path (Join-Path $resolved "frontend\Dockerfile"))
}

function Resolve-ProjectRoot {
    param([string]$Root)

    if (-not [string]::IsNullOrWhiteSpace($Root)) {
        if (-not (Test-ProjectRootCandidate $Root)) {
            throw "Invalid ProjectRoot: $Root. Expected a docgen-webapp directory containing backend/Dockerfile and frontend/Dockerfile."
        }
        return (Resolve-Path $Root).Path
    }

    $candidates = @(
        $PWD.Path,
        (Join-Path $PSScriptRoot "..")
    )

    foreach ($candidate in $candidates) {
        if (Test-ProjectRootCandidate $candidate) {
            return (Resolve-Path $candidate).Path
        }
    }

    throw "Cannot resolve ProjectRoot automatically. Please run the script inside docgen-webapp or pass -ProjectRoot explicitly."
}

function Invoke-Tool {
    param(
        [Parameter(Mandatory = $true)][string]$Command,
        [string]$WorkDir = ""
    )
    Write-Host ("      > " + $Command) -ForegroundColor DarkGray
    if ($DryRun) {
        return
    }
    if ([string]::IsNullOrWhiteSpace($WorkDir)) {
        Invoke-Expression $Command
        return
    }
    Push-Location $WorkDir
    try {
        Invoke-Expression $Command
    }
    finally {
        Pop-Location
    }
}

function Resolve-DefaultTag {
    param([string]$Root)
    if (-not [string]::IsNullOrWhiteSpace($Tag)) {
        return $Tag
    }
    try {
        $gitTag = (git -C $Root rev-parse --short HEAD).Trim()
        if (-not [string]::IsNullOrWhiteSpace($gitTag)) {
            return $gitTag
        }
    }
    catch {
        # fallback below
    }
    return (Get-Date -Format "yyyyMMdd-HHmmss")
}

function Publish-Image {
    param(
        [string]$ContextDir,
        [string]$ImageBase,
        [string]$ImageTag
    )

    $taggedImage = "${ImageBase}:${ImageTag}"
    Invoke-Tool -Command ("docker build -t `"$taggedImage`" `"$ContextDir`"") -WorkDir $ContextDir
    Invoke-Tool -Command ("docker push `"$taggedImage`"")

    if (-not $SkipLatest) {
        $latestImage = "${ImageBase}:latest"
        Invoke-Tool -Command ("docker tag `"$taggedImage`" `"$latestImage`"")
        Invoke-Tool -Command ("docker push `"$latestImage`"")
    }
}

$resolvedProjectRoot = Resolve-ProjectRoot -Root $ProjectRoot
$resolvedTag = Resolve-DefaultTag -Root $resolvedProjectRoot

$backendDir = Join-Path $resolvedProjectRoot "backend"
$frontendDir = Join-Path $resolvedProjectRoot "frontend"

$backendImageBase = "$Registry/$Repository/$BackendImageName"
$frontendImageBase = "$Registry/$Repository/$FrontendImageName"

Write-Step "Context"
Write-Host ("      projectRoot: " + $resolvedProjectRoot)
Write-Host ("      registry:    " + $Registry)
Write-Host ("      repository:  " + $Repository)
Write-Host ("      tag:         " + $resolvedTag)
Write-Host ("      backend:     " + $backendImageBase)
Write-Host ("      frontend:    " + $frontendImageBase)

if (-not $SkipLogin) {
    if (-not $DryRun -and [string]::IsNullOrWhiteSpace($Username)) {
        throw "Harbor username is required. Pass -Username or set HARBOR_USERNAME."
    }
    if (-not $DryRun -and [string]::IsNullOrWhiteSpace($Password)) {
        throw "Harbor password is required. Pass -Password or set HARBOR_PASSWORD."
    }

    Write-Step "Docker login"
    if ($DryRun) {
        $dryRunUser = if ([string]::IsNullOrWhiteSpace($Username)) { "<username>" } else { $Username }
        Write-Host ("      > <password hidden> | docker login " + $Registry + " -u " + $dryRunUser + " --password-stdin") -ForegroundColor DarkGray
    } else {
        $Password | docker login $Registry -u $Username --password-stdin
    }
    Write-Ok "Docker login completed."
} else {
    Write-Host "[WARN] Skip login enabled." -ForegroundColor Yellow
}

Write-Step "Build and push backend image"
Publish-Image -ContextDir $backendDir -ImageBase $backendImageBase -ImageTag $resolvedTag

Write-Step "Build and push frontend image"
Publish-Image -ContextDir $frontendDir -ImageBase $frontendImageBase -ImageTag $resolvedTag

Write-Ok "Docker build and push finished."
Write-Host ("      backend image:  " + $backendImageBase + ":" + $resolvedTag)
Write-Host ("      frontend image: " + $frontendImageBase + ":" + $resolvedTag)
