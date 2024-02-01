package zio.bank

import zio._
import zio.http._
import zio.json._

object JsonCodecs {
  implicit val accountEncoder: JsonEncoder[Account] = DeriveJsonEncoder.gen[Account]
  implicit val transactionStateEncoder: JsonEncoder[TransactionState] = DeriveJsonEncoder.gen[TransactionState]
  implicit val transactionEventEncoder: JsonEncoder[TransactionEvent] = DeriveJsonEncoder.gen[TransactionEvent]
  implicit val transactionEncoder: JsonEncoder[Transaction] = DeriveJsonEncoder.gen[Transaction]
  implicit val transactionRequestDecoder: JsonDecoder[TransactionRequest] = DeriveJsonDecoder.gen[TransactionRequest]
}

object BankHttpApp {

  def apply(bankService: BankService): Http[Any, Nothing, Request, Response] = {
    import JsonCodecs._

    Http.collectZIO[Request] {
      case Method.GET -> Root / "account" / accountId =>
        bankService.getAccountDetails(accountId).map {
          case Some((account, balance)) => Response.json(account.toJson ++ s""", "balance": $balance""")
          case None => Response.status(Status.NotFound)
        }

      case Method.GET -> Root / "transaction" / "history" / accountId =>
        bankService.getTransactions(accountId).map { transactions =>
          Response.json(transactions.toJson)
        }
    }
  }
}
