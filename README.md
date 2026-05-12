# Analyse de sentiment (Projet 5)

Ce projet fait une chose simple et utile: **deviner l'humeur d'un avis**. On nettoie le texte, on le vectorise, puis trois modèles donnent leur verdict. Tout s'affiche dans un dashboard clair, avec une zone de test pour essayer vos propres phrases.

## Ce que vous pouvez faire

- Ouvrir le dashboard et lire les métriques
- Tester un commentaire en direct (zone interactive)
- Recalculer les résultats avec le script Python
- Voir une validation croisée 5-fold dans le dashboard

## Démarrage rapide

### 1) Voir le dashboard

```bash
dashboard.html
```

Si les données ne chargent pas (bloqué par le navigateur), lancez un petit serveur local:

```bash
python -m http.server 8000
```

Puis ouvrez http://localhost:8000/dashboard.html

### 2) Recalculer les resultats (Python)

```bash
cd spark-project
pip install scikit-learn numpy
python generate_results.py
```

## Structure minimale

```
sentiment/
├── dashboard.html
├── resultats.json
└── spark-project/
    ├── generate_results.py
    └── src/main/scala/com/sentiment/SentimentAnalysis.scala
```

## A propos des donnees

Le dashboard **affiche** ce qui est dans `resultats.json`. Pour mettre a jour les chiffres, relancez `generate_results.py` (ou le pipeline Scala) puis rafraichissez le dashboard. Les fichiers de jeu de données lourds restent locaux et sont ignores par git.

## Auteur

Mariam BARBOUCH
