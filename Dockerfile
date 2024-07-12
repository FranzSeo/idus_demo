# Dockerfile
FROM openjdk:8
RUN apt-get update && apt-get install -y wget gnupg2 curl
RUN wget https://archive.apache.org/dist/spark/spark-3.1.2/spark-3.1.2-bin-hadoop3.2.tgz \
    && tar -xzf spark-3.1.2-bin-hadoop3.2.tgz \
    && mv spark-3.1.2-bin-hadoop3.2 /opt/spark \
    && rm spark-3.1.2-bin-hadoop3.2.tgz

# Set environment variables
ENV SPARK_HOME=/opt/spark
ENV PATH=$SPARK_HOME/bin:$PATH

# Install Hive
RUN wget https://archive.apache.org/dist/hive/hive-2.3.9/apache-hive-2.3.9-bin.tar.gz \
    && tar -xzf apache-hive-2.3.9-bin.tar.gz \
    && mv apache-hive-2.3.9-bin /opt/hive \
    && rm apache-hive-2.3.9-bin.tar.gz

# Set environment variables for Hive
ENV HIVE_HOME=/opt/hive
ENV PATH=$HIVE_HOME/bin:$PATH

# Create directories for data and logs
RUN mkdir -p /data/input /data/output /data/logs

# Set working directory
WORKDIR /app

# Copy the application JAR file
COPY target/spark-app-1.0-SNAPSHOT.jar /app/spark-app.jar

CMD ["spark-submit", "--class", "com.idus.SparkMain", "/app/spark-app.jar"]
