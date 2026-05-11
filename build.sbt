name := "sentiment-analysis"
version := "1.0"
scalaVersion := "2.13.17"

val sparkVersion = "4.1.1"

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core"  % sparkVersion % "provided",
  "org.apache.spark" %% "spark-sql"   % sparkVersion % "provided",
  "org.apache.spark" %% "spark-mllib" % sparkVersion % "provided"
)