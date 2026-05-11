#!/bin/bash
# Serveur Web pour Sentiment Analysis Dashboard

PORT=8000

echo "🚀 Démarrage du serveur web..."
echo "📍 Accédez à: http://localhost:$PORT"
echo ""
echo "AppuyezCtrl+C pour arrêter"

# Utiliser le serveur intégré de Python
python3 -m http.server $PORT --directory .
