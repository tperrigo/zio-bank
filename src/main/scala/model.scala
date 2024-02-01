package zio.bank

case class Account(id: String, userId: String)

case class TransactionRequest(accountId: String, amount: BigDecimal, description: String)

case class Transaction(transactionId: String, accountId: String, amount: BigDecimal, description: String, events: List[TransactionEvent])

case class TransactionEvent(transactionId: String, state: TransactionState, timestamp: Long)

sealed abstract class TransactionState(state: String)

object TransactionState {
  case object PENDING extends TransactionState("pending")
  case object COMPLETED extends TransactionState("completed")
  case class FAILED(reason: String) extends TransactionState("failed")
}


