# Generate JWT Secret Script
# Usage: Run .\generate-jwt-secret.ps1 in PowerShell

$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$bytes = New-Object byte[] 64
$rng.GetBytes($bytes)
$secret = [Convert]::ToBase64String($bytes)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Generated JWT Secret:" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host $secret -ForegroundColor Yellow
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Please copy this secret to JWT_SECRET in .env file" -ForegroundColor White
Write-Host ""
Write-Host "Auto update .env file? (Y/N)" -ForegroundColor Cyan
$response = Read-Host

if ($response -eq 'Y' -or $response -eq 'y') {
    $envFile = ".env"
    if (Test-Path $envFile) {
        $content = Get-Content $envFile -Raw
        $updatedContent = $content -replace 'JWT_SECRET=.*', "JWT_SECRET=$secret"
        Set-Content -Path $envFile -Value $updatedContent -NoNewline
        Write-Host "[OK] .env file updated" -ForegroundColor Green
    } else {
        Write-Host "[Error] .env file not found" -ForegroundColor Red
    }
}
