package com.sentiment

import org.apache.spark.sql.SparkSession
import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.feature.{Tokenizer, StopWordsRemover, HashingTF, IDF, Word2Vec}
import org.apache.spark.ml.classification.{LogisticRegression, NaiveBayes, LinearSVC}
import org.apache.spark.ml.evaluation.BinaryClassificationEvaluator
import org.apache.spark.ml.tuning.{CrossValidator, ParamGridBuilder}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._
import java.io.{File, PrintWriter}
import java.util.Locale

object SentimentAnalysis {

  case class Metrics(nom: String, f1: Double, accuracy: Double, precision: Double, recall: Double)
  case class CM(modelName: String, tp: Int, tn: Int, fp: Int, fn: Int)
  case class CVResult(nom: String, auc: Double, params: String)

  private def stemWord(word: String): String = {
    val w = word.toLowerCase(Locale.ROOT)
    if (w.length <= 3)            w
    else if (w.endsWith("ement")) w.dropRight(5)
    else if (w.endsWith("ments")) w.dropRight(5)
    else if (w.endsWith("tion"))  w.dropRight(4)
    else if (w.endsWith("ions"))  w.dropRight(4)
    else if (w.endsWith("eaux"))  w.dropRight(1)
    else if (w.endsWith("aux"))   w.dropRight(1)
    else if (w.endsWith("es") && w.length > 4) w.dropRight(2)
    else if (w.endsWith("s")  && w.length > 4) w.dropRight(1)
    else if (w.endsWith("e")  && w.length > 4) w.dropRight(1)
    else w
  }

  private def normalize(text: String): String = {
    if (text == null) return ""
    "[\\p{Punct}\\p{IsPunctuation}]+".r
      .replaceAllIn(text.toLowerCase(Locale.ROOT), " ")
      .split("\\s+")
      .filter(_.nonEmpty)
      .map(stemWord)
      .mkString(" ")
  }

  private def computeMetrics(name: String, preds: org.apache.spark.sql.DataFrame, total: Long) = {
    val tp = preds.filter(col("prediction") === 1.0 && col("sentiment") === 1.0).count().toInt
    val tn = preds.filter(col("prediction") === 0.0 && col("sentiment") === 0.0).count().toInt
    val fp = preds.filter(col("prediction") === 1.0 && col("sentiment") === 0.0).count().toInt
    val fn = preds.filter(col("prediction") === 0.0 && col("sentiment") === 1.0).count().toInt

    val acc  = (tp + tn).toDouble / total
    val prec = if (tp + fp > 0) tp.toDouble / (tp + fp) else 0.0
    val rec  = if (tp + fn > 0) tp.toDouble / (tp + fn) else 0.0
    val f1   = if (prec + rec > 0) 2 * prec * rec / (prec + rec) else 0.0

    (Metrics(name, f1, acc, prec, rec), CM(name, tp, tn, fp, fn))
  }

  private def r4(x: Double) = math.round(x * 10000) / 10000.0

  def main(args: Array[String]): Unit = {

    val spark = SparkSession.builder()
      .appName("SentimentAnalysis")
      .master("local[*]")
      .getOrCreate()

    spark.sparkContext.setLogLevel("ERROR")
    import spark.implicits._

    try {

      // chargement du dataset Amazon Review Polarity
      // format: sans header, 3 colonnes (polarity, title, text)
      val dataPath = if (args.nonEmpty) args(0) else "train.csv"

      val schema = StructType(Seq(
        StructField("polarity", IntegerType),
        StructField("title",    StringType),
        StructField("text",     StringType)
      ))

      val raw = spark.read
        .schema(schema)
        .option("header", "false")
        .csv(dataPath)

      // polarity 2 = positif (1), polarity 1 = negatif (0)
      val df = raw
        .withColumn("texte",    concat_ws(" ", col("title"), col("text")))
        .withColumn("sentiment", when(col("polarity") === 2, 1.0).otherwise(0.0))
        .select("texte", "sentiment")
        .na.drop()

      // echantillon equilibre 5000 par classe
      val balanced = df.filter(col("sentiment") === 1.0).limit(5000)
        .union(df.filter(col("sentiment") === 0.0).limit(5000))
        .cache()

      val total = balanced.count()
      val nPos  = balanced.filter(col("sentiment") === 1.0).count()
      val nNeg  = balanced.filter(col("sentiment") === 0.0).count()
      println(s"dataset: $total avis ($nPos positifs, $nNeg negatifs)")
      balanced.show(5, truncate = 70)

      // split train/test 80-20
      val Array(train, test) = balanced.randomSplit(Array(0.8, 0.2), seed = 42)
      val nTest = test.count()
      println(s"train=${train.count()}  test=$nTest")

      // preprocessing NLP
      val normUdf = udf((t: String) => normalize(t))
      val trainPrep = train.withColumn("stemmed", normUdf(col("texte")))
      val testPrep  = test.withColumn("stemmed",  normUdf(col("texte")))

      val tokenizer = new Tokenizer()
        .setInputCol("stemmed").setOutputCol("words")

      val stopwords = new StopWordsRemover()
        .setInputCols(Array("words")).setOutputCol("filtered")
        .setStopWords(
          StopWordsRemover.loadDefaultStopWords("english") ++
          StopWordsRemover.loadDefaultStopWords("french")
        )

      val hashTF = new HashingTF()
        .setInputCol("filtered").setOutputCol("raw_features").setNumFeatures(256)

      val idf = new IDF()
        .setInputCol("raw_features").setOutputCol("features")

      val w2v = new Word2Vec()
        .setInputCol("filtered").setOutputCol("features")
        .setVectorSize(100).setMinCount(1).setSeed(42)

      // entrainement TF-IDF + 3 classifieurs
      val classifiers = Seq(
        ("LR",  new LogisticRegression().setMaxIter(100).setRegParam(0.01)
                    .setLabelCol("sentiment").setFeaturesCol("features")),
        ("NB",  new NaiveBayes().setSmoothing(1.0)
                    .setLabelCol("sentiment").setFeaturesCol("features")),
        ("SVM", new LinearSVC().setMaxIter(100).setRegParam(0.01)
                    .setLabelCol("sentiment").setFeaturesCol("features"))
      )

      val tfidfModels = classifiers.map { case (name, clf) =>
        val pipeline = new Pipeline().setStages(Array(tokenizer, stopwords, hashTF, idf, clf))
        println(s"entrainement TF-IDF + $name...")
        val model = pipeline.fit(trainPrep)
        (name, model)
      }

      val tfidfResults = tfidfModels.map { case (name, model) =>
        val p = model.transform(testPrep)
        val (m, cm) = computeMetrics(s"TF-IDF + $name", p, nTest)
        println(s"TF-IDF + $name: F1=${r4(m.f1)} Acc=${r4(m.accuracy)} Prec=${r4(m.precision)} Rec=${r4(m.recall)}")
        (m, cm)
      }

      // Word2Vec + LR
      val lrW2V = new LogisticRegression().setMaxIter(100).setRegParam(0.01)
        .setLabelCol("sentiment").setFeaturesCol("features")
      val w2vModel = new Pipeline().setStages(Array(tokenizer, stopwords, w2v, lrW2V)).fit(trainPrep)
      val (w2vMetrics, w2vCM) = computeMetrics("Word2Vec + LR", w2vModel.transform(testPrep), nTest)
      println(s"Word2Vec + LR: F1=${r4(w2vMetrics.f1)} Acc=${r4(w2vMetrics.accuracy)}")

      // validation croisee 5-fold
      val evaluator = new BinaryClassificationEvaluator()
        .setLabelCol("sentiment").setMetricName("areaUnderROC")

      val lrCV = new LogisticRegression().setLabelCol("sentiment").setFeaturesCol("features")
      val lrGrid = new ParamGridBuilder()
        .addGrid(lrCV.maxIter, Array(50, 100))
        .addGrid(lrCV.regParam, Array(0.01, 0.1))
        .build()
      val lrCVModel = new CrossValidator()
        .setEstimator(new Pipeline().setStages(Array(tokenizer, stopwords, hashTF, idf, lrCV)))
        .setEvaluator(evaluator).setEstimatorParamMaps(lrGrid).setNumFolds(5).setSeed(42)
        .fit(trainPrep)
      val lrBestAUC = lrCVModel.avgMetrics.max
      val lrBestIdx = lrCVModel.avgMetrics.indexOf(lrBestAUC)
      println(s"CV LR meilleur AUC-ROC: ${r4(lrBestAUC)}")

      val nbCV = new NaiveBayes().setLabelCol("sentiment").setFeaturesCol("features")
      val nbGrid = new ParamGridBuilder()
        .addGrid(nbCV.smoothing, Array(0.5, 1.0, 2.0))
        .build()
      val nbCVModel = new CrossValidator()
        .setEstimator(new Pipeline().setStages(Array(tokenizer, stopwords, hashTF, idf, nbCV)))
        .setEvaluator(evaluator).setEstimatorParamMaps(nbGrid).setNumFolds(5).setSeed(42)
        .fit(trainPrep)
      val nbBestAUC = nbCVModel.avgMetrics.max
      val nbBestIdx = nbCVModel.avgMetrics.indexOf(nbBestAUC)
      println(s"CV NB meilleur AUC-ROC: ${r4(nbBestAUC)}")

      val cvResults = Seq(
        CVResult("TF-IDF + LR (CV)", lrBestAUC,
          s"maxIter=${lrGrid(lrBestIdx)(lrCV.maxIter)}, regParam=${lrGrid(lrBestIdx)(lrCV.regParam)}"),
        CVResult("TF-IDF + NB (CV)", nbBestAUC,
          s"smoothing=${nbGrid(nbBestIdx)(nbCV.smoothing)}")
      )

      // generation resultats.json
      val allMetrics = tfidfResults.map(_._1) :+ w2vMetrics
      val allCMs     = tfidfResults.map(_._2) :+ w2vCM

      val samplePreds = tfidfModels.head._2
        .transform(testPrep.limit(5))
        .select("texte", "sentiment", "prediction")
        .collect()
        .map(r => Map("texte" -> r.getString(0), "reel" -> r.getDouble(1).toInt, "predit" -> r.getDouble(2).toInt))

      val json =
        s"""{
  "dataset": { "total": ${total.toInt}, "positifs": ${nPos.toInt}, "negatifs": ${nNeg.toInt},
    "train": ${train.count().toInt}, "test": ${nTest.toInt} },
  "modeles": [
${allMetrics.map(m => s"""    {"nom":"${m.nom}","f1":${r4(m.f1)},"accuracy":${r4(m.accuracy)},"precision":${r4(m.precision)},"recall":${r4(m.recall)}}""").mkString(",\n")}
  ],
  "crossValidation": { "folds": 5, "metric": "AUC-ROC", "models": [
${cvResults.map(c => s"""    {"nom":"${c.nom}","bestAUC":${r4(c.auc)},"bestParams":"${c.params}"}""").mkString(",\n")}
  ]},
  "confusionMatrices": [
${allCMs.map(c => s"""    {"modelName":"${c.modelName}","tp":${c.tp},"tn":${c.tn},"fp":${c.fp},"fn":${c.fn}}""").mkString(",\n")}
  ],
  "predictions": [
${samplePreds.map(p => s"""    {"texte":"${p("texte").toString.replace("\"","\\\"").take(80)}","reel":${p("reel")},"predit":${p("predit")}}""").mkString(",\n")}
  ]
}"""

      val w = new PrintWriter(new File("resultats.json"))
      w.write(json); w.close()
      println("resultats.json sauvegarde")

      println("\n--- resultats ---")
      allMetrics.foreach(m =>
        println(f"${m.nom}%-30s F1=${m.f1}%.4f  Acc=${m.accuracy}%.4f  Prec=${m.precision}%.4f  Rec=${m.recall}%.4f"))

    } finally {
      spark.stop()
    }
  }
}