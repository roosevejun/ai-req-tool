param(
    [string]$BackendBaseUrl = "http://localhost:8080",
    [string]$Username = "admin",
    [string]$Password = "Admin@123",
    [int]$MaxChatRounds = 6
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

function Invoke-ApiJson {
    param(
        [Parameter(Mandatory = $true)][ValidateSet("GET", "POST", "PUT", "DELETE")] [string]$Method,
        [Parameter(Mandatory = $true)][string]$Path,
        [object]$Body = $null,
        [string]$Token = ""
    )

    $uri = ($BackendBaseUrl.TrimEnd("/") + $Path)
    $headers = @{}
    if ($Token) {
        $headers["Authorization"] = "Bearer $Token"
    }

    $invokeParams = @{
        Uri         = $uri
        Method      = $Method
        Headers     = $headers
        ContentType = "application/json; charset=utf-8"
    }
    if ($null -ne $Body) {
        $invokeParams["Body"] = ($Body | ConvertTo-Json -Depth 20 -Compress)
    }

    $resp = Invoke-RestMethod @invokeParams
    if ($null -eq $resp) {
        throw "API response is null: $Method $Path"
    }
    if ($resp.code -ne 0) {
        throw "API failed: $Method $Path => code=$($resp.code), message=$($resp.message), traceId=$($resp.traceId)"
    }
    return $resp.data
}

function Invoke-ApiDownload {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][string]$OutputFile,
        [string]$Token = ""
    )

    $uri = ($BackendBaseUrl.TrimEnd("/") + $Path)
    $headers = @{}
    if ($Token) {
        $headers["Authorization"] = "Bearer $Token"
    }
    Invoke-WebRequest -Uri $uri -Method GET -Headers $headers -OutFile $OutputFile | Out-Null
}

function To-Array {
    param([object]$Value)
    if ($null -eq $Value) {
        return @()
    }
    if ($Value -is [System.Array]) {
        return $Value
    }
    return @($Value)
}

function Build-ChatReply {
    param([object[]]$UnconfirmedItems)
    if ($UnconfirmedItems.Count -eq 0) {
        return "Please complete all missing requirement details with concrete and testable defaults."
    }
    $lines = @("Please resolve all unconfirmed items and include them in the final PRD:")
    foreach ($item in $UnconfirmedItems) {
        $lines += "- ${item}: provide a concrete, implementable and testable answer."
    }
    return ($lines -join "`n")
}

function Build-ForcedClarifyReply {
    return @"
Please confirm and apply the following concrete defaults into the PRD:

[Map type / provider]
- Use OpenStreetMap as default map provider.
- Use WGS84 coordinate system (EPSG:4326).
- Default zoom level 12, allowed zoom range 3-18.
- Enable pan, zoom, and marker click interaction.

[Realtime update]
- Refresh vehicle location and online status every 5 seconds.
- Maximum acceptable end-to-end delay: 10 seconds.

[Online status display]
- Online = green marker, offline = gray marker, abnormal = red marker.
- Show last heartbeat time in tooltip/popup.

[History playback]
- Keep and display last 5 minutes trajectory by default.
- Playback controls: play/pause, 1x/2x speed.

[Scope and acceptance]
- Include these defaults as accepted requirement details.
- If any item is still unclear, use these defaults and mark as "assumed default" in PRD.

[Template / model]
- Template style: Generic software requirement template.
- Model type for generation: GPT-4o-mini compatible chat-completions style.
- Output language: Chinese.
"@
}

function Build-ModelTypeReply {
    return @"
Please confirm these template/model details and mark them as confirmed:
- Template category: Generic software requirements template.
- Model type: GPT-4o-mini compatible chat-completions model.
- Output language: Chinese.
- If backend runtime uses another compatible model, keep this as default assumption and continue generation.
"@
}

function Extract-TemplateQuestion {
    param([string]$Message)
    if ([string]::IsNullOrWhiteSpace($Message)) {
        return ""
    }
    $part = $Message
    $marker = "Suggested reply template:"
    $idx = $Message.IndexOf($marker)
    if ($idx -ge 0) {
        $part = $Message.Substring($idx + $marker.Length)
    }
    $m = [regex]::Match($part, "\[(?<q>[^\]]+)\]", [System.Text.RegularExpressions.RegexOptions]::Singleline)
    if ($m.Success) {
        return ($m.Groups["q"].Value.Trim())
    }
    return ""
}

function Build-TemplateAnswerReply {
    param([string]$QuestionText)
    if ([string]::IsNullOrWhiteSpace($QuestionText)) {
        return ""
    }
    return @"
[$QuestionText]
- Confirmed with concrete default: use generic software requirement template + map visualization defaults.
- Map provider: OpenStreetMap; coordinate system: WGS84; zoom default 12 (range 3-18).
- Realtime refresh every 5s; online/offline/abnormal status color coding.
- Keep 5-minute history playback with play/pause and 1x/2x speed.
- If this item still has ambiguity, apply these defaults and mark as assumed default in PRD.
"@
}

function Build-CloseAllUnconfirmedReply {
    param([object[]]$UnconfirmedItems)
    $items = To-Array $UnconfirmedItems
    $lines = @(
        "For this test run, confirm ALL current unconfirmed items with concrete defaults.",
        "Move all items into confirmedItems, set unconfirmedItems to empty, and set pendingQuestion to empty.",
        "Do not introduce any new unconfirmed item.",
        ""
    )
    if ($items.Count -eq 0) {
        $lines += "[No remaining item]"
        $lines += "- Confirmed. Please proceed to READY state."
    } else {
        foreach ($item in $items) {
            $lines += ("[" + $item + "]")
            $lines += "- Confirmed with default implementation-ready values."
            $lines += "- Map: OpenStreetMap, WGS84, default zoom 12 (range 3-18)."
            $lines += "- Realtime refresh 5s; online/offline/abnormal status colors."
            $lines += "- 5-minute history playback enabled."
            $lines += ""
        }
    }
    return ($lines -join "`n")
}

Write-Host "===== P0 E2E Check: project-requirement-ai-workbench =====" -ForegroundColor Magenta
Write-Host "Backend: $BackendBaseUrl"
Write-Host "User: $Username"

Write-Step "1) Health check /api/docgen/health"
$health = Invoke-ApiJson -Method GET -Path "/api/docgen/health"
if ($health.status -ne "UP") {
    throw "Health check failed: status=$($health.status)"
}
Write-Ok "Health check passed"

Write-Step "2) Login and fetch token"
$loginData = Invoke-ApiJson -Method POST -Path "/api/system/auth/login" -Body @{
    username = $Username
    password = $Password
}
$token = [string]$loginData.token
if ([string]::IsNullOrWhiteSpace($token)) {
    throw "Login success but token is empty"
}
Write-Ok "Login succeeded"

Write-Step "3) Validate current user /api/system/auth/me"
$me = Invoke-ApiJson -Method GET -Path "/api/system/auth/me" -Token $token
Write-Ok ("Current user: " + $me.username + " (id=" + $me.userId + ")")

$ts = Get-Date -Format "yyyyMMddHHmmss"
$projectKey = "P0-$ts"
$projectName = "P0-Project-$ts"

Write-Step "4) Create project"
$projectId = Invoke-ApiJson -Method POST -Path "/api/projects" -Token $token -Body @{
    projectKey  = $projectKey
    projectName = $projectName
    description = "Created by P0 e2e check script"
    visibility  = "PRIVATE"
}
Write-Ok ("Project created, id=" + $projectId)

Write-Step "5) Verify project list"
$projects = To-Array (Invoke-ApiJson -Method GET -Path "/api/projects" -Token $token)
$projectExists = $projects | Where-Object { $_.id -eq $projectId }
if ($null -eq $projectExists) {
    throw "Project not found in list, id=$projectId"
}
Write-Ok "Project exists in list"

Write-Step "6) Create requirement"
$requirementId = Invoke-ApiJson -Method POST -Path "/api/projects/$projectId/requirements" -Token $token -Body @{
    title    = "Vehicle location and online status display"
    summary  = "Get location and online status from ThingsBoard and show on map."
    priority = "P1"
    status   = "DRAFT"
}
Write-Ok ("Requirement created, id=" + $requirementId)

Write-Step "7) Verify requirement list"
$requirements = To-Array (Invoke-ApiJson -Method GET -Path "/api/projects/$projectId/requirements" -Token $token)
$requirementExists = $requirements | Where-Object { $_.id -eq $requirementId }
if ($null -eq $requirementExists) {
    throw "Requirement not found in list, id=$requirementId"
}
Write-Ok "Requirement exists in list"

Write-Step "8) Create AI workbench job"
$createJob = Invoke-ApiJson -Method POST -Path "/api/requirements/$requirementId/docgen/jobs" -Token $token -Body @{
    businessDescription = "Display vehicle realtime location and online status from ThingsBoard on a map."
    previousPrdMarkdown = ""
}
$jobId = [string]$createJob.jobId
if ([string]::IsNullOrWhiteSpace($jobId)) {
    throw "Create job succeeded but jobId is empty"
}
Write-Ok ("Job created, id=" + $jobId)

Write-Step "9) Clarify and generate PRD"
$ready = [bool]$createJob.readyToGenerate
$status = [string]$createJob.status
$chatRounds = 0

while (-not $ready -and $chatRounds -lt $MaxChatRounds) {
    $chatRounds++
    $job = Invoke-ApiJson -Method GET -Path "/api/requirements/$requirementId/docgen/jobs/$jobId" -Token $token
    $unconfirmed = To-Array $job.unconfirmedItems
    $ready = [bool]$job.readyToGenerate
    $status = [string]$job.status
    if ($ready) {
        break
    }
    $message = Build-ChatReply -UnconfirmedItems $unconfirmed
    $chatResp = Invoke-ApiJson -Method POST -Path "/api/requirements/$requirementId/docgen/jobs/$jobId/chat" -Token $token -Body @{
        message = $message
    }
    $ready = [bool]$chatResp.readyToGenerate
    $status = [string]$chatResp.status
    Write-Host ("  - chat round " + $chatRounds + ", status=" + $status + ", readyToGenerate=" + $ready)
}

if (-not $ready) {
    Write-Warn "Job is still not readyToGenerate. Sending forced structured clarify reply."
    $forcedReply = Build-ForcedClarifyReply
    $forcedChatResp = Invoke-ApiJson -Method POST -Path "/api/requirements/$requirementId/docgen/jobs/$jobId/chat" -Token $token -Body @{
        message = $forcedReply
    }
    $ready = [bool]$forcedChatResp.readyToGenerate
    $status = [string]$forcedChatResp.status
    Write-Host ("  - forced clarify, status=" + $status + ", readyToGenerate=" + $ready)
}

if (-not $ready) {
    Write-Warn "Still not readyToGenerate, trying generate endpoint directly."
}

$generateAttempts = 0
$generateMaxAttempts = 4
$generate = $null
while ($generateAttempts -lt $generateMaxAttempts) {
    $generateAttempts++
    $generateResp = Invoke-RestMethod -Uri ($BackendBaseUrl.TrimEnd("/") + "/api/requirements/$requirementId/docgen/jobs/$jobId/generate") `
        -Method POST `
        -Headers @{ Authorization = "Bearer $token" } `
        -ContentType "application/json; charset=utf-8"

    if ($null -eq $generateResp) {
        throw "Generate response is null"
    }
    if ($generateResp.code -eq 0) {
        $generate = $generateResp.data
        break
    }

    if ($generateResp.code -eq 10001) {
        Write-Warn ("Generate blocked by unconfirmed items (attempt " + $generateAttempts + "/" + $generateMaxAttempts + ").")
        $jobNow = Invoke-ApiJson -Method GET -Path "/api/requirements/$requirementId/docgen/jobs/$jobId" -Token $token
        $unconfirmedNow = To-Array $jobNow.unconfirmedItems

        $templateQ = Extract-TemplateQuestion -Message ([string]$generateResp.message)
        $modelReply = ""
        if (-not [string]::IsNullOrWhiteSpace($templateQ)) {
            $modelReply = Build-TemplateAnswerReply -QuestionText $templateQ
        }
        if ([string]::IsNullOrWhiteSpace($modelReply)) {
            $modelReply = Build-CloseAllUnconfirmedReply -UnconfirmedItems $unconfirmedNow
        } else {
            $modelReply = $modelReply + "`n`n" + (Build-CloseAllUnconfirmedReply -UnconfirmedItems $unconfirmedNow)
        }
        if ([string]::IsNullOrWhiteSpace($modelReply)) {
            $modelReply = Build-ModelTypeReply
        }
        $chatResp = Invoke-ApiJson -Method POST -Path "/api/requirements/$requirementId/docgen/jobs/$jobId/chat" -Token $token -Body @{
            message = $modelReply
        }
        $ready = [bool]$chatResp.readyToGenerate
        $status = [string]$chatResp.status
        if (-not [string]::IsNullOrWhiteSpace($templateQ)) {
            Write-Host ("  - template clarify sent for [" + $templateQ + "], unresolvedCount=" + @($unconfirmedNow).Count + ", status=" + $status + ", readyToGenerate=" + $ready)
        } else {
            Write-Host ("  - fallback clarify sent, unresolvedCount=" + @($unconfirmedNow).Count + ", status=" + $status + ", readyToGenerate=" + $ready)
        }
        continue
    }

    throw "Generate API failed: code=$($generateResp.code), message=$($generateResp.message), traceId=$($generateResp.traceId)"
}

if ($null -eq $generate) {
    throw "Generate did not succeed after $generateMaxAttempts attempts"
}

$status = [string]$generate.status
if ($status -ne "COMPLETED") {
    throw "Generate finished but status is not COMPLETED: $status"
}
$prd = [string]$generate.prdMarkdown
if ([string]::IsNullOrWhiteSpace($prd)) {
    throw "Generate succeeded but prdMarkdown is empty"
}
Write-Ok "PRD generated successfully"

Write-Step "10) Validate versions"
$versions = To-Array (Invoke-ApiJson -Method GET -Path "/api/requirements/$requirementId/versions" -Token $token)
$versionsCount = @($versions).Count
if ($versionsCount -le 0) {
    throw "No requirement versions found"
}
$currentVersion = $versions | Where-Object { $_.isCurrent -eq $true } | Select-Object -First 1
if ($null -eq $currentVersion) {
    $currentVersion = $versions | Sort-Object id -Descending | Select-Object -First 1
}
$versionId = [long]$currentVersion.id
$versionDetail = Invoke-ApiJson -Method GET -Path "/api/requirements/$requirementId/versions/$versionId" -Token $token
if ([string]::IsNullOrWhiteSpace([string]$versionDetail.prdMarkdown)) {
    throw "Version detail has empty prdMarkdown"
}
Write-Ok ("Version check passed, versionId=" + $versionId + ", versionNo=" + $versionDetail.versionNo)

Write-Step "11) Export markdown file"
$artifactsDir = Join-Path $PSScriptRoot "artifacts"
if (-not (Test-Path $artifactsDir)) {
    New-Item -ItemType Directory -Path $artifactsDir | Out-Null
}
$exportFile = Join-Path $artifactsDir ("prd-requirement-" + $requirementId + "-version-" + $versionId + ".md")
Invoke-ApiDownload -Path "/api/requirements/$requirementId/versions/$versionId/export" -OutputFile $exportFile -Token $token
if ((Get-Item $exportFile).Length -le 0) {
    throw "Export file is empty: $exportFile"
}
Write-Ok ("Export completed: " + $exportFile)

Write-Host ""
Write-Host "===== P0 E2E Check Completed =====" -ForegroundColor Magenta
Write-Host ("projectId     : " + $projectId)
Write-Host ("requirementId : " + $requirementId)
Write-Host ("jobId         : " + $jobId)
Write-Host ("versionId     : " + $versionId)
Write-Host ("exportFile    : " + $exportFile)
Write-Host ""
Write-Warn "Manual check for restart persistence:"
Write-Host ("After backend restart, call GET " + $BackendBaseUrl.TrimEnd("/") + "/api/requirements/" + $requirementId + "/docgen/jobs/" + $jobId)
