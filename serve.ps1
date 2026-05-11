# Serveur Web pour Sentiment Analysis Dashboard
# Utilisation: .\serve.ps1

$PORT = 8000
$BasePath = $PSScriptRoot

Write-Host "🚀 Demarrage du serveur web..." -ForegroundColor Green
Write-Host "📍 Accedez a: http://localhost:$PORT" -ForegroundColor Cyan
Write-Host ""
Write-Host "Appuyez sur Ctrl+C pour arreter" -ForegroundColor Yellow
Write-Host ""

# Démarrer le serveur Python
Set-Location $BasePath
python -m http.server $PORT
