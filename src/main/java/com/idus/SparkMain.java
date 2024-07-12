// src/main/java/com/idus/SparkMain.java
package com.idus;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.SaveMode;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SparkMain {
    public static void main(String[] args) {
        SparkSession spark = SparkSession.builder()
                .appName("IdusUserActivityLogProcessor")
                .enableHiveSupport()
                .getOrCreate();

        System.out.println("Spark session created");

        String dataPath = "/data/input";  // 데이터를 저장하는 경로
        String outputPath = "/data/output";  // 출력 데이터를 저장하는 경로

        // 디렉토리 내의 모든 파일을 읽어들입니다.
        List<String> inputFiles;
        try (Stream<Path> paths = Files.walk(Paths.get(dataPath))) {
            inputFiles = paths.filter(Files::isRegularFile)
                    .map(Path::toString)
                    .filter(file -> file.endsWith(".csv"))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        System.out.println("Input files: " + inputFiles);

        for (String filePath : inputFiles) {
            System.out.println("Processing file: " + filePath);
            Dataset<Row> rowData = spark.read().option("header", "true").csv(filePath);

            // 이벤트 시간 필드를 기준으로 파티션을 생성
            rowData = rowData.withColumn("event_date", rowData.col("event_time").substr(1, 10))
                    .withColumn("event_time", rowData.col("event_time").cast("timestamp"));

            String[] parts = filePath.split("/");
            String fileName = parts[parts.length - 1];
            String partitionedOutputPath = outputPath + "/year=" + fileName.substring(0, 4)
                    + "/month=" + fileName.substring(5, 7);

            rowData.write().mode(SaveMode.Overwrite).partitionBy("event_date")
                    .parquet(partitionedOutputPath);

            System.out.println("Written data to: " + partitionedOutputPath);
        }

        // Hive External Table 생성
        spark.sql("CREATE EXTERNAL TABLE IF NOT EXISTS user_activity_logs ("
                + "event_time TIMESTAMP, event_type STRING, product_id STRING, "
                + "category_id STRING, category_code STRING, brand STRING, "
                + "price DOUBLE, user_id STRING, user_session STRING) "
                + "PARTITIONED BY (event_date STRING) "
                + "STORED AS PARQUET LOCATION '" + outputPath + "'");

        System.out.println("Hive external table created");

        spark.stop();
        System.out.println("Spark session stopped");
    }
}
