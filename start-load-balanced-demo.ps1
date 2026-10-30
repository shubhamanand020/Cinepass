# Cinepass Backend Multi-Instance & Load Balancing Launcher
# Starts Eureka Server first, then launches all microservices including 2 instances of Movie Service (8082 & 8085)
# to demonstrate Round-Robin load balancing via Spring Cloud Gateway.

Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host "   🎬 Cinepass Load-Balanced Microservices Architecture Launcher   " -ForegroundColor Cyan
Write-Host "   Target: Review 2 - Microservices, JWT Auth & Load Balancing   " -ForegroundColor Cyan
Write-Host "=================================================================" -ForegroundColor Cyan

$backendDir = Split-Path -Parent $MyInvocation.MyCommand.Path

# 1. Start Eureka Server (Port 8761)
Write-Host "[1/7] Launching Eureka Server (Port 8761)..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$backendDir\eureka_server'; `$host.UI.RawUI.WindowTitle='[Cinepass] Eureka Server :8761'; .\mvnw.cmd spring-boot:run"
Write-Host "Waiting 12 seconds for Eureka Server to initialize..." -ForegroundColor Gray
Start-Sleep -Seconds 12

# 2. Start User Service (Port 8081)
Write-Host "[2/7] Launching User Service (Port 8081)..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$backendDir\user-service'; `$host.UI.RawUI.WindowTitle='[Cinepass] User Service :8081'; .\mvnw.cmd spring-boot:run"

# 3. Start Movie Service - Instance 1 (Port 8082)
Write-Host "[3/7] Launching Movie Service - Instance 1 (Port 8082)..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$backendDir\movie-service'; `$host.UI.RawUI.WindowTitle='[Cinepass] Movie Service [Inst 1] :8082'; .\mvnw.cmd spring-boot:run"

# 4. Start Movie Service - Instance 2 (Port 8085 for Load Balancing)
Write-Host "[4/7] Launching Movie Service - Instance 2 (Port 8085 - Load Balanced)..." -ForegroundColor Cyan
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$backendDir\movie-service'; `$host.UI.RawUI.WindowTitle='[Cinepass] Movie Service [Inst 2] :8085'; .\mvnw.cmd spring-boot:run `"-Dspring-boot.run.arguments=--server.port=8085`""

# 5. Start Show Service (Port 8084)
Write-Host "[5/7] Launching Show Service (Port 8084)..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$backendDir\show-service'; `$host.UI.RawUI.WindowTitle='[Cinepass] Show Service :8084'; .\mvnw.cmd spring-boot:run"

# 6. Start Booking Service (Port 8083)
Write-Host "[6/7] Launching Booking Service (Port 8083)..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$backendDir\booking-service'; `$host.UI.RawUI.WindowTitle='[Cinepass] Booking Service :8083'; .\mvnw.cmd spring-boot:run"

Write-Host "Waiting 10 seconds before starting API Gateway..." -ForegroundColor Gray
Start-Sleep -Seconds 10

# 7. Start API Gateway (Port 8080)
Write-Host "[7/7] Launching API Gateway (Port 8080)..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$backendDir\api-gateway'; `$host.UI.RawUI.WindowTitle='[Cinepass] API Gateway :8080'; .\mvnw.cmd spring-boot:run"

Write-Host "`nAll 7 service instances launched!" -ForegroundColor Green
Write-Host "Eureka Registry: http://localhost:8761 (shows 2 MOVIE-SERVICE instances)" -ForegroundColor Green
Write-Host "API Gateway:     http://localhost:8080" -ForegroundColor Green
Write-Host "Run .\test-load-balancing.ps1 to verify load distribution across port 8082 and 8085." -ForegroundColor Cyan
