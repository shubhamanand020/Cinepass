# Cinepass Review 2 - Comprehensive End-to-End CRUD & Security Verification Suite
# Runs against API Gateway at http://localhost:8080

$ErrorActionPreference = "Continue"
$gateway = "http://localhost:8080"
$passCount = 0
$failCount = 0

function Assert-Test {
    param(
        [string]$TestName,
        [bool]$Condition,
        [string]$Details = ""
    )
    if ($Condition) {
        Write-Host "  [PASS] $TestName" -ForegroundColor Green
        if ($Details) { Write-Host "         $Details" -ForegroundColor Gray }
        $script:passCount++
    } else {
        Write-Host "  [FAIL] $TestName" -ForegroundColor Red
        if ($Details) { Write-Host "         Reason: $Details" -ForegroundColor Magenta }
        $script:failCount++
    }
}

Write-Host "`n==================================================================" -ForegroundColor Cyan
Write-Host "        CINEPASS REVIEW 2: FULL CRUD & SECURITY TEST SUITE        " -ForegroundColor Cyan
Write-Host "==================================================================" -ForegroundColor Cyan
Write-Host "Target Gateway: $gateway" -ForegroundColor Yellow

$timestamp = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$testEmail = "reviewer_$timestamp@cinepass.com"
$testPassword = "Password@123"

# -------------------------------------------------------------------------
# 1. USER SERVICE CRUD & AUTHENTICATION
# -------------------------------------------------------------------------
Write-Host "`n--- [1] Testing User Service CRUD & Authentication ---" -ForegroundColor Cyan

# 1.1 Create User (Registration)
$registerBody = @{
    name = "Review 2 User"
    email = $testEmail
    password = $testPassword
} | ConvertTo-Json

try {
    $regRes = Invoke-RestMethod -Uri "$gateway/api/users" -Method Post -Body $registerBody -ContentType "application/json"
    Assert-Test -TestName "1.1 Create User (POST /api/users)" -Condition ($regRes.id -and $regRes.email -eq $testEmail) -Details "Created User ID: $($regRes.id)"
    $userId = $regRes.id
} catch {
    Assert-Test -TestName "1.1 Create User (POST /api/users)" -Condition $false -Details $_.Exception.Message
}

# 1.2 User Login & JWT Generation
$loginBody = @{
    email = $testEmail
    password = $testPassword
} | ConvertTo-Json

$token = $null
try {
    $loginRes = Invoke-RestMethod -Uri "$gateway/api/auth/login" -Method Post -Body $loginBody -ContentType "application/json"
    $token = $loginRes.token
    Assert-Test -TestName "1.2 User Login & Token Issuance (POST /api/auth/login)" -Condition ($token -and $token.Length -gt 20) -Details "Token received (Length: $($token.Length))"
} catch {
    Assert-Test -TestName "1.2 User Login (POST /api/auth/login)" -Condition $false -Details $_.Exception.Message
}

$authHeaders = @{
    "Authorization" = "Bearer $token"
    "Content-Type" = "application/json"
}

# 1.3 Read User by ID
try {
    $userRes = Invoke-RestMethod -Uri "$gateway/api/users/$userId" -Method Get -Headers $authHeaders
    Assert-Test -TestName "1.3 Read User by ID (GET /api/users/$userId)" -Condition ($userRes.id -eq $userId) -Details "Fetched email: $($userRes.email)"
} catch {
    Assert-Test -TestName "1.3 Read User by ID" -Condition $false -Details $_.Exception.Message
}

# 1.4 Read All Users
try {
    $allUsers = Invoke-RestMethod -Uri "$gateway/api/users" -Method Get -Headers $authHeaders
    Assert-Test -TestName "1.4 Read All Users (GET /api/users)" -Condition ($allUsers.Count -ge 1) -Details "Total users: $($allUsers.Count)"
} catch {
    Assert-Test -TestName "1.4 Read All Users" -Condition $false -Details $_.Exception.Message
}

# 1.5 Update User Profile
$updateUserBody = @{
    name = "Review 2 User Updated"
} | ConvertTo-Json

try {
    $updateUserRes = Invoke-RestMethod -Uri "$gateway/api/users/$userId" -Method Put -Body $updateUserBody -Headers $authHeaders
    Assert-Test -TestName "1.5 Update User (PUT /api/users/$userId)" -Condition ($updateUserRes.name -eq "Review 2 User Updated") -Details "Updated Name: $($updateUserRes.name)"
} catch {
    Assert-Test -TestName "1.5 Update User" -Condition $false -Details $_.Exception.Message
}

# 1.6 Delete Temporary User
$tempEmail = "temp_$timestamp@cinepass.com"
try {
    $tempReg = Invoke-RestMethod -Uri "$gateway/api/users" -Method Post -Body (@{ name = "Temp User"; email = $tempEmail; password = "password" } | ConvertTo-Json) -ContentType "application/json"
    $delUserRes = Invoke-WebRequest -Uri "$gateway/api/users/$($tempReg.id)" -Method Delete -Headers $authHeaders
    Assert-Test -TestName "1.6 Delete User (DELETE /api/users/$($tempReg.id))" -Condition ($delUserRes.StatusCode -eq 204) -Details "Status Code: $($delUserRes.StatusCode)"
} catch {
    Assert-Test -TestName "1.6 Delete User" -Condition $false -Details $_.Exception.Message
}

# -------------------------------------------------------------------------
# 2. MOVIE SERVICE CRUD
# -------------------------------------------------------------------------
Write-Host "`n--- [2] Testing Movie Service CRUD ---" -ForegroundColor Cyan

# 2.1 Create Movie (Protected)
$movieBody = @{
    title = "Inception Interstellar $timestamp"
    description = "A mind-bending sci-fi blockbuster"
    genre = "Sci-Fi"
    language = "English"
    duration = 148
    releaseDate = "2026-11-01"
    posterUrl = "https://images.unsplash.com/photo-1536440136628-849c177e76a1"
} | ConvertTo-Json

$movieId = $null
try {
    $movieRes = Invoke-RestMethod -Uri "$gateway/api/movies" -Method Post -Body $movieBody -Headers $authHeaders
    $movieId = $movieRes.id
    Assert-Test -TestName "2.1 Create Movie (POST /api/movies)" -Condition ($movieRes.id -and $movieRes.title -eq "Inception Interstellar $timestamp") -Details "Created Movie ID: $movieId"
} catch {
    Assert-Test -TestName "2.1 Create Movie" -Condition $false -Details $_.Exception.Message
}

# 2.2 Read All Movies (Public)
try {
    $allMovies = Invoke-RestMethod -Uri "$gateway/api/movies" -Method Get
    Assert-Test -TestName "2.2 Read All Movies (GET /api/movies - Public)" -Condition ($allMovies.Count -ge 1) -Details "Total movies: $($allMovies.Count)"
} catch {
    Assert-Test -TestName "2.2 Read All Movies" -Condition $false -Details $_.Exception.Message
}

# 2.3 Read Movie by ID (Public)
try {
    $singleMovie = Invoke-RestMethod -Uri "$gateway/api/movies/$movieId" -Method Get
    Assert-Test -TestName "2.3 Read Movie by ID (GET /api/movies/$movieId - Public)" -Condition ($singleMovie.id -eq $movieId) -Details "Movie: $($singleMovie.title)"
} catch {
    Assert-Test -TestName "2.3 Read Movie by ID" -Condition $false -Details $_.Exception.Message
}

# 2.4 Update Movie (Protected)
$updateMovieBody = @{
    title = "Inception Interstellar (Director's Cut)"
    description = "Extended 4K remaster"
    genre = "Sci-Fi/Thriller"
    language = "English"
    duration = 165
    releaseDate = "2026-11-01"
    posterUrl = "https://images.unsplash.com/photo-1536440136628-849c177e76a1"
} | ConvertTo-Json

try {
    $updateMovieRes = Invoke-RestMethod -Uri "$gateway/api/movies/$movieId" -Method Put -Body $updateMovieBody -Headers $authHeaders
    Assert-Test -TestName "2.4 Update Movie (PUT /api/movies/$movieId)" -Condition ($updateMovieRes.duration -eq 165) -Details "Updated Title: $($updateMovieRes.title)"
} catch {
    Assert-Test -TestName "2.4 Update Movie" -Condition $false -Details $_.Exception.Message
}

# 2.5 Delete Movie (Protected)
try {
    $tempMovie = Invoke-RestMethod -Uri "$gateway/api/movies" -Method Post -Body (@{ title="Temp Movie"; description="temp"; genre="Drama"; language="English"; duration=90; releaseDate="2026-12-01"; posterUrl="url" } | ConvertTo-Json) -Headers $authHeaders
    $delMovieRes = Invoke-WebRequest -Uri "$gateway/api/movies/$($tempMovie.id)" -Method Delete -Headers $authHeaders
    Assert-Test -TestName "2.5 Delete Movie (DELETE /api/movies/$($tempMovie.id))" -Condition ($delMovieRes.StatusCode -eq 204) -Details "Status Code: $($delMovieRes.StatusCode)"
} catch {
    Assert-Test -TestName "2.5 Delete Movie" -Condition $false -Details $_.Exception.Message
}

# -------------------------------------------------------------------------
# 3. SHOW SERVICE CRUD (SCREENS & SHOWS)
# -------------------------------------------------------------------------
Write-Host "`n--- [3] Testing Show Service CRUD (Screens & Shows) ---" -ForegroundColor Cyan

# 3.1 Create Screen (Protected)
$screenBody = @{
    name = "IMAX Laser Screen 1"
    theatreName = "Cinepass PVR Grand"
    rows = 5
    seatsPerRow = 10
    premiumRows = 2
} | ConvertTo-Json

$screenId = $null
try {
    $screenRes = Invoke-RestMethod -Uri "$gateway/api/screens" -Method Post -Body $screenBody -Headers $authHeaders
    $screenId = $screenRes.id
    Assert-Test -TestName "3.1 Create Screen (POST /api/screens)" -Condition ($screenRes.id -and $screenRes.rows -eq 5) -Details "Created Screen ID: $screenId"
} catch {
    Assert-Test -TestName "3.1 Create Screen" -Condition $false -Details $_.Exception.Message
}

# 3.2 Read All Screens (Public)
try {
    $allScreens = Invoke-RestMethod -Uri "$gateway/api/screens" -Method Get
    Assert-Test -TestName "3.2 Read All Screens (GET /api/screens - Public)" -Condition ($allScreens.Count -ge 1) -Details "Total screens: $($allScreens.Count)"
} catch {
    Assert-Test -TestName "3.2 Read All Screens" -Condition $false -Details $_.Exception.Message
}

# 3.3 Read Screen by ID (Public)
try {
    $singleScreen = Invoke-RestMethod -Uri "$gateway/api/screens/$screenId" -Method Get
    Assert-Test -TestName "3.3 Read Screen by ID (GET /api/screens/$screenId - Public)" -Condition ($singleScreen.id -eq $screenId) -Details "Screen Name: $($singleScreen.name)"
} catch {
    Assert-Test -TestName "3.3 Read Screen by ID" -Condition $false -Details $_.Exception.Message
}

# 3.4 Update Screen (Protected)
$updateScreenBody = @{
    name = "IMAX Laser Luxe Screen 1"
    theatreName = "Cinepass PVR Grand"
    rows = 6
    seatsPerRow = 10
    premiumRows = 2
} | ConvertTo-Json

try {
    $updateScreenRes = Invoke-RestMethod -Uri "$gateway/api/screens/$screenId" -Method Put -Body $updateScreenBody -Headers $authHeaders
    Assert-Test -TestName "3.4 Update Screen (PUT /api/screens/$screenId)" -Condition ($updateScreenRes.rows -eq 6) -Details "Updated Rows: $($updateScreenRes.rows)"
} catch {
    Assert-Test -TestName "3.4 Update Screen" -Condition $false -Details $_.Exception.Message
}

# 3.5 Create Show (Protected)
$showTime = (Get-Date).AddDays(1).ToString("yyyy-MM-ddTHH:mm:ss")
$showBody = @{
    movieId = $movieId
    screenId = $screenId
    showTime = $showTime
    basePrice = 250.00
    premiumPrice = 450.00
} | ConvertTo-Json

$showId = $null
try {
    $showRes = Invoke-RestMethod -Uri "$gateway/api/shows" -Method Post -Body $showBody -Headers $authHeaders
    $showId = $showRes.id
    Assert-Test -TestName "3.5 Create Show (POST /api/shows)" -Condition ($showRes.id -and $showRes.movieId -eq $movieId) -Details "Created Show ID: $showId"
} catch {
    Assert-Test -TestName "3.5 Create Show" -Condition $false -Details $_.Exception.Message
}

# 3.6 Read Shows by Movie (Public)
try {
    $showsForMovie = Invoke-RestMethod -Uri "$gateway/api/shows?movieId=$movieId" -Method Get
    Assert-Test -TestName "3.6 Read Shows by Movie (GET /api/shows?movieId=$movieId - Public)" -Condition ($showsForMovie.Count -ge 1) -Details "Shows count: $($showsForMovie.Count)"
} catch {
    Assert-Test -TestName "3.6 Read Shows by Movie" -Condition $false -Details $_.Exception.Message
}

# 3.7 Read Show by ID (Public)
try {
    $singleShow = Invoke-RestMethod -Uri "$gateway/api/shows/$showId" -Method Get
    Assert-Test -TestName "3.7 Read Show by ID (GET /api/shows/$showId - Public)" -Condition ($singleShow.id -eq $showId) -Details "Show Base Price: $($singleShow.basePrice)"
} catch {
    Assert-Test -TestName "3.7 Read Show by ID" -Condition $false -Details $_.Exception.Message
}

# 3.8 Generate Dynamic Seat Map (Public)
try {
    $seats = Invoke-RestMethod -Uri "$gateway/api/shows/$showId/seats" -Method Get
    Assert-Test -TestName "3.8 Dynamic Seat Map (GET /api/shows/$showId/seats - Public)" -Condition ($seats.Count -eq 60) -Details "Generated $($seats.Count) seats (6 rows x 10 seats)"
} catch {
    Assert-Test -TestName "3.8 Dynamic Seat Map" -Condition $false -Details $_.Exception.Message
}

# 3.9 Update Show (Protected)
$updateShowBody = @{
    movieId = $movieId
    screenId = $screenId
    showTime = $showTime
    basePrice = 280.00
    premiumPrice = 500.00
} | ConvertTo-Json

try {
    $updateShowRes = Invoke-RestMethod -Uri "$gateway/api/shows/$showId" -Method Put -Body $updateShowBody -Headers $authHeaders
    Assert-Test -TestName "3.9 Update Show (PUT /api/shows/$showId)" -Condition ($updateShowRes.basePrice -eq 280.00) -Details "Updated Base Price: $($updateShowRes.basePrice)"
} catch {
    Assert-Test -TestName "3.9 Update Show" -Condition $false -Details $_.Exception.Message
}

# 3.10 Delete Show & Screen (Protected)
try {
    $tempShow = Invoke-RestMethod -Uri "$gateway/api/shows" -Method Post -Body $showBody -Headers $authHeaders
    $delShowRes = Invoke-WebRequest -Uri "$gateway/api/shows/$($tempShow.id)" -Method Delete -Headers $authHeaders
    Assert-Test -TestName "3.10 Delete Show (DELETE /api/shows/$($tempShow.id))" -Condition ($delShowRes.StatusCode -eq 204) -Details "Status Code: $($delShowRes.StatusCode)"
} catch {
    Assert-Test -TestName "3.10 Delete Show" -Condition $false -Details $_.Exception.Message
}

# -------------------------------------------------------------------------
# 4. BOOKING SERVICE CRUD & LIFECYCLE
# -------------------------------------------------------------------------
Write-Host "`n--- [4] Testing Booking Service CRUD & Lifecycle ---" -ForegroundColor Cyan

# 4.1 Read Real-Time Seat Availability (Public)
try {
    $avail = Invoke-RestMethod -Uri "$gateway/api/shows/$showId/availability" -Method Get
    Assert-Test -TestName "4.1 Read Availability (GET /api/shows/$showId/availability - Public)" -Condition ($avail.Count -gt 0) -Details "Available seats count: $($avail.Count)"
} catch {
    Assert-Test -TestName "4.1 Read Availability" -Condition $false -Details $_.Exception.Message
}

# 4.2 Hold Seats (Atomic Lock & 5-minute Timer)
$holdBody = @{
    showId = $showId
    seatNumbers = @("A1", "A2")
} | ConvertTo-Json

$bookingId = $null
try {
    $holdRes = Invoke-RestMethod -Uri "$gateway/api/bookings/hold" -Method Post -Body $holdBody -Headers $authHeaders
    $bookingId = $holdRes.id
    Assert-Test -TestName "4.2 Hold Seats (POST /api/bookings/hold)" -Condition ($holdRes.id -and $holdRes.status -eq "HELD" -and $holdRes.holdExpiresAt) -Details "Booking ID: $bookingId, Status: $($holdRes.status), Expires: $($holdRes.holdExpiresAt)"
} catch {
    Assert-Test -TestName "4.2 Hold Seats" -Condition $false -Details $_.Exception.Message
}

# 4.3 Confirm Booking (HELD -> CONFIRMED)
try {
    $confirmRes = Invoke-RestMethod -Uri "$gateway/api/bookings/$bookingId/confirm" -Method Post -Headers $authHeaders
    Assert-Test -TestName "4.3 Confirm Booking (POST /api/bookings/$bookingId/confirm)" -Condition ($confirmRes.status -eq "CONFIRMED") -Details "Status: $($confirmRes.status)"
} catch {
    Assert-Test -TestName "4.3 Confirm Booking" -Condition $false -Details $_.Exception.Message
}

# 4.4 Read Booking by ID (Protected)
try {
    $readBooking = Invoke-RestMethod -Uri "$gateway/api/bookings/$bookingId" -Method Get -Headers $authHeaders
    Assert-Test -TestName "4.4 Read Booking by ID (GET /api/bookings/$bookingId)" -Condition ($readBooking.id -eq $bookingId) -Details "Total Amount: $($readBooking.totalAmount)"
} catch {
    Assert-Test -TestName "4.4 Read Booking by ID" -Condition $false -Details $_.Exception.Message
}

# 4.5 Read User Bookings History (Protected)
try {
    $myBookings = Invoke-RestMethod -Uri "$gateway/api/bookings" -Method Get -Headers $authHeaders
    Assert-Test -TestName "4.5 Read User Bookings (GET /api/bookings)" -Condition ($myBookings.Count -ge 1) -Details "User bookings count: $($myBookings.Count)"
} catch {
    Assert-Test -TestName "4.5 Read User Bookings" -Condition $false -Details $_.Exception.Message
}

# 4.6 Hold & Cancel Booking Flow
try {
    $hold2 = Invoke-RestMethod -Uri "$gateway/api/bookings/hold" -Method Post -Body (@{ showId = $showId; seatNumbers = @("B1") } | ConvertTo-Json) -Headers $authHeaders
    $cancelRes = Invoke-RestMethod -Uri "$gateway/api/bookings/$($hold2.id)/cancel" -Method Post -Headers $authHeaders
    Assert-Test -TestName "4.6 Cancel Booking (POST /api/bookings/$($hold2.id)/cancel)" -Condition ($cancelRes.status -eq "CANCELLED") -Details "Booking Status: $($cancelRes.status)"
} catch {
    Assert-Test -TestName "4.6 Cancel Booking" -Condition $false -Details $_.Exception.Message
}

# 4.7 RESTful DELETE Booking Endpoint
try {
    $hold3 = Invoke-RestMethod -Uri "$gateway/api/bookings/hold" -Method Post -Body (@{ showId = $showId; seatNumbers = @("B2") } | ConvertTo-Json) -Headers $authHeaders
    $delBookingRes = Invoke-RestMethod -Uri "$gateway/api/bookings/$($hold3.id)" -Method Delete -Headers $authHeaders
    Assert-Test -TestName "4.7 RESTful DELETE Booking (DELETE /api/bookings/$($hold3.id))" -Condition ($delBookingRes.status -eq "CANCELLED") -Details "Status: $($delBookingRes.status)"
} catch {
    Assert-Test -TestName "4.7 RESTful DELETE Booking" -Condition $false -Details $_.Exception.Message
}

# -------------------------------------------------------------------------
# 5. JWT SECURITY, TOKEN VALIDATION & ERROR HANDLING
# -------------------------------------------------------------------------
Write-Host "`n--- [5] Testing JWT Security & Robust Error Handling ---" -ForegroundColor Cyan

# 5.1 Rejection of Missing Token (Protected Endpoint)
try {
    $unauthRes = Invoke-WebRequest -Uri "$gateway/api/movies" -Method Post -Body $movieBody -ContentType "application/json"
    Assert-Test -TestName "5.1 Reject Missing Token (Expect 401)" -Condition $false -Details "Unexpectedly succeeded"
} catch {
    $status = $_.Exception.Response.StatusCode.value__
    Assert-Test -TestName "5.1 Reject Missing Token (Expect 401)" -Condition ($status -eq 401) -Details "Correctly returned HTTP 401 Unauthorized"
}

# 5.2 Rejection of Invalid / Tampered Token
try {
    $badHeaders = @{ "Authorization" = "Bearer eyJhbGciOiJIUzI1NiJ9.invalidpayload.invalidsignature"; "Content-Type" = "application/json" }
    $badTokenRes = Invoke-WebRequest -Uri "$gateway/api/movies" -Method Post -Body $movieBody -Headers $badHeaders
    Assert-Test -TestName "5.2 Reject Invalid Token (Expect 401)" -Condition $false -Details "Unexpectedly succeeded"
} catch {
    $status = $_.Exception.Response.StatusCode.value__
    Assert-Test -TestName "5.2 Reject Invalid Token (Expect 401)" -Condition ($status -eq 401) -Details "Correctly returned HTTP 401 Unauthorized"
}

# 5.3 Rejection of Bad Credentials
try {
    $badLogin = Invoke-RestMethod -Uri "$gateway/api/auth/login" -Method Post -Body (@{ email = $testEmail; password = "WrongPassword999" } | ConvertTo-Json) -ContentType "application/json"
    Assert-Test -TestName "5.3 Reject Bad Credentials (Expect 401)" -Condition $false -Details "Unexpectedly succeeded"
} catch {
    $status = $_.Exception.Response.StatusCode.value__
    Assert-Test -TestName "5.3 Reject Bad Credentials (Expect 401)" -Condition ($status -eq 401) -Details "Correctly returned HTTP 401 Unauthorized"
}

# 5.4 Rejection of Duplicate Email Registration (Expect 409)
try {
    $dupReg = Invoke-RestMethod -Uri "$gateway/api/users" -Method Post -Body $registerBody -ContentType "application/json"
    Assert-Test -TestName "5.4 Reject Duplicate Email (Expect 409)" -Condition $false -Details "Unexpectedly succeeded"
} catch {
    $status = $_.Exception.Response.StatusCode.value__
    Assert-Test -TestName "5.4 Reject Duplicate Email (Expect 409)" -Condition ($status -eq 409) -Details "Correctly returned HTTP 409 Conflict"
}

# 5.5 Rejection of Seat Conflict / Double Booking (Expect 409)
try {
    # Attempting to hold A1 which is already confirmed
    $conflictRes = Invoke-RestMethod -Uri "$gateway/api/bookings/hold" -Method Post -Body (@{ showId = $showId; seatNumbers = @("A1") } | ConvertTo-Json) -Headers $authHeaders
    Assert-Test -TestName "5.5 Reject Seat Conflict (Expect 409)" -Condition $false -Details "Unexpectedly succeeded"
} catch {
    $status = $_.Exception.Response.StatusCode.value__
    Assert-Test -TestName "5.5 Reject Seat Conflict (Expect 409)" -Condition ($status -eq 409) -Details "Correctly returned HTTP 409 Conflict"
}

# -------------------------------------------------------------------------
# FINAL SUMMARY
# -------------------------------------------------------------------------
Write-Host "`n==================================================================" -ForegroundColor Cyan
Write-Host "                       TEST RESULTS SUMMARY                       " -ForegroundColor Cyan
Write-Host "==================================================================" -ForegroundColor Cyan
Write-Host "  PASSED: $passCount" -ForegroundColor Green
Write-Host "  FAILED: $failCount" -ForegroundColor $(if ($failCount -eq 0) { "Green" } else { "Red" })
if ($failCount -eq 0) {
    Write-Host "`n  🎉 ALL REVIEW 2 CRUD & SECURITY TESTS PASSED PERFECTLY (100%)" -ForegroundColor Green
} else {
    Write-Host "`n  ⚠️ SOME TESTS FAILED. PLEASE REVIEW LOGS ABOVE." -ForegroundColor Red
}
Write-Host "==================================================================`n" -ForegroundColor Cyan
