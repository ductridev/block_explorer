package org.constellation.blockexplorer.api.controller

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.sksamuel.elastic4s.{RequestFailure, RequestSuccess}
import org.constellation.blockexplorer.api.ResponseCreator
import org.constellation.blockexplorer.api.mapper.{JsonEncoder, JsonExtractor}
import org.constellation.blockexplorer.api.output.ElasticSearchService
import org.constellation.blockexplorer.schema.Balances

class BalancesController(
  elasticSearchService: ElasticSearchService,
  jsonEncoder: JsonEncoder,
  jsonExtractor: JsonExtractor
) {

  def findBy(hash: String): APIGatewayProxyResponseEvent = {
    def extractBalancesFrom(body: Option[String]): Seq[Balances] =
      body
        .flatMap(response => jsonExtractor.extractBalancesEsResult(response))
        .getOrElse(Seq.empty)

    val balances = if (hash == "latest") {
      elasticSearchService.findBalancesForLatestSnapshot()
    } else if (hash.forall(_.isDigit) && hash.length < 64) {
      elasticSearchService.findBalancesForSnapshotHeight(hash.toLong)
    } else {
      elasticSearchService.findBalancesForSnapshotHash(hash)
    }

    balances match {
      case RequestFailure(status, body, headers, error) =>
        ResponseCreator.errorResponse("ElasticSearch service error", 500)
      case RequestSuccess(status, body, headers, result) =>
        extractBalancesFrom(body) match {
          case Nil => ResponseCreator.errorResponse("Cannot find balances", 404)
          case x   => ResponseCreator.successResponse(jsonEncoder.balancesEncoder(x.head).toString())
        }
    }
  }

}
