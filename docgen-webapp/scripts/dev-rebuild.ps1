param(
    [string]$ProjectRoot = "g:\Agent\docgen-webapp",
    [string]$CommitMessage = "",
    [switch]$SkipCommit,
    [switch]$SkipBackend,
    [switch]$SkipFrontend,
    [switch]$NoCache = $true,
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

function Write-Warn {
    param([string]$Message)
    Write-Host ("[WARN] " + $Message) -ForegroundColor Yellow
}

function Invoke-Tool {
    param(
        [Parameter(Mandatory = $true)][string]$Command,
        [Parameter(Mandatory = $true)][string]$WorkDir
    )
    Write-Host ("      > " + $Command) -ForegroundColor DarkGray
    if ($DryRun) { return }
    Push-Location $WorkDir
    try {
        Invoke-Expression $Command
    }
    finally {
        Pop-Location
    }
}

function Resolve-ProjectContext {
    param([string]$Root)
    $projectAbs = (Resolve-Path $Root).Path
    $gitRoot = (git -C $projectAbs rev-parse --show-toplevel).Trim()
    if ([string]::IsNullOrWhiteSpace($gitRoot)) {
        throw "Cannot resolve git root from: $projectAbs"
    }
    $projectRel = ""
    $gitRootNorm = $gitRoot.Replace("/", "\").TrimEnd("\")
    $projectAbsNorm = $projectAbs.Replace("/", "\").TrimEnd("\")
    if ($projectAbsNorm.StartsWith($gitRootNorm, [System.StringComparison]::OrdinalIgnoreCase)) {
        $projectRel = $projectAbsNorm.Substring($gitRootNorm.Length).TrimStart("\", "/").Replace("\", "/")
    }
    if ($projectRel -eq ".") { $projectRel = "" }
    return @{
        ProjectAbs = $projectAbs
        GitRoot = $gitRoot
        ProjectRel = $projectRel
    }
}

function Is-ExcludedPath {
    param([string]$Path)
    $p = $Path.Replace("\", "/")
    if ($p -match "(^|/)\.m2(/|$)") { return $true }
    if ($p -match "(^|/)scripts/artifacts(/|$)") { return $true }
    if ($p -match "(^|/)node_modules(/|$)") { return $true }
    if ($p -match "(^|/)dist(/|$)") { return $true }
    return $false
}

function Is-AllowedUntracked {
    param([string]$Path)
    $ext = [System.IO.Path]::GetExtension($Path).ToLowerInvariant()
    $allowed = @(
        ".java", ".kt", ".groovy", ".xml", ".yml", ".yaml",
        ".sql", ".vue", ".ts", ".tsx", ".js", ".jsx",
        ".json", ".md", ".properties", ".ps1", ".sh",
        ".css", ".scss", ".html", ".txt"
    )
    return $allowed -contains $ext
}

function Stage-ProjectChanges {
    param(
        [string]$GitRoot,
        [string]$ProjectRel
    )

    $scope = if ([string]::IsNullOrWhiteSpace($ProjectRel)) { "." } else { $ProjectRel }
    $tracked = @(git -C $GitRoot diff --name-only -- $scope)
    $untracked = @(git -C $GitRoot ls-files --others --exclude-standard -- $scope)

    $toStage = New-Object System.Collections.Generic.List[string]

    foreach ($f in $tracked) {
        if ([string]::IsNullOrWhiteSpace($f)) { continue }
        if (Is-ExcludedPath $f) { continue }
        $toStage.Add($f)
    }

    foreach ($f in $untracked) {
        if ([string]::IsNullOrWhiteSpace($f)) { continue }
        if (Is-ExcludedPath $f) { continue }
        if (-not (Is-AllowedUntracked $f)) { continue }
        $toStage.Add($f)
    }

    $unique = @($toStage | Select-Object -Unique)
    if ($unique.Count -eq 0) {
        Write-Warn "No eligible changes found to stage in project scope."
        return @()
    }

    Write-Step "Staging eligible files"
    foreach ($f in $unique) {
        Invoke-Tool -Command ("git -C `"$GitRoot`" add -- `"$f`"") -WorkDir $GitRoot
    }

    return @(git -C $GitRoot diff --cached --name-only -- $scope)
}

function Commit-ProjectChanges {
    param(
        [string]$GitRoot,
        [string]$ProjectRel,
        [string]$Message
    )
    $scope = if ([string]::IsNullOrWhiteSpace($ProjectRel)) { "." } else { $ProjectRel }
    $staged = @(git -C $GitRoot diff --cached --name-only -- $scope)
    if ($staged.Count -eq 0) {
        Write-Warn "No staged changes to commit."
        return
    }

    if ([string]::IsNullOrWhiteSpace($Message)) {
        $Message = "chore: auto commit before docker rebuild $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
    }

    Write-Step "Committing changes"
    Invoke-Tool -Command ("git -C `"$GitRoot`" commit -m `"$Message`" -- `"$scope`"") -WorkDir $GitRoot
    Write-Ok "Commit completed."
}

function Invoke-ComposeCycle {
    param(
        [string]$ComposeFile,
        [string]$ServiceName = ""
    )
    $composeDir = Split-Path -Parent $ComposeFile

    Write-Step ("Docker down: " + $ComposeFile)
    Invoke-Tool -Command ("docker compose -f `"$ComposeFile`" down") -WorkDir $composeDir

    Write-Step ("Docker build: " + $ComposeFile)
    if ([string]::IsNullOrWhiteSpace($ServiceName)) {
        $buildCmd = "docker compose -f `"$ComposeFile`" build"
    } else {
        $buildCmd = "docker compose -f `"$ComposeFile`" build $ServiceName"
    }
    if ($NoCache) {
        $buildCmd += " --no-cache"
    }
    Invoke-Tool -Command $buildCmd -WorkDir $composeDir

    Write-Step ("Docker up: " + $ComposeFile)
    if ([string]::IsNullOrWhiteSpace($ServiceName)) {
        Invoke-Tool -Command ("docker compose -f `"$ComposeFile`" up -d") -WorkDir $composeDir
    } else {
        Invoke-Tool -Command ("docker compose -f `"$ComposeFile`" up -d $ServiceName") -WorkDir $composeDir
    }

    Write-Step ("Docker ps: " + $ComposeFile)
    Invoke-Tool -Command ("docker compose -f `"$ComposeFile`" ps") -WorkDir $composeDir
}

$ctx = Resolve-ProjectContext -Root $ProjectRoot
$backendCompose = Join-Path $ctx.ProjectAbs "backend\docker-compose.yml"
$frontendCompose = Join-Path $ctx.ProjectAbs "frontend\docker-compose.yml"

if (-not (Test-Path $backendCompose)) { throw "Backend compose file not found: $backendCompose" }
if (-not (Test-Path $frontendCompose)) { throw "Frontend compose file not found: $frontendCompose" }

Write-Step "Context"
Write-Host ("      project: " + $ctx.ProjectAbs)
Write-Host ("      gitRoot: " + $ctx.GitRoot)
Write-Host ("      scope:   " + $(if ([string]::IsNullOrWhiteSpace($ctx.ProjectRel)) { "." } else { $ctx.ProjectRel }))

if (-not $SkipCommit) {
    $staged = @(Stage-ProjectChanges -GitRoot $ctx.GitRoot -ProjectRel $ctx.ProjectRel)
    if ($staged.Count -gt 0) {
        Commit-ProjectChanges -GitRoot $ctx.GitRoot -ProjectRel $ctx.ProjectRel -Message $CommitMessage
    }
}
else {
    Write-Warn "Skip commit enabled."
}

if (-not $SkipBackend) {
    Invoke-ComposeCycle -ComposeFile $backendCompose -ServiceName "backend"
}
else {
    Write-Warn "Skip backend docker cycle."
}

if (-not $SkipFrontend) {
    Invoke-ComposeCycle -ComposeFile $frontendCompose
}
else {
    Write-Warn "Skip frontend docker cycle."
}

Write-Ok "Automation finished."
