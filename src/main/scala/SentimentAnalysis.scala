import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.feature._
import org.apache.spark.ml.classification._
import org.apache.spark.ml.evaluation.MulticlassClassificationEvaluator
import org.apache.spark.sql.types._
import java.io.{File, PrintWriter}

object SentimentAnalysis {
  def main(args: Array[String]): Unit = {

    val spark = SparkSession.builder()
      .appName("Sentiment Analysis")
      .master("local[*]")
      .getOrCreate()
    spark.sparkContext.setLogLevel("ERROR")
    import spark.implicits._

    // DONNÉES
    val reviewsData = Seq(
      (1,  "This product is absolutely amazing! Best purchase ever highly recommend", 1),
      (2,  "Terrible quality broke after one day complete waste of money", 0),
      (3,  "Outstanding fast delivery and excellent quality will buy again", 1),
      (4,  "Horrible experience product was defective and support was rude", 0),
      (5,  "Good value for the price works perfectly and looks great", 1),
      (6,  "Very disappointed description was misleading does not match", 0),
      (7,  "Superb product exceeded all my expectations fantastic quality", 1),
      (8,  "Not worth the money cheap materials and poor craftsmanship", 0),
      (9,  "Love it easy to use and very durable highly satisfied", 1),
      (10, "Awful does not work as advertised complete disappointment", 0),
      (11, "Excellent product great quality and fast shipping very happy", 1),
      (12, "Returned immediately product was damaged and poorly packaged", 0),
      (13, "Five stars works great every time highly recommend this", 1),
      (14, "Do not buy this worst purchase of my life ever", 0),
      (15, "Amazing quality very sturdy and well made worth every penny", 1),
      (16, "Brilliant product love everything about it perfect in every way", 1),
      (17, "Complete garbage stopped working after two days terrible product", 0),
      (18, "Incredible value for money best product I have ever bought", 1),
      (19, "Very cheap and flimsy fell apart within a week of use", 0),
      (20, "Absolutely love this works perfectly and looks beautiful so happy", 1),
      (21, "Broken on arrival packaging was terrible and service was slow", 0),
      (22, "Highly recommend this product very satisfied with purchase overall", 1),
      (23, "Not as described very misleading product listing waste of time", 0),
      (24, "Incredible product best I have used in years so satisfied", 1),
      (25, "Regret buying this stops working randomly and is very unreliable", 0),
      (26, "Perfect for my needs shipping was fast and packaging excellent", 1),
      (27, "Completely useless stopped working after three uses very frustrating", 0),
      (28, "Great product at great price shipping was quick and easy", 1),
      (29, "Horrible quality control bad customer support avoid this product", 0),
      (30, "Superb quality exceeded expectations definitely buying again soon", 1)
    ).toDF("id", "review_text", "label")

    val total    = reviewsData.count()
    val positifs = reviewsData.filter(col("label") === 1).count()
    val negatifs = reviewsData.filter(col("label") === 0).count()

    // PRÉTRAITEMENT
    val preprocessed = reviewsData
      .withColumn("text_clean", lower(col("review_text")))
      .withColumn("text_clean", regexp_replace(col("text_clean"), "[^a-z\\s]", ""))
      .withColumn("text_clean", trim(regexp_replace(col("text_clean"), "\\s+", " ")))
      .withColumn("label", col("label").cast(DoubleType))

    // PIPELINE
    val tokenizer        = new Tokenizer().setInputCol("text_clean").setOutputCol("tokens_raw")
    val stopWordsRemover = new StopWordsRemover().setInputCol("tokens_raw").setOutputCol("tokens")
    val hashingTF        = new HashingTF().setInputCol("tokens").setOutputCol("raw_features").setNumFeatures(500)
    val idf              = new IDF().setInputCol("raw_features").setOutputCol("features")
    val lr               = new LogisticRegression().setFeaturesCol("features").setLabelCol("label").setMaxIter(100)
    val nb               = new NaiveBayes().setFeaturesCol("features").setLabelCol("label")

    val Array(trainData, testData) = preprocessed.randomSplit(Array(0.8, 0.2), seed = 42)

    val evaluator = new MulticlassClassificationEvaluator()
      .setLabelCol("label").setPredictionCol("prediction")

    // MODÈLE LR
    val modelLR      = new Pipeline().setStages(Array(tokenizer, stopWordsRemover, hashingTF, idf, lr)).fit(trainData)
    val predsLR      = modelLR.transform(testData)
    val f1LR         = evaluator.setMetricName("f1").evaluate(predsLR)
    val accLR        = evaluator.setMetricName("accuracy").evaluate(predsLR)
    val precLR       = evaluator.setMetricName("weightedPrecision").evaluate(predsLR)
    val recLR        = evaluator.setMetricName("weightedRecall").evaluate(predsLR)

    // MODÈLE NB
    val modelNB      = new Pipeline().setStages(Array(tokenizer, stopWordsRemover, hashingTF, idf, nb)).fit(trainData)
    val predsNB      = modelNB.transform(testData)
    val f1NB         = evaluator.setMetricName("f1").evaluate(predsNB)
    val accNB        = evaluator.setMetricName("accuracy").evaluate(predsNB)
    val precNB       = evaluator.setMetricName("weightedPrecision").evaluate(predsNB)
    val recNB        = evaluator.setMetricName("weightedRecall").evaluate(predsNB)

    // TEST NOUVEAUX COMMENTAIRES
    val newReviews = Seq(
      ("This is an incredible product! I love it!", 1),
      ("Absolute garbage. I want my money back.", 0),
      ("It is okay nothing special but works fine.", 0),
      ("Best product ever! Amazing quality!", 1),
      ("Terrible experience very disappointed.", 0)
    ).toDF("review_text", "label")
      .withColumn("text_clean", lower(col("review_text")))
      .withColumn("text_clean", regexp_replace(col("text_clean"), "[^a-z\\s]", ""))
      .withColumn("label", col("label").cast(DoubleType))

    val newPreds = modelLR.transform(newReviews)
    val testResults = newPreds.select("review_text", "label", "prediction").collect()

    // GÉNÉRATION JSON
    val jsonContent = s"""
{
  "dataset": {
    "total": $total,
    "positifs": $positifs,
    "negatifs": $negatifs,
    "train": ${trainData.count()},
    "test": ${testData.count()}
  },
  "modeles": [
    {
      "nom": "TF-IDF + Régression Logistique",
      "f1": ${f1LR.formatted("%.4f")},
      "accuracy": ${accLR.formatted("%.4f")},
      "precision": ${precLR.formatted("%.4f")},
      "recall": ${recLR.formatted("%.4f")}
    },
    {
      "nom": "TF-IDF + Naive Bayes",
      "f1": ${f1NB.formatted("%.4f")},
      "accuracy": ${accNB.formatted("%.4f")},
      "precision": ${precNB.formatted("%.4f")},
      "recall": ${recNB.formatted("%.4f")}
    }
  ],
  "predictions": [
    ${testResults.map(r =>
      s"""{"texte": "${r.getString(0).replace("\"", "\\\"")}", "reel": ${r.getDouble(1).toInt}, "predit": ${r.getDouble(2).toInt}}"""
    ).mkString(",\n    ")}
  ]
}"""

    val pw = new PrintWriter(new File("C:\\users\\barbo\\sentiment\\resultats.json"))
    pw.write(jsonContent)
    pw.close()

    println("Resultats sauvegardes dans resultats.json")
    spark.stop()
  }
}