# 🤝 GUIDE DE CONTRIBUTION & MODIFICATION

## 📝 Table des Matières

1. [Ajouter de nouvelles données](#ajouter-de-nouvelles-données)
2. [Modifier le dashboard](#modifier-le-dashboard)
3. [Ajouter des modèles](#ajouter-des-modèles)
4. [Hyperparamètres](#hyperparamètres)
5. [Nouveaux algorithmes](#nouveaux-algorithmes)
6. [Testing](#testing)
7. [Git workflow](#git-workflow)

---

## 🎯 Ajouter de Nouvelles Données

### Étape 1: Préparer le CSV

Éditer `spark-project/data/reviews.csv`:

```csv
id,texte,sentiment
1,Excellent produit !,1
2,Terrible expérience,0
3,Très satisfait de mon achat,1
...
```

**Format:**
- Colonne 1: `id` (numéro unique)
- Colonne 2: `texte` (avis en FR ou EN)
- Colonne 3: `sentiment` (0=négatif, 1=positif)

### Étape 2: Générer les Résultats

**Option A: Python (Rapide)**
```bash
cd sentiment/spark-project
pip install -r requirements.txt
python generate_results.py
```

**Option B: Scala (Complet)**
```bash
cd sentiment/spark-project
sbt clean compile assembly
spark-submit --class com.sentiment.SentimentAnalysis \
  target/scala-2.13/SentimentAnalysis-assembly-1.0.jar
```

### Étape 3: Vérifier le Résultat

1. Ouvrir `sentiment/resultats.json`
2. Vérifier que les modèles ont été générés
3. Ouvrir `sentiment/dashboard.html`
4. Les graphiques doivent être à jour

---

## 🎨 Modifier le Dashboard

### Changer les Couleurs

Fichier: `sentiment/dashboard.html`

```javascript
// Chercher "CSS Variables" (ligne ~50)
const colors = {
    primary: '#007AFF',    // Bleu principal
    success: '#34C759',    // Vert
    warning: '#FF9500',    // Orange
    danger: '#FF3B30',     // Rouge
    light: '#F2F2F7',      // Gris clair
    dark: '#1C1C1E',       // Gris foncé
    white: '#FFFFFF'       // Blanc
};

// Modifier les valeurs hex
```

### Ajouter une Nouvelle Carte de Métrique

Chercher `<div class="metric-card">` dans le HTML et dupliquer:

```html
<div class="metric-card">
    <h3>Nouvelle Métrique</h3>
    <p class="metric-value" id="newMetric">0.00</p>
    <p class="metric-label">Description</p>
</div>
```

Puis dans JavaScript (fonction `updateMetrics()`):

```javascript
document.getElementById('newMetric').textContent = 
    (valeur * 100).toFixed(2) + '%';
```

### Ajouter un Nouveau Graphique

Utiliser Chart.js (déjà inclus):

```javascript
// Créer canvas
<canvas id="newChart"></canvas>

// JavaScript
const ctx = document.getElementById('newChart').getContext('2d');
const chart = new Chart(ctx, {
    type: 'bar',
    data: {
        labels: ['A', 'B', 'C'],
        datasets: [{
            label: 'Dataset',
            data: [10, 20, 30],
            backgroundColor: colors.primary
        }]
    }
});
```

---

## 🤖 Ajouter des Modèles

### Python (generate_results.py)

Chercher `# Train classifiers`:

```python
# Ajouter après les 3 modèles existants:

from sklearn.ensemble import RandomForestClassifier

rf = RandomForestClassifier(n_estimators=100, random_state=42)
rf.fit(X_train, y_train)

y_pred_rf = rf.predict(X_test)
f1_rf = f1_score(y_test, y_pred_rf)
accuracy_rf = accuracy_score(y_test, y_pred_rf)
precision_rf = precision_score(y_test, y_pred_rf)
recall_rf = recall_score(y_test, y_pred_rf)

models.append({
    'nom': 'TF-IDF + Random Forest',
    'f1': round(f1_rf, 4),
    'accuracy': round(accuracy_rf, 4),
    'precision': round(precision_rf, 4),
    'recall': round(recall_rf, 4)
})

# Ajouter aussi à confusionMatrices
```

### Scala (SentimentAnalysis.scala)

Chercher `// Train classifiers`:

```scala
import org.apache.spark.ml.classification.RandomForestClassifier

val rf = new RandomForestClassifier()
    .setLabelCol("label")
    .setFeaturesCol("features")
    .setNumTrees(100)

val rfModel = rf.fit(trainingData)
val predictions = rfModel.transform(testData)

// Ajouter à results array
```

---

## ⚙️ Hyperparamètres

### Logistic Regression

**Python:**
```python
from sklearn.linear_model import LogisticRegression

lr = LogisticRegression(
    max_iter=1000,          # Augmenter si nécessaire
    C=1.0,                  # 1/L2 regularization strength
    penalty='l2',           # Type de régularisation
    solver='lbfgs',         # Algorithme d'optimisation
    random_state=42
)
```

**Scala:**
```scala
new LogisticRegression()
    .setMaxIter(100)
    .setRegParam(0.3)
    .setElasticNetParam(0.0)
```

### Naive Bayes

**Python:**
```python
from sklearn.naive_bayes import MultinomialNB

nb = MultinomialNB(
    alpha=1.0              # Additive smoothing
)
```

### LinearSVC

**Python:**
```python
from sklearn.svm import LinearSVC

svc = LinearSVC(
    max_iter=2000,         # Augmenter si nécessaire
    C=1.0,                 # Inverse regularization strength
    random_state=42,
    dual='auto'            # Auto-détection pour petit dataset
)
```

### TF-IDF

**Python:**
```python
from sklearn.feature_extraction.text import TfidfVectorizer

vectorizer = TfidfVectorizer(
    max_features=256,      # Nombre de features (changer à 512, 1024...)
    ngram_range=(1, 2),    # (1,1)=unigrams ou (1,2)=uni+bigrams
    min_df=1,              # Min doc frequency
    max_df=0.9             # Max doc frequency
)
```

---

## 🔍 Nouveaux Algorithmes

### Ajouter XGBoost

**Installation:**
```bash
pip install xgboost
```

**Code (Python):**
```python
from xgboost import XGBClassifier

xgb = XGBClassifier(
    n_estimators=100,
    max_depth=5,
    learning_rate=0.1,
    random_state=42
)

xgb.fit(X_train, y_train)
y_pred_xgb = xgb.predict(X_test)

# Ajouter aux résultats...
```

### Ajouter BERT (Deep Learning)

**Installation:**
```bash
pip install transformers torch
```

**Code (Python):**
```python
from transformers import AutoTokenizer, AutoModelForSequenceClassification
import torch

model_name = "distilbert-base-multilingual-cased"
tokenizer = AutoTokenizer.from_pretrained(model_name)
model = AutoModelForSequenceClassification.from_pretrained(model_name, 
                                                           num_labels=2)

# Fine-tuning sur dataset...
```

### Ajouter Word2Vec

**Installation:**
```bash
pip install gensim
```

**Code (Python):**
```python
from gensim.models import Word2Vec
from sklearn.decomposition import PCA

# Entraîner Word2Vec
w2v = Word2Vec(sentences=tokenized_texts, 
               vector_size=300, 
               window=5,
               workers=4)

# Vectoriser les documents
vectors = []
for tokens in tokenized_texts:
    vector = w2v.wv[tokens].mean(axis=0)
    vectors.append(vector)

X = np.array(vectors)
# Puis entraîner les modèles sur X...
```

---

## 🧪 Testing

### Test Manual du Dashboard

```javascript
// Ouvrir console (F12)
// Tester la fonction NLP
nlpAnalyze("C'est magnifique!");  // Devrait retourner positif

// Tester le chargement des données
console.log(data);  // Affiche les résultats chargés
```

### Test des Modèles Python

```bash
cd spark-project
python -m pytest test_models.py
```

**Créer `test_models.py`:**
```python
from generate_results import load_data, train_models
import numpy as np

def test_load_data():
    X, y = load_data()
    assert len(X) == 30
    assert len(np.unique(y)) == 2

def test_models():
    X, y = load_data()
    from sklearn.model_selection import train_test_split
    X_train, X_test, y_train, y_test = train_test_split(X, y, 
                                                         test_size=0.2)
    models = train_models(X_train, y_train)
    assert len(models) == 3

if __name__ == '__main__':
    test_load_data()
    test_models()
    print("All tests passed!")
```

### Linter Python

```bash
pip install pylint flake8

# Vérifier le code
pylint spark-project/generate_results.py
flake8 spark-project/generate_results.py
```

---

## 🌳 Git Workflow

### Initialiser un Repository

```bash
cd sentiment
git init
git config user.name "Votre Nom"
git config user.email "votre.email@example.com"
```

### Commit Initial

```bash
git add .
git commit -m "Initial commit: Sentiment analysis pipeline

- Spark MLlib pipeline avec 3 modèles
- Dashboard interactive HTML5
- Python alternative
- 30-sample dataset"
```

### Branches pour Modification

```bash
# Créer une branche pour nouvelle feature
git checkout -b feature/add-xgboost

# Faire les modifications...
git add spark-project/generate_results.py
git commit -m "Add XGBoost model"

# Merger dans main
git checkout main
git merge feature/add-xgboost
```

### Commit Message Conventions

```
feat:     Nouvelle fonctionnalité
fix:      Bug fix
docs:     Documentation
style:    Formatage, pas de changement logique
refactor: Code restructuring
perf:     Performance improvement
test:     Ajouter/modifier tests
chore:    Maintenance

Exemple:
feat: Add GridSearchCV for hyperparameter tuning

- Implémente GridSearchCV pour tous les modèles
- Optimise C pour LogisticRegression
- Améliore F1-score de 5%
```

### Branches de Production

```
main              ← Version stable
├── develop       ← Développement
    ├── feature/xgboost
    ├── feature/word2vec
    └── feature/deployment

```

---

## 📈 Version Management

### Semantic Versioning (MAJOR.MINOR.PATCH)

```
Version 4.1.1
    ↓    ↓  ↓
 MAJOR  MINOR PATCH

4.0.0 = Version majeure (breaking changes)
4.1.0 = Nouvelle feature
4.1.1 = Bug fix
```

### Update Version

**Modifier VERSION dans:**
- `manifest.json` → "version"
- `Dockerfile` → FROM, SPARK version
- `build.sbt` → version
- `README.md` → heading

```bash
# Tag une version
git tag -a v4.1.1 -m "Release version 4.1.1"
git push origin v4.1.1
```

---

## 📚 Standards de Code

### Python Style (PEP 8)

```python
# ✅ BON
def train_models(X_train, y_train):
    """Train and evaluate classification models."""
    models = []
    
    # Logistic Regression
    lr = LogisticRegression()
    lr.fit(X_train, y_train)
    models.append(lr)
    
    return models

# ❌ MAUVAIS
def trainmodels(X_train,y_train):
    models=[]
    lr=LogisticRegression();lr.fit(X_train,y_train)
    models.append(lr);return models
```

### HTML Style

```html
<!-- ✅ BON -->
<div class="metric-card">
    <h3>Titre</h3>
    <p class="value" id="metricValue">0.00</p>
</div>

<!-- ❌ MAUVAIS -->
<DIV CLASS="metric-card"><H3>Titre</H3><p>0.00</p></DIV>
```

### Scala Style

```scala
// ✅ BON
val lr = new LogisticRegression()
    .setMaxIter(100)
    .setRegParam(0.3)

// ❌ MAUVAIS
val lr=new LogisticRegression().setMaxIter(100).setRegParam(0.3)
```

---

## 🚀 Checklist Avant Commit

- [ ] Code testé localement
- [ ] Pas d'erreurs de compilation/syntaxe
- [ ] Linter OK (pylint, flake8)
- [ ] Commentaires et docstrings présents
- [ ] JSON généré valide
- [ ] Dashboard charge les données
- [ ] Documentation mise à jour
- [ ] Pas de secrets dans le code
- [ ] Tests passent

---

## 📞 Questions Fréquentes

**Q: Puis-je modifier le dataset?**
A: Oui! Éditer `spark-project/data/reviews.csv` puis générer résultats.

**Q: Combien de temps pour générer?**
A: Python: 5-10s. Scala: 20-30s (compilation incluse).

**Q: Puis-je ajouter plus de modèles?**
A: Oui, suivre les instructions dans "Ajouter des modèles".

**Q: Quel est le meilleur modèle?**
A: Naive Bayes (F1=1.0). À valider sur plus de données.

**Q: Comment déployer?**
A: Lire `DEPLOYMENT.md` pour 4 options.

---

## 🔗 Ressources

- scikit-learn docs: https://scikit-learn.org/
- Spark MLlib: https://spark.apache.org/mllib/
- PEP 8: https://pep8.org/
- Git workflow: https://git-scm.com/book/en/v2/Git-Branching-Branching-Workflow

---

**Happy Contributing!** 🎉
