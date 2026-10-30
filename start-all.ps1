# Cinepass Backend Microservices Launcher
# Starts Eureka Server first, waits for it, then launches all services and API Gateway in separate PowerShell windows.

Write-Host "======================================================" -ForegroundColor Cyan
Write-Host "       🎬 Starting Cinepass Microservices System       " -ForegroundColor Cyan
Write-Host "======================================================" -ForegroundColor Cyan

$backendDir = Split-Path -Parent $MyInvocation.MyCommand.Path

# 1. Start Eureka Server (Port 8761)
Write-Host "[1/6] Launching Eureka Server (Port 8761)..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$backendDir\eureka_server'; `$host.UI.RawUI.WindowTitle='[Cinepass] Eureka Server :8761'; .\mvnw.cmd spring-boot:run"
Write-Host "Waiting 12 seconds for Eureka Server to initialize..." -ForegroundColor Gray
Start-Sleep -Seconds 12

# 2. Start User Service (Port 8081)
Write-Host "[2/6] Launching User Service (Port 8081)..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$backendDir\user-service'; `$host.UI.RawUI.WindowTitle='[Cinepass] User Service :8081'; .\mvnw.cmd spring-boot:run"

# 3. Start Movie Service (Port 8082)
Write-Host "[3/6] Launching Movie Service (Port 8082)..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$backendDir\movie-service'; `$host.UI.RawUI.WindowTitle='[Cinepass] Movie Service :8082'; .\mvnw.cmd spring-boot:run"

# 4. Start Show Service (Port 8084)
Write-Host "[4/6] Launching Show Service (Port 8084)..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$backendDir\show-service'; `$host.UI.RawUI.WindowTitle='[Cinepass] Show Service :8084'; .\mvnw.cmd spring-boot:run"

# 5. Start Booking Service (Port 8083)
Write-Host "[5/6] Launching Booking Service (Port 8083)..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$backendDir\booking-service'; `$host.UI.RawUI.WindowTitle='[Cinepass] Booking Service :8083'; .\mvnw.cmd spring-boot:run"

Write-Host "Waiting 10 seconds before starting API Gateway..." -ForegroundColor Gray
Start-Sleep -Seconds 10

# 6. Start API Gateway (Port 8080)
Write-Host "[6/6] Launching API Gateway (Port 8080)..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$backendDir\api-gateway'; `$host.UI.RawUI.WindowTitle='[Cinepass] API Gateway :8080'; .\mvnw.cmd spring-boot:run"

Write-Host "`nAll 6 services launched! Open http://localhost:8761 in your browser to inspect Eureka." -ForegroundColor Green
