# 🚀 GUIDE DE DÉPLOIEMENT EN PRODUCTION

## 📋 Options de Déploiement

### Option 1: Docker (Recommandé)

#### Installation de Docker
```bash
# Windows: Télécharger Docker Desktop
# https://www.docker.com/products/docker-desktop

# Linux
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
```

#### Lancer avec Docker
```bash
# Avec docker-compose (simple)
docker-compose up -d

# Ou manuellement
docker build -t sentiment-analysis:4.1.1 .
docker run -p 8000:8000 sentiment-analysis:4.1.1
```

#### Accéder
```
http://localhost:8000/
```

---

### Option 2: Déploiement Linux/Heroku

#### Prérequis
```bash
sudo apt-get update
sudo apt-get install python3 python3-pip openjdk-11-jdk git
```

#### Installation
```bash
git clone <your-repo>
cd sentiment
pip install -r spark-project/requirements.txt
python spark-project/generate_results.py
```

#### Lancer un serveur
```bash
# Python SimpleHTTPServer
python -m http.server 8000

# Ou avec Gunicorn + Flask
pip install gunicorn flask
# (créer app.py avec Flask)
gunicorn --workers 4 --bind 0.0.0.0:8000 app:app
```

---

### Option 3: Cloud Deployment

#### AWS EC2
```bash
# 1. Lancer instance Ubuntu 20.04 t2.medium
# 2. SSH dans l'instance
ssh -i key.pem ubuntu@<instance-ip>

# 3. Installer dépendances
sudo apt-get update && sudo apt-get install -y \
  python3 python3-pip openjdk-11-jdk

# 4. Cloner le repo
git clone <your-repo>
cd sentiment && pip install -r spark-project/requirements.txt

# 5. Générer résultats
python spark-project/generate_results.py

# 6. Lancer serveur
nohup python -m http.server 8000 > server.log 2>&1 &

# 7. Ouvrir au navigateur
# http://<instance-public-ip>:8000/
```

#### Google Cloud Run
```bash
# Créer Dockerfile.gcloud
# Déployer avec:
gcloud run deploy sentiment-analysis \
  --source . \
  --platform managed \
  --region us-central1 \
  --port 8000
```

#### Microsoft Azure
```bash
# Utiliser App Service
# Push vers Azure Container Registry
# Déployer depuis ACR
```

---

### Option 4: Kubernetes (Production Scale)

#### Chart Helm (Optionnel)
```bash
# Créer namespace
kubectl create namespace sentiment

# Déployer avec Helm
helm install sentiment ./helm-chart \
  --namespace sentiment \
  --values values.prod.yaml
```

#### Manifeste Kubernetes
```yaml
# deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: sentiment-analysis
  namespace: sentiment
spec:
  replicas: 3
  selector:
    matchLabels:
      app: sentiment-analysis
  template:
    metadata:
      labels:
        app: sentiment-analysis
    spec:
      containers:
      - name: sentiment
        image: sentiment-analysis:4.1.1
        ports:
        - containerPort: 8000
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "1Gi"
            cpu: "1000m"
```

```bash
kubectl apply -f deployment.yaml
kubectl expose deployment sentiment-analysis \
  --type LoadBalancer --port 80 --target-port 8000
```

---

## 🔒 Sécurité en Production

### HTTPS/TLS
```bash
# Utiliser Let's Encrypt avec Nginx
sudo apt-get install nginx certbot python3-certbot-nginx

# Obtenir certificat
sudo certbot certonly --nginx -d yourdomain.com

# Configurer Nginx
sudo nano /etc/nginx/sites-available/sentiment
```

**Configuration Nginx exemple:**
```nginx
upstream sentiment {
    server localhost:8000;
}

server {
    listen 443 ssl http2;
    server_name yourdomain.com;
    
    ssl_certificate /etc/letsencrypt/live/yourdomain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/yourdomain.com/privkey.pem;
    
    location / {
        proxy_pass http://sentiment;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}

server {
    listen 80;
    server_name yourdomain.com;
    return 301 https://$server_name$request_uri;
}
```

### Rate Limiting
```nginx
# Limiter les requêtes
limit_req_zone $binary_remote_addr zone=general:10m rate=10r/s;

location / {
    limit_req zone=general burst=20 nodelay;
    proxy_pass http://sentiment;
}
```

### CORS Policy
```python
# Si Flask backend
from flask_cors import CORS

app = Flask(__name__)
CORS(app, resources={
    r"/api/*": {"origins": ["https://yourdomain.com"]}
})
```

---

## 📊 Monitoring & Logging

### Logs
```bash
# Docker
docker logs sentiment-analysis

# Linux
tail -f /var/log/sentiment/app.log

# Kubernetes
kubectl logs -n sentiment deployment/sentiment-analysis
```

### Métriques (Prometheus)
```yaml
# prometheus.yml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'sentiment'
    static_configs:
      - targets: ['localhost:9090']
```

### Health Check
```bash
# Tester
curl http://localhost:8000/health

# Ou ajouter un endpoint
# GET /health → returns {"status": "ok"}
```

---

## 🔄 CI/CD Pipeline (GitHub Actions)

**.github/workflows/deploy.yml**
```yaml
name: Deploy Sentiment Analysis

on:
  push:
    branches: [main, production]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      
      - name: Build Docker image
        run: docker build -t sentiment:${{ github.sha }} .
      
      - name: Push to Docker Hub
        run: |
          docker login -u ${{ secrets.DOCKER_USER }} -p ${{ secrets.DOCKER_PASS }}
          docker push sentiment:${{ github.sha }}
      
      - name: Deploy to production
        run: |
          # SSH dans production et déployer
          ssh deploy@prod.example.com "docker pull sentiment:${{ github.sha }}"
```

---

## 🔧 Configuration pour Production

### Environment Variables
```bash
# .env
ENVIRONMENT=production
DEBUG=false
LOG_LEVEL=INFO
PORT=8000
WORKERS=4
```

### Performance Tuning

**Python/Gunicorn**
```bash
gunicorn --workers 4 \
  --worker-class sync \
  --worker-connections 1000 \
  --backlog 2048 \
  --bind 0.0.0.0:8000 \
  app:app
```

**Spark Configuration**
```bash
spark-submit \
  --executor-memory 4G \
  --driver-memory 2G \
  --num-executors 4 \
  --executor-cores 2 \
  --conf spark.default.parallelism=8 \
  sentiment.jar
```

---

## 📈 Scaling

### Horizontal Scaling (Docker)
```bash
# Docker Swarm
docker swarm init
docker stack deploy -c docker-compose.yml sentiment

# Scaling
docker service scale sentiment_web=3
```

### Vertical Scaling (Kubernetes)
```bash
# Augmenter les ressources
kubectl patch deployment sentiment-analysis -p \
  '{"spec":{"template":{"spec":{"containers":[{"name":"sentiment","resources":{"limits":{"memory":"2Gi","cpu":"2000m"}}}]}}}}'
```

---

## 🧪 Tests en Production

### Health Check
```bash
for i in {1..100}; do
  curl -s http://localhost:8000/ | head -c 1
  sleep 0.1
done
```

### Load Testing (Apache Bench)
```bash
ab -n 1000 -c 10 http://localhost:8000/
```

### Avec Locust
```python
# locustfile.py
from locust import HttpUser, task

class SentimentUser(HttpUser):
    @task
    def index(self):
        self.client.get("/")
```

```bash
locust -f locustfile.py --host=http://localhost:8000
```

---

## 🚨 Troubleshooting

| Problème | Solution |
|----------|----------|
| Port 8000 déjà utilisé | `lsof -i :8000` puis `kill <pid>` |
| Pas d'accès à resultats.json | Vérifier permissions: `chmod 644 resultats.json` |
| Spark out of memory | Augmenter `--executor-memory` dans spark-submit |
| Application lente | Augmenter workers/executors ou optimiser code |
| CORS errors | Ajouter headers CORS dans serveur frontend |

---

## 📚 Ressources

- Docker: https://docs.docker.com/
- Kubernetes: https://kubernetes.io/docs/
- Nginx: https://nginx.org/en/docs/
- Spark: https://spark.apache.org/docs/
- AWS EC2: https://aws.amazon.com/ec2/
- Google Cloud Run: https://cloud.google.com/run

---

## ✅ Checklist Déploiement

- [ ] HTTPS/TLS configuré
- [ ] Environment variables définies
- [ ] Logging activé
- [ ] Health checks fonctionnels
- [ ] Rate limiting configuré
- [ ] CORS policy définie
- [ ] Monitoring mis en place
- [ ] Backups planifiés
- [ ] Documentation mise à jour
- [ ] Équipe formée

---

**Prêt pour la production!** 🚀
