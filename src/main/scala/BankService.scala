package zio.bank

import java.util.UUID
import zio._
import java.util.concurrent.TimeUnit

trait BankService {
  def getAccountDetails(accountId: String): ZIO[Any, Nothing, Option[(Account, BigDecimal)]]

  def getAllAccounts: ZIO[Any, Nothing, List[(Account, BigDecimal)]]

  def createTransaction(transactionRequest: TransactionRequest): ZIO[Any, Nothing, Option[Transaction]]

  def getTransactions(accountId: String): ZIO[Any, Nothing, List[Transaction]]

  def completeTransaction(transactionId: String): ZIO[Any, Nothing, Option[Transaction]]
}

trait BankDataStore {
  def getAccount(accountId: String): ZIO[Any, Nothing, Option[Account]]
  def getAllAccounts: ZIO[Any, Nothing, List[Account]]
  def createTransaction(transaction: Transaction): ZIO[Any, Nothing, Transaction]
  def getTransactions(accountId: String): ZIO[Any, Nothing, List[Transaction]]
  def getTransaction(transactionId: String): ZIO[Any, Nothing, Option[Transaction]]
  def updateTransaction(updatedTransaction: Transaction): ZIO[Any, Nothing, Unit]
}

case class InMemoryDataStore(accounts: Ref[Map[String, Account]], transactions: Ref[Map[String, List[Transaction]]]) extends BankDataStore {

  override def getAccount(accountId: String): ZIO[Any, Nothing, Option[Account]] =
    accounts.get.map(_.get(accountId))

  override def getAllAccounts: ZIO[Any, Nothing, List[Account]] =
    accounts.get.map(_.values.toList)

  override def createTransaction(transaction: Transaction): ZIO[Any, Nothing, Transaction] = for {
    _ <- transactions.update { currentTransactions =>
      val updatedTransactions = currentTransactions.getOrElse(transaction.accountId, List.empty) :+ transaction
      currentTransactions.updated(transaction.accountId, updatedTransactions)
    }
  } yield transaction

  override def getTransactions(accountId: String): ZIO[Any, Nothing, List[Transaction]] =
    transactions.get.map(_.getOrElse(accountId, List.empty))

  override def getTransaction(transactionId: String): ZIO[Any, Nothing, Option[Transaction]] =
    transactions.get.map(_.values.flatten.find(_.transactionId == transactionId))

  override def updateTransaction(updatedTransaction: Transaction): ZIO[Any, Nothing, Unit] = for {
    _ <- transactions.update { currentTransactions =>
      val accountTransactions = currentTransactions.getOrElse(updatedTransaction.accountId, List.empty)
      val updatedTransactions = accountTransactions.map { t =>
        if (t.transactionId == updatedTransaction.transactionId) updatedTransaction else t
      }
      currentTransactions.updated(updatedTransaction.accountId, updatedTransactions)
    }
  } yield ()
}

case class BankServiceImpl(dataStore: BankDataStore) extends BankService {
  override def getAccountDetails(accountId: String): ZIO[Any, Nothing, Option[(Account, BigDecimal)]] = {
    for {
      accountOpt <- dataStore.getAccount(accountId)
      balance <- getCurrentBalance(accountId)
    } yield accountOpt.map(account => (account, balance))
  }

  override def getAllAccounts: ZIO[Any, Nothing, List[(Account, BigDecimal)]] = {
    for {
      accounts <- dataStore.getAllAccounts
      accountsWithBalances <- ZIO.collectAll(
                                accounts.map(account =>
                                  getCurrentBalance(account.id).map(balance => (account, balance))
                                )
                              )
    } yield accountsWithBalances
  }

  override def getTransactions(accountId: String): ZIO[Any, Nothing, List[Transaction]] = {
    dataStore.getTransactions(accountId)
  }

  override def createTransaction(transactionRequest: TransactionRequest): ZIO[Any, Nothing, Option[Transaction]] = {
    for {
      accountOpt <- dataStore.getAccount(transactionRequest.accountId)
      timestamp <- Clock.currentTime(TimeUnit.MILLISECONDS)
      result <- accountOpt match {
        case Some(_) =>
          val transactionId = UUID.randomUUID().toString
          val newTransactionEvent = TransactionEvent(transactionId, TransactionState.PENDING, timestamp)
          val newTransaction = Transaction(
            transactionId,
            transactionRequest.accountId,
            transactionRequest.amount,
            transactionRequest.description,
            List(newTransactionEvent)
          )
          dataStore.createTransaction(newTransaction).as(Some(newTransaction))

        case None => ZIO.succeed(None)
      }
    } yield result
  }

  def getCurrentBalance(accountId: String): ZIO[Any, Nothing, BigDecimal] = {
    for {
      transactions <- dataStore.getTransactions(accountId)
      completedTransactions = transactions.flatMap { transaction =>
        transaction.events
          .filter(_.state == TransactionState.COMPLETED)
          .maxByOption(_.timestamp)
          .map(_ => transaction) // Map to transaction if there is a completed event
      }
      // Sort transactions by the timestamp of their latest completed event
      sortedTransactions = completedTransactions.sortBy(_.events.maxBy(_.timestamp).timestamp)
    } yield sortedTransactions.map(_.amount).sum
  }

  override def completeTransaction(transactionId: String): ZIO[Any, Nothing, Option[Transaction]] = for {
    timestamp <- Clock.currentTime(TimeUnit.MILLISECONDS)
    transactionOpt <- dataStore.getTransaction(transactionId) // Retrieve the transaction
    updatedTransactionOpt <- transactionOpt match {
      case Some(transaction) =>
        val completedEvent = TransactionEvent(transactionId, TransactionState.COMPLETED, timestamp)
        val updatedTransaction = transaction.copy(events = transaction.events :+ completedEvent)
        for {
          _ <- dataStore.updateTransaction(updatedTransaction) // Update the transaction with the new event
        } yield Some(updatedTransaction)
      case None => ZIO.succeed(None) // Transaction not found
    }
  } yield updatedTransactionOpt
}

object BankServiceImpl {
  val live: ZLayer[BankDataStore, Nothing, BankService] = ZLayer.fromFunction { dataStore: BankDataStore =>
    BankServiceImpl(dataStore)
  }

  val test: ZLayer[Any, Nothing, BankService] = ZLayer {
    for {
      accountsRef <- Ref.make(Map.empty[String, Account])
      transactionsRef <- Ref.make(Map.empty[String, List[Transaction]])
      dataStore = InMemoryDataStore(accountsRef, transactionsRef)
    } yield BankServiceImpl(dataStore)
  }
}
