# install-pwsh.ps1
# 一键安装 PowerShell (pwsh) 脚本。需要以管理员权限运行。

# 强制使用 TLS1.2
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

function Ensure-Admin {
    $isAdmin = ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltinRole]::Administrator)
    if (-not $isAdmin) {
        Write-Output "当前未以管理员运行。尝试以管理员权限重新启动脚本..."
        Start-Process -FilePath powershell -ArgumentList "-NoProfile -ExecutionPolicy Bypass -File `"$PSCommandPath`"" -Verb RunAs
        exit
    }
}

Ensure-Admin

if (Get-Command pwsh -ErrorAction SilentlyContinue) {
    Write-Output "pwsh 已安装：$(pwsh --version)"
    exit 0
}

# 1) 尝试使用 winget
if (Get-Command winget -ErrorAction SilentlyContinue) {
    Write-Output "检测到 winget，使用 winget 安装 PowerShell..."
    winget install --id Microsoft.PowerShell -e --accept-source-agreements --accept-package-agreements
    Start-Sleep -Seconds 2
    if (Get-Command pwsh -ErrorAction SilentlyContinue) {
        Write-Output "安装成功：$(pwsh --version)"
        exit 0
    } else {
        Write-Warning "winget 安装结束，但未找到 pwsh。"
    }
}

# 2) 尝试使用 Chocolatey
if (Get-Command choco -ErrorAction SilentlyContinue) {
    Write-Output "检测到 Chocolatey，使用 choco 安装 PowerShell..."
    choco install powershell-core -y
    Start-Sleep -Seconds 2
    if (Get-Command pwsh -ErrorAction SilentlyContinue) {
        Write-Output "安装成功：$(pwsh --version)"
        exit 0
    } else {
        Write-Warning "choco 安装结束，但未找到 pwsh。"
    }
}

# 3) 回退：从 GitHub Releases 下载 MSI 并静默安装
Write-Output "尝试从 GitHub Releases 下载 MSI 并安装..."
$owner = 'PowerShell'
$repo = 'PowerShell'
$apiUrl = "https://api.github.com/repos/$owner/$repo/releases/latest"
try {
    $release = Invoke-RestMethod -Uri $apiUrl -UseBasicParsing -Headers @{ 'User-Agent' = 'PowerShell' }
} catch {
    Write-Warning "无法查询 GitHub Releases：$_"
    Write-Output "请手动从 https://github.com/PowerShell/PowerShell/releases 下载适用于 Windows 的 MSI 并以管理员身份运行。"
    exit 1
}

$asset = $release.assets | Where-Object { $_.name -match 'win-x64.*\\.msi$' } | Select-Object -First 1
if ($asset) {
    $msiUrl = $asset.browser_download_url
    $dest = Join-Path $env:TEMP $asset.name
    Write-Output "下载 $msiUrl 到 $dest ..."
    Invoke-WebRequest -Uri $msiUrl -OutFile $dest
    Write-Output "启动 MSI 安装程序（静默）..."
    Start-Process -FilePath msiexec.exe -ArgumentList '/i', "`"$dest`"", '/quiet', '/norestart' -Wait
    if (Get-Command pwsh -ErrorAction SilentlyContinue) {
        Write-Output "安装成功：$(pwsh --version)"
        exit 0
    } else {
        Write-Output "安装已运行。请打开新终端并运行 'pwsh --version' 验证安装。"
        exit 0
    }
} else {
    Write-Warning "未找到合适的 win-x64 MSI 资源。请手动访问 https://github.com/PowerShell/PowerShell/releases 下载并安装。"
    exit 1
}
