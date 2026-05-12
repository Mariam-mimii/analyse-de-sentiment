name := "SentimentAnalysis"
version := "1.0"
scalaVersion := "2.13.17"

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % "4.1.1",
  "org.apache.spark" %% "spark-sql" % "4.1.1",
  "org.apache.spark" %% "spark-mllib" % "4.1.1",
  "com.github.scopt" %% "scopt" % "4.1.0"
)
