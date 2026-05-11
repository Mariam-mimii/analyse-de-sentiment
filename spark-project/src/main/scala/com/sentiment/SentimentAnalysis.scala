package com.sentiment

import org.apache.spark.sql.SparkSession
import org.apache.spark.ml.{Pipeline, PipelineModel}
import org.apache.spark.ml.feature.{Tokenizer, StopWordsRemover, HashingTF, IDF}
import org.apache.spark.ml.classification.{LogisticRegression, NaiveBayes, LinearSVC}
import org.apache.spark.ml.evaluation.BinaryClassificationEvaluator
import org.apache.spark.mllib.evaluation.MulticlassMetrics
import org.apache.spark.sql.functions._
import scala.math._
import scala.util.Random
import java.io.{File, PrintWriter}
import scala.io.Source
import java.util.Locale

object SentimentAnalysis {

  case class Review(id: Int, texte: String, sentiment: Double)
  case class ModelMetrics(nom: String, f1: Double, accuracy: Double, precision: Double, recall: Double)
  case class ConfusionMatrix(modelName: String, tp: Int, tn: Int, fp: Int, fn: Int)

  private val punctuationPattern = "[\\p{Punct}\\p{IsPunctuation}]+".r

  private def stemWord(word: String): String = {
    val w = word.toLowerCase(Locale.ROOT)

    if (w.length <= 3) {
      w
    } else if (w.endsWith("ement")) {
      w.dropRight(5)
    } else if (w.endsWith("ments")) {
      w.dropRight(5)
    } else if (w.endsWith("tion")) {
      w.dropRight(4)
    } else if (w.endsWith("ions")) {
      w.dropRight(4)
    } else if (w.endsWith("eaux")) {
      w.dropRight(1)
    } else if (w.endsWith("aux")) {
      w.dropRight(1)
    } else if (w.endsWith("es") && w.length > 4) {
      w.dropRight(2)
    } else if (w.endsWith("s") && w.length > 4) {
      w.dropRight(1)
    } else if (w.endsWith("e") && w.length > 4) {
      w.dropRight(1)
    } else {
      w
    }
  }

  private def normalizeAndStem(text: String): String = {
    if (text == null) {
      ""
    } else {
      punctuationPattern.replaceAllIn(text.toLowerCase(Locale.ROOT), " ")
        .split("\\s+")
        .filter(_.nonEmpty)
        .map(stemWord)
        .mkString(" ")
    }
  }

  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      .appName("SentimentAnalysis")
      .master("local[*]")
      .getOrCreate()

    spark.sparkContext.setLogLevel("ERROR")

    println("=" * 80)
    println("SENTIMENT ANALYSIS PIPELINE - Spark MLlib")
    println("=" * 80)

    try {
      // 1. Load and explore dataset
      val dataPath = "spark-project/data/reviews.csv"
      val df = spark.read
        .option("header", "true")
        .option("inferSchema", "true")
        .csv(dataPath)

      println("\n[STEP 1] Dataset Exploration")
      println(s"Total samples: ${df.count()}")
      df.show(5)

      val positiveCount = df.filter(col("sentiment") === 1).count()
      val negativeCount = df.filter(col("sentiment") === 0).count()
      println(s"Positive samples: $positiveCount (${(positiveCount * 100 / df.count()).toInt}%)")
      println(s"Negative samples: $negativeCount (${(negativeCount * 100 / df.count()).toInt}%)")

      // 2. Train-test split (80-20)
      val Array(trainData, testData) = df.randomSplit(Array(0.8, 0.2), seed = 42)
      println(s"\n[STEP 2] Train-Test Split")
      println(s"Train set: ${trainData.count()} samples")
      println(s"Test set: ${testData.count()} samples")

      // 3. Build NLP Pipeline
      println("\n[STEP 3] NLP Pipeline Construction")

      val normalizeUdf = udf((text: String) => normalizeAndStem(text))

      val tokenizer = new Tokenizer()
        .setInputCol("stemmed_text")
        .setOutputCol("words")

      val stopWordsRemover = new StopWordsRemover()
        .setInputCols(Array("words"))
        .setOutputCol("filtered_words")
        .setStopWords(StopWordsRemover.loadDefaultStopWords("french") ++ StopWordsRemover.loadDefaultStopWords("english"))

      val hashingTF = new HashingTF()
        .setInputCol("filtered_words")
        .setOutputCol("raw_features")
        .setNumFeatures(256)

      val idf = new IDF()
        .setInputCol("raw_features")
        .setOutputCol("features")

      val preparedTrainData = trainData.withColumn("stemmed_text", normalizeUdf(col("texte")))
      val preparedTestData = testData.withColumn("stemmed_text", normalizeUdf(col("texte")))

      println("✓ Tokenizer configured")
      println("✓ StopWordsRemover configured (FR + EN)")
      println("✓ Text normalization and stemming configured")
      println("✓ HashingTF configured (256 features)")
      println("✓ IDF configured")

      // 4. Create and train models
      println("\n[STEP 4] Model Training")

      val models = Seq(
        ("LR", new LogisticRegression().setMaxIter(100).setRegParam(0.01).setLabelCol("sentiment").setFeaturesCol("features")),
        ("NB", new NaiveBayes().setLabelCol("sentiment").setFeaturesCol("features").setSmoothing(1.0)),
        ("SVM", new LinearSVC().setMaxIter(100).setRegParam(0.01).setLabelCol("sentiment").setFeaturesCol("features"))
      )

      val pipelines = models.map { case (name, classifier) =>
        val pipeline = new Pipeline().setStages(Array(tokenizer, stopWordsRemover, hashingTF, idf, classifier))
        (name, pipeline)
      }

      val trainedModels = pipelines.map { case (name, pipeline) =>
        println(s"  Training $name...")
        val model = pipeline.fit(preparedTrainData)
        println(s"  ✓ $name trained")
        (name, model)
      }

      // 5. Evaluate models
      println("\n[STEP 5] Model Evaluation")

      val evaluator = new BinaryClassificationEvaluator()
        .setLabelCol("sentiment")
        .setRawPredictionCol("rawPrediction")

      val metricsResults = trainedModels.map { case (name, model) =>
        val predictions = model.transform(preparedTestData)
        
        // Calculate basic metrics
        val correctPredictions = predictions.filter(col("prediction") === col("sentiment")).count()
        val accuracy = correctPredictions.toDouble / testData.count()

        // Calculate confusion matrix metrics
        val tp = predictions.filter(col("prediction") === 1 && col("sentiment") === 1).count().toInt
        val tn = predictions.filter(col("prediction") === 0 && col("sentiment") === 0).count().toInt
        val fp = predictions.filter(col("prediction") === 1 && col("sentiment") === 0).count().toInt
        val fn = predictions.filter(col("prediction") === 0 && col("sentiment") === 1).count().toInt

        val precision = if ((tp + fp) > 0) tp.toDouble / (tp + fp) else 0.0
        val recall = if ((tp + fn) > 0) tp.toDouble / (tp + fn) else 0.0
        val f1 = if ((precision + recall) > 0) 2 * (precision * recall) / (precision + recall) else 0.0

        println(s"  $name: F1=${f1.formatted("%.4f")}, Acc=${accuracy.formatted("%.4f")}, Prec=${precision.formatted("%.4f")}, Rec=${recall.formatted("%.4f")}")

        (name, ModelMetrics(name, f1, accuracy, precision, recall), ConfusionMatrix(name, tp, tn, fp, fn))
      }

      // 6. Generate results JSON
      println("\n[STEP 6] Generating Results JSON")

      val dataset = Map(
        "total" -> df.count().toInt,
        "positifs" -> positiveCount.toInt,
        "negatifs" -> negativeCount.toInt,
        "train" -> trainData.count().toInt,
        "test" -> testData.count().toInt
      )

      val modeles = metricsResults.map { case (_, metrics, _) =>
        Map(
          "nom" -> metrics.nom,
          "f1" -> round(metrics.f1 * 10000) / 10000.0,
          "accuracy" -> round(metrics.accuracy * 10000) / 10000.0,
          "precision" -> round(metrics.precision * 10000) / 10000.0,
          "recall" -> round(metrics.recall * 10000) / 10000.0
        )
      }

      val confusionMatrices = metricsResults.map { case (_, _, cm) =>
        Map(
          "modelName" -> cm.modelName,
          "tp" -> cm.tp,
          "tn" -> cm.tn,
          "fp" -> cm.fp,
          "fn" -> cm.fn
        )
      }

      // Get predictions for 5 test samples
      val testModelPredictions = trainedModels.head._2.transform(preparedTestData.limit(5))
      val predictions = testModelPredictions.select("texte", "sentiment", "prediction").collect().map { row =>
        Map(
          "texte" -> row.getString(0),
          "reel" -> row.getDouble(1).toInt,
          "predit" -> row.getDouble(2).toInt
        )
      }

      val jsonContent = s"""{
  "dataset": {
    "total": ${dataset("total")},
    "positifs": ${dataset("positifs")},
    "negatifs": ${dataset("negatifs")},
    "train": ${dataset("train")},
    "test": ${dataset("test")}
  },
  "modeles": [
${modeles.map { m =>
  s"""    {
      "nom": "${m("nom")}",
      "f1": ${m("f1")},
      "accuracy": ${m("accuracy")},
      "precision": ${m("precision")},
      "recall": ${m("recall")}
    }"""
}.mkString(",\n")}
  ],
  "confusionMatrices": [
${confusionMatrices.map { cm =>
  s"""    {
      "modelName": "${cm("modelName")}",
      "tp": ${cm("tp")},
      "tn": ${cm("tn")},
      "fp": ${cm("fp")},
      "fn": ${cm("fn")}
    }"""
}.mkString(",\n")}
  ],
  "predictions": [
${predictions.map { p =>
  s"""    {"texte": "${p("texte")}", "reel": ${p("reel")}, "predit": ${p("predit")}}"""
}.mkString(",\n")}
  ]
}"""

      val writer = new PrintWriter(new File("sentiment/resultats.json"))
      writer.write(jsonContent)
      writer.close()
      println("✓ Results saved to sentiment/resultats.json")

      println("\n" + "=" * 80)
      println("PIPELINE COMPLETED SUCCESSFULLY")
      println("=" * 80)

    } finally {
      spark.stop()
    }
  }
}
