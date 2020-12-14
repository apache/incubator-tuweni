# Ethereum Faucet

This example allows you to set up a faucet with Github authentication.

The application is written in Kotlin with Spring Boot, with Spring Web, Spring Security and Thymeleaf templates.

The app is configured with the values in src/main/resources/application.yml.

# Faucet

This web application creates an account on chain and allows folks to request money from it.

The faucet will top up their accounts up to to max balance that is permitted.

## Running locally

Start the faucet with the main method in `FaucetApplication`.

When the system starts, enter the password for your wallet. If it is the first time the application runs, the wallet is created.

Navigate to localhost:8080 and sign in using github.

You will then be greeted to a page where you can ask for funds.

In parallel, start Hyperledger Besu:

`$> besu --network=dev --rpc-http-enabled --host-allowlist=* --rpc-http-cors-origins=* --miner-enabled --miner-coinbase 0xfe3b557e8fb62b89f4916b721be55ceb828dbd73`

This allows to run Besu with just one node.

In the web page, note the faucet account address. Make sure to send money to that faucet account (you can use Metamask for this, and the dev network private keys are documented).

Now you can send money using the faucet. Enter any valid address and press OK.

The second time you ask for money, the faucet will detect the balance of the account matches the max the faucet with top up.

