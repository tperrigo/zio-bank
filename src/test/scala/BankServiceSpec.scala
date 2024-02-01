package zio.bank

import zio._
import zio.test._
import zio.test.Assertion._
import java.util.UUID

object BankServiceSpec extends ZIOSpecDefault {
  def spec = suite("BankService Tests")(
    test("create transactions and verify account balances") {
      for {
        // Initialize the in-memory data store with sample accounts
        accountsRef <- Ref.make(Map(
          "acc1" -> Account("acc1", "user1"),
          "acc2" -> Account("acc2", "user2")
        ))
        transactionsRef <- Ref.make(Map.empty[String, List[Transaction]])
        dataStore = InMemoryDataStore(accountsRef, transactionsRef)
        bankService = BankServiceImpl(dataStore)

        // Create transactions and store their IDs
        transactionIds <- ZIO.collectAll(Seq(
          bankService.createTransaction(TransactionRequest("acc1", 100, "Initial balance")),
          bankService.createTransaction(TransactionRequest("acc2", 50, "Initial balance")),
          bankService.createTransaction(TransactionRequest("acc1", 50, "Additional deposit")),
          bankService.createTransaction(TransactionRequest("acc1", -30, "Withdrawal"))
        )).map(_.flatten.map(_.transactionId))

        // Complete all transactions
        _ <- ZIO.foreach(transactionIds)(bankService.completeTransaction)

        // Retrieve account details to verify balances
        acc1Details <- bankService.getAccountDetails("acc1")
        acc2Details <- bankService.getAccountDetails("acc2")
        nonExistingAccDetails <- bankService.getAccountDetails("nonExistingAccount")
      } yield assert(acc1Details.map(_._2))(isSome(equalTo(BigDecimal(120)))) && // Expect acc1 balance to be 120 after completion
              assert(acc2Details.map(_._2))(isSome(equalTo(BigDecimal(50)))) && // Expect acc2 balance to be 50 after completion
              assert(nonExistingAccDetails)(isNone) // No details for non-existing account
    }
  )
}

