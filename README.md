# zio-bank

Demo Project for [DirectBooks)(https://www.directbooks.com/).

The `model.scala` class contains the domain entities for the Banking application. The core logic is implemented in the `BankService.scala` file,
and is modelled as a ZIO Service (using the ZIO Service Pattern 2.0). Banking transactions are stored in an append-only log; the current state for a
given Account is calculated on-demand by folding over its transaction history. (This is not as efficient as simply mutating data, but allows us to maintain the entire
domain history, and replay events to any point-in-time).

The web service functionality is not complete, but has been started in the `feature/web-service` branch, in the `BankApp.scala` file. It utilizes `zio-http` to model the endpoints,
but is missing the `POST` endpoint for adding transactions. This functionality has not been tested (I stubbed it out this morning before work, and intended to work on it tonight, but
it was suggested that I send the assessment in its current state).

