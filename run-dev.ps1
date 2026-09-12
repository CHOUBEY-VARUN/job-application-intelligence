$envFile = Join-Path $PSScriptRoot ".env"

if (-not (Test-Path $envFile)) {
    Write-Error ".env file not found."
    exit 1
}

Get-Content $envFile | ForEach-Object {
    if ($_ -match '^\s*([^#][^=]*)=(.*)$') {
        $name = $matches[1].Trim()
        $value = $matches[2].Trim()

        [Environment]::SetEnvironmentVariable($name, $value, "Process")
    }
}

$env:JAVA_HOME = "C:\Program Files\Java\jdk-22"

Write-Host "Starting backend..."
Set-Location (Join-Path $PSScriptRoot "backend")
.\mvnw.cmd spring-boot:run