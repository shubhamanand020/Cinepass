# Cinepass Microservices Graceful Stopper
# Stops all processes listening on Cinepass ports: 8761, 8080, 8081, 8082, 8083, 8084, 8085

$ports = @(8761, 8080, 8081, 8082, 8083, 8084, 8085)

Write-Host "Stopping Cinepass microservices on ports: $($ports -join ', ')..." -ForegroundColor Yellow

foreach ($port in $ports) {
    $conns = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
    if ($conns) {
        foreach ($conn in $conns) {
            $pidToStop = $conn.OwningProcess
            Write-Host "Killing PID $pidToStop on port $port..." -ForegroundColor Red
            Stop-Process -Id $pidToStop -Force -ErrorAction SilentlyContinue
        }
    } else {
        Write-Host "Port $port is already free." -ForegroundColor Gray
    }
}

Write-Host "All Cinepass backend services stopped." -ForegroundColor Green
