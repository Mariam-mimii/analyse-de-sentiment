# 🚀 DÉMARRAGE RAPIDE - 5 MINUTES

## ⚡ Option 1: Dashboard (Immédiat - RECOMMANDÉ)

```bash
# 1. Ouvrir le fichier
double-clic: sentiment/dashboard.html

# 2. C'est prêt! 
# - Les résultats se chargent depuis resultats.json
# - Zone interactive pour tester de nouveaux avis
```

**✓ Pas d'installation, pas de compilation**

---

## 🐍 Option 2: Python (2 minutes)

```bash
cd sentiment/spark-project

# Installer dépendances
pip install scikit-learn numpy

# Générer résultats
python generate_results.py

# ✓ resultats.json est régénéré
# ✓ Rafraîchir dashboard pour voir les nouveaux résultats
```

---

## 🏗️ Option 3: Scala/Spark (Complet)

```bash
cd sentiment/spark-project

# Compiler
sbt clean compile assembly

# Exécuter
spark-submit --class com.sentiment.SentimentAnalysis \
  target/scala-2.13/SentimentAnalysis-assembly-1.0.jar

# ✓ resultats.json généré depuis Spark MLlib
```

---

## 📊 Résultats Affichés

```
✅ 30 avis chargés (15 positifs, 15 négatifs)
✅ 3 modèles entraînés:
   - TF-IDF + Régression Logistique: F1=0.8333
   - TF-IDF + Naive Bayes: F1=1.0000 ⭐ MEILLEUR
   - TF-IDF + LinearSVC: F1=0.8333
✅ Matrices de confusion calculées
✅ Prédictions testées
✅ Rapport d'évaluation généré
```

---

## 🧪 Tester le NLP

Dans le dashboard, section **"Test interactif"**:

```
Écrivez: "C'est un produit excellent, je recommande!"
Résultat: ✓ POSITIF (Confiance: 95%)
Explication: Le commentaire exprime un sentiment positif grâce aux termes: "excellent", "recommande"
```

---

## 📁 Fichiers Clés

| Fichier | Rôle | Accès |
|---------|------|-------|
| `dashboard.html` | Interface interactive | Double-clic pour ouvrir |
| `rapport_evaluation.html` | Rapport détaillé | Lien dans dashboard |
| `resultats.json` | Données modèles | Chargé auto |
| `README.md` | Documentation complète | Lire en texte |
| `generate_results.py` | Générer résultats Python | `python generate_results.py` |

---

## ❌ Erreurs Courantes

| Erreur | Solution |
|--------|----------|
| Dashboard vide | Rafraîchir (F5) ou vérifier resultats.json |
| `ModuleNotFoundError: sklearn` | `pip install scikit-learn` |
| `FileNotFoundError: reviews.csv` | Vérifier chemin: `spark-project/data/` |
| Spark non trouvé | `pip install pyspark` (alternative) |

---

## 🎯 Prochaines Étapes

1. **Augmenter dataset** → Ajouter plus d'avis dans `reviews.csv`
2. **Modifier modèles** → Éditer `generate_results.py` ou `SentimentAnalysis.scala`
3. **Déployer en prod** → Utiliser `spark-submit` sur serveur
4. **Tester modèles avancés** → BERT, DistilBERT, etc.

---

## 📚 Documentation Complète

Voir [README.md](README.md) pour:
- ✅ Description complète du pipeline
- ✅ Architecture détaillée
- ✅ Résultats obtenus
- ✅ Concepts clés (TF-IDF, matrices de confusion, etc.)
- ✅ Recommandations d'amélioration

---

**Prêt à commencer?** 👉 **Ouvrir `dashboard.html`** 🎯
