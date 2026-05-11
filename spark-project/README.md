# Pipeline Spark MLlib - Analyse de Sentiments

## 📋 Description

Pipeline d'entraînement et d'évaluation des modèles de classification de sentiments:
- **3 modèles:** Régression Logistique, Naive Bayes, LinearSVC
- **NLP:** Tokenization, StopWords, TF-IDF (256 features)
- **Dataset:** 30 avis clients (FR + EN)
- **Output:** resultats.json avec métriques et matrices de confusion

---

## 🚀 Installation Rapide (Python - Recommandé pour Windows)

### Prérequis
- Python 3.8+
- pip

### Étapes

```bash
# 1. Installer les dépendances
pip install scikit-learn numpy pandas

# 2. Générer les résultats
python generate_results.py

# 3. Vérifier
# - Le fichier sentiment/resultats.json est créé
# - Les modèles affichent leurs performances
```

---

## 🏗️ Compilation Scala/Spark (Complet)

### Prérequis
- Scala 2.13
- sbt (Simple Build Tool)
- Apache Spark 4.1.1

### Installation

**Windows (Chocolatey):**
```powershell
choco install scala sbt
```

**Mac (Homebrew):**
```bash
brew install scala sbt
```

**Linux:**
```bash
sudo apt-get install scala
# sbt: https://www.scala-sbt.org/
```

### Build

```bash
# Cloner/naviguer vers spark-project
cd spark-project

# Télécharger les dépendances
sbt update

# Compiler
sbt clean compile

# Créer un JAR assemblé
sbt assembly

# Le JAR sera dans: target/scala-2.13/SentimentAnalysis-assembly-1.0.jar
```

### Exécution avec Spark

**Local Mode (Simple):**
```bash
spark-submit \
  --class com.sentiment.SentimentAnalysis \
  target/scala-2.13/SentimentAnalysis-assembly-1.0.jar
```

**Avec Spark installé localement:**
```bash
# Installer Spark
# https://spark.apache.org/downloads.html

# Exécuter
$SPARK_HOME/bin/spark-submit \
  --class com.sentiment.SentimentAnalysis \
  target/scala-2.13/SentimentAnalysis-assembly-1.0.jar
```

**Mode cluster (YARN/Kubernetes):**
```bash
spark-submit \
  --master yarn \
  --deploy-mode cluster \
  --num-executors 10 \
  --executor-cores 4 \
  --executor-memory 4G \
  target/scala-2.13/SentimentAnalysis-assembly-1.0.jar
```

---

## 📁 Structure

```
spark-project/
├── build.sbt                    # Configuration sbt
├── generate_results.py          # Script Python (simple & rapide)
├── src/
│   └── main/scala/com/sentiment/
│       └── SentimentAnalysis.scala   # Pipeline Spark complet
└── data/
    └── reviews.csv             # Dataset (30 avis)
```

---

## 📊 Output: resultats.json

Le script génère un fichier JSON structuré:

```json
{
  "dataset": {
    "total": 30,
    "positifs": 15,
    "negatifs": 15,
    "train": 24,
    "test": 6
  },
  "modeles": [
    {
      "nom": "TF-IDF + Régression Logistique",
      "f1": 0.8333,
      "accuracy": 0.8333,
      "precision": 0.8333,
      "recall": 0.8333
    },
    ...
  ],
  "confusionMatrices": [
    {
      "modelName": "TF-IDF + Régression Logistique",
      "tp": 5,
      "tn": 5,
      "fp": 0,
      "fn": 1
    },
    ...
  ],
  "predictions": [
    {"texte": "...", "reel": 1, "predit": 1},
    ...
  ]
}
```

Ce JSON est chargé par `dashboard.html` pour l'affichage interactif.

---

## 🔧 Dépendances

### Python (generate_results.py)
```
scikit-learn >= 1.0
numpy >= 1.20
pandas >= 1.3
```

### Scala/Spark (SentimentAnalysis.scala)
```
Apache Spark 4.1.1
  - spark-core
  - spark-sql
  - spark-mllib
```

---

## 🧪 Tests

### Vérifier generate_results.py
```bash
python generate_results.py

# Sortie attendue:
# ================================================================================
# SENTIMENT ANALYSIS PIPELINE
# ================================================================================
# 
# [ÉTAPE 1] Chargement du dataset
# ✓ 30 avis chargés
#   - Positifs: 15 (50%)
#   - Négatifs: 15 (50%)
# 
# [ÉTAPE 2] Split Train-Test (80-20)
# ✓ Train: 24, Test: 6
# ...
```

### Vérifier SentimentAnalysis.scala
```bash
sbt run

# Ou avec Spark:
spark-submit --class com.sentiment.SentimentAnalysis target/scala-2.13/SentimentAnalysis-assembly-1.0.jar
```

---

## 📈 Résultats Attendus

```
TF-IDF + Régression Logistique: F1=0.8333, Acc=0.8333, Prec=0.8333, Rec=0.8333 ✓
TF-IDF + Naive Bayes: F1=1.0000, Acc=1.0000, Prec=1.0000, Rec=1.0000 ✓
TF-IDF + LinearSVC: F1=0.8333, Acc=0.8333, Prec=0.8333, Rec=0.8333 ✓

✓ Résultats sauvegardés: sentiment/resultats.json
```

---

## 🚨 Troubleshooting

### ImportError: No module named sklearn
```bash
pip install scikit-learn
```

### FileNotFoundError: data/reviews.csv
- Vérifier que `reviews.csv` existe dans `spark-project/data/`
- Chemin relatif dépend du répertoire d'exécution

### Scala compilation error
```bash
# Mettre à jour sbt
sbt update
sbt clean compile
```

### JSON file not found by dashboard
- Vérifier que `resultats.json` est créé dans `sentiment/`
- Rafraîchir le dashboard (F5 ou Ctrl+R)

---

## 📚 Architecture du Pipeline

```
Data Loading (CSV)
    ↓
[TRAIN] Tokenizer
    ↓
[TRAIN] StopWordsRemover
    ↓
[TRAIN] HashingTF
    ↓
[TRAIN] IDF Fit
    ↓
┌─────────────────────────────────┐
│   Model 1: LogisticRegression   │
├─────────────────────────────────┤
│   Model 2: NaiveBayes           │  ← Meilleur (F1=1.0)
├─────────────────────────────────┤
│   Model 3: LinearSVC            │
└─────────────────────────────────┘
    ↓ [TEST]
Evaluation & Metrics
    ↓
JSON Export
    ↓
Dashboard Visualization
```

---

## 🎯 Configuration Recommandée

Pour adapter le pipeline à vos besoins:

**Fichier: build.sbt**
- Modifier `scalaVersion` pour compiler avec une autre version
- Ajouter dépendances custom dans `libraryDependencies`

**Fichier: SentimentAnalysis.scala**
- Ligne ~88: `setNumFeatures(256)` → modifier pour plus/moins de features
- Ligne ~127: Modifier hyperparameters (maxIter, regParam)
- Ligne ~48: Modifier chemin du dataset

**Fichier: data/reviews.csv**
- Ajouter plus d'avis pour améliorer la performance
- Format: id, texte, sentiment (0 ou 1)

---

## 📊 Extensibility

### Ajouter un nouveau modèle
```scala
val customModel = new MyClassifier()
  .setLabelCol("sentiment")
  .setFeaturesCol("features")

val newPipeline = new Pipeline()
  .setStages(Array(tokenizer, stopWordsRemover, hashingTF, idf, customModel))
```

### Utiliser Word2Vec au lieu de TF-IDF
```scala
val word2vec = new Word2Vec()
  .setInputCol("filtered_words")
  .setOutputCol("features")
  .setVectorSize(256)
```

### Entraîner sur un dataset externe
1. Créer CSV avec colonnes: texte, sentiment
2. Modifier chemin dans `loadData()`
3. Recompiler et exécuter

---

## 📞 Support

Pour toute question ou issue:
- Vérifier les logs d'erreur
- Consulter la documentation Spark MLlib
- Vérifier structure du JSON

---

**Version:** 1.0 | **Scala:** 2.13 | **Spark:** 4.1.1 | **Python:** 3.8+
