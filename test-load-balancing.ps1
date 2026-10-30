# Cinepass Review 2 - Load Balancing Verification Script
# Tests Spring Cloud Gateway LoadBalancer request distribution across multiple microservice instances.

$gateway = "http://localhost:8080"
$iterations = 10
$ports = @{}

Write-Host "`n=================================================================" -ForegroundColor Cyan
Write-Host "         CINEPASS LOAD BALANCING VERIFICATION TEST               " -ForegroundColor Cyan
Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host "Sending $iterations consecutive requests to $gateway/api/movies..." -ForegroundColor Yellow

for ($i = 1; $i -le $iterations; $i++) {
    try {
        $response = Invoke-WebRequest -Uri "$gateway/api/movies" -Method Get -TimeoutSec 5
        $portHeader = $response.Headers["X-Service-Port"]
        $serviceHeader = $response.Headers["X-Service-Name"]

        if (-not $portHeader) {
            # Fallback check in headers collection
            $portHeader = ($response.Headers.GetEnumerator() | Where-Object { $_.Key -match "X-Service-Port" }).Value
        }

        if ($portHeader) {
            if (-not $ports.ContainsKey($portHeader)) {
                $ports[$portHeader] = 0
            }
            $ports[$portHeader]++
            Write-Host "  Request #$i -> Dispatched to $serviceHeader on Port: $portHeader" -ForegroundColor Green
        } else {
            Write-Host "  Request #$i -> Status 200 OK (X-Service-Port header not returned)" -ForegroundColor Yellow
        }
    } catch {
        Write-Host "  Request #$i -> Error: $($_.Exception.Message)" -ForegroundColor Red
    }
    Start-Sleep -Milliseconds 250
}

Write-Host "`n-----------------------------------------------------------------" -ForegroundColor Cyan
Write-Host "                  LOAD BALANCING TRAFFIC DISTRIBUTION            " -ForegroundColor Cyan
Write-Host "-----------------------------------------------------------------" -ForegroundColor Cyan

if ($ports.Keys.Count -gt 1) {
    foreach ($p in $ports.Keys) {
        $count = $ports[$p]
        $pct = [Math]::Round(($count / $iterations) * 100, 1)
        Write-Host "  Instance on Port $p : $count requests ($pct%)" -ForegroundColor Cyan
    }
    Write-Host "`n  🎉 LOAD BALANCING VERIFIED: Traffic successfully distributed across $($ports.Keys.Count) instances!" -ForegroundColor Green
} elseif ($ports.Keys.Count -eq 1) {
    $singlePort = $ports.Keys | Select-Object -First 1
    Write-Host "  Single active instance detected on Port $singlePort ($($ports[$singlePort]) requests)." -ForegroundColor Yellow
    Write-Host "  To demonstrate multi-instance load balancing, launch instance 2 with:" -ForegroundColor Gray
    Write-Host "    .\start-load-balanced-demo.ps1" -ForegroundColor Cyan
    Write-Host "    OR: cd movie-service; .\mvnw.cmd spring-boot:run `"-Dspring-boot.run.arguments=--server.port=8085`"" -ForegroundColor Gray
} else {
    Write-Host "  No instance headers captured. Please ensure services are running." -ForegroundColor Red
}
Write-Host "=================================================================`n" -ForegroundColor Cyan
