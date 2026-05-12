# Analyse de sentiment (Projet 5)

Ce projet permet de prédire le sentiment d’un avis client (positif ou négatif).  
Le texte est nettoyé, transformé en variables numériques, puis analysé par trois modèles de machine learning.  
Les résultats sont affichés dans un dashboard interactif avec une zone de test.

---

## Ce que vous pouvez faire

- Ouvrir le dashboard et consulter les métriques des modèles  
- Tester un commentaire en direct  
- Recalculer les résultats avec le script Python  
- Voir une validation croisée 5-fold  

---

## Démarrage rapide

### 1) Lancer le dashboard

Ouvrez simplement :

dashboard.html

Si le chargement des données est bloqué par le navigateur :

```bash
python -m http.server 8000
Puis ouvrir :

http://localhost:8000/dashboard.html
cd spark-project
pip install scikit-learn numpy
python generate_results.py
sentiment/
├── dashboard.html
├── resultats.json
└── spark-project/
    ├── generate_results.py
    └── src/main/scala/com/sentiment/SentimentAnalysis.scala
    Le projet utilise le dataset Amazon Review Polarity.
Il contient des avis clients réels annotés avec une polarité :

1 = négatif
2 = positif

Un sous-ensemble équilibré est utilisé pour l’entraînement et les tests.

Le dashboard affiche uniquement le contenu du fichier resultats.json.
Pour mettre à jour les résultats, il faut relancer le script Python ou le pipeline Spark, puis rafraîchir la page.

Les fichiers de données volumineux ne sont pas versionnés dans Git.
Auteur

Mariam BARBOUCH