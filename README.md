# zio-bank

Demo Project for [DirectBooks)(https://www.directbooks.com/).

The `model.scala` class contains the domain entities for the Banking application. The core logic is implemented in the `BankingService.scala` file,
and is modelled as a ZIO Service (using the ZIO Service Pattern 2.0). Banking transactions are stored in an append-only log; the current state for a
given Account is calculated on-demand by folding over its transaction history. (This is not as efficient as simply mutating data, but allows us to maintain the entire
domain history, and replay events to any point-in-time).
