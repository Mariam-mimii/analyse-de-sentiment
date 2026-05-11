# Dockerfile pour l'analyse de sentiments

FROM python:3.10-slim

WORKDIR /app

# Installer les dépendances du système
RUN apt-get update && apt-get install -y \
    openjdk-11-jdk \
    wget \
    && rm -rf /var/lib/apt/lists/*

# Copier les fichiers du projet
COPY spark-project/requirements.txt .
COPY spark-project/data ./data
COPY spark-project/generate_results.py .

# Installer les dépendances Python
RUN pip install --no-cache-dir -r requirements.txt

# Installer Spark
RUN wget https://archive.apache.org/dist/spark/spark-4.1.1/spark-4.1.1-bin-hadoop3.tgz && \
    tar -xzf spark-4.1.1-bin-hadoop3.tgz && \
    mv spark-4.1.1-bin-hadoop3 /opt/spark && \
    rm spark-4.1.1-bin-hadoop3.tgz

ENV SPARK_HOME=/opt/spark
ENV PATH=$SPARK_HOME/bin:$PATH

# Générer les résultats
RUN python generate_results.py

# Exposer le port pour le serveur web (optionnel)
EXPOSE 8000

# Commande par défaut
CMD ["python", "-m", "http.server", "8000", "--directory", "/app/sentiment"]
