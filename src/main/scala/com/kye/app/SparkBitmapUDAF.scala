package com.kye.app

import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession

/**
 * Author: Leslie
 * Date: 2024/5/31 17:15
 * Describe:
 */
object SparkBitmapUDAF {
  def main(args: Array[String]): Unit = {
    val ss: SparkSession = SparkSession.builder().appName("SparkSql").master("local[*]").getOrCreate()
    val rdd: RDD[String] = ss.sparkContext.textFile("C:\\Users\\712972\\Desktop\\note\\spark\\sparkCode\\sparkSql\\src\\main\\resources\\t_account.csv")
    rdd.foreach(print(_))


    //    val rowRdd: RDD[Row] = rdd.map(x => {
    //      val words: Array[String] = x.split(",")
    //      val name: String = words(0).toString
    //      val sex: String = words(1).toString
    //      val age: Int = words(2).toInt
    //      Row(name, sex, age)
    //    })
    //    // rdd转df最正宗的写法，定义StructType字段名称和类型，能不能为空,顺序和row顺序一致
    //    val schema = types.StructType.apply(
    //      List(
    //        StructField("name", DataTypes.StringType, false),
    //        StructField("sex", DataTypes.StringType, false),
    //        StructField("age", DataTypes.IntegerType, false)
    //      )
    //    )
    //    val df: DataFrame = ss.createDataFrame(rowRdd, schema)
    //    df.show()

    ss.stop()

  }

}
