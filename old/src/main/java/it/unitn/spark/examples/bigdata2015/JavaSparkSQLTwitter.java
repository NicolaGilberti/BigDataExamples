package it.unitn.spark.examples.bigdata2015;

import java.util.ArrayList;
import java.util.List;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.sql.SQLContext;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.api.java.UDF1;
import org.apache.spark.sql.types.DataTypes;

/**
 * Load some tweets stored as JSON data and explore them.
 */
public class JavaSparkSQLTwitter {

	@SuppressWarnings({"deprecation", "serial"})
	public static void main(String[] args) {
		String inputFile = args[0];
		SparkConf conf = new SparkConf().setAppName("JavaSparkSQLTwitter").setMaster("local[2]");
		JavaSparkContext sc = new JavaSparkContext(conf);
		SQLContext sqlCtx = new SQLContext(sc);
		Dataset<Row> input = sqlCtx.jsonFile(inputFile);
		// Print the schema
		input.printSchema();
		// Register the input schema RDD
		input.registerTempTable("tweets");
		// Select tweets based on the retweetCount
		Dataset<Row> topTweets = sqlCtx.sql("SELECT text, retweetCount FROM tweets ORDER BY retweetCount LIMIT 10");
		List<Row> result = topTweets.collectAsList();
		for (Row row : result) {
			System.out.println("Text: " + row.get(0));
			System.out.println("Retweetcount: " + row.get(1));
		}
		JavaRDD<String> topTweetText = topTweets.toJavaRDD().map(new Function<Row, String>() {
			public String call(Row row) {
				return row.getString(0);
			}
		});
		System.out.println(topTweetText.collect());
		// Create a person and turn it into a Schema RDD
		ArrayList<HappyPerson> peopleList = new ArrayList<HappyPerson>();
		peopleList.add(new HappyPerson("holden", "coffee"));
		JavaRDD<HappyPerson> happyPeopleRDD = sc.parallelize(peopleList);
		Dataset<Row> happyPeopleSchemaRDD = sqlCtx.applySchema(happyPeopleRDD, HappyPerson.class);
		happyPeopleSchemaRDD.registerTempTable("happy_people");
		sqlCtx.udf().register("stringLengthJava", new UDF1<String, Integer>() {
			@Override
			public Integer call(String str) throws Exception {
				return str.length();
			}
		}, DataTypes.IntegerType);
		Dataset<Row> tweetLength = sqlCtx.sql("SELECT stringLengthJava(text) FROM tweets LIMIT 10");
		List<Row> lengths = tweetLength.collectAsList();
		for (Row row : lengths) {
			System.out.println("CIAO: " + row.get(0));
		}
		sc.stop();
	}
}