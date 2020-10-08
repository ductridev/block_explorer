package org.constellation.blockexplorer.schema

case class Balances(
  height: Long,
  hash: String,
  balances: Map[String, AddressBalance]
)

case class AddressBalance(balance: Long, rewardsBalance: Long)
