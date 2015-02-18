/**
 * Track the trackers
 * Copyright (C) 2014  Sebastian Schelter
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.ssc.trackthetrackers.analysis.statistics

import io.ssc.trackthetrackers.analysis.{Edge, FlinkUtils, GraphUtils}
import org.apache.flink.api.common.functions.RichMapFunction
import org.apache.flink.api.scala.ExecutionEnvironment
import org.apache.flink.api.scala._
import org.apache.flink.core.fs.FileSystem.WriteMode

object CompanyDistribution extends App {

  computeDistribution("/home/ssc/Desktop/trackthetrackers/sample2000.tsv",
    "/home/ssc/Entwicklung/datasets/webdatacommons-hyperlink2012-payleveldomain/pld-index",
    "/home/ssc/Desktop/trackthetrackers/out/companyDistribution/")

  def computeDistribution(trackingGraphFile: String, domainIndexFile: String, outputPath: String) = {

    implicit val env = ExecutionEnvironment.getExecutionEnvironment

    val edges = GraphUtils.readEdges(trackingGraphFile)
    val domains = GraphUtils.readVertices(domainIndexFile)

    val domainsByCompany = Map(

      //addthis.com
      1136762 -> "AddThis",

      //amazon.com
      2150098 -> "Amazon",
      //images-amazon.com
      18691888 -> "Amazon",

      //casalemedia.com
      6971664 -> "CasaleMedia",

      //facebook.net
      13237946 -> "Facebook",
      //fbcdn.net
      13481035 -> "Facebook",
      //facebook.com
      13237914 -> "Facebook",

      //google.com
      15964788 -> "Google",
      //doubleclick.net
      11142763 -> "Google",
      //googlesyndication.com
      15967902 -> "Google",
      //youtube.com
      42467638 -> "Google",
      //google-analytics.com
      15964105 -> "Google",
      //googleadservices.com
      15965227 -> "Google",
      //feedburner.com
      13536774 -> "Google",
      //recaptcha.net
      31564322 -> "Google",

      //photobucket.com
      29569518 -> "PhotoBucket",

      //statcounter.com
      35757837 -> "StatCounter",

      //twitter.com
      39224483 -> "Twitter",
      //twimg.com
      39210295 -> "Twitter",

      //yahooapis.com
      42207014 -> "Yahoo",
      //yahoo.com
      42206842 -> "Yahoo",
      //yahoo.net
      42206882 -> "Yahoo",
      //yimg.com
      42318764 -> "Yahoo",
      //tumblr.com
      39095913 -> "Yahoo",
      //flickr.com
      14050903 -> "Yahoo",
      //geocities.com
      15358455 -> "Yahoo"
    )

    val numTrackedHosts = edges.distinct("target").map { _ => Tuple1(1L) }.sum(0)

    val companyEdges = edges.filter { edge => domainsByCompany.contains(edge.src.toInt) }
                            .map { edge => domainsByCompany(edge.src.toInt) -> edge.target }
                            .distinct

    val companyCounts = FlinkUtils.countByStrKey(companyEdges, { t: (String, Long) => t._1 })

    val companyProbabilities = companyCounts.map(new CompanyProbability())
                                            .withBroadcastSet(numTrackedHosts, "numTrackedHosts")

    companyProbabilities.writeAsCsv(outputPath, fieldDelimiter = "\t", writeMode = WriteMode.OVERWRITE)
    env.execute()
  }

  class CompanyProbability() extends RichMapFunction[(String, Long), (String, Double)] {

    override def map(companyWithCount: (String, Long)): (String, Double) = {

      val numTrackedHosts = getRuntimeContext.getBroadcastVariable[Tuple1[Long]]("numTrackedHosts").get(0)._1

      companyWithCount._1 -> companyWithCount._2.toDouble / numTrackedHosts
    }
  }

}