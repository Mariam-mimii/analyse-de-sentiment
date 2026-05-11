#!/usr/bin/env python3
"""
Sentiment Analysis Pipeline - Python Implementation
Génère les résultats pour le dashboard
"""

import json
import csv
import re
from pathlib import Path
from sklearn.model_selection import train_test_split
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.naive_bayes import MultinomialNB
from sklearn.svm import LinearSVC
from sklearn.metrics import f1_score, accuracy_score, precision_score, recall_score, confusion_matrix
import numpy as np


def stem_word(word):
    """Stemming léger pour harmoniser le prétraitement FR/EN."""
    word = word.lower()

    if len(word) <= 3:
        return word
    if word.endswith('ement'):
        return word[:-5]
    if word.endswith('ments'):
        return word[:-5]
    if word.endswith('tion'):
        return word[:-4]
    if word.endswith('ions'):
        return word[:-4]
    if word.endswith('eaux'):
        return word[:-1]
    if word.endswith('aux'):
        return word[:-1]
    if word.endswith('es') and len(word) > 4:
        return word[:-2]
    if word.endswith('s') and len(word) > 4:
        return word[:-1]
    if word.endswith('e') and len(word) > 4:
        return word[:-1]
    return word


def normalize_text(text):
    """Nettoie et stemme un texte avant vectorisation."""
    cleaned = re.sub(r"[\W_]+", " ", text.lower(), flags=re.UNICODE)
    tokens = [stem_word(token) for token in cleaned.split() if token]
    return " ".join(tokens)

def load_data(csv_path):
    """Charge les données depuis le CSV"""
    texts = []
    labels = []
    
    with open(csv_path, 'r', encoding='utf-8') as f:
        reader = csv.DictReader(f)
        for row in reader:
            texts.append(normalize_text(row['texte']))
            labels.append(int(row['sentiment']))
    
    return texts, labels

def train_models(X_train_tfidf, y_train, X_test_tfidf, y_test):
    """Entraîne les 3 modèles et retourne les métriques"""
    
    models = {
        'TF-IDF + Régression Logistique': LogisticRegression(max_iter=100, random_state=42),
        'TF-IDF + Naive Bayes': MultinomialNB(alpha=1.0),
        'TF-IDF + LinearSVC': LinearSVC(max_iter=1000, random_state=42)
    }
    
    results = {}
    confusion_matrices = {}
    predictions_list = []
    
    for model_name, model in models.items():
        print(f"  Entraînement: {model_name}...", end=" ")
        
        # Entraînement
        model.fit(X_train_tfidf, y_train)
        
        # Prédictions
        y_pred = model.predict(X_test_tfidf)
        
        # Métriques
        f1 = f1_score(y_test, y_pred)
        acc = accuracy_score(y_test, y_pred)
        prec = precision_score(y_test, y_pred)
        rec = recall_score(y_test, y_pred)
        
        results[model_name] = {
            'f1': round(f1, 4),
            'accuracy': round(acc, 4),
            'precision': round(prec, 4),
            'recall': round(rec, 4)
        }
        
        # Matrice de confusion
        cm = confusion_matrix(y_test, y_pred)
        tn, fp, fn, tp = cm.ravel()
        confusion_matrices[model_name] = {
            'tp': int(tp),
            'tn': int(tn),
            'fp': int(fp),
            'fn': int(fn)
        }
        
        print(f"✓ (F1={f1:.4f})")
    
    # Sauvegarder 5 prédictions du premier modèle
    first_model = list(models.values())[0]
    y_pred_first = first_model.predict(X_test_tfidf)
    for i in range(min(5, len(X_test_tfidf.toarray()))):
        predictions_list.append({
            'texte': texts_test[i],
            'reel': int(y_test[i]),
            'predit': int(y_pred_first[i])
        })
    
    return results, confusion_matrices, predictions_list

def main():
    """Fonction principale"""
    print("=" * 80)
    print("SENTIMENT ANALYSIS PIPELINE")
    print("=" * 80)
    
    # 1. Charger les données
    print("\n[ÉTAPE 1] Chargement du dataset")
    csv_path = Path("spark-project/data/reviews.csv")
    if not csv_path.exists():
        print(f"❌ Fichier non trouvé: {csv_path}")
        return
    
    global texts_test
    texts, labels = load_data(csv_path)
    print(f"✓ {len(texts)} avis chargés")
    
    pos_count = sum(1 for l in labels if l == 1)
    neg_count = len(labels) - pos_count
    print(f"  - Positifs: {pos_count} ({pos_count*100//len(labels)}%)")
    print(f"  - Négatifs: {neg_count} ({neg_count*100//len(labels)}%)")
    
    # 2. Split train-test
    print("\n[ÉTAPE 2] Split Train-Test (80-20)")
    X_train, X_test, y_train, y_test = train_test_split(
        texts, labels, test_size=0.2, random_state=42, stratify=labels
    )
    texts_test = X_test
    print(f"✓ Train: {len(X_train)}, Test: {len(X_test)}")
    
    # 3. Vectorisation TF-IDF
    print("\n[ÉTAPE 3] Vectorisation TF-IDF")
    vectorizer = TfidfVectorizer(max_features=256, lowercase=True, 
                                  ngram_range=(1, 2), min_df=1, max_df=0.9)
    X_train_tfidf = vectorizer.fit_transform(X_train)
    X_test_tfidf = vectorizer.transform(X_test)
    print(f"✓ Vocabulaire: {len(vectorizer.get_feature_names_out())} termes")
    print(f"✓ Matrice train: {X_train_tfidf.shape}")
    print(f"✓ Matrice test: {X_test_tfidf.shape}")
    
    # 4. Entraînement et évaluation
    print("\n[ÉTAPE 4] Entraînement des modèles")
    results, confusion_matrices, predictions = train_models(
        X_train_tfidf, y_train, X_test_tfidf, y_test
    )
    
    # 5. Générer JSON
    print("\n[ÉTAPE 5] Génération du fichier résultats.json")
    
    output_data = {
        "dataset": {
            "total": len(labels),
            "positifs": pos_count,
            "negatifs": neg_count,
            "train": len(X_train),
            "test": len(X_test)
        },
        "modeles": [
            {
                "nom": name,
                "f1": metrics['f1'],
                "accuracy": metrics['accuracy'],
                "precision": metrics['precision'],
                "recall": metrics['recall']
            }
            for name, metrics in results.items()
        ],
        "confusionMatrices": [
            {
                "modelName": name,
                **cm_data
            }
            for name, cm_data in confusion_matrices.items()
        ],
        "predictions": predictions
    }
    
    output_path = Path("sentiment/resultats.json")
    with open(output_path, 'w', encoding='utf-8') as f:
        json.dump(output_data, f, indent=2, ensure_ascii=False)
    
    print(f"✓ Résultats sauvegardés: {output_path}")
    
    # 6. Afficher le rapport
    print("\n[RÉSULTATS]")
    print("-" * 80)
    for model_name, metrics in results.items():
        print(f"{model_name}:")
        print(f"  F1-Score:   {metrics['f1']:.4f}")
        print(f"  Accuracy:   {metrics['accuracy']:.4f}")
        print(f"  Precision:  {metrics['precision']:.4f}")
        print(f"  Recall:     {metrics['recall']:.4f}")
        print()
    
    print("=" * 80)
    print("PIPELINE COMPLÉTÉ AVEC SUCCÈS ✓")
    print("=" * 80)

if __name__ == '__main__':
    main()
