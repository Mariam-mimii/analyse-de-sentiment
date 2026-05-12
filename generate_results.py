import json
import csv
import re
import sys
from pathlib import Path
from sklearn.model_selection import train_test_split, cross_validate, StratifiedKFold
from sklearn.pipeline import Pipeline
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.naive_bayes import MultinomialNB
from sklearn.svm import LinearSVC
from sklearn.metrics import (f1_score, accuracy_score, precision_score,
                              recall_score, confusion_matrix, make_scorer)
import numpy as np


def stem_word(word):
    w = word.lower()
    if len(w) <= 3:
        return w
    if w.endswith('ement'):
        return w[:-5]
    if w.endswith('ments'):
        return w[:-5]
    if w.endswith('tion'):
        return w[:-4]
    if w.endswith('ions'):
        return w[:-4]
    if w.endswith('eaux'):
        return w[:-1]
    if w.endswith('aux'):
        return w[:-1]
    if w.endswith('es') and len(w) > 4:
        return w[:-2]
    if w.endswith('s') and len(w) > 4:
        return w[:-1]
    if w.endswith('e') and len(w) > 4:
        return w[:-1]
    return w


def normalize_text(text):
    cleaned = re.sub(r"[\W_]+", " ", text.lower(), flags=re.UNICODE)
    tokens = [stem_word(t) for t in cleaned.split() if t]
    return " ".join(tokens)


def build_pipeline(model):
    return Pipeline([
        ('tfidf', TfidfVectorizer(
            max_features=256, lowercase=True,
            ngram_range=(1, 2), min_df=1, max_df=0.9)),
        ('clf', model)
    ])


def load_data(csv_path, max_per_class=5000):
    # format Amazon Review Polarity: sans header, colonnes polarity/title/text
    # polarity 1 = negatif (label 0), polarity 2 = positif (label 1)
    pos, neg = [], []
    with open(csv_path, 'r', encoding='utf-8') as f:
        reader = csv.reader(f)
        for row in reader:
            if len(row) < 3:
                continue
            polarity = row[0].strip()
            text = normalize_text(row[1].strip() + ' ' + row[2].strip())
            if polarity == '2' and len(pos) < max_per_class:
                pos.append((text, 1))
            elif polarity == '1' and len(neg) < max_per_class:
                neg.append((text, 0))
            if len(pos) >= max_per_class and len(neg) >= max_per_class:
                break
    data = pos + neg
    return [d[0] for d in data], [d[1] for d in data]


def cross_validate_models(texts, labels):
    models = {
        'TF-IDF + Regression Logistique': LogisticRegression(max_iter=100, random_state=42),
        'TF-IDF + Naive Bayes':           MultinomialNB(alpha=1.0),
        'TF-IDF + LinearSVC':             LinearSVC(max_iter=1000, random_state=42)
    }
    cv = StratifiedKFold(n_splits=5, shuffle=True, random_state=42)
    scoring = {
        'f1':        make_scorer(f1_score, zero_division=0),
        'accuracy':  make_scorer(accuracy_score),
        'precision': make_scorer(precision_score, zero_division=0),
        'recall':    make_scorer(recall_score, zero_division=0)
    }
    results = {}
    for name, model in models.items():
        print(f"  CV {name}...", end=" ")
        scores = cross_validate(
            build_pipeline(model), texts, labels,
            cv=cv, scoring=scoring, n_jobs=-1, error_score='raise'
        )
        results[name] = {
            'f1_mean':        round(float(np.mean(scores['test_f1'])), 4),
            'f1_std':         round(float(np.std(scores['test_f1'])), 4),
            'accuracy_mean':  round(float(np.mean(scores['test_accuracy'])), 4),
            'accuracy_std':   round(float(np.std(scores['test_accuracy'])), 4),
            'precision_mean': round(float(np.mean(scores['test_precision'])), 4),
            'precision_std':  round(float(np.std(scores['test_precision'])), 4),
            'recall_mean':    round(float(np.mean(scores['test_recall'])), 4),
            'recall_std':     round(float(np.std(scores['test_recall'])), 4),
        }
        print(f"F1={results[name]['f1_mean']:.4f} (+/-{results[name]['f1_std']:.4f})")
    return results


def train_models(X_train, y_train, X_test, y_test, texts_test):
    models = {
        'TF-IDF + Regression Logistique': LogisticRegression(max_iter=100, random_state=42),
        'TF-IDF + Naive Bayes':           MultinomialNB(alpha=1.0),
        'TF-IDF + LinearSVC':             LinearSVC(max_iter=1000, random_state=42)
    }
    results, cms, preds = {}, {}, []

    for name, model in models.items():
        print(f"  {name}...", end=" ")
        model.fit(X_train, y_train)
        y_pred = model.predict(X_test)

        results[name] = {
            'f1':        round(f1_score(y_test, y_pred), 4),
            'accuracy':  round(accuracy_score(y_test, y_pred), 4),
            'precision': round(precision_score(y_test, y_pred), 4),
            'recall':    round(recall_score(y_test, y_pred), 4)
        }
        tn, fp, fn, tp = confusion_matrix(y_test, y_pred).ravel()
        cms[name] = {'tp': int(tp), 'tn': int(tn), 'fp': int(fp), 'fn': int(fn)}
        print(f"F1={results[name]['f1']:.4f}")

    first = list(models.values())[0]
    y_pred_first = first.predict(X_test)
    for i in range(min(5, X_test.shape[0])):
        preds.append({
            'texte':  texts_test[i],
            'reel':   int(y_test[i]),
            'predit': int(y_pred_first[i])
        })
    return results, cms, preds


def export_model_weights(vectorizer, model, path):
    weights = {
        "vocab":       vectorizer.get_feature_names_out().tolist(),
        "idf":         vectorizer.idf_.tolist(),
        "coef":        model.coef_[0].tolist(),
        "bias":        float(model.intercept_[0]),
        "ngram_range": list(vectorizer.ngram_range)
    }
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(weights, f, ensure_ascii=False)
    print(f"  poids exportes: {path}")


def main():
    root = Path(__file__).resolve().parent
    train_csv = root / "train.csv"

    if not train_csv.exists():
        print(f"train.csv introuvable dans {root}")
        sys.exit(1)

    print("chargement des donnees...")
    texts, labels = load_data(train_csv)
    pos = sum(1 for l in labels if l == 1)
    neg = len(labels) - pos
    print(f"  {len(texts)} avis ({pos} positifs, {neg} negatifs)")

    print("\nvalidation croisee 5-fold")
    cv_results = cross_validate_models(texts, labels)

    print("\nsplit train/test 80-20")
    X_train, X_test, y_train, y_test = train_test_split(
        texts, labels, test_size=0.2, random_state=42, stratify=labels
    )
    print(f"  train={len(X_train)}, test={len(X_test)}")

    print("\nvectorisation TF-IDF")
    vectorizer = TfidfVectorizer(
        max_features=256, lowercase=True,
        ngram_range=(1, 2), min_df=1, max_df=0.9
    )
    X_train_tfidf = vectorizer.fit_transform(X_train)
    X_test_tfidf  = vectorizer.transform(X_test)
    print(f"  vocabulaire: {len(vectorizer.get_feature_names_out())} termes")

    print("\nentrainement")
    lr_model = LogisticRegression(max_iter=100, random_state=42)
    lr_model.fit(X_train_tfidf, y_train)

    results, cms, predictions = train_models(
        X_train_tfidf, y_train, X_test_tfidf, y_test, X_test
    )

    print("\nexport poids modele")
    export_model_weights(vectorizer, lr_model, root / "model_weights.json")

    output = {
        "dataset": {
            "total":    len(labels),
            "positifs": pos,
            "negatifs": neg,
            "train":    len(X_train),
            "test":     len(X_test)
        },
        "modeles": [
            {
                "nom":       name,
                "f1":        m['f1'],
                "accuracy":  m['accuracy'],
                "precision": m['precision'],
                "recall":    m['recall'],
                "cv":        cv_results.get(name, {})
            }
            for name, m in results.items()
        ],
        "crossValidation": {
            "folds":  5,
            "models": [{"nom": n, **v} for n, v in cv_results.items()]
        },
        "confusionMatrices": [
            {"modelName": n, **cm} for n, cm in cms.items()
        ],
        "predictions": predictions
    }

    out_path = root / "resultats.json"
    with open(out_path, 'w', encoding='utf-8') as f:
        json.dump(output, f, indent=2, ensure_ascii=False)
    print(f"\nresultats.json sauvegarde: {out_path}")

    print("\n--- resultats ---")
    for name, m in results.items():
        print(f"{name}")
        print(f"  F1={m['f1']:.4f}  Acc={m['accuracy']:.4f}  "
              f"Prec={m['precision']:.4f}  Rec={m['recall']:.4f}")


if __name__ == '__main__':
    main()