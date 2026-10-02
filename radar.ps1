<#
Uso diário: sobe o Radar de Vagas (se ainda não estiver rodando), busca vagas novas e abre a página.

  .\radar.ps1            # usa a porta 8080
  .\radar.ps1 -Porta 8090

Na primeira vez compila o projeto (precisa de Java 21). Os dados ficam em .\data.
#>
param([int]$Porta = 8080)

$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
$base = "http://localhost:$Porta"

function Rodando {
    try { Invoke-RestMethod "$base/api/jobs?status=NOVA" -TimeoutSec 2 | Out-Null; return $true } catch { return $false }
}

if (-not (Rodando)) {
    $jar = Get-ChildItem target\radar-vagas-*.jar -ErrorAction SilentlyContinue | Select-Object -First 1
    if (-not $jar) {
        Write-Host 'Compilando (só na primeira vez)...'
        & .\mvnw.cmd -q -DskipTests package
        $jar = Get-ChildItem target\radar-vagas-*.jar | Select-Object -First 1
    }
    Write-Host 'Iniciando o Radar de Vagas...'
    Start-Process java -ArgumentList '-jar', $jar.FullName, "--server.port=$Porta" -WindowStyle Hidden
    $limite = (Get-Date).AddMinutes(2)
    while (-not (Rodando)) {
        if ((Get-Date) -gt $limite) { throw 'O app não subiu em 2 minutos.' }
        Start-Sleep -Seconds 2
    }
}

$r = Invoke-RestMethod "$base/api/collect" -Method Post
Write-Host ("{0} vagas lidas, {1} novas." -f $r.read, $r.saved)
if ($r.failedSources.Count) { Write-Host ("Fontes com falha: " + ($r.failedSources -join ', ')) -ForegroundColor Yellow }
Start-Process $base
